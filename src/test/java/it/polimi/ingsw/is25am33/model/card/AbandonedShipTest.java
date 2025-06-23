package it.polimi.ingsw.is25am33.model.card;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.*;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.model.UnknownStateException;

public class AbandonedShipTest {

    private GameModel gameModel;
    private GameClientNotifier gameClientNotifier;
    private AbandonedShip abandonedShip;
    private Player player1;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel("1234", 1, false) {
            @Override
            public void setCurrGameState(GameState state) {

            }
        };
        gameClientNotifier = new GameClientNotifier(gameModel, new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);

        player1 = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        gameModel.getPlayers().put(player1.getNickname(), player1);
        player1.setGameClientNotifier(gameClientNotifier);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.getFlyingBoard().insertPlayer(player1);
        abandonedShip = new AbandonedShip();
        abandonedShip.setGame(gameModel);
        abandonedShip.setReward(5);
        gameModel.setCurrAdventureCard(abandonedShip);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.VISIT_LOCATION, abandonedShip.getFirstState());
    }

    @Test
    void testUnknownStateException() {
        abandonedShip.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class,
                () -> abandonedShip.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe sollevare UnknownStateException per stati sconosciuti");
    }

    @Test
    void testToClientCard() {
        ClientCard card = abandonedShip.toClientCard();
        assertTrue(card instanceof ClientAbandonedShip);
    }

    @Test
    void testHandleCrewMalusFlow() {
        abandonedShip.setCurrState(CardState.REMOVE_CREW_MEMBERS);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Prepara Storage e aggiungi cubi
        Cabin cabin1 = new Cabin(connectors);
        Cabin cabin2 = new Cabin(connectors);
        cabin1.fillCabin(CrewMember.HUMAN);
        cabin2.fillCabin(CrewMember.HUMAN);
        shipBoard.getShipMatrix()[6][7] = cabin1;
        shipBoard.getShipMatrix()[7][6] = cabin2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenCabins(List.of(new Coordinates(6,7 ),new Coordinates(7,6 )))
                .build();


        abandonedShip.play(choices);

        assertEquals(1, cabin1.getInhabitants().size());
        assertEquals(1, cabin2.getInhabitants().size());

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, abandonedShip.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testPlayerWantToVisit() {
        abandonedShip.setCurrState(CardState.VISIT_LOCATION);

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setWantsToVisit(true)
                .build();

        abandonedShip.play(choices);

        assertEquals(CardState.REMOVE_CREW_MEMBERS, abandonedShip.getCurrState());

    }

    @Test
    void testPlayerDontWantToVisit() {

        abandonedShip.setCurrState(CardState.VISIT_LOCATION);

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setWantsToVisit(false)
                .build();

        abandonedShip.play(choices);

        assertEquals(CardState.END_OF_CARD, abandonedShip.getCurrState(), "Stato dovrebbe essere END_OF_CARD");

    }


}


