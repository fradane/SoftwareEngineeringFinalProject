package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientMeteoriteStorm;
import it.polimi.ingsw.is25am33.client.model.card.ClientPirates;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class PiratesTest {
    private GameModel gameModel;
    private Pirates pirates;
    private GameClientNotifier gameClientNotifier;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel("1234", 2, false) {
            @Override
            public void setCurrGameState(GameState state) {
            }
        };
        gameClientNotifier = new GameClientNotifier(gameModel, new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        pirates = new Pirates();
        pirates.setGame(gameModel);
        pirates.setRequiredFirePower(1);
        pirates.setShots(List.of());

        // Inizializzazione giocatore
        Player player = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        player.setGameClientNotifier(gameClientNotifier);
        gameModel.getPlayers().put("luca", player);
        gameModel.setCurrPlayer(player);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().insertPlayer(player);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(pirates);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_CANNONS, pirates.getFirstState());
    }

    @Test
    void testPlayUnknownStateException() {
        pirates.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class, () -> pirates.play(new PlayerChoicesDataStructure()));
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = pirates.toClientCard();
        assertTrue(clientCard instanceof ClientPirates);
    }

    @Test
    void testPlayerDefeatPirates() {
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        assertEquals(CardState.ACCEPT_THE_REWARD, pirates.getCurrState());
    }

    @Test
    void testPlayerNotDefeatPirates() {
        pirates.setRequiredFirePower(3);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        assertEquals(CardState.THROW_DICES, pirates.getCurrState());
    }

    @Test
    void testPlayerNotDefeatPiratesWithNextPlayer() {
        Player player1 = new Player("fra", new Level2ShipBoard(PlayerColor.RED, gameClientNotifier, false), PlayerColor.RED);
        gameModel.addPlayer("fra",PlayerColor.RED,null);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        pirates.setRequiredFirePower(3);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        assertEquals(CardState.CHOOSE_CANNONS, pirates.getCurrState());
    }

    @Test
    void testPlayerTiePiratesWithNextPlayer() {
        Player player1 = new Player("fra", new Level2ShipBoard(PlayerColor.RED, gameClientNotifier, false), PlayerColor.RED);
        gameModel.addPlayer("fra",PlayerColor.RED,null);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        pirates.setRequiredFirePower(2);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        assertEquals(CardState.CHOOSE_CANNONS, pirates.getCurrState());
    }

    @Test
    void testPlayerTiePirates() {
        pirates.setRequiredFirePower(2);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        assertEquals(CardState.END_OF_CARD, pirates.getCurrState());
    }

    @Test
    void testPlayerAcceptRewardAfterTheyDefeatAPlayer() {
        pirates.setRequiredFirePower(3);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        pirates.setCurrState(CardState.ACCEPT_THE_REWARD);

        PlayerChoicesDataStructure playerChoicesDataStructure1 = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(true)
                .build();

        pirates.play(playerChoicesDataStructure1);

        assertEquals(CardState.THROW_DICES, pirates.getCurrState());

    }

    @Test
    void testPlayerAcceptReward() {
        pirates.setRequiredFirePower(1);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        pirates.setCurrState(CardState.ACCEPT_THE_REWARD);

        PlayerChoicesDataStructure playerChoicesDataStructure1 = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(true)
                .build();

        pirates.play(playerChoicesDataStructure1);

        assertEquals(CardState.END_OF_CARD, pirates.getCurrState());

    }

    @Test
    void testCheckShipBoardAfterAttackHasNextShot() {
        pirates.setRequiredFirePower(5);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        pirates.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        pirates.setShots(List.of(new SmallShot(Direction.NORTH), new BigShot(Direction.NORTH)));
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(pirates);
        pirates.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.THROW_DICES, pirates.getCurrState());
    }

    @Test
    void testCheckShipBoardAfterAttackEndedCard() {
        pirates.setRequiredFirePower(5);
        pirates.setCurrState(CardState.CHOOSE_CANNONS);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates cannonCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cannon cannon = new DoubleCannon(connectors);
        shipBoard.getShipMatrix()[6][7] = cannon;
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(cannonCoord))
                .build();

        pirates.play(playerChoicesDataStructure);

        pirates.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        pirates.setShots(List.of());
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(pirates);
        pirates.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.END_OF_CARD, pirates.getCurrState());
    }


}
