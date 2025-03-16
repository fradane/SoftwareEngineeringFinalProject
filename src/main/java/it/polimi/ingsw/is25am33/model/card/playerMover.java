package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.board.FlyingBoard;

public interface playerMover {

    default void movePlayer(FlyingBoard flyingBoard, Player player, int steps) {

        flyingBoard.move(player, steps);

    }

}
