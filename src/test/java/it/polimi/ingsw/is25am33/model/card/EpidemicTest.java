package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CrewMember;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.game.Game;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EpidemicTest {

    private Game game;
    private AdventureCard card;
    private FlyingBoard flyingBoard;
    private Cabin cabin12, cabin11;
    private Cabin cabin21, cabin22;

    @BeforeEach
    void setUp() {

        cabin11 = new Cabin(null);
        cabin12 = new Cabin(null);

        cabin11.fillCabin(CrewMember.HUMAN);
        cabin12.fillCabin(CrewMember.BROWN_ALIEN);

        cabin21 = new Cabin(null);
        cabin22 = new Cabin(null);

        cabin21.fillCabin(CrewMember.PURPLE_ALIEN);
        cabin22.fillCabin(CrewMember.BROWN_ALIEN);

        ShipBoard shipBoard = new Level2ShipBoard(null) {

            public Set<Cabin> cabinWithNeighbors() {
                return new HashSet<>();
            }

        };

        ShipBoard shipBoard1 = new Level2ShipBoard(null) {

            public Set<Cabin> cabinWithNeighbors() {
                return new HashSet<>(Set.of(cabin11, cabin12));
            }

        };

        ShipBoard shipBoard2 = new Level2ShipBoard(null) {
            public Set<Cabin> cabinWithNeighbors() {
                return new HashSet<>(Set.of(cabin21, cabin22));
            }
        };


        List<Player> players = new ArrayList<>(List.of(new Player("fra", shipBoard),
                new Player("ali", shipBoard),
                new Player("luc", shipBoard2),
                new Player("mar", shipBoard1)));

        game = new Game(flyingBoard, players);

        card = new Epidemic();

        game.setCurrAdventureCard(card);
        game.setCurrRanking(players);

        card.setCurrState(CardState.START_CARD);
        game.startCard();

    }

    @Test
    void TestToCheckIfTheMethodEndsProperly() {

        card.play(null);
        assertEquals(CardState.END_OF_CARD, card.getCurrState());

    }

    @Test
    void TestToCheckIfTheMethodRemovesCrewMembersCorrectly() {

        card.play(null);

        assertEquals(1, cabin11.getInhabitants().size());
        assertEquals(0, cabin12.getInhabitants().size());

        assertEquals(0, cabin21.getInhabitants().size());
        assertEquals(0, cabin22.getInhabitants().size());

        assertEquals(CardState.END_OF_CARD, card.getCurrState());

    }

    @Test
    void TestToCheckIfTheUnknownStateExceptionIsThrown() {

        card.setCurrState(CardState.END_OF_CARD);

        Exception e = assertThrows(UnknownStateException.class, () -> {
            card.play(null);
        });

        assertEquals("Unknown current state", e.getMessage());

    }

}