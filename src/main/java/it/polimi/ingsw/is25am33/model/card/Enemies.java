package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;

public abstract class Enemies extends AdventureCard {

    /**
     * Default constructor for the Enemies class.
     * Initializes an instance of an enemy. This constructor is used as part of the
     * abstract Enemies class, which may be extended by specific enemy types.
     */
    Enemies() {}

    /**
     * Represents the number of steps a player is required to move back
     * when interacting with an enemy in the context of the game.
     * This variable is configurable and can be set or retrieved by
     * the corresponding setter and getter methods in the class.
     */
    protected int stepsBack;
    /**
     * Represents the firepower required to defeat the enemy.
     * This value is used to determine the player's necessary strength
     * to overcome the enemy during gameplay.
     */
    protected int requiredFirePower;

    /**
     * Sets the number of steps the player or entity must move back when encountering this enemy.
     *
     * @param stepsBack the number of steps to move back
     */
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    /**
     * Sets the required firepower for the enemy.
     *
     * @param requiredFirePower the firepower value required to overcome the enemy
     */
    public void setRequiredFirePower(int requiredFirePower) {
        this.requiredFirePower = requiredFirePower;
    }

    /**
     * Retrieves the number of steps to move back associated with the current enemy.
     *
     * @return the number of steps the player is required to move back
     */
    public int getStepsBack() {
        return stepsBack;
    }

    /**
     * Returns the required firepower needed to defeat the enemy.
     *
     * @return the required firepower as an integer
     */
    public int getRequiredFirePower() {
        return requiredFirePower;
    }

}
