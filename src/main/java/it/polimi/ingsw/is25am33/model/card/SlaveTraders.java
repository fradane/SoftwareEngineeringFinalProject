package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class SlaveTraders extends AdvancedEnemies implements PlayerMover, CrewMemberRemover, DoubleCannonActivator {

    private int crewMalus;
    private final static List<GameState> cardStates = List.of(GameState.CHOOSE_CANNONS, GameState.ACCEPT_THE_REWARD, GameState.REMOVE_CREW_MEMBERS);

    public SlaveTraders(int crewMalus, Game game) {
        super(game);
        this.crewMalus = crewMalus;
    }

    @Override
    public GameState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case ACCEPT_THE_REWARD:
                this.currPlayerDecidedToGetTheReward(playerChoices.hasAcceptedTheReward());
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

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, game.getCurrPlayer());

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

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            game.getCurrPlayer().addCredits(reward);
            movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);
        }

        game.setCurrState(GameState.END_OF_CARD);

    }

    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException{

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
