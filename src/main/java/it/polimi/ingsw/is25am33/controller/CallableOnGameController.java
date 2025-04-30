package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface CallableOnGameController extends Remote {

    void showMessage(String s) throws RemoteException;

    Component playerPicksHiddenComponent(String nickname) throws IOException;

    void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates) throws IOException;

    void playerWantsToReserveFocusedComponent(String nickname) throws IOException;

    void playerWantsToReleaseFocusedComponent(String nickname) throws IOException;

    void playerChoseToEndBuildShipBoardPhase(String nickname) throws IOException;

    Component[][] getShipBoardOf(String otherPlayerNickname, String askerNickname) throws IOException;

    Component playerPicksVisibleComponent(String nickname, Integer choice) throws RemoteException;

    Map<Integer, Component> showPlayerVisibleComponent(String nickname) throws RemoteException;

    void playerWantsToVisitLocation(String nickname, Boolean choice) throws RemoteException;

    void playerWantsToThrowDices(String nickname) throws RemoteException;

    void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException;

    void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException;

    void playerChoseCabin(String nickname, List<Coordinates> cabin) throws RemoteException;

    void playerWantsToVisitPlanet(String nickname, int choice) throws RemoteException;

    void playerWantsToAcceptTheReward(String nickname, Boolean choice) throws RemoteException;

    void playerHandleSmallDanObj(String nickname, Coordinates shieldCoords, Coordinates batteryBoxCoords) throws RemoteException;

    void playerHandleBigMeteorite(String nickname, Coordinates doubleCannonCoords, Coordinates batteryBoxCoords) throws RemoteException;

    void playerHandleBigShot(String nickname) throws RemoteException;

    void playerChoseStorage(String nickname, Coordinates storageCoords) throws RemoteException;

    void spreadEpidemic(String nickname) throws RemoteException;

    void stardustEvent(String nickname) throws RemoteException;

}