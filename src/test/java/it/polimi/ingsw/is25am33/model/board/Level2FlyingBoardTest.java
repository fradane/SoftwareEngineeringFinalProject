package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Level2FlyingBoardTest {
    private FlyingBoard board;
    private Player player1, player2, player3, player4;

    @BeforeEach
    void setUp() {
        board = new Level2FlyingBoard();
        //player1 = new Player("Alice", new Level1ShipBoard(null));
        //player2 = new Player("Luca", new Level1ShipBoard(null));
        //player3 = new Player("Marco", new Level1ShipBoard(null));

        board.insertPlayer(player1);
        board.insertPlayer(player2);
        board.insertPlayer(player3);
    }

    @Test
    void TestInsertPlayer() {
        //Player player5 = new Player("Luigi", new Level1ShipBoard(null));
        //board.insertPlayer(player5);
        //assertTrue(board.getCurrentRanking().contains(player5));
    }

    @Test
    void TestAddOutPlayer() {
        board.addOutPlayer(player1);
        assertFalse(board.getCurrentRanking().contains(player1));
        assertTrue(board.getOutPlayers().contains(player1));
    }

    @Test
    void TestGetDoubledPlayer() {
        List<Player> doubledPlayers = board.getDoubledPlayers();

        assertEquals(2, doubledPlayers.size());
        assertTrue(doubledPlayers.contains(player4));

        assertFalse(board.getCurrentRanking().contains(player4));
        assertTrue(board.getOutPlayers().contains(player4));
    }

    @Test
    void TestMovePlayerPositiveOffset() {
        board.movePlayer(player1, 3);
        assertEquals(28, board.getPlayerPosition(player1));
    }

    @Test
    void TestMovePlayerNegativeOffset() {
        board.movePlayer(player1, -3);
        assertEquals(22, board.getPlayerPosition(player1));
    }

    @Test
    void TestMovePlayerOccupiedPosition(){
        board.movePlayer(player4, 3);
        board.movePlayer(player1, -10);

        assertEquals(9, board.getPlayerPosition(player4));
        assertEquals(14, board.getPlayerPosition(player1));
    }

    @Test
    void TestGetCurrentRanking(){
        List<Player> currentRanking = board.getCurrentRanking();
        assertEquals(4, currentRanking.size());
        assertTrue(board.getCurrentRanking().contains(player4));
        assertEquals(board.getCurrentRanking().get(0), player1);
        assertEquals(board.getCurrentRanking().get(2), player4);
    }
}