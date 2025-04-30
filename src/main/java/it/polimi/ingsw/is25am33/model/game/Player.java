package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameContext;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;

import java.io.Serializable;
import java.rmi.RemoteException;

public class Player implements Serializable {
    private final String nickname;
    private int ownedCredits;
    private GameContext gameContext;
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

    public void setGameContext(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    public GameContext getGameContext() {
        return gameContext;
    }

    public String getNickname() {
        return nickname;
    }

    public void addCredits(int number) {
        try{
            ownedCredits += number;

            for(String s: gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(s).notifyPlayerCredits(s, nickname, ownedCredits );
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }
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

}

