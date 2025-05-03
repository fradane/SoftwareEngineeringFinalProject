package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Engine;

import java.util.Collections;
import java.util.List;

public class FreeSpace extends AdventureCard implements PlayerMover {

    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_ENGINES);

    public FreeSpace() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_ENGINES:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    public void currPlayerChoseEnginesToActivate(List<Engine> chosenDoubleEngines, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        // TODO controllare se un giocatore non ha engine
        if (chosenDoubleEngines == null || chosenBatteryBoxes == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEngines.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenBatteryBoxes, box) > box.getRemainingBatteries())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);
        int stepsForward = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsForward);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
        } else {
            setCurrState(CardState.END_OF_CARD);
        }
    }

    @Override
    public String toString() {
        return """
           ┌────────────────────────────┐
           │         FreeSpace          │
           └────────────────────────────┘
           """;
    }

}
