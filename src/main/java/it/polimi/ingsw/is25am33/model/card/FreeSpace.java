package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Engine;

import java.util.Collections;
import java.util.List;

public class FreeSpace extends AdventureCard implements PlayerMover {

    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_ENGINES);

    public FreeSpace() {}

    public void currPlayerChoseEnginesToActivate(List<Engine> chosenDoubleEngines, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException, IllegalStateException {

        if (currState != GameState.CHOOSE_ENGINES)
            throw new IllegalStateException("Not the right state");

        if (chosenDoubleEngines.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenDoubleEngines, box) > box.getAvailableBattery())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);
        int stepsForward = game.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines.stream());
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsForward);

        if (game.hasNextPlayer()) {
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }


}
