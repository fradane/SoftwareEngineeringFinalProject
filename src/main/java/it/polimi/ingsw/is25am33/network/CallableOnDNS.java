package it.polimi.ingsw.is25am33.network;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CallableOnDNS extends Remote {

    GameController getController(String gameId) throws RemoteException ;

    GameInfo getGameInfo(String gameId) throws RemoteException;

    boolean registerWithNickname(String nickname, CallableOnClientController controller) throws IOException;

    GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws IOException;

    boolean joinGame(String gameId, String nickname, PlayerColor color) throws IOException;

    void leaveGameBeforeCreation(String nickname) throws IOException;

    void pingToServerFromClient(String nickname) throws IOException;

    void pongToServerFromClient(String nickname) throws IOException;
}
