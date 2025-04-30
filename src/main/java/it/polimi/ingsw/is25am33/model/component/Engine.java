package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.Map;

/**
 * Represents an engine component, extending the {@code Component} class.
 * Implements the {@code Rotatable} interface.
 */
public class Engine extends Component implements Rotatable {

    /**
     * The direction in which the engine provides power.
     * Defaults to {@code Direction.SOUTH}.
     */
    private Direction powerDirection = Direction.SOUTH;

    /**
     * Default constructor for {@code Engine}.
     */
    public Engine() {
        type = "Engine";
    }

    /**
     * Constructor that allows initializing the engine with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public Engine(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }

    @Override
    public String toString() {
        return "Engine{" +
                ", connectors = " + this.getConnectors() +
                ", powerDirection = " + powerDirection +
                '}';
    }


    /**
     * Gets the current power direction of the engine.
     *
     * @return the current {@code Direction} of power output
     */
    public Direction getPowerDirection() {
        return powerDirection;
    }

    /**
     * Rotates the power direction based on the engine's rotation.
     */
    public void rotatePowerDirection() {
        for (int i = 0; i < getRotation() % 4; i++) {
            this.powerDirection = shiftDirection(this.powerDirection);
        }
    }

    /**
     * Changes the engine's orientation and updates the power direction accordingly.
     */
    @Override
    public void changeOrientation() {
        super.changeOrientation();
        rotatePowerDirection();
    }
}
