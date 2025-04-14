package it.polimi.ingsw.is25am33.model.card;

public abstract class AdvancedEnemies extends Enemies {

    public AdvancedEnemies() {}

    protected int reward;

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }
}
