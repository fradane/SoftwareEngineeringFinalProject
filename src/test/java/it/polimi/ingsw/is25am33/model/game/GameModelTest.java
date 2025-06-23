package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.card.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.BigShot;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class GameModelTest{

    private GameModel gameModel;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel("1234",2,false){
            @Override
            public void setCurrGameState(GameState state) {

            }
        };
        gameModel.setGameClientNotifier(new GameClientNotifier(null, new ConcurrentHashMap<>()));
        gameModel.addPlayer("luca", PlayerColor.YELLOW,null);
        gameModel.addPlayer("marco", PlayerColor.BLUE,null);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());

    }

    @Test
    void resetPlayerIterator() {
        if(gameModel.getCurrPlayer()==null) {
            gameModel.resetPlayerIterator();
            assertEquals(gameModel.getCurrPlayer(), gameModel.getCurrRanking().get(0));
        }else if(gameModel.hasNextPlayer()){
            gameModel.nextPlayer();
            assertEquals(gameModel.getCurrPlayer(), gameModel.getCurrRanking().get(1));
        }else{
            gameModel.resetPlayerIterator();
            assertEquals(gameModel.getCurrPlayer(), gameModel.getCurrRanking().get(0));
        }

    }

    @Test
    void restartHourglass() throws RemoteException, NoSuchFieldException, IllegalAccessException {

        Field flipsLeftField = GameModel.class.getDeclaredField("flipsLeft");
        flipsLeftField.setAccessible(true);
        flipsLeftField.set(gameModel, 2);

        Field numClientsFinishedTimerField = GameModel.class.getDeclaredField("numClientsFinishedTimer");
        numClientsFinishedTimerField.setAccessible(true);
        numClientsFinishedTimerField.set(gameModel, 2);

        Field isRestartInProgressField = GameModel.class.getDeclaredField("isRestartInProgress");
        isRestartInProgressField.setAccessible(true);
        isRestartInProgressField.set(gameModel, false);

        gameModel.restartHourglass("luca");

        assertEquals(1, (Integer) flipsLeftField.get(gameModel));
        assertEquals(0, (Integer) numClientsFinishedTimerField.get(gameModel));
        assertFalse((Boolean)isRestartInProgressField.get(gameModel));

        flipsLeftField.set(gameModel, 0);
        isRestartInProgressField.set(gameModel, false);

        assertThrows(RemoteException.class, () -> {
            gameModel.restartHourglass("luca");
        });

        flipsLeftField.set(gameModel, 2);
        isRestartInProgressField.set(gameModel, true);

        assertThrows(RemoteException.class, () -> {
            gameModel.restartHourglass("luca");
        });
    }

    @Test
     void getPlayerWithPrettiestShip() {
        Player lucaPlayer = gameModel.getPlayers().get("luca");
        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        Storage storage = new StandardStorage(connectors,3);
        lucaPlayer.getPersonalBoard().setFocusedComponent(storage);
        lucaPlayer.getPersonalBoard().placeComponentWithFocus(6,7);

        assertEquals(1, gameModel.getPlayerWithPrettiestShip().size());
        assertTrue(gameModel.getPlayerWithPrettiestShip().contains(gameModel.getPlayers().get("marco")));

    }

    @Test
    void calculatePlayersCredits() {
        //TODO lo ha fatto marco
    }

    @Test
    void removePlayer() {
        gameModel.removePlayer("luca");
        assertEquals(1, gameModel.getPlayers().size());
        assertFalse(gameModel.getPlayers().containsKey("luca"));
    }

    @Test
    void notifyInvalidShipBoards() {
        Player lucaPlayer = gameModel.getPlayers().get("luca");
        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        Storage storage = new StandardStorage(connectors,3);
        lucaPlayer.getPersonalBoard().setFocusedComponent(storage);
        lucaPlayer.getPersonalBoard().placeComponentWithFocus(6,7);

        gameModel.notifyInvalidShipBoards();

        assertDoesNotThrow(() -> gameModel.notifyInvalidShipBoards());
    }

    @Test
    void notifyValidShipBoards() {
        Player lucaPlayer = gameModel.getPlayers().get("luca");
        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        Storage storage = new StandardStorage(connectors,3);
        lucaPlayer.getPersonalBoard().setFocusedComponent(storage);
        lucaPlayer.getPersonalBoard().placeComponentWithFocus(7,6);

        gameModel.notifyValidShipBoards();

        assertDoesNotThrow(() -> gameModel.notifyValidShipBoards());
    }

    @Test
    void areAllShipsCorrect() {

        assertTrue(gameModel.areAllShipsCorrect());

        Player lucaPlayer = gameModel.getPlayers().get("luca");
        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Engine engine = new Engine(connectors);
        lucaPlayer.getPersonalBoard().setFocusedComponent(engine);
        lucaPlayer.getPersonalBoard().placeComponentWithFocus(7,6);

        assertTrue(gameModel.areAllShipsCorrect());

    }

    @Test
    void checkAndTransitionToNextPhase() {
        gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
        gameModel.checkAndTransitionToNextPhase();

        assertEquals(GameState.SETUP, gameModel.getCurrGameState());
    }

    @Test
    void updateShipBoardAfterBeenHit() {
        AdventureCard card = new FreeSpace();
        gameModel.setCurrAdventureCard(card);
        card.setGame(gameModel);

        Player lucaPlayer = gameModel.getPlayers().get("luca");
        gameModel.setCurrPlayer(lucaPlayer);
        DangerousObj currDangerousObj = new BigShot(Direction.NORTH);
        currDangerousObj.setCoordinates(6);
        gameModel.setCurrDangerousObj(currDangerousObj);
        gameModel.updateShipBoardAfterBeenHit();

        assertTrue(lucaPlayer.getPersonalBoard().getIncorrectlyPositionedComponentsCoordinates().contains(new Coordinates(6, 6)));

    }

    @Test
    void handleCrewPlacementPhase() {

        GameModel flightTest = new GameModel("1234",2,true){
            @Override
            public void setCurrGameState(GameState state) {
                if (state == GameState.CREATE_DECK) {
                    // NON CHIAMARE SUPER, NON CREARE IL DECK
                    return;
                }
            }
        };
        flightTest.setGameClientNotifier(new GameClientNotifier(null, new ConcurrentHashMap<>()));
        flightTest.addPlayer("luca", PlayerColor.YELLOW,null);
        flightTest.addPlayer("marco", PlayerColor.BLUE,null);
        flightTest.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        flightTest.handleCrewPlacementPhase();

        Map<Direction,ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        Cabin cabin = new Cabin(connectors);
        Player lucaPlayer = flightTest.getPlayers().get("luca");
        lucaPlayer.getPersonalBoard().setFocusedComponent(cabin);
        lucaPlayer.getPersonalBoard().placeComponentWithFocus(7 ,6);

        assertEquals(GameState.SETUP, gameModel.getCurrGameState());
        gameModel.handleCrewPlacementPhase();
        assertNotEquals(GameState.CREATE_DECK, gameModel.getCurrGameState());

    }

    @Test
    void testHourglassEnded() throws NoSuchFieldException, IllegalAccessException {

        Field numClientsFinishedTimerField = GameModel.class.getDeclaredField("numClientsFinishedTimer");
        Field flipsLeftField = GameModel.class.getDeclaredField("flipsLeft");
        Field isRestartInProgressField = GameModel.class.getDeclaredField("isRestartInProgress");
        Field maxPlayersField = GameModel.class.getDeclaredField("maxPlayers");

        numClientsFinishedTimerField.setAccessible(true);
        flipsLeftField.setAccessible(true);
        isRestartInProgressField.setAccessible(true);
        maxPlayersField.setAccessible(true);

        flipsLeftField.set(gameModel, 1);
        isRestartInProgressField.set(gameModel, false);
        numClientsFinishedTimerField.set(gameModel, 0);
        maxPlayersField.set(gameModel, 2);

        gameModel.hourglassEnded();

        int numClients = (int) numClientsFinishedTimerField.get(gameModel);
        assertEquals(1, numClients);

        flipsLeftField.set(gameModel, 0);
        isRestartInProgressField.set(gameModel, false);
        numClientsFinishedTimerField.set(gameModel, gameModel.getPlayers().size());
        gameModel.hourglassEnded();

        boolean isRestart = (boolean) isRestartInProgressField.get(gameModel);
        assertTrue(isRestart);

    }

    @Test
    void testMarkCrewPlacementCompleted() throws Exception {
        Field crewPlacementCompletedField = GameModel.class.getDeclaredField("crewPlacementCompleted");
        crewPlacementCompletedField.setAccessible(true);

        Map<String, Boolean> crewPlacementCompleted = new HashMap<>();
        crewPlacementCompleted.put("luca", false);
        crewPlacementCompleted.put("marco", false);
        crewPlacementCompletedField.set(gameModel, crewPlacementCompleted);

        gameModel.markCrewPlacementCompleted("luca");
        Map<String, Boolean> mapAfterCall = (Map<String, Boolean>) crewPlacementCompletedField.get(gameModel);
        assertTrue(mapAfterCall.get("luca"));
        assertFalse(mapAfterCall.get("marco"));

        gameModel.markCrewPlacementCompleted("marco");

        mapAfterCall = (Map<String, Boolean>) crewPlacementCompletedField.get(gameModel);
        assertTrue(mapAfterCall.get("luca"));
        assertTrue(mapAfterCall.get("marco"));

        assertEquals(GameState.SETUP, gameModel.getCurrGameState());
    }

}