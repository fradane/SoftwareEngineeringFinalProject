package it.polimi.ingsw.is25am33.network;
import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for DNS (Discovery and Naming Service) operations in the game.
 * Provides methods for client-server communication, game management, and player registration.
 * Extends Remote to enable RMI (Remote Method Invocation) functionality.
 */
public interface CallableOnDNS extends Remote {

    /**
     * Retrieves the game controller for a specific game.
     *
     * @param gameId The unique identifier of the game
     * @return The GameController associated with the specified game
     * @throws RemoteException If a remote communication error occurs
     */
    GameController getController(String gameId) throws RemoteException;

    /**
     * Gets information about a specific game.
     *
     * @param gameId The unique identifier of the game
     * @return GameInfo object containing information about the specified game
     * @throws RemoteException If a remote communication error occurs
     */
    GameInfo getGameInfo(String gameId) throws RemoteException;

    /**
     * Registers a player with the specified nickname and associates it with a client controller.
     *
     * @param nickname The nickname to register
     * @param controller The client controller to associate with the nickname
     * @return true if registration was successful, false otherwise
     * @throws IOException If an I/O error occurs during registration
     */
    boolean registerWithNickname(String nickname, CallableOnClientController controller) throws IOException;

    /**
     * Creates a new game with the specified parameters.
     *
     * @param color The color chosen by the creating player
     * @param numPlayers The maximum number of players allowed in the game
     * @param isTestFlight Whether the game is a test flight
     * @param nickname The nickname of the player creating the game
     * @return GameInfo object with information about the created game
     * @throws IOException If an I/O error occurs during game creation
     */
    GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws IOException;

    /**
     * Adds a player to an existing game.
     *
     * @param gameId The unique identifier of the game to join
     * @param nickname The nickname of the player joining the game
     * @param color The color chosen by the joining player
     * @return true if the player successfully joined the game, false otherwise
     * @throws IOException If an I/O error occurs during the join operation
     */
    boolean joinGame(String gameId, String nickname, PlayerColor color) throws IOException;

    /**
     * Removes a player from the game before it has been created/started.
     *
     * @param nickname The nickname of the player to remove
     * @throws IOException If an I/O error occurs during the leave operation
     */
    void leaveGameBeforeCreation(String nickname) throws IOException;

    /**
     * Sends a ping from the client to the server to check connection status.
     *
     * @param nickname The nickname of the client sending the ping
     * @throws IOException If an I/O error occurs during the ping operation
     */
    void pingToServerFromClient(String nickname) throws IOException;

    /**
     * Sends a pong response from the client to the server, typically in response to a server ping.
     *
     * @param nickname The nickname of the client sending the pong
     * @throws IOException If an I/O error occurs during the pong operation
     */
    void pongToServerFromClient(String nickname) throws IOException;
}