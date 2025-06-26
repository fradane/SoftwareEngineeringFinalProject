package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
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

public class DNS extends UnicastRemoteObject implements CallableOnDNS {
    //ad ogni game il suo gameController
    public static final Map<String, GameController> gameControllers = new ConcurrentHashMap<>();
    //ad ogni client il suo controller
    private final Map<String, CallableOnClientController> clients = new ConcurrentHashMap<>();
    // ad ogni il client il suo Game se esiste
    private final Map<String, GameController> clientGame = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private static Thread socketThread;
    private static Thread rmiThread;
    private static final ServerPingPongManager serverPingPongManager = new ServerPingPongManager();

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

    public DNS() throws RemoteException {
        super();
    }

    public static Map<String, GameController> getGameControllers() {
        return gameControllers;
    }

    public Map<String, CallableOnClientController> getClients() {
        return clients;
    }

    public Map<String, GameController> getClientGame() {
        return clientGame;
    }
    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException{
        return gameControllers.get(gameId).getGameInfo();
    }

    @Override
    public GameController getController(String gameId) throws RemoteException {
        return gameControllers.get(gameId);
    }

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


    private void handleDisconnection(String nickname) {
            clients.remove(nickname);

            // Atomic remove-and-check: if was in game, get controller, otherwise null
            GameController gameController = clientGame.remove(nickname);
            
            if (gameController != null) {
                // Was in game, handle disconnection
                leaveGameAfterCreation(gameController, nickname, true);
            } else {
                // Was not in game, handle lobby disconnection
                leaveGameBeforeCreation(nickname);
            }
    }

    public void leaveGameAfterCreation(GameController gameController, String nickname, Boolean isFirst){
        clients.remove(nickname);
        gameController.leaveGameAfterCreation(nickname, isFirst);
    }

    public List<GameInfo> getAvailableGames() {
        // ConcurrentHashMap is already thread-safe, no external synchronization needed
        return gameControllers.values().stream()
                .map(GameController::getGameInfo)
                .filter(game -> !game.isStarted() && !game.isFull())
                .toList();
    }

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
        // Aggiungi il player alla partita
        newGameController.addPlayer(nickname, color, clients.get(nickname));
        clientGame.put(nickname,newGameController);
        System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

        // Notifica tutti i client in attesa che è stata creata una nuova partita
        notifyAvailableGamesToWaitingClients();

        return newGameController.getGameInfo();
    }

    public void removeGame(String gameId) {
        gameControllers.remove(gameId);
        // Notifica tutti i client in attesa che una partita è stata rimossa
        notifyAvailableGamesToWaitingClients();
    }

    public void pingToClientFromServer(String nickname) throws IOException{
       clients.get(nickname).pingToClientFromServer(nickname);
    }

    public void pongToServerFromClient(String nickname) throws IOException{
        //System.out.println("Pong ricevuto da " + nickname);
        serverPingPongManager.onPongReceived(nickname, ()->handleDisconnection(nickname));
    }

    public void pingToServerFromClient(String nickname) throws IOException{
        //System.out.println("Ping ricevuto da " + nickname);
        clients.get(nickname).pongToClientFromServer(nickname);
    }

    public void leaveGameBeforeCreation(String nickname){
        clients.remove(nickname);
        System.out.println(nickname + " left the lobby");
    }


    @Override
    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws IOException {
        if (!clients.containsKey(nickname)) {
            return false;
        }

        // TODO
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
     * Notifica tutti i client non in partita della lista aggiornata dei giochi disponibili
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
     * 
     * @param numPlayers number of players for the game
     * @param isTestFlight whether this is a test flight game
     * @return the unique game ID that was generated and used
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
