package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallableOnClientController extends Remote {

    void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException;

    void notifyGameStarted(String nickname, GameState gameState) throws RemoteException;

}
