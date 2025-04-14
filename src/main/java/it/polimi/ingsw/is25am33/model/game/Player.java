package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameContext;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.ObserverManager;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;

import java.util.function.BiConsumer;

import java.io.Serializable;

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
        ownedCredits += number;

        DTO dto = new DTO();
        dto.setPlayer(this);
        dto.setNum(ownedCredits);

        BiConsumer<Observer,String> notifyCredits= Observer::notifyPlayerCredits;

        //gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "creditsUpdate", dto ), notifyCredits);

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

}

