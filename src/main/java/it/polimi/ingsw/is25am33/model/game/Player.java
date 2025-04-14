package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;

import java.io.Serializable;

public class Player implements Serializable {
    private final String nickname;
    private int ownedCredits;
    private final ShipBoard personalBoard;
    private final PlayerColor playerColor;

    public Player(String nickname, ShipBoard personalBoard, PlayerColor playerColor) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
        this.playerColor = playerColor;
    }

    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    public void addCredits(int number) {
        ownedCredits += number;
    }

    public ShipBoard getPersonalBoard() {
        return personalBoard;
    }

    public int getOwnedCredits() {
        return ownedCredits;
    }

    public void setOwnedCredits(int ownedCredits) {
        this.ownedCredits = ownedCredits;
    }

    public String getNickname() {
        return nickname;
    }
}

