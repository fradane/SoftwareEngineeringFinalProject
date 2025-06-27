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

class FreeSpaceTest {
    private GameModel gameModel;
    private FreeSpace freeSpace;
    private GameClientNotifier gameClientNotifier;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel("1234", 2, false){
            @Override
            public void setCurrGameState(GameState state) {
            }
        };
        gameClientNotifier = new GameClientNotifier(new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        freeSpace = new FreeSpace();
        freeSpace.setGame(gameModel);

        Player player = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        gameModel.getPlayers().put("luca", player);
        gameModel.setCurrPlayer(player);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().insertPlayer(player);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_ENGINES, freeSpace.getFirstState());
    }

    @Test
    void testPlayUnknownStateException() {
        freeSpace.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class, () -> freeSpace.play(new PlayerChoicesDataStructure()));
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = freeSpace.toClientCard();
        assertTrue(clientCard instanceof ClientFreeSpace);
    }

    @Test
    void testCurrPlayerChoseEnginesToActivateMovesPlayer() {
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        FreeSpace freeSpaceCard = new FreeSpace();
        freeSpaceCard.setGame(gameModel);

        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates engineCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Engine engine = new DoubleEngine(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);

        shipBoard.getShipMatrix()[6][7] = engine;
        shipBoard.getShipMatrix()[7][6] = batteryBox;


        int prevPosition = gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer());

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleEngines(List.of(engineCoord))
                .build();

        freeSpaceCard.setCurrState(CardState.CHOOSE_ENGINES);
        freeSpaceCard.play(playerChoicesDataStructure);

        // Verifiche
        assertEquals(prevPosition + 2, gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
        assertEquals(CardState.END_OF_CARD, freeSpaceCard.getCurrState());
    }




}