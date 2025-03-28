package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

public abstract class Enemies extends AdventureCard {

    Enemies(Game game) {
        super(game);
    }

    protected int stepsBack;
    protected int requiredFirePower;

}
