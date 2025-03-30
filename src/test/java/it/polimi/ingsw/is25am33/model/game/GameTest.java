package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.Level2FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    void Test() {

        // server
        Player player1 = new Player("fra", new Level2ShipBoard(PlayerColor.RED));
        Player player2 = new Player("marc", new Level2ShipBoard(PlayerColor.BLUE));
        Player player3 = new Player("luc", new Level2ShipBoard(PlayerColor.GREEN));
        Player player4 = new Player("ali", new Level2ShipBoard(PlayerColor.YELLOW));

        List<Player> players = new ArrayList<>(List.of(player1, player2, player3, player4));

        Game game = new Game(new Level2FlyingBoard(), players);

        game.getCurrGameState().run(game);

    }

}