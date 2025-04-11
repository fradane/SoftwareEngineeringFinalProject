package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.ObserverManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;

public class Player {
    private String nickname;
    private int ownedCredits;
    private final ShipBoard personalBoard;

    public Player(String nickname, ShipBoard personalBoard) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
    }

    public String getNickname() {
        return nickname;
    }

    public void addCredits(int number) {
        ownedCredits += number;

        DTO dto = new DTO();
        dto.setPlayer(this);
        dto.setNum(ownedCredits);
        ObserverManager.getInstance().notifyAll(new GameEvent("creditsUpdate",dto));
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

