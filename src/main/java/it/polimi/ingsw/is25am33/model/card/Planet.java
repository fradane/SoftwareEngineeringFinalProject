package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.io.Serializable;
import java.util.List;

public class Planet implements Serializable {

    /**
     * Indicates whether the planet is currently occupied or unavailable for operations.
     * This variable is set to {@code true} when the planet is marked as busy,
     * and {@code false} when it is free for use.
     */
    private boolean isBusy = false;
    /**
     * Represents the list of cargo cubes that serve as a reward associated with the planet.
     * Each element in the list is an instance of the {@link CargoCube} enum, representing
     * a specific type of cargo cube with an associated value. The reward can be assigned
     * or modified to reflect the available rewards for the planet.
     */
    private List<CargoCube> reward;

    /**
     * Constructs a new Planet with the specified list of CargoCubes as a reward.
     *
     * @param cargoCubes a list of CargoCubes that represents the reward for the planet
     */
    public Planet(List<CargoCube> cargoCubes) {
        this.isBusy = false;
        this.reward = cargoCubes;
    }

    /**
     * Default constructor for the Planet class.
     * Initializes a new Planet instance with default values.
     */
    public Planet() {}

    /**
     * Sets the list of CargoCube rewards for the planet.
     *
     * @param reward the list of CargoCube objects to be assigned as rewards.
     *               Each CargoCube represents a specific type of reward with an associated value.
     */
    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    /**
     * Marks the state of the planet as busy, indicating that it is no
     * longer available for further interactions or operations.
     */
    public void setNoMoreAvailable() {
        isBusy = true;
    }

    /**
     * Indicates whether the planet is currently busy.
     *
     * @return true if the planet is marked as busy, false otherwise.
     */
    public boolean isBusy() {
        return isBusy;
    }

    /**
     * Sets the busy status of the planet.
     *
     * @param busy a boolean value indicating whether the planet is busy (true) or not (false)
     */
    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    /**
     * Retrieves the list of CargoCube objects associated with the reward.
     *
     * @return a list of CargoCube objects representing the reward
     */
    public List<CargoCube> getReward() {
        return reward;
    }
    
}
