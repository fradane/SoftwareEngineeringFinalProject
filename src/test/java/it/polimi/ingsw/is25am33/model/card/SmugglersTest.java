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

public class SmugglersTest {

    private GameModel gameModel;
    private GameClientNotifier gameClientNotifier;
    private Smugglers smugglers;
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

        smugglers = new Smugglers();
        smugglers.setGame(gameModel);
        gameModel.setCurrAdventureCard(smugglers);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_CANNONS, smugglers.getFirstState());
    }

    @Test
    void testAcceptTheRewardFlow() {
        smugglers.setCurrState(CardState.ACCEPT_THE_REWARD);
        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(true)
                .build();
        smugglers.play(choices);
        assertEquals(CardState.HANDLE_CUBES_REWARD, smugglers.getCurrState(),
                "Stato dovrebbe passare ad HANDLE_CUBES_REWARD quando si accetta la ricompensa");
    }

    @Test
    void testDeclineTheRewardFlow() {
        smugglers.setCurrState(CardState.ACCEPT_THE_REWARD);
        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(false)
                .build();

        smugglers.play(choices);
        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState(),
                "Stato dovrebbe passare ad END_OF_CARD quando si rifiuta la ricompensa");
    }

    @Test
    void testUnknownStateException() {
        smugglers.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class,
                () -> smugglers.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe sollevare UnknownStateException per stati sconosciuti");
    }

    @Test
    void testToClientCard() {
        ClientCard card = smugglers.toClientCard();
        assertTrue(card instanceof ClientSmugglers, "La carta client dovrebbe essere di tipo ClientSmugglers");
    }

    @Test
    void testHandleCubesRewardFlow() {
        //TODO modificare questi test con funzionalità di redistribuzione dei cubi
//        // Prepara la carta
//        smugglers.setReward(Arrays.asList(CargoCube.GREEN, CargoCube.BLUE));
//        smugglers.setCurrState(CardState.HANDLE_CUBES_REWARD);
//
//        ShipBoard shipBoard = player1.getPersonalBoard();
//        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
//        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
//        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
//        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
//        connectors.put(Direction.WEST,ConnectorType.SINGLE);
//        // Aggiungiamo due Storage liberi sulla plancia
//        Storage storage1 = new StandardStorage(connectors,2);
//        storage1.addCube(CargoCube.GREEN);
//        storage1.addCube(CargoCube.BLUE);
//        Storage storage2 = new StandardStorage(connectors, 3);
//        shipBoard.getShipMatrix()[6][7] = storage1;
//        shipBoard.getShipMatrix()[7][6] = storage2;
//
//        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
//                .Builder()
//                .setChosenStorage(Arrays.asList(new Coordinates(6,7), new Coordinates(7 ,6)))
//                .build();
//
//        smugglers.play(choices);
//
//        // Verifica che gli Storage contengano i cubi assegnati
//        assertTrue( storage1.getStockedCubes().contains(CargoCube.GREEN), "Il primo Storage dovrebbe contenere RED");
//        assertTrue(storage2.getStockedCubes().contains(CargoCube.BLUE), "Il secondo Storage dovrebbe contenere BLUE");
//
//        // Verifica stato e reset
//        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testHandleCubesRewardHeChooseStorageLessTheCube() {
        // Prepara la carta
        smugglers.setReward(Arrays.asList(CargoCube.GREEN, CargoCube.BLUE));
        smugglers.setCurrState(CardState.HANDLE_CUBES_REWARD);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Aggiungiamo due Storage liberi sulla plancia
        Storage storage1 = new StandardStorage(connectors,2);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        Storage storage2 = new StandardStorage(connectors, 3);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(Arrays.asList(new Coordinates(6,7)))
                .build();

        smugglers.play(choices);

        // Verifica che gli Storage contengano i cubi assegnati
        assertTrue( storage1.getStockedCubes().contains(CargoCube.GREEN), "Il primo Storage dovrebbe contenere RED");
        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testHandleCubesRewardFlowWithoutChosenStorage() {
        // Prepara la carta
        smugglers.setReward(Arrays.asList(CargoCube.GREEN, CargoCube.BLUE));
        smugglers.setCurrState(CardState.HANDLE_CUBES_REWARD);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Aggiungiamo due Storage liberi sulla plancia
        Storage storage1 = new StandardStorage(connectors,2);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        Storage storage2 = new StandardStorage(connectors, 3);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(List.of())
                .build();

        smugglers.play(choices);

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testHandleCubesMalusFlow() {
        smugglers.setCurrState(CardState.HANDLE_CUBES_MALUS);

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


        smugglers.play(choices);

        // Verifica che il cubo di maggior valore (RED) sia stato rimosso
        assertEquals(1, storage1.getStockedCubes().size(), "Lo storage dovrebbe contenere un solo cubo dopo la rimozione");
        assertEquals(2, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndHeLost() {
        smugglers.setRequiredFirePower(5);
        smugglers.setCurrState(CardState.CHOOSE_CANNONS);

        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);

        shipBoard.getShipMatrix()[6][7] = cannon;
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        smugglers.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.HANDLE_CUBES_MALUS, smugglers.getCurrState());
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndHeWon() {
        smugglers.setRequiredFirePower(1);
        smugglers.setCurrState(CardState.CHOOSE_CANNONS);

        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);

        shipBoard.getShipMatrix()[6][7] = cannon;
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        smugglers.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.ACCEPT_THE_REWARD, smugglers.getCurrState());
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndTie() {
        smugglers.setRequiredFirePower(2);
        smugglers.setCurrState(CardState.CHOOSE_CANNONS);

        // Setup dei componenti sulla ShipBoard
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);

        shipBoard.getShipMatrix()[6][7] = cannon;
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        smugglers.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.END_OF_CARD, smugglers.getCurrState());
    }
}

