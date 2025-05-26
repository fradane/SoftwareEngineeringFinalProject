
package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ConnectionManager;
import it.polimi.ingsw.is25am33.network.rmi.RMIServerRunnable;
import it.polimi.ingsw.is25am33.network.socket.SocketServerManager;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DNS extends UnicastRemoteObject implements CallableOnDNS {

    public static final Map<String, GameController> gameControllers = new ConcurrentHashMap<>();    // GameId - gameController
    public final ConnectionManager connectionManager;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws RemoteException {

        // create the dns to handle any type of connection
        DNS dns = new DNS();

        Thread socketThread = new Thread(new SocketServerManager(dns));
        socketThread.start();

        // starts RMI thread
        Thread rmiThread = new Thread(new RMIServerRunnable(dns));
        rmiThread.start();

    }

    public DNS() throws RemoteException {
        super();
        connectionManager = new ConnectionManager(this);
    }

    public static Map<String, GameController> getGameControllers() {
        return gameControllers;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
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
        boolean result = connectionManager.registerWithNickname(nickname, controller);
        if (result) {
            System.out.println("New user registered with nickname: " + nickname);
            new Thread(() -> {
                try {
                    controller.notifyGameInfos(nickname, getAvailableGames());
                } catch (IOException e) {
                    System.err.println("Remote Exception");
                }
            }).start();
        }
        return result;
    }

    public List<GameInfo> getAvailableGames() {
        return gameControllers.values().stream()
                .map(GameController::getGameInfo)
                .filter(game -> !game.isStarted() && !game.isFull())
                .toList();
    }

    @Override
    public GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws RemoteException {

        GameInfo newGameInfo = connectionManager.createGame(color, numPlayers, isTestFlight, nickname);

        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<GameInfo> availableGames = getAvailableGames();

        connectionManager.getClients()
                .forEach((clientNickname, clientController) -> {
                    Future<?> future = executor.submit(() -> {
                        try {
                            if (!gameControllers.containsKey(clientNickname))
                                clientController.notifyGameInfos(clientNickname, availableGames);
                        } catch (IOException e) {
                            System.err.println("Remote Exception");
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

        return newGameInfo;
    }

    @Override
    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws RemoteException{
        return connectionManager.joinGame(gameId, nickname, color);
    }

    public void removeGame(String gameId) {
        gameControllers.remove(gameId);

        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<GameInfo> availableGames = getAvailableGames();

        connectionManager.getClients()
                .forEach((clientNickname, clientController) -> {
                    Future<?> future = executor.submit(() -> {
                        try {
                            if (!gameControllers.containsKey(clientNickname))
                                clientController.notifyGameInfos(clientNickname, availableGames);
                        } catch (IOException e) {
                            System.err.println("Remote Exception");
                        }
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
}
