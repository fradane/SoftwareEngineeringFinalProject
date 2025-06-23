package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;

public abstract class AdvancedEnemies extends Enemies {

    /**
     * Constructs an instance of the AdvancedEnemies class.
     * This constructor initializes the object with default values
     * for its attributes and provides the base for extending
     * advanced enemy functionality beyond the Enemies class.
     */
    public AdvancedEnemies() {}

    /**
     * Represents the reward points associated with an advanced enemy.
     * The reward is typically granted upon defeating the enemy.
     */
    protected int reward;

    /**
     * Retrieves the reward value associated with this instance.
     *
     * @return the reward value as an integer.
     */
    public int getReward() {
        return reward;
    }

    /**
     * Sets the reward value for an advanced enemy.
     *
     * @param reward the reward amount to assign
     */
    public void setReward(int reward) {
        this.reward = reward;
    }

    /**
     * Converts the current instance of the class to a corresponding {@code ClientCard}.
     *
     * @return a {@code ClientCard} representation of the current instance.
     */
    @Override
    public ClientCard toClientCard() {
        return null;
    }
}
