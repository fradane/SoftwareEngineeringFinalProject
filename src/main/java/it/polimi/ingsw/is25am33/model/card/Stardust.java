package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

public class Stardust extends AdventureCard implements playerMover {

    @Override
    public void effect(Game game) {

        game.getPlayers()
                .forEach(p -> game.getFlyingBoard()
                        .movePlayer(game.getFlyingBoard(), p, p.getPersonalBoard().countExposed() * -1);

    }

}
