package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallableOnClientController extends Remote {

    void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException;

    void notifyGameStarted(String nickname, GameState gameState, GameInfo gameInfo) throws RemoteException;

}
