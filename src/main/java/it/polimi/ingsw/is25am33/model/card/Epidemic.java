package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.game.Game;

public class Epidemic extends AdventureCard{

    @Override
    public void effect(Game game) {

        game.getPlayers()
                .stream()
                .flatMap(p -> p.getPersonalBoard().cabinWithNeighbors())
                .forEach(Cabin::removeMember);
    }

}
