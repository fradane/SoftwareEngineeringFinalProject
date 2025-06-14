package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;

import java.io.Serializable;

public class Player implements Serializable {
    private final String nickname;
    private int ownedCredits;
    private GameClientNotifier gameClientNotifier;
    private final ShipBoard personalBoard;
    private final PlayerColor playerColor;
    private boolean isEarlyLanded;

    public Player(String nickname, ShipBoard personalBoard, PlayerColor playerColor) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
        this.playerColor = playerColor;
    }

    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    public void setGameClientNotifier(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    public GameClientNotifier getGameContext() {
        return gameClientNotifier;
    }

    public String getNickname() {
        return nickname;
    }

    public void addCredits(int number) {

        ownedCredits += number;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerCredits(nicknameToNotify, nickname, ownedCredits);
        });

    }

    public ShipBoard getPersonalBoard() {
        return personalBoard;
    }

    public Component[][] getPersonalBoardAsMatrix() {
        return personalBoard.getShipMatrix();
    }

    public int getOwnedCredits() {
        return ownedCredits;
    }

    public void setOwnedCredits(int ownedCredits) {
        this.ownedCredits = ownedCredits;
    }

    public boolean isEarlyLanded() {
        return isEarlyLanded;
    }

    public void setEarlyLanded(boolean earlyLanded) {
        isEarlyLanded = earlyLanded;
    }
}

