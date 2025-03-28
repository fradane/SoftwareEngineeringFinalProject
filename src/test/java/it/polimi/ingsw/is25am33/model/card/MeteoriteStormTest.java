package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.BigMeteorite;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.dangerousObj.SmallMeteorite;
import it.polimi.ingsw.is25am33.model.game.Game;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Not;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MeteoriteStormTest {

    private Game game;
    private MeteoriteStorm card;

    private Meteorite big;
    private Meteorite small;

    Player player1 = new Player("Alice");
    Player player2 = new Player("Bob");
    Player player3 = new Player("Charlie");
    Player player4 = new Player("Dana");

    @BeforeEach
    void setUp() {

        List<Player> players = List.of(player1, player2, player3, player4);
        game = new Game(new Level2FlyingBoard(21), players);
        game.setCurrRanking(players);
        big = new BigMeteorite(Direction.NORTH);
        small = new SmallMeteorite(Direction.SOUTH);

        List<Meteorite> meteorites = new ArrayList<>();
        meteorites.add(small);
        meteorites.add(big);

        card = new MeteoriteStorm(meteorites, game);
        game.setCurrState(GameState.START_CARD);
        game.setCurrAdventureCard(card);
        game.startCard();

    }

    @Test
    void testGetFirstState() {
        assertEquals(GameState.THROW_DICES, card.getFirstState());
    }

    @Test
    void testPlayThrowDicesChangesState() throws UnknownStateException {
        card.play(null);
        assertEquals(GameState.DANGEROUS_ATTACK, card.currState);
        assertEquals(big, game.getCurrDangerousObj());
    }

    @Test
    void testUnknownStateThrows() {
        game.setCurrState(GameState.END_OF_CARD);
        card.currState = GameState.END_OF_CARD;
        assertThrows(UnknownStateException.class, () -> card.play(null));
    }


    public class TestShipBoard1 extends ShipBoard {

        public void handleDangerousObject(DangerousObj obj) {
            System.out.println("");
        }

        @Override
        public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
            return false;
        }

        public boolean isExposed(int pos, Direction direction) throws IllegalArgumentException {
            return true;
        }

        public boolean isItGoingToHitTheShip(DangerousObj obj) {
            return true;
        }

    }

    static class TestShipBoard2 extends ShipBoard {

        @Override
        public void handleDangerousObject(DangerousObj obj) {

        }

        @Override
        public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
            return false;
        }

    }


}