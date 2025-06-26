package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Player;

public interface PlayerMover {

    /**
     * Moves a player on the specified flying board by a given number of steps.
     *
     * @param flyingBoard The flying board where the player will be moved.
     * @param player The player to be moved.
     * @param steps The number of steps the player should move. A positive number moves the player forward,
     *              while a negative number moves the player backward.
     */
    default void movePlayer(FlyingBoard flyingBoard, Player player, int steps)  {

        flyingBoard.movePlayer(player, steps);

    }

}
