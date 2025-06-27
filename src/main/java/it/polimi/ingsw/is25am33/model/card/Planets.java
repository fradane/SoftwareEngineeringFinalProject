package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CubesRedistributionHandler;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Planets extends AdventureCard implements PlayerMover, CubesRedistributionHandler {
    private List<Planet> availablePlanets;
    private final Map<String, Planet> playerPlanet = new ConcurrentHashMap<>();
    private int stepsBack;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_PLANET, CardState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets() {
        this.cardName = this.getClass().getSimpleName();
    }

    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public Map<String, Planet> getPlayerPlanet() {
        return playerPlanet;
    }

    public void setCurrPlanet(Planet planet) {
        this.currentPlanet= planet;
    }

    @JsonIgnore
    public Planet getCurrentPlanet() {
        return currentPlanet;
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {
        //TODO aggiustare Planets con Socket. Attulmente non funziona, ricontrollare dopo merge con Luca

        switch (currState) {
            case CHOOSE_PLANET:
                try {
                    this.currPlayerWantsToVisit(playerChoices.getChosenPlanetIndex());
                } catch (IllegalIndexException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLE_CUBES_REWARD:

                this.handleStorageUpdates(playerChoices.getStorageUpdates().orElseThrow());

                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @JsonIgnore
    public List<CargoCube> getPlayerReward(String playerNickname) {
        return playerPlanet.get(playerNickname).getReward();
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    private void currPlayerWantsToVisit (int chosenPlanetIndex) throws IllegalIndexException, IndexOutOfBoundsException {

        if (chosenPlanetIndex != 0) {

            currentPlanet = availablePlanets.get(chosenPlanetIndex - 1);

            if (currentPlanet.isBusy())
                throw new IllegalIndexException("Planet has already been chosen");

            playerPlanet.put(gameModel.getCurrPlayer().getNickname(), currentPlanet);
            //availablePlanets.remove(chosenPlanetIndex-1);
            availablePlanets.get(chosenPlanetIndex - 1).setBusy(true);

            currentPlanet.setNoMoreAvailable();

            ClientCard clientCard = this.toClientCard();

            String currPlayerNickname = gameModel.getCurrPlayer().getNickname();

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerVisitedPlanet(nicknameToNotify, currPlayerNickname, clientCard);
            });

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCurrAdventureCardUpdate(nicknameToNotify, clientCard);
            });

            setCurrState(CardState.HANDLE_CUBES_REWARD);

        } else if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

    /**
     * Gestisce gli aggiornamenti degli storage tramite la nuova struttura dati.
     * 
     * @param storageUpdates mappa degli aggiornamenti degli storage
     */
    private void handleStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
        try {
            validateStorageUpdates(storageUpdates, gameModel);
            applyStorageUpdates(storageUpdates, gameModel);
            
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });
            
            // Muovi il giocatore indietro
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
            
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyRankingUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
            });
            
            proceedToNextPlayerOrEndCard();
            
        } catch (IllegalArgumentException e) {
            // Gestione errore con retry
            String currentPlayer = gameModel.getCurrPlayer().getNickname();
            gameModel.getGameClientNotifier().notifyClients(
                Set.of(currentPlayer),
                (nickname, clientController) -> {
                    clientController.notifyStorageError(nickname, e.getMessage());
                }
            );
            
            // Ripristina stato shipboard
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });
            
            // Rimani in HANDLE_CUBES_REWARD per il retry
        }
    }

    /**
     * Helper method per procedere al prossimo giocatore o terminare la carta
     */
    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    public ClientCard toClientCard() {
        return new ClientPlanets(cardName, imageName, availablePlanets, playerPlanet, stepsBack);
    }

}