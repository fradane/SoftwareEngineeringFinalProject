package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;

public abstract class AdvancedEnemies extends Enemies {

    public AdvancedEnemies() {}

    protected int reward;

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    @Override
    public ClientCard toClientCard() {
        //TODO
        return null;
    }
}
