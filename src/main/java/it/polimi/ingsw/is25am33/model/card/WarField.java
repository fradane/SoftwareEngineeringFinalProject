package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.game.Player;
import javafx.util.Pair;

import java.util.*;

public class WarField extends AdventureCard implements PlayerMover, DoubleCannonActivator, CrewMemberRemover {

    private int stepsBack;
    private int crewMalus;
    private List<Shot> shots;
    private List<String> shotIDs;
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<CardState, CardState> categories;
    private Pair<Player, Double> leastResourcedPlayer;
    private Iterator<CardState> phasesIterator;

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

//        switch (currState) {
//            case EVALUATE_CANNON_POWER:
//                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
//                break;
//            case EVALUATE_ENGINE_POWER:
//                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
//                break;
//            case EVALUATE_CREW_MEMBERS:
//                this.countCrewMembers();
//                break;
//            case REMOVE_CREW_MEMBERS:
//                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
//                break;
//            case HANDLE_CUBES_MALUS:
//                this.currPlayerChose
//                break;
//            default:
//                throw new IllegalStateException("Uknown current state");
//        }

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

    public void initilizePhasesIterator() {
        phasesIterator = categories.keySet().iterator();
    }

    // TODO completare la classe

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

        if (categories.get(currState) == CardState.STEPS_BACK)
            movePlayer(gameModel.getFlyingBoard(), leastResourcedPlayer.getKey(), stepsBack);
        else
            setCurrState(categories.get(currState) );

    }




    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException {

        removeMemberProcess(chosenCabins, crewMalus);

        if (phasesIterator.hasNext())
            setCurrState(phasesIterator.next());
        else
            setCurrState(CardState.END_OF_CARD);

    }


}
