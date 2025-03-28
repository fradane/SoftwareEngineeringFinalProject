package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Stardust extends AdventureCard implements PlayerMover {

    private static final List<GameState> cardStates = List.of(GameState.STARDUST);

    public Stardust(Game game) {
        super(game);
    }

    @Override
    public GameState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case STARDUST:
                this.moveNotCorrectlyAssembledShips();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    private void moveNotCorrectlyAssembledShips() {

        game.getPlayers()
                .forEach(p -> movePlayer(game.getFlyingBoard(), p, p.getPersonalBoard().countExposed() * -1));

        currState = GameState.END_OF_CARD;
        game.setCurrState(currState);

    }

}
