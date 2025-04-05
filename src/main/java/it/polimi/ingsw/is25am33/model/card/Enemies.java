package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

public abstract class Enemies extends AdventureCard {

    Enemies() {}

    protected int stepsBack;
    protected int requiredFirePower;

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setRequiredFirePower(int requiredFirePower) {
        this.requiredFirePower = requiredFirePower;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getRequiredFirePower() {
        return requiredFirePower;
    }
}
