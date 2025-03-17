package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;

public class Player {
    private String nickname;
    private int ownedCredits;
    private final ShipBoard personalBoard;

    public Player(String nickname, ShipBoard personalBoard) {
        this.nickname = nickname;
        this.personalBoard = personalBoard;
        ownedCredits = 0;
    }

    public void addCredits(int number) {
        ownedCredits += number;
    }

    public ShipBoard getPersonalBoard() {
        return personalBoard;
    }
}
