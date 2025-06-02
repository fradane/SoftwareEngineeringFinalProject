
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
    private static ServerPingPongManager serverPingPongManager= new ServerPingPongManager();

    public static void main(String[] args) throws RemoteException {

        try {// create the dns to handle any type of connection
            DNS dns = new DNS();

            //thread per ricevere le connessioni socket
            socketThread = new Thread(new SocketServerManager(dns));
            socketThread.start();

            // starts RMI thread
            rmiThread = new Thread(new RMIServerRunnable(dns));
            rmiThread.start();
        }catch(Exception e) {
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
    public GameInfo getGameInfo(String gameId) throws RemoteException {
        return gameControllers.get(gameId).getGameInfo();
    }

    @Override
    public GameController getController(String gameId) throws RemoteException {
        return gameControllers.get(gameId);
    }

    @Override
    public boolean registerWithNickname(String nickname, CallableOnClientController controller) throws RemoteException {
        if (clients.containsKey(nickname)) return false;
        clients.put(nickname, controller);
        System.out.println("New user registered with nickname: " + nickname);

        new Thread(()-> {
            serverPingPongManager.start(
                    nickname,
                    () -> {
                        try {
                            pingToClientFromServer(nickname);
                        } catch (IOException e) {
                            System.err.println("Errore ping pong: " + e.getMessage());
                        }
                    }
            );
        }).start();

        new Thread(() -> {
            try {
                controller.notifyGameInfos(nickname, getAvailableGames());
            } catch (IOException e) {
                System.err.println("Remote Exception");
            }
        }).start();

        return true;
    }


    private void handleDisconnection(String nickname) {
            clients.remove(nickname);

            if(clientGame.containsKey(nickname)) {
                leaveGameAfterCreation(clientGame.get(nickname), nickname, true);
            }
            else{
                leaveGameBeforeCreation(nickname);
            }
    }

    public void leaveGameAfterCreation(GameController gameController, String nickname, Boolean isFirst){
        clients.remove(nickname);
        gameController.leaveGameAfterCreation(nickname, isFirst);
    }

    public List<GameInfo> getAvailableGames() {
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

        // unique ID for this gameModel
        String gameId = generateUniqueGameId();

        // creates a new controller for this gameModel
        GameController newGameController = new GameController(gameId, numPlayers, isTestFlight, this);
        synchronized (newGameController) {
            gameControllers.put(gameId, newGameController);

            System.out.println("GameModel created: " + gameId + " by " + nickname +
                    " for " + numPlayers + " players" + (isTestFlight ? " (Test Flight)" : ""));

            // Aggiungi il player alla partita
            newGameController.addPlayer(nickname, color, clients.get(nickname));
            clientGame.put(nickname,newGameController);
            System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

        }

        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<GameInfo> availableGames = getAvailableGames();

        clients.forEach((clientNickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try {
                    if(!clientGame.containsKey(clientNickname))
                        clientController.notifyGameInfos(clientNickname, availableGames);
                } catch (IOException e) {
                    System.err.println("errore nella notifica di aggiunta a "+ clientNickname);
                }
            });
            futureNicknames.put(future, clientNickname);
        });

        futureNicknames.forEach((future, clientNickname) -> {
            try {
                //TODO riabbassare a 5 secondi
                future.get(1000, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Timeout nella notifica del client: " + clientNickname);
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Errore nella notifica del client: " + clientNickname);
                e.printStackTrace();
            }
        });

        return newGameController.getGameInfo();
    }

    public void removeGame(String gameId) {
        gameControllers.remove(gameId);
        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<GameInfo> availableGames = getAvailableGames();

        clients.forEach((clientNickname, clientController) -> {
                    Future<?> future = executor.submit(() -> {
                        try {
                            if(!clientGame.containsKey(clientNickname))
                                clientController.notifyGameInfos(clientNickname, availableGames);
                        } catch (IOException e) {}
                    });
                    futureNicknames.put(future, clientNickname);
                });

        futureNicknames.forEach((future, clientNickname) -> {
            try {
                //TODO riabbassare a 1 secondo
                future.get(1000, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Timeout nella notifica del client: " + clientNickname);
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Errore nella notifica del client: " + clientNickname);
                e.printStackTrace();
            }
        });

    }

    public void pingToClientFromServer(String nickname) throws IOException{
       clients.get(nickname).pingToClientFromServer(nickname);
    }

    public void pongToServerFromClient(String nickname) throws IOException{
        //System.out.println("Pong ricevuto da " + nickname);
        serverPingPongManager.onPongReceived(nickname, () -> handleDisconnection(nickname));
    }

    public void pingToServerFromClient(String nickname) throws IOException{
        //System.out.println("Ping ricevuto da " + nickname);
        clients.get(nickname).pongToClientFromServer(nickname);
    }


    public void leaveGameBeforeCreation(String nickname){
        clients.remove(nickname);
        System.out.println( nickname + " left the lobby");
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

            controller.notifyNewPlayerJoined(gameId, nickname, color).start();

            System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

            // if the game is completed, it starts automatically
            if (players.size() == gameInfo.getMaxPlayers()) {
                controller.startGame();
            }

            return true;
        }
    }

    private String generateUniqueGameId() {
        String gameId;
        do {
            gameId = UUID.randomUUID().toString().substring(0, 8);
        } while (gameControllers.containsKey(gameId));

        return gameId;
    }

}
