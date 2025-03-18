package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Player;

public interface PlayerMover {

    default void movePlayer(FlyingBoard flyingBoard, Player player, int steps) {

        flyingBoard.movePlayer(player, steps);

    }

}
