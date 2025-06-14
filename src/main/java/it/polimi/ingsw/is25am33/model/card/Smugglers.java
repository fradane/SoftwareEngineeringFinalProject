package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Smugglers extends Enemies implements PlayerMover, DoubleCannonActivator {

    private int cubeMalus;
    private List<CargoCube> reward;
    private int stepsBack;
    private int requiredFirePower;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.HANDLE_CUBES_REWARD, CardState.HANDLE_CUBES_MALUS);

    public Smugglers() {
        this.cardName = this.getClass().getSimpleName();
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    @Override
    public CardState getFirstState() {
        return null;
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {
        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case ACCEPT_THE_REWARD:
                this.currPlayerDecidedToGetTheReward(playerChoices.hasAcceptedTheReward());
                break;
            case HANDLE_CUBES_MALUS:
                this.currPlayerChoseStorageToRemove(playerChoices.getChosenStorage().orElseThrow());
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
        //TODO

        return null;
    }

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());

        if (currPlayerCannonPower > requiredFirePower)
            setCurrState(CardState.ACCEPT_THE_REWARD);
        else
            setCurrState(CardState.HANDLE_CUBES_MALUS);
    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {
        if (hasPlayerAcceptedTheReward)
            setCurrState(CardState.HANDLE_CUBES_REWARD);
        else
            setCurrState(CardState.END_OF_CARD);
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

    private void currPlayerChoseStorageToRemove(List<Coordinates> chosenStorageCoords) {

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

        if (chosenStorages.size() < cubeMalus)
            throw new IllegalArgumentException("Not enough storages");

        chosenStorages.stream().distinct().forEach(storage -> {
            if (Collections.frequency(chosenStorages, storage) > storage.getMaxCapacity() - storage.getStockedCubes().size())
                throw new IllegalArgumentException("The number of required storages is not enough");
        });

        chosenStorages.forEach(storage -> {
            List<CargoCube> sortedStorage = storage.getStockedCubes();
            sortedStorage.sort(CargoCube.byValue);
            CargoCube lessValuableCargoCube = sortedStorage.getFirst();
            storage.removeCube(lessValuableCargoCube);
        });

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    //TODO
    @Override
    public String toString() {
        return """
        %s
        ┌────────────────────────────┐
        │     Smugglers               │
        ├────────────────────────────┤
        │ Cube Malus:     x%-2d     │
        """;
    }


}
