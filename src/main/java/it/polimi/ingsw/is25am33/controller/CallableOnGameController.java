package it.polimi.ingsw.is25am33.controller;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallableOnGameController extends Remote {

    void showMessage(String s) throws RemoteException;

}
