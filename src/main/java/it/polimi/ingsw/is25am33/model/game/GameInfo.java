package it.polimi.ingsw.is25am33.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Represents the information of a game session, including details
 * about the game identifier, associated game controller, maximum number
 * of players, connected players, and game status.
 *
 * This class implements {@link Serializable} to allow its information
 * to be transmitted or stored as required.
 */
public class GameInfo implements Serializable {
    /**
     * A unique identifier for a game session. This variable distinguishes
     * different game sessions within the system. It is assigned to a
     * specific instance of the game and is used to track and reference
     * the associated game session.
     */
    private String gameId;
    /**
     * Represents the game controller associated with a game session that allows communication
     * and interaction with the server-side logic during gameplay.
     *
     * This controller is responsible for handling various actions performed by players within
     * the game, including choosing components, performing game actions, and responding to
     * game events. It serves as the bridge between the game session information and the
     * server's game logic.
     *
     * Implemented as a {@link CallableOnGameController}, it enables remote method invocation
     * for controlling and managing gameplay state remotely.
     */
    private CallableOnGameController gameController;
    /**
     * Represents the maximum number of players allowed to participate in a game session.
     * This value is used to determine if new players can join the game or if the game is full.
     */
    private int maxPlayers;
    /**
     * Represents a mapping of connected players in the current game session.
     * The map's keys are unique player nicknames, and the corresponding values
     * are the {@link PlayerColor} assigned to each player.
     *
     * This structure is used to track the association between player identities
     * and their chosen or assigned colors within the game.
     */
    private Map<String, PlayerColor> connectedPlayers;
    /**
     * Indicates whether the game session has started.
     * This variable represents the current status of the game;
     * it is set to {@code true} when the game is active and ongoing,
     * and to {@code false} when the game has not yet started or has ended.
     */
    private boolean isStarted;
    /**
     * Indicates whether the game session is running in "Test Flight" mode.
     *
     * Test Flight mode could be used for testing purposes or an alternative
     * operational mode, distinguishing the session from regular gameplay.
     */
    private boolean isTestFlight;

    /**
     * Constructs a new GameInfo instance with the specified parameters.
     *
     * @param gameId            a unique identifier for the game session
     * @param gameController    the game controller associated with this game session, used for handling remote client calls
     * @param maxPlayers        the maximum number of players allowed in the game
     * @param connectedPlayers  a map of currently connected players, where the key is the player's nickname and the value is their associated color
     * @param isStarted         a flag indicating whether the game session has started
     * @param isTestFlight      a flag indicating whether the game session is in test flight mode
     */
    public GameInfo(String gameId, CallableOnGameController gameController, int maxPlayers, Map<String, PlayerColor> connectedPlayers, boolean isStarted, boolean isTestFlight) {
        this.gameId = gameId;
        this.gameController = gameController;
        this.maxPlayers = maxPlayers;
        this.connectedPlayers = connectedPlayers;
        this.isStarted = isStarted;
        this.isTestFlight = isTestFlight;
    }

    /**
     * Default constructor for the GameInfo class.
     * Initializes an instance of GameInfo with default values for all fields.
     * <p>
     * This constructor can be used to create an empty GameInfo object,
     * allowing the properties to be set individually using setter methods.
     */
    public GameInfo() {}

    /**
     * Retrieves the map of connected players where each entry maps a player's nickname
     * to their associated {@link PlayerColor}.
     *
     * @return a map containing the nicknames of the connected players as keys
     *         and their associated {@link PlayerColor} as values.
     */
    public Map<String, PlayerColor> getConnectedPlayers() {
        return connectedPlayers;
    }

    /**
     * Sets the unique identifier for the game session.
     *
     * @param gameId the unique identifier to be assigned to the game session
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /**
     * Sets the maximum number of players allowed in the game session.
     *
     * @param maxPlayers the maximum number of players that can participate in the game session
     */
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    /**
     * Updates the map of connected players in the game session.
     *
     * @param connectedPlayers a map where the keys represent player nicknames (as strings)
     *                         and the values represent their associated {@code PlayerColor}.
     */
    public void setConnectedPlayers(Map<String, PlayerColor> connectedPlayers) {
        this.connectedPlayers = connectedPlayers;
    }

    /**
     * Updates the game session's started status.
     *
     * @param started the new started status of the game session.
     *                Set to {@code true} if the game has started, otherwise {@code false}.
     */
    public void setStarted(boolean started) {
        isStarted = started;
    }

    /**
     * Sets the test flight status for the game session.
     *
     * @param testFlight a boolean indicating whether the test flight mode is enabled (true) or disabled (false)
     */
    public void setTestFlight(boolean testFlight) {
        isTestFlight = testFlight;
    }

    /**
     * Retrieves the unique identifier for the game session.
     *
     * @return a string representing the unique game identifier.
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Retrieves the game controller associated with the game session.
     *
     * @return the instance of {@code CallableOnGameController} for managing
     *         the actions and interactions related to the game session.
     */
    @JsonIgnore
    public CallableOnGameController getGameController() {
        return gameController;
    }

    /**
     * Retrieves the maximum number of players allowed in this game session.
     *
     * @return the maximum number of players that can join the game.
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Retrieves the nicknames of all players currently connected to the game session.
     *
     * @return a set of strings representing the nicknames of the connected players.
     */
    @JsonIgnore
    public Set<String> getConnectedPlayersNicknames() {
        return connectedPlayers.keySet();
    }

    /**
     * Checks whether the game session is marked as a test flight.
     *
     * @return true if the game session is a test flight, false otherwise.
     */
    public boolean isTestFlight() {
        return isTestFlight;
    }

    /**
     * Checks if the game session has started.
     *
     * @return true if the game session is in a started state, otherwise false.
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Checks whether the current instance has reached its maximum allowed players.
     *
     * @return true if the number of connected players is greater than or equal to the maximum allowed players, false otherwise.
     */
    @JsonIgnore
    public boolean isFull() {
        return connectedPlayers.size() >= maxPlayers;
    }

}
