package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CallableOnGameController extends Remote {

    void showMessage(String s) throws IOException;

    void playerPicksHiddenComponent(String nickname) throws IOException;

    void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) throws IOException;

    void playerWantsToReserveFocusedComponent(String nickname) throws IOException;

    void playerWantsToReleaseFocusedComponent(String nickname) throws IOException;

    void playerEndsBuildShipBoardPhase(String nickname) throws IOException;

    void playerPicksVisibleComponent(String nickname, Integer choice) throws IOException;

    void playerWantsToVisitLocation(String nickname, Boolean choice) throws IOException;

    void playerWantsToThrowDices(String nickname) throws IOException;

    void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws IOException;

    void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws IOException;

    void playerWantsToVisitPlanet(String nickname, int choice) throws IOException;

    void playerWantsToAcceptTheReward(String nickname, Boolean choice) throws IOException;

    void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords) throws IOException;

    void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords) throws IOException;

    void playerHandleBigShot(String nickname) throws IOException;

    void playerChoseStorage(String nickname, List<Coordinates> storageCoords) throws IOException;

    void spreadEpidemic(String nickname) throws IOException;

    void stardustEvent(String nickname) throws IOException;

    void playerWantsToRestartHourglass(String nickname) throws IOException;

    void notifyHourglassEnded(String nickname) throws IOException;

    void leaveGameAfterCreation(String nickname, Boolean isFirst) throws IOException;

    void playerWantsToRemoveComponent(String nickname, Coordinates coordinate) throws IOException;

    void playerChoseShipPart(String nickname, Set<Coordinates> shipPart) throws IOException;

    void playerWantsToFocusReservedComponent(String nickname, int choice) throws IOException;

    void playerPlacesPawn(String nickname) throws IOException;

    void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException;

    void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException;

    void requestPrefabShips(String nickname) throws IOException;

    void requestSelectPrefabShip(String nickname, String prefabShipId) throws IOException;

    void playerWantsToLand(String nickname) throws IOException;

    void startCheckShipBoardAfterAttack(String nickname) throws IOException;

    void debugSkipToLastCard() throws IOException;

    void evaluatedCrewMembers(String nickname) throws IOException;

}