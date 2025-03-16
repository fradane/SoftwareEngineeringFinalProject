package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlaveTraders extends AdvancedEnemies implements playerMover {

    private int crewMalus;
    private final static List<GameState> cardStates = List.of(GameState.CHOOSE_CANNONS, GameState.ACCEPT_THE_REWARD, GameState.REMOVE_CREW_MEMBERS);

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException, IllegalStateException {

        if (currState != GameState.CHOOSE_CANNONS) throw new IllegalStateException("Not the right state");

        if (chosenDoubleCannons.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenDoubleCannons, box) > box.getAvailableBatteries())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(box -> box.useBattery());

        int currPlayerCannonPower = game.getCurrPlayer().getPersonalBoard().computeTotalCannonPower(chosenDoubleCannons);

        if (currPlayerCannonPower > requiredFirePower) {

            currState = GameState.ACCEPT_THE_REWARD;
            game.setCurrState(currState);

        } else if (currPlayerCannonPower == requiredFirePower) {

            if (game.hasNextPlayer()) {
                game.nextPlayer();
            } else {
                game.setCurrState(GameState.END_OF_CARD);
            }

        } else {

            currState = GameState.REMOVE_CREW_MEMBERS;
            game.setCurrState(currState);

        }

    }

    public void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (currState != GameState.ACCEPT_THE_REWARD) throw new IllegalStateException("Not the right state");

        if (hasPlayerAcceptedTheReward) {
            game.getCurrPlayer().addCredits(reward);
            movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);
        }

        game.setCurrState(GameState.END_OF_CARD);

    }

    public void currPlayerChoseRemovableCrewMembers (List<Cabin> chosenCabins) throws IllegalStateException, IllegalArgumentException{

        if (currState != GameState.REMOVE_CREW_MEMBERS) throw new IllegalStateException("Not the right state");

        if (chosenCabins.size() != crewMalus) throw new IllegalArgumentException("Not the right amount of crew members");

        chosenCabins.stream().distinct().forEach(cabin -> {
            if (Collections.frequency(chosenCabins, cabin) > cabin.getInhabitants().size()) throw new IllegalArgumentException("The number of required crew members is not enough");
        });

        chosenCabins.forEach(cabin -> cabin.removeMember());

        if (game.hasNextPlayer()) {
            game.nextPlayer();
            currState = GameState.CHOOSE_CANNONS;
            game.setCurrState(currState);
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

}
