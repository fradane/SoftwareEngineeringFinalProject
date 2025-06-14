package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientAbandonedStation;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.List;

public class AbandonedStation extends AdventureCard implements PlayerMover {

    private int stepsBack;
    private int requiredCrewMembers;
    private List<CargoCube> reward;
    private static final List<CardState> cardStates = List.of(CardState.VISIT_LOCATION, CardState.HANDLE_CUBES_REWARD);

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case VISIT_LOCATION:
                try {
                    this.currPlayerWantsToVisit(playerChoices.isWantsToVisit());
                } catch (IllegalDecisionException e) {
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

    @Override
    public ClientCard toClientCard() {
        return new ClientAbandonedStation(cardName, imageName, requiredCrewMembers, reward, stepsBack);
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getRequiredCrewMembers() {
        return requiredCrewMembers;
    }

    public List<CargoCube> getReward() {
        return reward;
    }

    public AbandonedStation(int stepsBack, int requiredCrewMembers, List<CargoCube> reward) {
        this.stepsBack = stepsBack;
        this.requiredCrewMembers = requiredCrewMembers;
        this.reward = reward;
    }

    public AbandonedStation() {
        this.cardName = this.getClass().getSimpleName();
    }

    private void currPlayerWantsToVisit(boolean wantsToVisit) throws IllegalDecisionException {
        try {
            if (wantsToVisit) {
                if (gameModel.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < requiredCrewMembers)
                    throw new IllegalDecisionException("Player has not enough crew members");
                setCurrState(CardState.HANDLE_CUBES_REWARD);
            } else if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.VISIT_LOCATION);
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.DRAW_CARD);
            }
        } catch (Exception e) {
            System.err.println("Error in currPlayerWantsToVisit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void currPlayerChoseCargoCubeStorage(List<Coordinates> chosenStorageCoords) {
        List<CargoCube> stationRewards = new ArrayList<>(reward);

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
            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " cannot accept any rewards due to lack of storage space");
            proceedToNextPlayerOrEndCard();
            return;
        }

        // Caso 2: Il giocatore ha scelto meno storage dei reward disponibili
        if (chosenStorages.size() < stationRewards.size()) {
            List<CargoCube> rewardsToProcess = stationRewards.subList(0, chosenStorages.size());
            List<CargoCube> discardedRewards = stationRewards.subList(chosenStorages.size(), stationRewards.size());

            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " can only accept " + chosenStorages.size() +
                    " out of " + stationRewards.size() + " rewards");
            System.out.println("Discarded rewards: " + discardedRewards);

            stationRewards = rewardsToProcess;
        }

        // Caso 3: Il giocatore ha scelto più storage dei reward
        if (chosenStorages.size() > stationRewards.size()) {
            chosenStorages = chosenStorages.subList(0, stationRewards.size());
        }

        // Validazione: controlla che i cubi RED vadano solo in SpecialStorage
        for (int i = 0; i < Math.min(chosenStorages.size(), stationRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = stationRewards.get(i);

            if (storage == null) {
                continue;
            }

            if (!(storage instanceof SpecialStorage) && cube == CargoCube.RED) {
                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
            }
        }

        // Processa i cubi effettivamente posizionabili
        for (int i = 0; i < Math.min(chosenStorages.size(), stationRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = stationRewards.get(i);

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

            storage.addCube(cube);
            System.out.println("Added " + cube + " to storage");
        }

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
        });

        // Muovi il giocatore indietro
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

//        gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
//            clientController.notifyRankingUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
//        });

        // Termina la carta
        setCurrState(CardState.END_OF_CARD);
        gameModel.resetPlayerIterator();
        gameModel.setCurrGameState(GameState.DRAW_CARD);
    }

    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.VISIT_LOCATION);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }
    }

    @Override
    public String toString() {
        return String.format("""
           %s
           ┌────────────────────────────┐
           │     Abandoned Station      │
           ├────────────────────────────┤
           │ Crew Required:     x%-2d     │
           │ Reward Cubes:      x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """, imageName, requiredCrewMembers, reward != null ? reward.size() : 0, stepsBack);
    }

}
