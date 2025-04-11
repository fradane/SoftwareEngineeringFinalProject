package it.polimi.ingsw.is25am33.network.rmi.client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.common.VirtualServer;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RMIClientNetworkManager extends UnicastRemoteObject implements ClientNetworkManager {
    private final String nickname;
    private VirtualServer server;
    private boolean connected;

    public RMIClientNetworkManager(String nickname) throws RemoteException {
        super();
        this.nickname = nickname;
        this.connected = false;
    }

    @Override
    public void connectToServer(String serverHost) throws RemoteException {
        try{
            server = (VirtualServer) Naming.lookup("rmi://" + serverHost + ":" +
                    NetworkConfiguration.RMI_PORT + "/" + NetworkConfiguration.RMI_SERVER_NAME);

            // Verifica se il nickname è disponibile
            if (server.isNicknameInUse(nickname)) {
                throw new Exception("Nickname already in use");
            }

            server.registerClient(nickname, this);

            connected = true;
            System.out.println("Connected to server as " + nickname);

        }catch (Exception e){
            throw new RemoteException("Could not connect to server: " + e.getMessage());
        }
    }

    @Override
    public List<GameInfo> getAvailableGames() throws RemoteException {
        if (!connected || server == null) {
            throw new RemoteException("Not connected to server");
        }

        return server.getAvailableGames();
    }

    public String createGame(PlayerColor color, int numPlayers, boolean isTestFlight) throws RemoteException {
        if (!connected || server == null) {
            throw new RemoteException("Not connected to server");
        }

        String gameId = server.createGame(nickname, color, numPlayers, isTestFlight);

        return gameId;
    }

    @Override
    public boolean joinGame(String gameId, PlayerColor color) throws RemoteException {
        if (!connected || server == null) {
            throw new RemoteException("Not connected to server");
        }

        boolean success = server.joinGame(gameId, nickname, color);

        return success;
    }

    @Override
    public void leaveGame(String gameId) throws RemoteException{
        if (connected && server != null) {
            try {
                if (gameId != null) {
                    server.leaveGame(gameId, nickname);
                    gameId = null;
                }
                System.out.println("Game left: " + gameId);
            } catch (RemoteException e) {
                System.err.println("Error during leaving: " + e.getMessage());
            }
        }
    }

    @Override
    public void disconnect() throws RemoteException{
        if (connected && server != null) {
            try {
                server.unregisterClient(nickname);
                connected = false;
            } catch (RemoteException e) {
                System.err.println("Error during disconnection: " + e.getMessage());
            }
        }
    }


    @Override
    public void notifyConnectionSuccessful() throws RemoteException {
        System.out.println("Connection successful!");
    }

    @Override
    public void notifyError(String errorMessage) throws RemoteException {
        System.err.println("Error: " + errorMessage);
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) throws RemoteException {
        System.out.println(nickname + " joined the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) throws RemoteException {
        System.out.println(nickname + " left the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyGameStarted(GameState gameState) throws RemoteException {
        System.out.println("Game started! Initial state: " + gameState);
        System.out.println("The game is now in progress...");
    }

    @Override
    public void notifyGameEnded(String reason) throws RemoteException {
        System.out.println("Game ended. Reason: " + reason);
    }

    @Override
    public String getNickname() throws RemoteException {
        return nickname;
    }
}
