package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.Iterator;
import java.util.List;

public class AbandonedStation extends AdventureCard implements PlayerMover {

    private int stepsBack;
    private int requiredCrewMembers;
    private List<CargoCube> reward;
    private Iterator<CargoCube> rewardIterator;
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

    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
        this.rewardIterator = reward.iterator();
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

    public AbandonedStation() {}

    private void currPlayerWantsToVisit (boolean wantsToVisit) throws IllegalDecisionException {

        if (wantsToVisit) {
            if (game.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < requiredCrewMembers)
                throw new IllegalDecisionException("Player has not enough crew members");
            currState = CardState.REMOVE_CREW_MEMBERS;
        } else if (game.hasNextPlayer()) {
            game.nextPlayer();
        } else {
            currState = CardState.END_OF_CARD;
        }

    }

    private void currPlayerChoseCargoCubeStorage (Storage chosenStorage) {

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
            currState = CardState.END_OF_CARD;
        }

    }


}
