package it.polimi.ingsw.is25am33.model.card;

import static org.junit.jupiter.api.Assertions.*;

import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.dangerousObj.SmallShot;
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

public class WarFieldTest {

    private GameModel gameModel;
    private GameClientNotifier gameClientNotifier;
    private WarField warField;
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

        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.getFlyingBoard().insertPlayer(player1);

        warField = new WarField();
        warField.setGame(gameModel);
        Map<CardState,CardState> categories = new ConcurrentHashMap<>();
        categories.put(CardState.EVALUATE_CREW_MEMBERS, CardState.STEPS_BACK);
        categories.put(CardState.CHOOSE_ENGINES, CardState.REMOVE_CREW_MEMBERS);
        categories.put(CardState.CHOOSE_CANNONS, CardState.DANGEROUS_ATTACK);
        warField.setCategories(categories);
        warField.setShots(new ArrayList<>(List.of(new SmallShot(Direction.NORTH))));
        gameModel.setCurrAdventureCard(warField);
    }

    @Test
    void testUnknownStateException() {
        warField.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class,
                () -> warField.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe sollevare UnknownStateException per stati sconosciuti");
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = warField.toClientCard();
        assertTrue(clientCard instanceof ClientWarField);
    }

    @Test
    void testCurrPlayerChoseEnginesToActivate() {
        warField.setCurrState(CardState.CHOOSE_ENGINES);
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

        warField.play(playerChoicesDataStructure);

        // Verifiche
        assertEquals(1,batteryBox.getRemainingBatteries());
        assertEquals(CardState.REMOVE_CREW_MEMBERS, warField.getCurrState());
    }

    @Test
    void testCurrPlayerChoseCannonToActivate() {
        warField.setCurrState(CardState.CHOOSE_CANNONS);
        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon= new DoubleCannon(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);

        shipBoard.getShipMatrix()[6][7] = cannon;
        shipBoard.getShipMatrix()[7][6] = batteryBox;


        int prevPosition = gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer());

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        warField.play(playerChoicesDataStructure);

        // Verifiche
        assertEquals(1,batteryBox.getRemainingBatteries());
        assertEquals(CardState.THROW_DICES, warField.getCurrState());
    }

    @Test
    void testEvaluatedCrewMembers (){
        warField.setStepsBack(-4);
        warField.getFirstState();
        warField.setCurrState(CardState.EVALUATE_CREW_MEMBERS);
        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cabin cabin= new Cabin(connectors);
        cabin.fillCabin(CrewMember.HUMAN);
        shipBoard.getShipMatrix()[6][7] = cabin;

        warField.play(new PlayerChoicesDataStructure());

        assertEquals(2,gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
    }

    @Test
    void testHandleCrewMalusFlow() {
        warField.getFirstState();
        warField.setCurrState(CardState.REMOVE_CREW_MEMBERS);

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


        warField.play(choices);

        assertEquals(1, cabin1.getInhabitants().size());
        assertEquals(1, cabin2.getInhabitants().size());

    }

    @Test
    void testHandleCubesMalusFlow() {
        warField.getFirstState();
        warField.setCurrState(CardState.HANDLE_CUBES_MALUS);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Prepara Storage e aggiungi cubi
        Storage storage1 = new StandardStorage(connectors,2);
        Storage storage2 = new StandardStorage(connectors, 3);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        // Prepara una BatteryBox
        BatteryBox batteryBox = new BatteryBox(connectors, 3);
        shipBoard.getShipMatrix()[7][8] = batteryBox;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(List.of(new Coordinates(6,7 )))
                .setChosenBatteryBoxes(List.of(new Coordinates(7,8 )))
                .build();


        warField.play(choices);

        // Verifica che il cubo di maggior valore (RED) sia stato rimosso
        assertEquals(1, storage1.getStockedCubes().size(), "Lo storage dovrebbe contenere un solo cubo dopo la rimozione");
        assertEquals(2, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");

    }
}