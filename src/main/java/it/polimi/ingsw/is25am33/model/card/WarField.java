package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import javafx.util.Pair;

import java.util.*;

public class WarField extends AdventureCard implements PlayerMover, DoubleCannonActivator, CrewMemberRemover, ShotSenderCard {

    private int cubeMalus;
    private int stepsBack;
    private int crewMalus;
    private List<Shot> shots;
    private List<String> shotIDs;
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<CardState, CardState> categories;
    private Pair<Player, Double> leastResourcedPlayer;
    private Iterator<CardState> phasesIterator;
    private Iterator<Shot> shotIterator;

    public WarField() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    @JsonIgnore
    public CardState getFirstState() {
        return null;
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {

        switch (currState) {
            case EVALUATE_CANNON_POWER:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case EVALUATE_ENGINE_POWER:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case EVALUATE_CREW_MEMBERS:
                this.countCrewMembers();
                break;
            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;
            case HANDLE_CUBES_MALUS:
                this.currPlayerChoseStorageToRemove(playerChoices.getChosenStorage().orElseThrow());
                break;
            case THROW_DICES:
                this.throwDices();
                break;
            case DANGEROUS_ATTACK:
                ((Shot) gameModel.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            default:
                throw new IllegalStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        //TODO
        return null;
    }

    public void convertIdsToShots() {
        shots = shotIDs.stream()
                .map(id -> {
                    try {
                        return shotCreator.get(id).call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    public int getCubeMalus() {
        return cubeMalus;
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setCategories(Map<CardState, CardState> categories) {
        this.categories = categories;
    }

    public List<String> getShotIDs() {
        return shotIDs;
    }

    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    public List<Shot> getShots() {
        return shots;
    }

    public Map<CardState, CardState> getCategories() {
        return categories;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    public void initilizeCardIterator() {
        phasesIterator = categories.keySet().iterator();
        shotIterator = shots.iterator();
    }

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());

        if (leastResourcedPlayer == null || currPlayerCannonPower < leastResourcedPlayer.getValue())
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), currPlayerCannonPower);

        if (gameModel.hasNextPlayer())
            gameModel.nextPlayer();
        else
            handleMalus();

    }

    public void currPlayerChoseEnginesToActivate(List<Engine> chosenDoubleEngines, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        if (chosenDoubleEngines == null || chosenBatteryBoxes == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEngines.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenBatteryBoxes, box) > box.getRemainingBatteries())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);
        int currPlayerEnginePower = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);

        if (leastResourcedPlayer == null || currPlayerEnginePower < leastResourcedPlayer.getValue())
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), (double) currPlayerEnginePower);

        if (gameModel.hasNextPlayer())
            gameModel.nextPlayer();
        else
            handleMalus();

    }

    public void countCrewMembers() {

        gameModel.getPlayers().keySet()
                .stream()
                .map(nickname -> gameModel.getPlayers().get(nickname))
                .min(Comparator.comparingInt(player -> player.getPersonalBoard().getCrewMembers().size()))
                .ifPresent(player -> {
                    leastResourcedPlayer = new Pair<>(player, (double) player.getPersonalBoard().getCrewMembers().size());
                });

        handleMalus();
    }

    private void handleMalus() {

        if (categories.get(currState) == CardState.STEPS_BACK) {
            movePlayer(gameModel.getFlyingBoard(), leastResourcedPlayer.getKey(), stepsBack);
            if (phasesIterator.hasNext()) {
                setCurrState(phasesIterator.next());
                gameModel.resetPlayerIterator();
            } else {
                setCurrState(CardState.END_OF_CARD);
            }
        } else {
            if (categories.get(currState) == CardState.DANGEROUS_ATTACK)
                setCurrState(CardState.THROW_DICES);
            else
                setCurrState(categories.get(currState));
            gameModel.setCurrPlayer(leastResourcedPlayer.getKey());
        }

    }

    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException {
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        //non viene fatto il controllo se sono tutte cabine perchè già fatto lato client
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        removeMemberProcess(chosenCabins, crewMalus);

        if (phasesIterator.hasNext()) {
            setCurrState(phasesIterator.next());
            gameModel.resetPlayerIterator();
        } else
            setCurrState(CardState.END_OF_CARD);

    }

    private void throwDices() {

        if (shotIterator == null) shotIterator = shots.iterator();

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currShot);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }

    private void currPlayerChoseStorageToRemove(List<Storage> chosenStorage) {

        if (chosenStorage.size() != cubeMalus)
            throw new IllegalArgumentException("Incorrect number of storages");

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

        if (phasesIterator.hasNext()) {
            gameModel.resetPlayerIterator();
            setCurrState(phasesIterator.next());
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    @Override
    public void playerDecidedHowToDefendTheirSelvesFromSmallShot(Shield chosenShield, BatteryBox chosenBatteryBox) {

        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip(currShot)) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenShield.getDirections().stream().anyMatch(d -> d == currShot.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                chosenBatteryBox.useBattery();

            } else {
                personalBoard.handleDangerousObject(currShot);
            }
        }

        if (shotIterator.hasNext()) {
            gameModel.setCurrDangerousObj(shotIterator.next());
        } else if (phasesIterator.hasNext()) {
            setCurrState(phasesIterator.next());
            gameModel.resetPlayerIterator();
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    @Override
    public void playerIsAttackedByABigShot() {

        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip(currShot)) {
            personalBoard.handleDangerousObject(currShot);
        }

        if (shotIterator.hasNext()) {
            gameModel.setCurrDangerousObj(shotIterator.next());
        } else if (phasesIterator.hasNext()) {
            setCurrState(phasesIterator.next());
            gameModel.resetPlayerIterator();
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    // TODO
    @Override
    public String toString() {
        return "WarField";
    }


}
