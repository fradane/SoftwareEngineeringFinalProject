package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FreeSpace extends AdventureCard implements playerMover {

    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_ENGINES);
    private Iterator<GameState> stateIterator = cardStates.iterator();

    public FreeSpace() {
        currState = stateIterator.next();
    }

    public void currPlayerChoseEnginesToActivate(List<Engine> chosenDoubleEngines, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException, IllegalStateException {

        if (currState != GameState.CHOOSE_ENGINES)
            throw new IllegalStateException("Not the right state");

        if (chosenDoubleEngines.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenDoubleEngines, box) > box.getAvailableBatteries())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(box -> box.useBattery());
        int stepsForward = game.getCurrPlayer().getPersonalBoard().countSingleEngine() + chosenDoubleEngines.size();
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsForward);

        if (game.hasNextPlayer()) {
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }


}
