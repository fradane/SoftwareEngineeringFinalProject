package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Epidemic extends AdventureCard{

    private static final List<GameState> cardStates = List.of(GameState.EPIDEMIC);

    public Epidemic(Game game) {
        super(game);
    }

    @Override
    public GameState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case EPIDEMIC:
                this.removeInfectedCrewMembers();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    public void removeInfectedCrewMembers() {

        game.getPlayers()
                .stream()
                .flatMap(p -> p.getPersonalBoard().cabinWithNeighbors().stream())
                .forEach(Cabin::removeMember);

        currState = GameState.END_OF_CARD;
        game.setCurrState(currState);

    }

}
