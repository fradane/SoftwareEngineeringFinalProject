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

public class SlaveTradersTest {

    private GameModel gameModel;
    private GameClientNotifier gameClientNotifier;
    private SlaveTraders slaveTraders;
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

        slaveTraders = new SlaveTraders();
        slaveTraders.setGame(gameModel);
        gameModel.setCurrAdventureCard(slaveTraders);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_CANNONS, slaveTraders.getFirstState());
    }

    @Test
    void testAcceptTheRewardFlow() {
        slaveTraders.setStepsBack(-4);
        slaveTraders.setCurrState(CardState.ACCEPT_THE_REWARD);
        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(true)
                .build();
        slaveTraders.play(choices);
        assertEquals(2,gameModel.getFlyingBoard().getPlayerPosition(player1));
        assertEquals(CardState.END_OF_CARD, slaveTraders.getCurrState());
    }

    @Test
    void testDeclineTheRewardFlow() {
        slaveTraders.setCurrState(CardState.ACCEPT_THE_REWARD);
        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(false)
                .build();

        slaveTraders.play(choices);
        assertEquals(6,gameModel.getFlyingBoard().getPlayerPosition(player1));
        assertEquals(CardState.END_OF_CARD, slaveTraders.getCurrState(),
                "Stato dovrebbe passare ad END_OF_CARD quando si rifiuta la ricompensa");
    }

    @Test
    void testUnknownStateException() {
        slaveTraders.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class,
                () -> slaveTraders.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe sollevare UnknownStateException per stati sconosciuti");
    }

    @Test
    void testToClientCard() {
        ClientCard card = slaveTraders.toClientCard();
        assertTrue(card instanceof ClientSlaveTraders);
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndHeLost() {
        slaveTraders.setRequiredFirePower(5);
        slaveTraders.setCurrState(CardState.CHOOSE_CANNONS);

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

        slaveTraders.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.REMOVE_CREW_MEMBERS, slaveTraders.getCurrState());
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndHeWon() {
        slaveTraders.setRequiredFirePower(1);
        slaveTraders.setCurrState(CardState.CHOOSE_CANNONS);

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

        slaveTraders.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.ACCEPT_THE_REWARD, slaveTraders.getCurrState());
    }

    @Test
    void testCurrPlayerChoseCannonToActivateAndTie() {
        slaveTraders.setRequiredFirePower(2);
        slaveTraders.setCurrState(CardState.CHOOSE_CANNONS);

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

        slaveTraders.play(playerChoicesDataStructure);

        assertEquals(1, batteryBox.getRemainingBatteries(), "La battery box dovrebbe risultare usata");
        assertEquals(CardState.END_OF_CARD, slaveTraders.getCurrState());
    }

    @Test
    void testHandleCrewMalusFlow() {
        slaveTraders.setCurrState(CardState.REMOVE_CREW_MEMBERS);

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


        slaveTraders.play(choices);

        assertEquals(1, cabin1.getInhabitants().size());
        assertEquals(1, cabin2.getInhabitants().size());

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, slaveTraders.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

}

