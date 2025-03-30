package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Epidemic extends AdventureCard{

    private static final List<CardState> cardStates = List.of(CardState.EPIDEMIC);

    public Epidemic() {}

    @Override
    public CardState getFirstState() {
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

        currState = CardState.END_OF_CARD;

    }

}
