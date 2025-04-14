package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.rmi.RemoteException;
import java.util.List;

public interface ClientNetworkManager extends VirtualClient {
    void connectToServer(String serverHost) throws RemoteException;
    boolean isNicknameAvailable(String nickname) throws RemoteException;
    boolean isColorAvailable(String gameId, PlayerColor color) throws RemoteException;
    void registerWithNickname(String nickname) throws RemoteException;
    List<GameInfo> getAvailableGames() throws RemoteException;
    GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight) throws RemoteException;
    boolean joinGame(String gameId, PlayerColor color) throws RemoteException;
    void leaveGame(String gameId) throws RemoteException;
    void disconnect() throws RemoteException;
    void notifyGameStateChange(String json) throws RemoteException;
    void notifyComponentTableUpdate(String json) throws RemoteException;
    void notifyCurrAdventureCard(String json) throws RemoteException;
    void notifyCardStateChange(String json) throws RemoteException;
}
