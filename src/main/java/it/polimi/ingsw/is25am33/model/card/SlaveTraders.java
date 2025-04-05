package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CardState;
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
    private final static List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.REMOVE_CREW_MEMBERS);

    public SlaveTraders(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public SlaveTraders() {
        this.cardName = this.getClass().getSimpleName();
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    @Override
    public CardState getFirstState() {
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

            currState = CardState.ACCEPT_THE_REWARD;

        } else if (currPlayerCannonPower == requiredFirePower) {

            if (game.hasNextPlayer()) {
                game.nextPlayer();
            } else {
                currState = CardState.END_OF_CARD;
            }

        } else {

            currState = CardState.REMOVE_CREW_MEMBERS;

        }

    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            game.getCurrPlayer().addCredits(reward);
            movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);
        }

        currState = CardState.END_OF_CARD;

    }

    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException{

        removeMemberProcess(chosenCabins, crewMalus);

        if (game.hasNextPlayer()) {
            game.nextPlayer();
            currState = CardState.CHOOSE_CANNONS;
        } else {
            currState = CardState.END_OF_CARD;
        }

    }

}
