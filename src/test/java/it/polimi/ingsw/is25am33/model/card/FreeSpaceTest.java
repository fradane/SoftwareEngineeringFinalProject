package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.Level2FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.game.Game;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.Test;

import javax.smartcardio.Card;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreeSpaceTest {

    @Test
    void testFreeSpace() {

        Game game = new Game(new Level2FlyingBoard(12));
        AdventureCard freeSpace = new FreeSpace();




    }

}