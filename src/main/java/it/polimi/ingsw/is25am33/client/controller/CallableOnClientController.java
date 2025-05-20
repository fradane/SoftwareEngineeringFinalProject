package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface CallableOnClientController extends Remote {

    void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws IOException;

    void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException;

    void notifyGameStarted(String nickname, GameInfo gameInfo) throws IOException;

    void notifyGameState(String nickname, GameState gameState) throws IOException;

    void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws IOException;

    void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException;

    void notifyCurrAdventureCard( String nickname, String adventureCard) throws IOException;

    void notifyCardState(String nickname, CardState cardState) throws IOException;

    void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException;

    void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException;

    void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException;

    void notifyAddVisibleComponents(String nicknameToNotify, int index, Component component) throws IOException;

    void notifyRemoveVisibleComponents(String nicknameToNotify, int index) throws IOException;

    void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException;

    void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException;

    void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws IOException;

    void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException;

    void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException;

    void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException;

    void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDeck) throws IOException;

    void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException;

    void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws IOException;

    void notifyRemovalResult(String nicknameToNotify, boolean success) throws IOException;

    void notifyShipCorrect(String nicknameToNotify ) throws IOException;

    void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayer) throws IOException;

}
