package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.rmi.RMIServerRunnable;
import it.polimi.ingsw.is25am33.network.socket.SocketServerManager;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Domain Name Service (DNS) server implementation for managing distributed game sessions.
 * This class serves as the central registry and coordinator for game instances and client connections
 * in a distributed gaming environment. It handles both RMI and Socket-based network protocols,
 * manages game lifecycle operations, and coordinates communication between clients and game controllers.
 *
 * The DNS maintains thread-safe mappings of games to controllers, clients to their controllers,
 * and clients to their active games. It provides services for game creation, player registration,
 * game discovery, and connection management with built-in ping-pong mechanisms for connection monitoring.
 *
 * @author Your development team
 * @version 1.0
 * @since 1.0
 */
public class DNS extends UnicastRemoteObject implements CallableOnDNS {
    //every game has its own controller
    public static final Map<String, GameController> gameControllers = new ConcurrentHashMap<>();
    //every client has its controller
    private final Map<String, CallableOnClientController> clients = new ConcurrentHashMap<>();
    // every client has its game, if it exists
    private final Map<String, GameController> clientGame = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private static Thread socketThread;
    private static Thread rmiThread;
    private static final ServerPingPongManager serverPingPongManager = new ServerPingPongManager();

    /**
     * Main entry point for the DNS server application.
     * Parses command line arguments for server IP configuration, initializes the DNS service,
     * and starts both Socket and RMI network protocol threads. The server listens on both
     * protocols simultaneously to support different client connection types.
     *
     * @param args command line arguments; supports "-ip [address]" to specify server IP address.
     *             If not provided, defaults to "localhost"
     * @throws RemoteException if an error occurs during DNS server initialization
     */
    public static void main(String[] args) throws RemoteException {

        // Parse command line arguments for -ip parameter
        String serverIP = "localhost"; // Default to localhost

        for (int i = 0; i < args.length; i++) {
            if ("-ip".equals(args[i]) && i + 1 < args.length) {
                serverIP = args[i + 1];
                break;
            }
        }

        System.out.println("[DNS] Server IP configured: " + serverIP);

        try {// create the dns to handle any type of connection
            DNS dns = new DNS();

            //thread per ricevere le connessioni socket
            socketThread = new Thread(new SocketServerManager(dns));
            socketThread.start();

            // starts RMI thread with specified IP
            rmiThread = new Thread(new RMIServerRunnable(dns, serverIP));
            rmiThread.start();
        } catch(Exception e) {
            socketThread.interrupt();
            rmiThread.interrupt();
        }

    }

    /**
     * Constructs a new DNS server instance.
     * Initializes the remote object capabilities required for RMI communication
     * and prepares the internal data structures for managing games and clients.
     *
     * @throws RemoteException if an error occurs during remote object initialization
     */
    public DNS() throws RemoteException {
        super();
    }

    /**
     * Returns the thread-safe map of all active game controllers.
     * Provides access to the central registry of game instances managed by this DNS server.
     * The returned map is concurrent and can be safely accessed by multiple threads.
     *
     * @return a ConcurrentHashMap mapping game IDs to their corresponding GameController instances
     */
    public static Map<String, GameController> getGameControllers() {
        return gameControllers;
    }

    /**
     * Returns the map of registered client controllers.
     * Provides access to all currently connected clients and their associated controller interfaces.
     * This map is used for direct communication with clients and connection management.
     *
     * @return a map of client nicknames to their CallableOnClientController instances
     */
    public Map<String, CallableOnClientController> getClients() {
        return clients;
    }

    /**
     * Returns the map of clients to their active games.
     * Tracks which clients are currently participating in which games, enabling
     * proper game state management and client-game association tracking.
     *
     * @return a map of client nicknames to their associated GameController instances
     */
    public Map<String, GameController> getClientGame() {
        return clientGame;
    }

    /**
     * Retrieves game information for the specified game ID.
     * Provides access to game metadata and current state information through
     * the associated game controller.
     *
     * @param gameId the unique identifier of the game
     * @return the GameInfo object containing game details and current state
     * @throws RemoteException if the game ID is not found or a network error occurs
     */
    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException{
        return gameControllers.get(gameId).getGameInfo();
    }

    /**
     * Retrieves the game controller for the specified game ID.
     * Provides direct access to the controller managing the game instance,
     * enabling advanced game operations and state manipulation.
     *
     * @param gameId the unique identifier of the game
     * @return the GameController instance managing the specified game
     * @throws RemoteException if the game ID is not found or a network error occurs
     */
    @Override
    public GameController getController(String gameId) throws RemoteException {
        return gameControllers.get(gameId);
    }

    /**
     * Registers a new client with the specified nickname and controller interface.
     * Attempts to register the client in the system, starts ping-pong monitoring for
     * connection health, and notifies the client of available games. Registration
     * fails if the nickname is already in use.
     *
     * @param nickname the desired nickname for the client; must be unique
     * @param controller the client controller interface for bidirectional communication
     * @return true if registration succeeds, false if the nickname is already taken
     * @throws RemoteException if a network communication error occurs during registration
     */
    @Override
    public boolean registerWithNickname(String nickname, CallableOnClientController controller) throws RemoteException {
        if (clients.putIfAbsent(nickname, controller) != null) return false;
        System.out.println("New user registered with nickname: " + nickname);

        new Thread(()->{
            serverPingPongManager.start(
                    nickname,
                    ()-> {
                        try {
                            pingToClientFromServer(nickname);
                        } catch (IOException e) {
                            System.err.println("Remote Exception in pingToClientFromServer: " + e.getMessage());
                        }
                    }
            );
        }).start();

        new Thread(() -> {
            try {
                controller.notifyGameInfos(nickname, getAvailableGames());
            } catch (IOException e) {
                System.err.println("Remote Exception in notifyGameInfos: " + e.getMessage());
            }
        }).start();

        return true;
    }

    /**
     * Handles client disconnection and performs comprehensive cleanup.
     * Removes the client from all internal mappings, notifies other players in the same game
     * about the disconnection, stops ping-pong monitoring, and cleans up the entire game
     * if necessary. This method is synchronized to prevent race conditions during cleanup.
     *
     * @param nickname the nickname of the disconnecting client
     */
    public void handleDisconnection(String nickname) {

        synchronized (this) {  // Sincronizza sulla GameController instance per evitare race condition
            GameController gameController = clientGame.remove(nickname);

            if (gameController == null) {
                System.out.println("Player " + nickname + " left.");
                return;
            }

            clients.remove(nickname);
            gameController.getClientControllers().remove(nickname);
            Set<String> playerLeft = new HashSet<>(gameController.getClientControllers().keySet());

            gameController.getGameModel().getGameClientNotifier().notifyClients(
                    playerLeft,
                    (nicknameToNotify, controller) -> controller.notifyPlayerDisconnected(nicknameToNotify, nickname)
            );

            String gameInfoId = gameController.getGameInfo().getGameId();

            serverPingPongManager.stop(nickname);
            System.out.println("[" + gameInfoId + "] Player " + nickname + " left the game");

            playerLeft.forEach(player -> {
                serverPingPongManager.stop(player);
                clients.remove(player);
                clientGame.remove(player);
                System.out.println("[" + gameInfoId + "] Player " + player + " left the game");
            });

            gameControllers.remove(gameInfoId);
            System.out.println("[" + gameInfoId + "] Deleted!");
        }

    }

    /**
     * Retrieves the list of games available for joining.
     * Returns games that are not yet started and not at maximum capacity.
     * The list is generated from the thread-safe game controllers map and
     * reflects the current state of available games.
     *
     * @return a list of GameInfo objects representing joinable games
     */
    public List<GameInfo> getAvailableGames() {
        // ConcurrentHashMap is already thread-safe, no external synchronization needed
        return gameControllers.values().stream()
                .map(GameController::getGameInfo)
                .filter(game -> !game.isStarted() && !game.isFull())
                .toList();
    }

    /**
     * Creates a new game with the specified parameters and adds the creator as the first player.
     * Validates the client registration and player count, generates a unique game ID,
     * creates the game controller, and notifies all waiting clients about the new game.
     * The game starts automatically when it reaches maximum capacity.
     *
     * @param color the player color chosen by the game creator
     * @param numPlayers the maximum number of players for the game; must be between 2 and 4
     * @param isTestFlight whether this game is in test flight mode with different rules
     * @param nickname the nickname of the client creating the game
     * @return the GameInfo object of the newly created game
     * @throws RemoteException if the client is not registered, player count is invalid, or a network error occurs
     */
    @Override
    public GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws RemoteException {

        if (!clients.containsKey(nickname)) {
            throw new RemoteException("Client not registered");
        }

        if (numPlayers < 2 || numPlayers > 4) {
            throw new RemoteException("Invalid number of players: must be between 2 and 4");
        }

        // generates unique ID and creates controller atomically
        String gameId = generateUniqueGameIdAndCreate(numPlayers, isTestFlight);
        GameController newGameController = gameControllers.get(gameId);

        System.out.println("GameModel created: " + gameId + " by " + nickname +
                " for " + numPlayers + " players" + (isTestFlight ? " (Test Flight)" : ""));
        // Add players to the game
        newGameController.addPlayer(nickname, color, clients.get(nickname));
        clientGame.put(nickname,newGameController);
        System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

        //notify every waiting clients that has been created a new game
        notifyAvailableGamesToWaitingClients();

        return newGameController.getGameInfo();
    }

    /**
     * Removes a game from the active games registry.
     * Cleans up the game controller mapping and notifies all waiting clients
     * about the updated list of available games.
     *
     * @param gameId the unique identifier of the game to remove
     */
    public void removeGame(String gameId) {
        gameControllers.remove(gameId);
        //notify every waiting clients that has been removed a game
        notifyAvailableGamesToWaitingClients();
    }

    /**
     * Sends a ping message from the server to the specified client.
     * This method is part of the keep-alive mechanism to monitor client connectivity.
     * If the client is not found, the ping is silently ignored.
     *
     * @param nickname the nickname of the client to ping
     * @throws IOException if a network communication error occurs during ping transmission
     */
    public void pingToClientFromServer(String nickname) throws IOException {
        CallableOnClientController clientController = clients.get(nickname);
        if (clientController == null) return;
        clientController.pingToClientFromServer(nickname);
    }

    /**
     * Handles pong responses received from clients.
     * Resets the timeout counter for the client and sets up disconnection handling
     * if the client fails to respond to future pings. This method is part of the
     * bidirectional keep-alive mechanism.
     *
     * @param nickname the nickname of the client sending the pong response
     * @throws IOException if a network communication error occurs
     */
    public void pongToServerFromClient(String nickname) throws IOException{
        //System.out.println("Pong ricevuto da " + nickname);
        serverPingPongManager.onPongReceived(nickname, ()-> {
            synchronized (DNS.this) {
                if (clientGame.get(nickname) == null) return;
            }
            handleDisconnection(nickname);
        });
    }

    /**
     * Handles ping messages received from clients.
     * Responds with a pong message to maintain the bidirectional keep-alive mechanism.
     * If the client is not found, the ping is silently ignored.
     *
     * @param nickname the nickname of the client that sent the ping
     * @throws IOException if a network communication error occurs during pong response
     */
    public void pingToServerFromClient(String nickname) throws IOException{
        CallableOnClientController clientController = clients.get(nickname);
        if (clientController == null) return;
        clientController.pongToClientFromServer(nickname);
    }

    /**
     * Handles client disconnection before game creation.
     * Delegates to the main disconnection handler to perform cleanup operations
     * for clients who disconnect while browsing available games.
     *
     * @param nickname the nickname of the client leaving before creating or joining a game
     */
    public void leaveGameBeforeCreation(String nickname) {
        handleDisconnection(nickname);
    }

    /**
     * Attempts to add a client to an existing game with the specified color.
     * Validates client registration, game existence, game state, and color availability.
     * If successful, adds the player to the game, notifies other players, and starts
     * the game automatically if it reaches maximum capacity.
     *
     * @param gameId the unique identifier of the game to join
     * @param nickname the nickname of the client attempting to join
     * @param color the desired player color for the joining client
     * @return true if the join operation succeeds, false if the color is unavailable
     * @throws IOException if the client is not registered, game doesn't exist, game is started/full,
     *                     or the client is already in the game
     */
    @Override
    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws IOException {
        if (!clients.containsKey(nickname)) {
            return false;
        }

        if (!gameControllers.containsKey(gameId)) {
            return false;
        }

        GameController controller = gameControllers.get(gameId);
        synchronized (controller) {
            GameInfo gameInfo = controller.getGameInfo();

            if (gameInfo.isStarted()) {
                throw new RemoteException("GameModel already started");
            }

            if (gameInfo.isFull()) {
                throw new RemoteException("GameModel is full");
            }

            if (gameInfo.getConnectedPlayersNicknames().contains(nickname)) {
                throw new RemoteException("You are already in this gameModel");
            }

            if (gameInfo.getConnectedPlayers().containsValue(color)) {
                return false;
            }

            controller.addPlayer(nickname, color, clients.get(nickname));
            clientGame.put(nickname,controller);
            gameInfo = controller.getGameInfo();

            final GameInfo finalGameInfo = gameInfo;

            Set<String> players = gameInfo.getConnectedPlayersNicknames();

            controller.getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyNewPlayerJoined(nicknameToNotify, gameId, nickname, color);
            });

            // Notifica tutti i client in attesa che la lista partite è cambiata
            // notify every waiting client that the list of games has changed
            notifyAvailableGamesToWaitingClients();

            System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

            // if the game is completed, it starts automatically
            if (players.size() == gameInfo.getMaxPlayers()) {
                controller.startGame();
            }

            return true;
        }
    }

    /**
     * Notifies all clients not currently in a game about the updated list of available games.
     * Uses an executor service to send notifications concurrently with timeout handling
     * to prevent blocking on unresponsive clients. Failed notifications are logged
     * but do not affect other clients.
     */
    private void notifyAvailableGamesToWaitingClients() {
        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<GameInfo> availableGames = getAvailableGames();

        clients.forEach((clientNickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try {
                    if(!clientGame.containsKey(clientNickname))
                        clientController.notifyGameInfos(clientNickname, availableGames);
                } catch (IOException e) {
                    System.err.println("errore nella notifica di aggiornamento a "+ clientNickname);
                }
            });
            futureNicknames.put(future, clientNickname);
        });

        futureNicknames.forEach((future, clientNickname) -> {
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Timeout nella notifica del client: " + clientNickname);
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Errore nella notifica del client: " + clientNickname);
            }
        });
    }

    /**
     * Generates a unique game ID and atomically creates and inserts the GameController.
     * This prevents race conditions where multiple threads could generate the same ID.
     * Uses UUID generation with truncation to create readable game identifiers.
     *
     * @param numPlayers number of players for the game
     * @param isTestFlight whether this is a test flight game
     * @return the unique game ID that was generated and used
     * @throws RemoteException if an error occurs during game controller creation
     */
    private String generateUniqueGameIdAndCreate(int numPlayers, boolean isTestFlight) throws RemoteException {
        String gameId;
        GameController newGameController;

        do {
            gameId = UUID.randomUUID().toString().substring(0, 8);
            newGameController = new GameController(gameId, numPlayers, isTestFlight, this);
        } while (gameControllers.putIfAbsent(gameId, newGameController) != null);

        return gameId;
    }

}