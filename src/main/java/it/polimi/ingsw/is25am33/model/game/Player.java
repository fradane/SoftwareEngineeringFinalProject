package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;

import java.io.Serializable;

/**
 * Represents a player in the game, encapsulating properties such as nickname, credits,
 * personal game board, color, and notifier for client communication.
 * Manages player-specific actions and state, including credit management,
 * early landing status, and interactions with the game client through notifications.
 *
 * Implements Serializable to allow the player's state to be serialized when needed.
 */
public class Player implements Serializable {
    /**
     * The nickname of the player. This is a unique identifier for the player
     * within the game and is used to distinguish them from other players.
     */
    private final String nickname;
    /**
     * Represents the number of credits owned by the player.
     * This value directly tracks the player's in-game currency or resources.
     */
    private int ownedCredits;
    /**
     * Handles the communication between the player and the game clients.
     * Acts as a mediator to send notifications such as credit updates,
     * disconnection alerts, and game state changes to the associated clients.
     *
     * This variable is used for notifying individual players or broadcasting messages
     * to multiple or all connected clients in the game. It ensures that the game state
     * remains synchronized across all clients.
     */
    private GameClientNotifier gameClientNotifier;
    /**
     * Represents the player's personal ShipBoard, which holds the ship's components
     * and configurations. This field is initialized when the Player is created and
     * remains constant throughout the player's lifetime.
     */
    private final ShipBoard personalBoard;
    /**
     * Represents the color associated with the player.
     * This color uniquely identifies the player's team or position in the game.
     */
    private final PlayerColor playerColor;
    /**
     * Indicates whether the player has landed earlier than expected
     * in a game or scenario. This flag is used to track or manage
     * game mechanics related to early landing events.
     */
    private boolean isEarlyLanded;

    /**
     * Constructs a Player instance with the specified nickname, personal game board, and player color.
     *
     * @param nickname the nickname of the player
     * @param personalBoard the player's personal game board
     * @param playerColor the color representing the player
     */
    public Player(String nickname, ShipBoard personalBoard, PlayerColor playerColor) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
        this.playerColor = playerColor;
    }

    /**
     * Retrieves the color associated with the player.
     *
     * @return the player's color as a {@link PlayerColor} enumeration value.
     */
    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    /**
     * Sets the {@link GameClientNotifier} instance for this player.
     *
     * @param gameClientNotifier The {@code GameClientNotifier} responsible for handling client notifications
     *                           associated with this player.
     */
    public void setGameClientNotifier(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    /**
     * Retrieves the nickname associated with this object.
     *
     * @return the nickname as a String
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Updates the player's owned credits by adding the specified amount.
     * Notifies all connected clients about the updated credits for the player.
     *
     * @param number The number of credits to add to the player's owned credits.
     */
    public void addCredits(int number) {

        ownedCredits += number;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerCredits(nicknameToNotify, nickname, ownedCredits);
        });

    }

    /**
     * Retrieves the personal board associated with the player.
     *
     * @return the ShipBoard object representing the player's personal board.
     */
    public ShipBoard getPersonalBoard() {
        return personalBoard;
    }

    /**
     * Retrieves the personal board represented as a matrix of components.
     *
     * @return A two-dimensional array of Component objects representing the current state of the personal board.
     */
    public Component[][] getPersonalBoardAsMatrix() {
        return personalBoard.getShipMatrix();
    }

    /**
     * Retrieves the number of credits owned by the player.
     *
     * @return the number of credits currently owned by the player
     */
    public int getOwnedCredits() {
        return ownedCredits;
    }

    /**
     * Sets the number of credits owned by the player.
     *
     * @param ownedCredits the number of credits to be set as owned by the player
     */
    public void setOwnedCredits(int ownedCredits) {
        this.ownedCredits = ownedCredits;
    }

    /**
     * Checks whether the player has landed early.
     *
     * @return true if the player has landed early, false otherwise.
     */
    public boolean isEarlyLanded() {
        return isEarlyLanded;
    }

    /**
     * Sets the early landed status for the player.
     *
     * @param earlyLanded a boolean indicating whether the player has landed early
     */
    public void setEarlyLanded(boolean earlyLanded) {
        isEarlyLanded = earlyLanded;
    }
}

