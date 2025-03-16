package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class AbandonedShip extends AdventureCard implements playerMover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    private static final List<GameState> cardStates = List.of(GameState.VISIT_LOCATION, GameState.REMOVE_CREW_MEMBERS);

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public void currPlayerWantsToVisit (boolean wantsToVisit) throws IllegalStateException, IllegalDecisionException {

        if (currState != GameState.VISIT_ABANDONED_LOCATION) throw new IllegalStateException("Not the right state");

        if (wantsToVisit) {
            if (game.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < crewMalus)
                throw new IllegalDecisionException("Player has not enough crew members");
            currState = GameState.REMOVE_CREW_MEMBERS;
            game.setCurrState(currState);
        } else if (game.hasNextPlayer()) {
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

    public void currPlayerChoseRemovableCrewMembers (List<Cabin> chosenCabins) throws IllegalStateException, IllegalArgumentException {

        if (currState != GameState.REMOVE_CREW_MEMBERS)
            throw new IllegalStateException("Not the right state");

        if (chosenCabins.size() != crewMalus)
            throw new IllegalArgumentException("Not the right amount of crew members");

        chosenCabins.stream().distinct().forEach(cabin -> {
            if (Collections.frequency(chosenCabins, cabin) > cabin.getInhabitants().size())
                throw new IllegalArgumentException("The number of required crew members is not enough");
        });

        chosenCabins.forEach(cabin -> cabin.removeMember());

        game.getCurrPlayer().addCredits(reward);
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);

        game.setCurrState(GameState.END_OF_CARD);

    }

}
