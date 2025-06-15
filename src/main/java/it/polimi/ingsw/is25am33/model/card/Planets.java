package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
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
import java.util.concurrent.ConcurrentHashMap;

public class Planets extends AdventureCard implements PlayerMover {
    private List<Planet> availablePlanets;
    private Map<String, Planet> playerPlanet = new ConcurrentHashMap<>();
    private int stepsBack;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_PLANET, CardState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets(List<Planet> availablePlanets, int stepsBack) {
        this.availablePlanets = availablePlanets;
        this.stepsBack = stepsBack;
    }

    public Planets() {
        this.cardName = this.getClass().getSimpleName();
    }

    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    public int getStepsBack() {
        return stepsBack;
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

        switch (currState) {
            case CHOOSE_PLANET:
                try {
                    this.currPlayerWantsToVisit(playerChoices.getChosenPlanetIndex());
                } catch (IllegalIndexException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLE_CUBES_REWARD:
                this.currPlayerChoseCargoCubeStorage(playerChoices.getChosenStorage().orElseThrow());
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
            availablePlanets.remove(chosenPlanetIndex-1);

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
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    }

    private void currPlayerChoseCargoCubeStorage(List<Coordinates> chosenStorageCoords) {
        List<CargoCube> planetRewards = new ArrayList<>(currentPlanet.getReward());

        //non viene fatto il controllo se sono tutte storage perchè già fatto lato client
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Storage> chosenStorages = new ArrayList();
        for (Coordinates coords : chosenStorageCoords) {
            if (coords.isCoordinateInvalid()) {
                // Coordinate invalide (-1,-1) indicano che questo cubo non può essere salvato
                chosenStorages.add(null);
            } else {
                Component component = shipBoard.getComponentAt(coords);
                if (component instanceof Storage) {
                    chosenStorages.add((Storage) component);
                } else {
                    // Se le coordinate non puntano a uno storage, aggiungi null
                    chosenStorages.add(null);
                }
            }
        }

        // Caso 1: Il giocatore non ha scelto nessuno storage
        if (chosenStorages.isEmpty()) {
            // Scarta tutti i reward - il giocatore non può accettare nessun cubo
            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " cannot accept any rewards due to lack of storage space");

            // Procedi con il prossimo giocatore o termina la carta
            proceedToNextPlayerOrEndCard();
            return;
        }

        // Caso 2: Il giocatore ha scelto meno storage dei reward disponibili
        if (chosenStorages.size() < planetRewards.size()) {
            // Mantieni solo i primi N reward, dove N = numero di storage scelti
            List<CargoCube> rewardsToProcess = planetRewards.subList(0, chosenStorages.size());
            List<CargoCube> discardedRewards = planetRewards.subList(chosenStorages.size(), planetRewards.size());

            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " can only accept " + chosenStorages.size() +
                    " out of " + planetRewards.size() + " rewards");
            System.out.println("Discarded rewards: " + discardedRewards);

            planetRewards = rewardsToProcess;
        }

        // Caso 3: Il giocatore ha scelto più storage dei reward (non dovrebbe succedere, ma gestiamo)
        if (chosenStorages.size() > planetRewards.size()) {
            // Usa solo i primi N storage, dove N = numero di reward
            chosenStorages = chosenStorages.subList(0, planetRewards.size());
        }

        // Validazione: controlla che i cubi RED vadano solo in SpecialStorage
        for (int i = 0; i < Math.min(chosenStorages.size(), planetRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = planetRewards.get(i);

            // Salta gli storage null (coordinate invalide dal client)
            if (storage == null) {
                continue;
            }

            if (!(storage instanceof SpecialStorage) && cube == CargoCube.RED) {
                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
            }
        }

        // Processa i cubi effettivamente posizionabili
        for (int i = 0; i < Math.min(chosenStorages.size(), planetRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = planetRewards.get(i);

            // Salta gli storage null (il giocatore non può/non vuole piazzare questo cubo)
            if (storage == null) {
                System.out.println("Cube " + cube + " discarded - no valid storage selected");
                continue;
            }

            // Se lo storage è pieno, rimuovi il cubo meno prezioso
            if (storage.isFull()) {
                List<CargoCube> sortedStorage = new ArrayList<>(storage.getStockedCubes());
                sortedStorage.sort(CargoCube.byValue);
                CargoCube lessValuableCargoCube = sortedStorage.get(0);
                storage.removeCube(lessValuableCargoCube);
                System.out.println("Removed " + lessValuableCargoCube + " to make space for " + cube);
            }

            // Aggiungi il cubo allo storage
            storage.addCube(cube);
            System.out.println("Added " + cube + " to storage");
        }

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
        });

        // Muovi il giocatore indietro
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRankingUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
        });

        // Procedi con il prossimo giocatore o termina la carta
        proceedToNextPlayerOrEndCard();


//        if (chosenStorage.size() != currentPlanet.getReward().size())
//            throw new IllegalArgumentException("Incorrect number of storages");
//
//        IntStream.range(0, chosenStorage.size()).forEach(i -> {
//            if (!(chosenStorage.get(i) instanceof SpecialStorage) && currentPlanet.getReward().get(i) == CargoCube.RED)
//                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
//        });
//
//        chosenStorage.forEach(storage -> {
//            if(storage.isFull()) {
//                List<CargoCube> sortedStorage = storage.getStockedCubes();
//                sortedStorage.sort(CargoCube.byValue);
//                CargoCube lessValuableCargoCube = sortedStorage.getFirst();
//                storage.removeCube(lessValuableCargoCube);
//            }
//            storage.addCube(currentPlanet.getReward().removeFirst());
//        });
//
//        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
//
//
//        if (gameModel.hasNextPlayer()) {
//            gameModel.nextPlayer();
//            setCurrState(CardState.CHOOSE_PLANET);
//        } else {
//            setCurrState(CardState.END_OF_CARD);
//        }

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
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }
    }


    @Override
    public String toString() {
        String firstString = String.format("""
           %s
           ┌────────────────────────────┐
           │          Planets           │
           ├────────────────────────────┤
           │ Planets:           x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """, imageName,
                availablePlanets != null ? availablePlanets.size() : 0,
                stepsBack);

        StringBuilder secondString = new StringBuilder("   ");
        if (availablePlanets != null && !availablePlanets.isEmpty()) {
            secondString.append("Planet Rewards:\n");
            for (int i = 0; i < availablePlanets.size(); i++) {
                List<CargoCube> reward = availablePlanets.get(i).getReward();
                String cubes = reward
                        .stream()
                        .map(Enum::name)
                        .toList()
                        .toString()
                        .replaceAll("[\\[\\]]", "");
                secondString.append(String.format("   Planet %d: %s%n", i + 1, cubes));
            }
        }

        return firstString + secondString;
    }

    public ClientCard toClientCard() {
        return new ClientPlanets(cardName, imageName, availablePlanets, playerPlanet, stepsBack);
    }

}
