package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

public abstract class AdventureCard {

    protected int level;

    public abstract void effect(Game game);

    public int getLevel() {
        return level;
    }
}
