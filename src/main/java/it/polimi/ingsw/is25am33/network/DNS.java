package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ConnectionManager;
import it.polimi.ingsw.is25am33.network.rmi.RMIServerRunnable;
import it.polimi.ingsw.is25am33.network.socket.SocketServerRunnable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DNS extends UnicastRemoteObject implements CallableOnDNS {

    public static final Map<String, GameController> gameControllers = new ConcurrentHashMap<>();
    public final ConnectionManager connectionManager;

    public static void main(String[] args) throws RemoteException {

        // create the dns to handle any type of connection
        DNS dns = new DNS();

        Thread socketThread = new Thread(new SocketServerRunnable(dns));
        socketThread.start();

        // starts RMI thread
        Thread rmiThread = new Thread(new RMIServerRunnable(dns));
        rmiThread.start();

    }

    public DNS() throws RemoteException {
        super();
        connectionManager = new ConnectionManager();
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
        if (result) System.out.println("New user registered with nickname: " + nickname);
        return result;
    }

    @Override
    public List<GameInfo> getAvailableGames() throws RemoteException {
        return gameControllers.values().stream()
                .map(GameController::getGameInfo)
                .filter(game -> !game.isStarted() && !game.isFull())
                .toList();
    }

    @Override
    public GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws RemoteException {
        return connectionManager.createGame(color, numPlayers, isTestFlight, nickname);
    }

    @Override
    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws RemoteException{
        return connectionManager.joinGame(gameId, nickname, color);
    }




}
