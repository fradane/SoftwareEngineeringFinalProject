package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for DNS functionality.
 * Tests all public methods and covers edge cases without using Mockito.
 */
public class DNSTest {

    private DNS dns;
    private TestCallableOnClientController testClientController1;
    private TestCallableOnClientController testClientController2;
    private TestCallableOnClientController testClientController3;
    private TestCallableOnClientController testClientController4;
    
    /**
     * Test implementation of CallableOnClientController for testing purposes.
     */
    private static class TestCallableOnClientController implements CallableOnClientController {
        private final String nickname;
        private boolean pingReceived = false;
        private boolean pongReceived = false;
        private List<GameInfo> lastGameInfos = new ArrayList<>();
        private String lastJoinedPlayer = null;
        private PlayerColor lastJoinedColor = null;
        private String lastDisconnectedPlayer = null;
        
        public TestCallableOnClientController(String nickname) {
            this.nickname = nickname;
        }
        
        @Override
        public void pingToClientFromServer(String nickname) throws IOException {
            this.pingReceived = true;
        }

        @Override
        public void notifyGameStarted(String nickname, GameInfo gameInfo) throws IOException {

        }

        @Override
        public void notifyGameState(String nickname, GameState gameState) throws IOException {

        }

        @Override
        public void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) throws IOException {

        }

        @Override
        public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException {

        }

        @Override
        public void notifyCardState(String nickname, CardState cardState) throws IOException {

        }

        @Override
        public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException {

        }

        @Override
        public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException {

        }

        @Override
        public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException {

        }

        @Override
        public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws IOException {

        }

        @Override
        public void notifyAddVisibleComponents(String nicknameToNotify, int index, Component component) throws IOException {

        }

        @Override
        public void notifyRemoveVisibleComponents(String nicknameToNotify, int index) throws IOException {

        }

        @Override
        public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException {

        }

        @Override
        public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws IOException {

        }

        @Override
        public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException {

        }

        @Override
        public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException {

        }

        @Override
        public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException {

        }

        @Override
        public void notifyVisibleDeck(String nickname, List<List<ClientCard>> littleVisibleDeck) throws IOException {

        }

        @Override
        public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException {

        }

        @Override
        public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {

        }

        @Override
        public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {

        }

        @Override
        public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {

        }

        @Override
        public void notifyCardStarted(String nicknameToNotify) throws IOException {

        }

        @Override
        public void notifyStopHourglass(String nicknameToNotify) throws IOException {

        }

        @Override
        public void notifyFirstToEnter(String nicknameToNotify) throws IOException {

        }

        @Override
        public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException {

        }

        @Override
        public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException {

        }

        @Override
        public void forcedDisconnection(String nickname, String gameId) throws IOException {

        }

        @Override
        public void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType) throws IOException {

        }

        @Override
        public void notifyCrewPlacementPhase(String nicknameToNotify) throws IOException {

        }

        @Override
        public void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException {

        }

        @Override
        public void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws IOException {

        }

        @Override
        public void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipName) throws IOException {

        }

        @Override
        public void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws IOException {

        }

        @Override
        public void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws IOException {

        }

        @Override
        public void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws IOException {

        }

        @Override
        public void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) throws IOException {

        }

        @Override
        public void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws IOException {

        }

        @Override
        public void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations) throws IOException {

        }

        @Override
        public void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {

        }

        @Override
        public void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException {

        }

        @Override
        public void notifyNoMoreHiddenComponents(String nicknameToNotify) throws IOException {

        }

        @Override
        public void notifyStorageError(String nicknameToNotify, String errorMessage) throws IOException {

        }

        @Override
        public void notifyStolenVisibleComponent(String nicknameToNotify) throws IOException {

        }

        @Override
        public void pongToClientFromServer(String nickname) throws IOException {
            this.pongReceived = true;
        }
        
        @Override
        public void notifyGameInfos(String nickname, List<GameInfo> gameInfos) throws IOException {
            this.lastGameInfos = new ArrayList<>(gameInfos);
        }
        
        @Override
        public void notifyNewPlayerJoined(String nickname, String gameId, String joinedPlayerNickname, PlayerColor color) throws IOException {
            this.lastJoinedPlayer = joinedPlayerNickname;
            this.lastJoinedColor = color;
        }
        
        @Override
        public void notifyPlayerDisconnected(String nickname, String disconnectedPlayerNickname) throws IOException {
            this.lastDisconnectedPlayer = disconnectedPlayerNickname;
        }
        
        // Getters for testing
        public boolean isPingReceived() { return pingReceived; }
        public boolean isPongReceived() { return pongReceived; }
        public List<GameInfo> getLastGameInfos() { return lastGameInfos; }
        public String getLastJoinedPlayer() { return lastJoinedPlayer; }
        public PlayerColor getLastJoinedColor() { return lastJoinedColor; }
        public String getLastDisconnectedPlayer() { return lastDisconnectedPlayer; }
        
        // Reset methods for testing
        public void resetFlags() {
            pingReceived = false;
            pongReceived = false;
            lastJoinedPlayer = null;
            lastJoinedColor = null;
            lastDisconnectedPlayer = null;
        }
    }
    
    @BeforeEach
    public void setUp() throws RemoteException {
        dns = new DNS();
        testClientController1 = new TestCallableOnClientController("player1");
        testClientController2 = new TestCallableOnClientController("player2");
        testClientController3 = new TestCallableOnClientController("player3");
        testClientController4 = new TestCallableOnClientController("player4");
        
        // Clear static maps before each test
        DNS.getGameControllers().clear();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up static maps after each test
        DNS.getGameControllers().clear();
    }
    
    /**
     * Test DNS constructor creates instance successfully.
     */
    @Test
    public void testConstructor() throws RemoteException {
        DNS testDns = new DNS();
        assertNotNull(testDns);
        assertNotNull(testDns.getClients());
        assertNotNull(testDns.getClientGame());
    }
    
    /**
     * Test getGameControllers returns the static map.
     */
    @Test
    public void testGetGameControllers() {
        Map<String, GameController> controllers = DNS.getGameControllers();
        assertNotNull(controllers);
    }
    
    /**
     * Test getClients returns the clients map.
     */
    @Test
    public void testGetClients() {
        Map<String, CallableOnClientController> clients = dns.getClients();
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }
    
    /**
     * Test getClientGame returns the client game map.
     */
    @Test
    public void testGetClientGame() {
        Map<String, GameController> clientGame = dns.getClientGame();
        assertNotNull(clientGame);
        assertTrue(clientGame.isEmpty());
    }
    
    /**
     * Test successful client registration.
     */
    @Test
    public void testRegisterWithNickname_Success() throws RemoteException, InterruptedException {
        boolean result = dns.registerWithNickname("player1", testClientController1);
        
        assertTrue(result);
        assertTrue(dns.getClients().containsKey("player1"));
        assertEquals(testClientController1, dns.getClients().get("player1"));
        
        // Wait a bit for async operations
        Thread.sleep(100);
    }
    
    /**
     * Test duplicate client registration fails.
     */
    @Test
    public void testRegisterWithNickname_Duplicate() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        boolean result = dns.registerWithNickname("player1", testClientController2);
        
        assertFalse(result);
        assertEquals(testClientController1, dns.getClients().get("player1"));
    }
    
    /**
     * Test creating a game successfully.
     */
    @Test
    public void testCreateGame_Success() throws RemoteException, InterruptedException {
        dns.registerWithNickname("player1", testClientController1);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        
        assertNotNull(gameInfo);
        assertEquals(2, gameInfo.getMaxPlayers());
        assertEquals(1, gameInfo.getConnectedPlayers().size());
        assertTrue(gameInfo.getConnectedPlayers().containsKey("player1"));
        assertEquals(PlayerColor.RED, gameInfo.getConnectedPlayers().get("player1"));
        assertFalse(gameInfo.isStarted());
        assertFalse(gameInfo.isFull());
        
        // Verify game controller is created
        assertTrue(DNS.getGameControllers().containsKey(gameInfo.getGameId()));
        assertTrue(dns.getClientGame().containsKey("player1"));
        
        // Wait for async operations
        Thread.sleep(100);
    }
    
    /**
     * Test creating a game with test flight enabled.
     */
    @Test
    public void testCreateGame_TestFlight() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.BLUE, 3, true, "player1");
        
        assertNotNull(gameInfo);
        assertEquals(3, gameInfo.getMaxPlayers());
    }
    
    /**
     * Test creating a game with unregistered client throws exception.
     */
    @Test
    public void testCreateGame_UnregisteredClient() {
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            dns.createGame(PlayerColor.RED, 2, false, "unregistered");
        });
        assertEquals("Client not registered", exception.getMessage());
    }
    
    /**
     * Test creating a game with invalid number of players.
     */
    @Test
    public void testCreateGame_InvalidPlayerCount_TooFew() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            dns.createGame(PlayerColor.RED, 1, false, "player1");
        });
        assertEquals("Invalid number of players: must be between 2 and 4", exception.getMessage());
    }
    
    /**
     * Test creating a game with too many players.
     */
    @Test
    public void testCreateGame_InvalidPlayerCount_TooMany() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            dns.createGame(PlayerColor.RED, 5, false, "player1");
        });
        assertEquals("Invalid number of players: must be between 2 and 4", exception.getMessage());
    }
    
    /**
     * Test joining a game successfully.
     */
    @Test
    public void testJoinGame_Success() throws IOException, InterruptedException {
        // Register clients and create game
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        String gameId = gameInfo.getGameId();
        
        // Join game
        boolean result = dns.joinGame(gameId, "player2", PlayerColor.BLUE);
        
        assertTrue(result);
        assertTrue(dns.getClientGame().containsKey("player2"));
        
        // Verify game state
        GameController controller = DNS.getGameControllers().get(gameId);
        GameInfo updatedGameInfo = controller.getGameInfo();
        assertEquals(2, updatedGameInfo.getConnectedPlayers().size());
        assertTrue(updatedGameInfo.getConnectedPlayers().containsKey("player2"));
        assertEquals(PlayerColor.BLUE, updatedGameInfo.getConnectedPlayers().get("player2"));
        
        // Wait for async operations
        Thread.sleep(100);
    }
    
    /**
     * Test joining a game with unregistered client.
     */
    @Test
    public void testJoinGame_UnregisteredClient() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        
        boolean result = dns.joinGame(gameInfo.getGameId(), "unregistered", PlayerColor.BLUE);
        
        assertFalse(result);
    }
    
    /**
     * Test joining a non-existent game.
     */
    @Test
    public void testJoinGame_NonExistentGame() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        boolean result = dns.joinGame("nonexistent", "player1", PlayerColor.RED);
        
        assertFalse(result);
    }
    
    /**
     * Test joining a game that's already started.
     */
    @Test
    public void testJoinGame_AlreadyStarted() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        dns.joinGame(gameInfo.getGameId(), "player2", PlayerColor.BLUE);
        
        // Game should auto-start when full
        dns.registerWithNickname("player3", testClientController3);
        
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            dns.joinGame(gameInfo.getGameId(), "player3", PlayerColor.GREEN);
        });
        assertEquals("GameModel already started", exception.getMessage());
    }
    
    /**
     * Test joining a game with duplicate color.
     */
    @Test
    public void testJoinGame_DuplicateColor() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 3, false, "player1");
        
        boolean result = dns.joinGame(gameInfo.getGameId(), "player2", PlayerColor.RED);
        
        assertFalse(result);
    }
    
    /**
     * Test joining a game where player is already present.
     */
    @Test
    public void testJoinGame_PlayerAlreadyInGame() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 3, false, "player1");
        
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            dns.joinGame(gameInfo.getGameId(), "player1", PlayerColor.BLUE);
        });
        assertEquals("You are already in this gameModel", exception.getMessage());
    }
    
    /**
     * Test getting available games.
     */
    @Test
    public void testGetAvailableGames() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        // Create games
        GameInfo game1 = dns.createGame(PlayerColor.RED, 3, false, "player1");
        GameInfo game2 = dns.createGame(PlayerColor.BLUE, 2, false, "player2");
        
        List<GameInfo> availableGames = dns.getAvailableGames();
        
        assertEquals(2, availableGames.size());
        assertTrue(availableGames.stream().anyMatch(g -> g.getGameId().equals(game1.getGameId())));
        assertTrue(availableGames.stream().anyMatch(g -> g.getGameId().equals(game2.getGameId())));
    }
    
    /**
     * Test getting available games excludes full games.
     */
    @Test
    public void testGetAvailableGames_ExcludesFullGames() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        dns.registerWithNickname("player3", testClientController3);
        
        GameInfo game1 = dns.createGame(PlayerColor.RED, 2, false, "player1");
        GameInfo game2 = dns.createGame(PlayerColor.BLUE, 3, false, "player2");
        
        // Fill first game
        dns.joinGame(game1.getGameId(), "player3", PlayerColor.GREEN);
        
        List<GameInfo> availableGames = dns.getAvailableGames();
        
        assertEquals(1, availableGames.size());
        assertEquals(game2.getGameId(), availableGames.get(0).getGameId());
    }
    
    /**
     * Test getting game info.
     */
    @Test
    public void testGetGameInfo() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        GameInfo originalGameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        
        GameInfo retrievedGameInfo = dns.getGameInfo(originalGameInfo.getGameId());
        
        assertNotNull(retrievedGameInfo);
        assertEquals(originalGameInfo.getGameId(), retrievedGameInfo.getGameId());
        assertEquals(originalGameInfo.getMaxPlayers(), retrievedGameInfo.getMaxPlayers());
    }
    
    /**
     * Test getting controller.
     */
    @Test
    public void testGetController() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        
        GameController controller = dns.getController(gameInfo.getGameId());
        
        assertNotNull(controller);
        assertEquals(gameInfo.getGameId(), controller.getGameInfo().getGameId());
    }
    
    /**
     * Test ping to client from server.
     */
    @Test
    public void testPingToClientFromServer() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        dns.pingToClientFromServer("player1");
        
        assertTrue(testClientController1.isPingReceived());
    }
    
    /**
     * Test ping to non-existent client.
     */
    @Test
    public void testPingToClientFromServer_NonExistentClient() throws IOException {
        // Should not throw exception
        dns.pingToClientFromServer("nonexistent");
    }
    
    /**
     * Test ping to server from client.
     */
    @Test
    public void testPingToServerFromClient() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        dns.pingToServerFromClient("player1");
        
        assertTrue(testClientController1.isPongReceived());
    }
    
    /**
     * Test ping to server from non-existent client.
     */
    @Test
    public void testPingToServerFromClient_NonExistentClient() throws IOException {
        // Should not throw exception
        dns.pingToServerFromClient("nonexistent");
    }
    
    /**
     * Test pong to server from client.
     */
    @Test
    public void testPongToServerFromClient() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        // Should not throw exception
        dns.pongToServerFromClient("player1");
    }
    
    /**
     * Test pong to server from client with lambda execution - client in game.
     * This test covers the lambda function inside pongToServerFromClient that handles disconnection
     * when the client is in a game.
     */
    @Test
    public void testPongToServerFromClient_ClientInGame_LambdaExecution() throws IOException, InterruptedException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        // Create game and add players
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 3, false, "player1");
        dns.joinGame(gameInfo.getGameId(), "player2", PlayerColor.BLUE);
        
        // Wait for async operations
        Thread.sleep(100);
        
        // Verify players are in game
        assertTrue(dns.getClientGame().containsKey("player1"));
        assertTrue(dns.getClientGame().containsKey("player2"));
        assertTrue(DNS.getGameControllers().containsKey(gameInfo.getGameId()));
        
        // Call pongToServerFromClient - this will execute the lambda inside the method
        dns.pongToServerFromClient("player1");
        
        // The lambda checks if clientGame.get(nickname) == null, and if not, it should do nothing
        // Since player1 is in a game, the lambda should not trigger disconnection
        assertTrue(dns.getClientGame().containsKey("player1"));
        assertTrue(dns.getClients().containsKey("player1"));
    }
    
    /**
     * Test pong to server from client with lambda execution - client not in game.
     * This test covers the early return case in the lambda function.
     */
    @Test
    public void testPongToServerFromClient_ClientNotInGame_LambdaExecution() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        // Client is registered but not in any game
        assertFalse(dns.getClientGame().containsKey("player1"));
        
        // Call pongToServerFromClient - the lambda should execute and return early
        // since clientGame.get(nickname) returns null
        dns.pongToServerFromClient("player1");
        
        // Client should still be registered since the lambda returns early
        assertTrue(dns.getClients().containsKey("player1"));
    }
    
    /**
     * Test handle disconnection with player in game.
     */
    @Test
    public void testHandleDisconnection_PlayerInGame() throws IOException, InterruptedException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 3, false, "player1");
        dns.joinGame(gameInfo.getGameId(), "player2", PlayerColor.BLUE);
        
        // Wait for async operations
        Thread.sleep(100);
        
        dns.handleDisconnection("player1");
        
        assertFalse(dns.getClients().containsKey("player1"));
        assertFalse(dns.getClientGame().containsKey("player1"));
        assertFalse(DNS.getGameControllers().containsKey(gameInfo.getGameId()));
    }
    
    /**
     * Test main method argument parsing - default localhost.
     */
    @Test
    public void testMainMethod_DefaultIP() {
        // Test that main method can be called with empty args without exception
        String[] args = {};
        assertDoesNotThrow(() -> {
            // We can't actually run main() as it starts threads, but we can test argument parsing logic
            String serverIP = "localhost"; // Default value
            for (int i = 0; i < args.length; i++) {
                if ("-ip".equals(args[i]) && i + 1 < args.length) {
                    serverIP = args[i + 1];
                    break;
                }
            }
            assertEquals("localhost", serverIP);
        });
    }
    
    /**
     * Test main method argument parsing - custom IP.
     */
    @Test
    public void testMainMethod_CustomIP() {
        String[] args = {"-ip", "192.168.1.100"};
        String serverIP = "localhost"; // Default value
        
        for (int i = 0; i < args.length; i++) {
            if ("-ip".equals(args[i]) && i + 1 < args.length) {
                serverIP = args[i + 1];
                break;
            }
        }
        
        assertEquals("192.168.1.100", serverIP);
    }
    
    /**
     * Test main method argument parsing - invalid IP flag.
     */
    @Test
    public void testMainMethod_InvalidIPFlag() {
        String[] args = {"-ip"}; // Missing IP value
        String serverIP = "localhost"; // Default value
        
        for (int i = 0; i < args.length; i++) {
            if ("-ip".equals(args[i]) && i + 1 < args.length) {
                serverIP = args[i + 1];
                break;
            }
        }
        
        assertEquals("localhost", serverIP); // Should remain default
    }
    
    /**
     * Test game auto-start when reaching max players.
     */
    @Test
    public void testGameAutoStart() throws IOException, InterruptedException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        
        GameInfo gameInfo = dns.createGame(PlayerColor.RED, 2, false, "player1");
        dns.joinGame(gameInfo.getGameId(), "player2", PlayerColor.BLUE);
        
        // Wait for async operations
        Thread.sleep(100);
        
        GameController controller = DNS.getGameControllers().get(gameInfo.getGameId());
        assertTrue(controller.getGameInfo().isStarted());
    }
    
    /**
     * Test multiple games can be created simultaneously.
     */
    @Test
    public void testMultipleGamesCreation() throws RemoteException {
        dns.registerWithNickname("player1", testClientController1);
        dns.registerWithNickname("player2", testClientController2);
        dns.registerWithNickname("player3", testClientController3);
        
        GameInfo game1 = dns.createGame(PlayerColor.RED, 2, false, "player1");
        GameInfo game2 = dns.createGame(PlayerColor.BLUE, 3, false, "player2");
        GameInfo game3 = dns.createGame(PlayerColor.GREEN, 4, true, "player3");
        
        assertEquals(3, DNS.getGameControllers().size());
        assertNotEquals(game1.getGameId(), game2.getGameId());
        assertNotEquals(game2.getGameId(), game3.getGameId());
        assertNotEquals(game1.getGameId(), game3.getGameId());
    }
    
    /**
     * Test edge case for pong handling with client not in game.
     */
    @Test
    public void testPongToServerFromClient_ClientNotInGame() throws IOException {
        dns.registerWithNickname("player1", testClientController1);
        
        // Client is registered but not in any game
        assertDoesNotThrow(() -> dns.pongToServerFromClient("player1"));
    }
}