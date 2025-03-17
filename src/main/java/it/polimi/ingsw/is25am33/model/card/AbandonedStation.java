package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class AbandonedStation extends AdventureCard implements cargoCubesHandler, playerMover {

    private int stepsBack;
    private int requiredCrewMembers;
    private List<CargoCube> reward;
    private final Iterator<CargoCube> rewardIterator;
    private static final List<GameState> cardStates = List.of(GameState.VISIT_LOCATION, GameState.HANDLE_CUBES_REWARD);

    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public AbandonedStation(int stepsBack, int requiredCrewMembers, List<CargoCube> reward) {
        this.stepsBack = stepsBack;
        this.requiredCrewMembers = requiredCrewMembers;
        this.rewardIterator = reward.iterator();
        this.reward = reward;
    }

    public void currPlayerWantsToVisit (boolean wantsToVisit) throws IllegalStateException, IllegalDecisionException {

        if (currState != GameState.VISIT_LOCATION)
            throw new IllegalStateException("Not the right state");

        if (wantsToVisit) {
            if (game.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < requiredCrewMembers)
                throw new IllegalDecisionException("Player has not enough crew members");
            currState = GameState.REMOVE_CREW_MEMBERS;
            game.setCurrState(currState);
        } else if (game.hasNextPlayer()){
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

    public void currPlayerChoseCargoCubeStorage (Storage chosenStorage) {

        if (currState != GameState.HANDLE_CUBES_REWARD)
            throw new IllegalStateException("Not the right state");

        if(chosenStorage.isFull()) {
            List<CargoCube> sortedStorage = chosenStorage.getStockedCubes();
            sortedStorage.sort(CargoCube.byValue);
            CargoCube lessValuableCargoCube = sortedStorage.getFirst();
            chosenStorage.removeCube(lessValuableCargoCube);
        }

        chosenStorage.addCube(rewardIterator.next());

        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);

        if (rewardIterator.hasNext()) {
            rewardIterator.next();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }


}
