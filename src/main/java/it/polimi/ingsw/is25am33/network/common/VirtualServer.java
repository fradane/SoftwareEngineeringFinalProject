package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;
import it.polimi.ingsw.is25am33.network.common.VirtualClient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface VirtualServer extends Remote {
    /**
     * Registra un client per ricevere notifiche
     * @param nickname Il nome del giocatore
     * @param clientCallback L'interfaccia remota del client
     */
    void registerClient(String nickname, VirtualClient clientCallback) throws RemoteException;

    /**
     * Rimuove la registrazione di un client
     * @param nickname Il nome del giocatore da deregistrare
     */
    void unregisterClient(String nickname) throws RemoteException;

    /**
     * Ottiene la lista delle partite disponibili
     * @return Lista delle partite non ancora iniziate
     */
    List<GameInfo> getAvailableGames() throws RemoteException;

    /**
     * Crea una nuova partita
     * @param nickname Il creatore della partita
     * @param numPlayers Numero di giocatori attesi
     * @param isTestFlight Se è un volo di prova
     * @return ID della partita creata
     */
    String createGame(String nickname, PlayerColor color,  int numPlayers, boolean isTestFlight) throws RemoteException;

    /**
     * Unisce un giocatore a una partita esistente
     * @param gameId ID della partita
     * @param nickname Nome del giocatore
     * @return true se l'operazione è riuscita
     */
    boolean joinGame(String gameId, String nickname, PlayerColor color) throws RemoteException;

    /**
     * Fa uscire un giocatore da una partita
     * @param gameId ID della partita
     * @param nickname Nome del giocatore
     */
    void leaveGame(String gameId, String nickname) throws RemoteException;

    /**
     * Verifica se un nickname è già in uso
     * @param nickname Nome da verificare
     * @return true se il nickname è già in uso
     */
    boolean isNicknameInUse(String nickname) throws RemoteException;

    String playerChoseComponentFromTable(String nickname, String coordinatesJson) throws RemoteException;

    void playerChoseToEndBuildShipBoardPhase(String nickname) throws RemoteException;

    boolean playerWantsToSeeShipBoardOf(String chosenPlayerNickname, String nickname) throws RemoteException;

    void playerWantsToPlaceFocusedComponent(String nickname, String coordinatesJson) throws RemoteException;

    void playerWantsToReserveFocusedComponent(String nickname) throws RemoteException;

    void playerWantsToReleaseFocusedComponent(String nickname) throws RemoteException;
}
