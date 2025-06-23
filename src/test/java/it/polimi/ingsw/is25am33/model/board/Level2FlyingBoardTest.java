package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class Level2FlyingBoardTest {
    private GameModel gameModel = new GameModel("1234", 2, false);
    private FlyingBoard board = new Level2FlyingBoard();
    private Player player1, player2;
    private GameClientNotifier gameClientNotifier= new GameClientNotifier(gameModel, new ConcurrentHashMap<>());

    @BeforeEach
    void setUp() {
        board.setGameClientNotifier(gameClientNotifier);
        player1 = new Player("Alice", new Level2ShipBoard(PlayerColor.RED, gameClientNotifier,false), PlayerColor.RED );
        player2 = new Player("Luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier,false), PlayerColor.YELLOW );

    }

    @Test
    void TestInsertPlayer(){
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        assertTrue(board.getCurrentRanking().contains(player1));
        assertTrue(board.getCurrentRanking().contains(player2));
    }

    @Test
    void TestAddOutPlayer() {
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.addOutPlayer(player1,false);
        assertFalse(board.getCurrentRanking().contains(player1));
        assertTrue(board.getOutPlayers().contains(player1));
    }

    @Test
    void TestGetDoubledPlayer() {
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.movePlayer(player1, 30);
        board.getDoubledPlayers();
        assertFalse(board.getCurrentRanking().contains(player2));
        assertTrue(board.getOutPlayers().contains(player2));
    }

    @Test
    void TestMovePlayerPositiveOffset() {
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.movePlayer(player1, 3);
        assertEquals(9, board.getPlayerPosition(player1));
    }

    @Test
    void TestMovePlayerNegativeOffset() {
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.movePlayer(player1, -3);
        assertEquals(2, board.getPlayerPosition(player1));
    }

    @Test
    void TestMovePlayerOccupiedPosition(){
        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.movePlayer(player2, 3);

        assertEquals(6, board.getPlayerPosition(player1));
        assertEquals(7, board.getPlayerPosition(player2));
    }

}