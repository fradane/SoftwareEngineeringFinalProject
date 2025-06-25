package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CallableOnClientController extends Remote {

    void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws IOException;

    void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException;

    void notifyGameStarted(String nickname, GameInfo gameInfo) throws IOException;

    void notifyGameState(String nickname, GameState gameState) throws IOException;

    void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) throws IOException;

    void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException;

    void notifyCardState(String nickname, CardState cardState) throws IOException;

    void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException;

    void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException;

    void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException;

    void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws IOException;

    void notifyAddVisibleComponents(String nicknameToNotify, int index, Component component) throws IOException;

    void notifyRemoveVisibleComponents(String nicknameToNotify, int index) throws IOException;

    void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException;

    void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException;

    void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException;

    void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException;

    void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException;

    void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException;

    void notifyVisibleDeck(String nickname, List<List<ClientCard>> littleVisibleDeck) throws IOException;

    void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException;

    void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws IOException;

    void notifyRemovalResult(String nicknameToNotify, boolean success) throws IOException;

    void notifyShipCorrect(String nicknameToNotify ) throws IOException;

    void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayer) throws IOException;

    void notifyInvalidShipBoard(String nicknameToNotify,String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException;

    void notifyValidShipBoard(String nicknameToNotify,String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException;

    void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify,String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts , Map<Class<?>, List<Component>> componentsPerType) throws RemoteException;

    void notifyCardStarted(String nicknameToNotify)throws IOException;

    void notifyStopHourglass(String nicknameToNotify) throws IOException;

    void notifyFirstToEnter(String nicknameToNotify) throws IOException;

    void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException;

    void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException;

    void forcedDisconnection(String nickname, String gameId) throws IOException;

    void pingToClientFromServer(String nickname) throws IOException;

    void pongToClientFromServer(String nickname) throws IOException;

    void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType ) throws IOException;

    void notifyCrewPlacementPhase(String nicknameToNotify) throws IOException;

    void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException;

    void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws IOException;

    void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipName) throws IOException;

    void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws IOException;

    void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws IOException;

    void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws IOException;

    void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip ) throws IOException;

    void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws IOException;

    void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations) throws IOException;

    void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException;

    void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException;

    void notifyStorageError(String nicknameToNotify, String errorMessage) throws IOException;
}
