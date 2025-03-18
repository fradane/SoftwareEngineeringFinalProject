package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Cannon;

import java.util.List;

public class SlaveTraders extends AdvancedEnemies implements PlayerMover, CrewMemberRemover, DoubleCannonActivator {

    private int crewMalus;
    private final static List<GameState> cardStates = List.of(GameState.CHOOSE_CANNONS, GameState.ACCEPT_THE_REWARD, GameState.REMOVE_CREW_MEMBERS);

    public SlaveTraders(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException, IllegalStateException {

        if (currState != GameState.CHOOSE_CANNONS)
            throw new IllegalStateException("Not the right state");

        int currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, game.getCurrPlayer());

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

        removeMemberProcess(chosenCabins, crewMalus);

        if (game.hasNextPlayer()) {
            game.nextPlayer();
            currState = GameState.CHOOSE_CANNONS;
            game.setCurrState(currState);
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

}
