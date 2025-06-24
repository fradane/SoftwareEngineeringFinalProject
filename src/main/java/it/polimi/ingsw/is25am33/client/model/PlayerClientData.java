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

    public PlayerClientData(String nickname, PlayerColor color, boolean isTestFlight, boolean isGui) {
        this.nickname = nickname;
        this.credits = 0;
        this.color = color;
        this.shipBoard = isTestFlight ? new Level1ShipBoard(color, new GameClientNotifier(null, new ConcurrentHashMap<>()), isGui) : new Level2ShipBoard(color, new GameClientNotifier(null, new ConcurrentHashMap<>()), isGui);
        this.shipBoard.setPlayer(new Player(nickname, (ShipBoard) this.shipBoard, color));
        this.flyingBoardPosition = 0;
        this.isOut = false;
    }

    public PlayerColor getColor() {
        return color;
    }

    public void setColor(PlayerColor color) {
        this.color = color;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getFlyingBoardPosition() {
        return flyingBoardPosition;
    }

    public void setFlyingBoardPosition(int flyingBoardPosition) {
        this.flyingBoardPosition = flyingBoardPosition;
    }

    public ShipBoardClient getShipBoard() {
        return shipBoard;
    }

    public void setShipBoard(ShipBoardClient shipBoard) {
        this.shipBoard = shipBoard;
    }

    public void setLanded(boolean isLanded) {
        this.isLanded = isLanded;
    }

    public boolean isLanded() {
        return isLanded;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean out) {
        isOut = out;
    }
}
