package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;


public class AbandonedShip extends AdventureCard implements PlayerMover, CrewMemberRemover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    private static final List<GameState> cardStates = List.of(GameState.VISIT_LOCATION, GameState.REMOVE_CREW_MEMBERS);

    public AbandonedShip(int crewMalus, int stepsBack, int reward, Game game) {
        super(game);
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.reward = reward;
    }

    @Override
    public GameState getFirstState() {
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

    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException {

        removeMemberProcess(chosenCabins, crewMalus);

        chosenCabins.forEach(Cabin::removeMember);

        game.getCurrPlayer().addCredits(reward);
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);

        game.setCurrState(GameState.END_OF_CARD);

    }

}
