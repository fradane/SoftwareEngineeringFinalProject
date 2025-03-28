package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.Collections;
import java.util.List;

public class FreeSpace extends AdventureCard implements PlayerMover {

    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_ENGINES);

    public FreeSpace(Game game) {
        super(game);
    }

    @Override
    public GameState getFirstState() {
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

        if (chosenDoubleEngines == null || chosenBatteryBoxes == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEngines.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenBatteryBoxes, box) > box.getAvailableBattery())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);
        int stepsForward = game.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);
        movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsForward);

        if (game.hasNextPlayer()) {
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }


}
