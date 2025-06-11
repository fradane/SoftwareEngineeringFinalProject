package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;

import java.util.*;
import java.util.stream.IntStream;

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

    private void currPlayerChoseCannonsToActivate(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();
        List<Cannon> chosenDoubleCannons = new ArrayList<>();

        for(Coordinates chosenDoubleCannonCoord : chosenDoubleCannonsCoords) {
            chosenDoubleCannons.add((Cannon) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenDoubleCannonCoord));
        }

        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

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

    private void currPlayerChoseCargoCubeStorage(List<Storage> chosenStorage) {

        if (chosenStorage.size() != reward.size())
            throw new IllegalArgumentException("Incorrect number of storages");

        IntStream.range(0, chosenStorage.size()).forEach(i -> {
            if (!(chosenStorage.get(i) instanceof SpecialStorage) && reward.get(i) == CargoCube.RED)
                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
        });

        chosenStorage.forEach(storage -> {
            if(storage.isFull()) {
                List<CargoCube> sortedStorage = storage.getStockedCubes();
                sortedStorage.sort(CargoCube.byValue);
                CargoCube lessValuableCargoCube = sortedStorage.getFirst();
                storage.removeCube(lessValuableCargoCube);
            }
            storage.addCube(reward.removeFirst());
        });

        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
        setCurrState( CardState.END_OF_CARD);
    }

    private void currPlayerChoseStorageToRemove(List<Storage> chosenStorage) {

        if (chosenStorage.size() < cubeMalus)
            throw new IllegalArgumentException("Not enough storages");

        chosenStorage.stream().distinct().forEach(storage -> {
            if (Collections.frequency(chosenStorage, storage) > storage.getMaxCapacity() - storage.getStockedCubes().size())
                throw new IllegalArgumentException("The number of required storages is not enough");
        });

        chosenStorage.forEach(storage -> {
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
