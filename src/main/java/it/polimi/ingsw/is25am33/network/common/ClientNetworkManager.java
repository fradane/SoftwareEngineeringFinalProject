package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientNetworkManager extends VirtualClient {
    void connectToServer(String serverHost) throws RemoteException;
    List<GameInfo> getAvailableGames() throws RemoteException;
    String createGame(PlayerColor color, int numPlayers, boolean isTestFlight) throws RemoteException;
    boolean joinGame(String gameId, PlayerColor color) throws RemoteException;
    void leaveGame(String gameId) throws RemoteException;
    void disconnect() throws RemoteException;
}
