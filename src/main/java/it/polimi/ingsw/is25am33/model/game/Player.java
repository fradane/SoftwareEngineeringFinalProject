package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameContext;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.ObserverManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;

import java.util.function.BiConsumer;

public class Player {
    private String nickname;
    private int ownedCredits;
    private GameContext gameContext;
    private final ShipBoard personalBoard;

    public Player(String nickname, ShipBoard personalBoard) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
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

        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "creditsUpdate", dto ), notifyCredits);

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

