package it.polimi.ingsw.is25am33.model.card;

import static org.junit.jupiter.api.Assertions.*;

import it.polimi.ingsw.is25am33.model.dangerousObj.BigMeteorite;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.SmallMeteorite;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.*;
import it.polimi.ingsw.is25am33.client.model.card.*;

class MeteoriteStormTest {

    private GameModel gameModel;
    private MeteoriteStorm meteoriteStorm;
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
        meteoriteStorm = new MeteoriteStorm();
        meteoriteStorm.setMeteorites(List.of(new SmallMeteorite(Direction.NORTH), new BigMeteorite(Direction.NORTH)));
        meteoriteStorm.setGame(gameModel);

        // Inizializzazione giocatore
        Player player = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        gameModel.getPlayers().put("luca", player);
        gameModel.setCurrPlayer(player);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().insertPlayer(player);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(meteoriteStorm);

    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.THROW_DICES, meteoriteStorm.getFirstState());
    }

    @Test
    void testPlayUnknownStateException() {
        meteoriteStorm.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class, () -> meteoriteStorm.play(new PlayerChoicesDataStructure()));
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = meteoriteStorm.toClientCard();
        assertTrue(clientCard instanceof ClientMeteoriteStorm);
    }

    @Test
    void testPlayerDecidedHowToDefendTheirSelvesFromSmallMeteoriteDestroysIfNotProtected() {
        // Posizioniamo un meteorite sulla ShipBoard e NON mettiamo scudo
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Engine engine = new Engine(connectors);
        shipBoard.getShipMatrix()[6][7] = engine;

        // Simuliamo un meteorite sulla coordinata
        DangerousObj meteorite = new SmallMeteorite(Direction.NORTH);
        meteorite.setCoordinates(7);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of())
                .setChosenShield(List.of())
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifichiamo che la casella sulla ShipBoard NON contenga più l'engine
        assertTrue(shipBoard.getIncorrectlyPositionedComponentsCoordinates().contains(new Coordinates(6, 7)));
    }

    @Test
    void testPlayerDecidedHowToDefendTheirSelvesFromSmallMeteoriteUsesShieldAndBattery() {
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates shieldCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Shield shield = new Shield(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[6][7] = shield;
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        DangerousObj meteorite = new SmallMeteorite(Direction.NORTH);
        meteorite.setCoordinates(7);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenShield(List.of(shieldCoord))
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifica che la batteria si sia scaricata
        assertEquals(1, ((BatteryBox) shipBoard.getShipMatrix()[7][6]).getRemainingBatteries());

        // Verifica che la carta NON si chiuda prematuramente
        assertNotEquals(CardState.END_OF_CARD, meteoriteStorm.getCurrState());
    }

    @Test
    void testSmallMeteoriteDontHitTheshipIfShieldIsUsedAndBatteryIsUsed() {
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates shieldCoord = new Coordinates(6, 7);
        Coordinates batteryCoord = new Coordinates(7, 6);

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Shield shield = new Shield(connectors);
        BatteryBox batteryBox = new BatteryBox(connectors,2);
        shipBoard.getShipMatrix()[6][7] = shield;
        shipBoard.getShipMatrix()[7][6] = batteryBox;

        DangerousObj meteorite = new SmallMeteorite(Direction.NORTH);
        meteorite.setCoordinates(9);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenShield(List.of(shieldCoord))
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifica che la batteria si sia scaricata
        assertEquals(1, ((BatteryBox) shipBoard.getShipMatrix()[7][6]).getRemainingBatteries());

        // Verifica che la carta NON si chiuda prematuramente
        assertNotEquals(CardState.END_OF_CARD, meteoriteStorm.getCurrState());
    }

    @Test
    void testPlayerDecidedHowToDefendTheirSelvesFromBigMeteoriteDestroysIfNotProtected() {
        // Posizioniamo un meteorite sulla ShipBoard e NON mettiamo cannoni
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Engine engine = new Engine(connectors);
        shipBoard.getShipMatrix()[6][7] = engine;

        // Simuliamo un meteorite sulla coordinata
        DangerousObj meteorite = new BigMeteorite(Direction.NORTH);
        meteorite.setCoordinates(7);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of())
                .setChosenDoubleCannons(List.of())
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifichiamo che la casella sulla ShipBoard NON contenga più l'engine
        assertTrue(shipBoard.getIncorrectlyPositionedComponentsCoordinates().contains(new Coordinates(6, 7)));
    }

    @Test
    void testPlayerDecidedHowToDefendTheirSelvesFromBigMeteoriteUsesCannonAndBattery() {
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates doubleCannonCoord = new Coordinates(6, 7);
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

        DangerousObj meteorite = new BigMeteorite(Direction.NORTH);
        meteorite.setCoordinates(7);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(doubleCannonCoord))
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifica che la batteria si sia scaricata
        assertEquals(1, ((BatteryBox) shipBoard.getShipMatrix()[7][6]).getRemainingBatteries());

        assertNotEquals(CardState.END_OF_CARD, meteoriteStorm.getCurrState());
    }

    @Test
    void testBigMeteoirteDontHitTheshipIfBatteryIsUsedAndCannonIsUsed() {
        meteoriteStorm.setCurrState(CardState.DANGEROUS_ATTACK);
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Coordinates doubleCannonCoord = new Coordinates(6, 7);
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

        DangerousObj meteorite = new BigMeteorite(Direction.NORTH);
        meteorite.setCoordinates(8);
        gameModel.setCurrDangerousObj(meteorite);

        PlayerChoicesDataStructure playerChoicesDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(List.of(batteryCoord))
                .setChosenDoubleCannons(List.of(doubleCannonCoord))
                .build();

        meteoriteStorm.play(playerChoicesDataStructure);

        // Verifica che la batteria si sia scaricata
        assertEquals(1, ((BatteryBox) shipBoard.getShipMatrix()[7][6]).getRemainingBatteries());

        assertNotEquals(CardState.END_OF_CARD, meteoriteStorm.getCurrState());
    }

    @Test
    void testCheckShipBoardAfterAttackHasNextMeteorite() {
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(meteoriteStorm);
        meteoriteStorm.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        meteoriteStorm.setMeteorites(List.of(new SmallMeteorite(Direction.NORTH), new BigMeteorite(Direction.NORTH)));

        meteoriteStorm.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.THROW_DICES, meteoriteStorm.getCurrState());
    }

    @Test
    void testCheckShipBoardAfterAttackHasNextPlayer() {
        Player player1 = new Player("fra", new Level2ShipBoard(PlayerColor.RED, gameClientNotifier, false), PlayerColor.RED);
        gameModel.addPlayer("fra",PlayerColor.RED,null);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(meteoriteStorm);
        meteoriteStorm.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        meteoriteStorm.setMeteorites(List.of(new SmallMeteorite(Direction.NORTH), new BigMeteorite(Direction.NORTH)));

        meteoriteStorm.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.DANGEROUS_ATTACK, meteoriteStorm.getCurrState());
    }

    @Test
    void testCheckShipBoardAfterAttackEndCard() {
        gameModel.resetPlayerIterator();
        gameModel.setCurrAdventureCard(meteoriteStorm);
        meteoriteStorm.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        meteoriteStorm.setMeteorites(List.of());

        meteoriteStorm.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.END_OF_CARD, meteoriteStorm.getCurrState());
    }


}
