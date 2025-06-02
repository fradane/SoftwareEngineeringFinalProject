package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Cannon;

import java.util.ArrayList;
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

    @Override
    public ClientCard toClientCard() {
        //TODO
        return null;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());

        if (currPlayerCannonPower > requiredFirePower) {

            setCurrState( CardState.ACCEPT_THE_REWARD);

        } else if (currPlayerCannonPower == requiredFirePower) {

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
            } else {
               setCurrState( CardState.END_OF_CARD);
            }

        } else {

            setCurrState( CardState.REMOVE_CREW_MEMBERS );

        }

    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            gameModel.getCurrPlayer().addCredits(reward);
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
        }

        setCurrState(CardState.END_OF_CARD);

    }

    private void currPlayerChoseRemovableCrewMembers(List<Cabin> chosenCabins) throws IllegalArgumentException{

        removeMemberProcess(chosenCabins, crewMalus);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        } else {
            setCurrState(CardState.END_OF_CARD);
        }

    }

    @Override
    public String toString() {
        return String.format("""
           %s
           ┌────────────────────────────┐
           │        SlaveTraders        │
           ├────────────────────────────┤
           │ firePower             x%-2d  │
           │ crewMalus             x%-2d  │
           │ reward                x%-2d  │
           │ stepsBack             %-2d   │
           └────────────────────────────┘
           """, imageName, requiredFirePower, crewMalus, reward, stepsBack);
    }

}
