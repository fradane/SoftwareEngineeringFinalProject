package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.board.Level1ShipBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the client-side data of a player in the game.
 * This class is used to manage information specific to a player,
 * such as their nickname, associated color, current credits,
 * position on the flying board, and their ship board data.
 */
public class PlayerClientData {

    private String nickname;
    private int credits;
    private boolean isLanded;
    private int flyingBoardPosition;
    private ShipBoardClient shipBoard;
    private PlayerColor color;
    private boolean isOut;

    /**
     * Constructs a new PlayerClientData instance.
     *
     * @param nickname     The player's nickname
     * @param color        The player's color
     * @param isTestFlight Whether the game is in test flight mode
     * @param isGui        Whether the game is using GUI
     */
    public PlayerClientData(String nickname, PlayerColor color, boolean isTestFlight, boolean isGui) {
        this.nickname = nickname;
        this.credits = 0;
        this.color = color;
        this.shipBoard = isTestFlight ? new Level1ShipBoard(color, new GameClientNotifier( new ConcurrentHashMap<>()), isGui) : new Level2ShipBoard(color, new GameClientNotifier( new ConcurrentHashMap<>()), isGui);
        this.shipBoard.setPlayer(new Player(nickname, (ShipBoard) this.shipBoard, color));
        this.flyingBoardPosition = 0;
        this.isOut = false;
    }

    /**
     * Gets the player's color.
     *
     * @return The PlayerColor assigned to this player
     */
    public PlayerColor getColor() {
        return color;
    }

    /**
     * Sets the player's color.
     *
     * @param color The PlayerColor to assign to this player
     */
    public void setColor(PlayerColor color) {
        this.color = color;
    }

    /**
     * Gets the player's nickname.
     *
     * @return The nickname of the player
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the player's nickname.
     *
     * @param nickname The new nickname for the player
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Gets the player's current credits.
     *
     * @return The number of credits the player has
     */
    public int getCredits() {
        return credits;
    }

    /**
     * Sets the player's credits.
     *
     * @param credits The number of credits to set
     */
    public void setCredits(int credits) {
        this.credits = credits;
    }

    /**
     * Gets the player's position on the flying board.
     *
     * @return The current position on the flying board
     */
    public int getFlyingBoardPosition() {
        return flyingBoardPosition;
    }

    /**
     * Sets the player's position on the flying board.
     *
     * @param flyingBoardPosition The new position on the flying board
     */
    public void setFlyingBoardPosition(int flyingBoardPosition) {
        this.flyingBoardPosition = flyingBoardPosition;
    }

    /**
     * Gets the player's ship board.
     *
     * @return The ShipBoardClient instance associated with this player
     */
    public ShipBoardClient getShipBoard() {
        return shipBoard;
    }

    /**
     * Sets the player's ship board.
     *
     * @param shipBoard The new ShipBoardClient to assign to this player
     */
    public void setShipBoard(ShipBoardClient shipBoard) {
        this.shipBoard = shipBoard;
    }

    /**
     * Sets whether the player has landed.
     *
     * @param isLanded True if the player has landed, false otherwise
     */
    public void setLanded(boolean isLanded) {
        this.isLanded = isLanded;
    }

    /**
     * Checks if the player has landed.
     *
     * @return True if the player has landed, false otherwise
     */
    public boolean isLanded() {
        return isLanded;
    }

    /**
     * Checks if the player is out of the game.
     *
     * @return True if the player is out, false otherwise
     */
    public boolean isOut() {
        return isOut;
    }

    /**
     * Sets whether the player is out of the game.
     *
     * @param out True to set the player as out, false otherwise
     */
    public void setOut(boolean out) {
        isOut = out;
    }
}
