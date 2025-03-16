package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Stardust extends AdventureCard implements playerMover {

    private static final List<GameState> cardStates = List.of(GameState.STARDUST);


    public void moveNotCorrectlyAssembledShips() throws IllegalStateException {

        if (currState != GameState.STARDUST) throw new IllegalStateException("Not the right state");

        game.getPlayers()
                .forEach(p -> movePlayer(game.getFlyingBoard(), p, p.getPersonalBoard().countExposed() * -1));

        currState = GameState.END_OF_CARD;
        game.setCurrState(currState);

    }

}
