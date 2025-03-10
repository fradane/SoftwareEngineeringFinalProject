package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;

public class FreeSpace extends AdventureCard implements playerMover {

    @Override
    public void effect(Game game) {

        FlyingBoard flyingBoard = game.getFlyingBoard();

        flyingBoard.getCurrentRanking()
                .forEach(p -> movePlayer(flyingBoard, p, p.getPersonalBoard().countTotalEnginePower());


    }

}
