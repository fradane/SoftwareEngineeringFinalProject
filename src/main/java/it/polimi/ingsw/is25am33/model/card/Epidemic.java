package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Epidemic extends AdventureCard{

    private static final List<GameState> cardStates = List.of(GameState.EPIDEMIC);

    public Epidemic() {}

    public void removeInfectedCrewMembers() throws IllegalStateException{

        if (currState != GameState.EPIDEMIC)
            throw new IllegalStateException("Not the right state");

        game.getPlayers()
                .stream()
                .flatMap(p -> p.getPersonalBoard().cabinWithNeighbors().stream())
                .forEach(Cabin::removeMember);

        currState = GameState.END_OF_CARD;
        game.setCurrState(currState);

    }

}
