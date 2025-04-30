package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Cabin;

import java.rmi.RemoteException;
import java.util.List;


public class AbandonedShip extends AdventureCard implements PlayerMover, CrewMemberRemover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    private static final List<CardState> cardStates = List.of(CardState.VISIT_LOCATION, CardState.REMOVE_CREW_MEMBERS);

    public AbandonedShip(int crewMalus, int stepsBack, int reward) {
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.reward = reward;
    }

    public AbandonedShip() {
        this.cardName = this.getClass().getSimpleName();
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getReward() {
        return reward;
    }

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

            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public void setLevel(int level) {
        this.level = level;
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

    private void currPlayerWantsToVisit(boolean wantsToVisit) throws IllegalDecisionException {

        if (wantsToVisit) {
            if (gameModel.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < crewMalus)
                throw new IllegalDecisionException("Player has not enough crew members");
            setCurrState(CardState.REMOVE_CREW_MEMBERS);
        } else if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException {

        removeMemberProcess(chosenCabins, crewMalus);

        chosenCabins.forEach(Cabin::removeMember);

        gameModel.getCurrPlayer().addCredits(reward);
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        setCurrState(CardState.END_OF_CARD);

    }

}
