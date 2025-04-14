package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.Observer;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VirtualClient extends Remote, Observer {
    /**
     * Notifica una connessione riuscita
     */
    void notifyConnectionSuccessful() throws RemoteException;

    /**
     * Notifica un errore
     */
    void notifyError(String errorMessage) throws RemoteException;

    /**
     * Notifica che un giocatore si è unito a una partita
     */
    void notifyPlayerJoined(String nickname, GameInfo gameInfo) throws RemoteException;

    /**
     * Notifica che un giocatore ha lasciato una partita
     */
    void notifyPlayerLeft(String nickname, GameInfo gameInfo) throws RemoteException;

    /**
     * Notifica l'inizio della partita
     */
    void notifyGameStarted(GameState gameState) throws RemoteException;

    /**
     * Notifica la fine della partita
     */
    void notifyGameEnded(String reason) throws RemoteException;

    // Identification methods
    String getNickname() throws RemoteException;
}
