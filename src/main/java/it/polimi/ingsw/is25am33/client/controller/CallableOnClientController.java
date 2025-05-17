package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.collections.ObservableList;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface CallableOnClientController extends Remote {

    //void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws RemoteException;

    void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws RemoteException;

    void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException;

    void notifyGameStarted(String nickname, GameInfo gameInfo) throws RemoteException;

    void notifyGameState(String nickname, GameState gameState) throws RemoteException;

    void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws RemoteException;

    void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws RemoteException;

    void notifyCurrAdventureCard( String nickname, String adventureCard) throws RemoteException;

    void notifyCardState(String nickname, CardState cardState) throws RemoteException;

    void notifyChooseComponent(String nicknameToNotify, String nickname, Component component) throws RemoteException;

    void notifyReleaseComponent(String nicknameToNotify, String nickname) throws RemoteException;

    void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws RemoteException;

    void notifyAddVisibleComponents(String nicknameToNotify, int index, Component component) throws RemoteException;

    void notifyRemoveVisibleComponents(String nicknameToNotify, int index) throws RemoteException;

    void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException;

    void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException;

    void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws RemoteException;

    void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws RemoteException;

    void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws RemoteException;

    void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws RemoteException;

    void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDeck) throws RemoteException;

    void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws RemoteException;

    void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws RemoteException;

    void notifyRemovalResult(String nicknameToNotify, boolean success) throws RemoteException;

    void notifyShipCorrect(String nicknameToNotify ) throws RemoteException;

}
