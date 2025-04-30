package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.rmi.RemoteException;

public interface PlayerMover {

    default void movePlayer(FlyingBoard flyingBoard, Player player, int steps)  {

        flyingBoard.movePlayer(player, steps);

    }

}
