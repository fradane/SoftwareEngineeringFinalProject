package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

public abstract class AdvancedEnemies extends Enemies {

    public AdvancedEnemies(Game game) {
        super(game);
    }

    protected int reward;

}
