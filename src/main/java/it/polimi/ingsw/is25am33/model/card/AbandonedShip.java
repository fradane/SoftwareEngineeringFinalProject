package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.component.Cabin;

import java.util.List;


public class AbandonedShip extends AdventureCard implements PlayerMover, CrewMemberRemover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    private static final List<GameState> cardStates = List.of(GameState.VISIT_LOCATION, GameState.REMOVE_CREW_MEMBERS);

    public AbandonedShip(int crewMalus, int stepsBack, int reward) {
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.reward = reward;
    }

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

        if (currState != GameState.VISIT_LOCATION)
            throw new IllegalStateException("Not the right state");

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

        removeMemberProcess(chosenCabins, crewMalus);

        chosenCabins.forEach(Cabin::removeMember);

        game.getCurrPlayer().addCredits(reward);
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);

        game.setCurrState(GameState.END_OF_CARD);

    }

}
