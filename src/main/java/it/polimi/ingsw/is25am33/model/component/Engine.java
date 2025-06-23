package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents an engine component, extending the {@code Component} class.
 * Implements the {@code Rotatable} interface.
 */
public class Engine extends Component implements Rotatable, Serializable {

    /**
     * The direction in which the engine provides power.
     * Defaults to {@code Direction.SOUTH}.
     */
    private Direction fireDirection = Direction.SOUTH;

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

    /**
     * Retrieves the name of this component.
     *
     * @return a string representing the name of the component
     */
    @Override
    @JsonIgnore
    public String getComponentName() {
        return "Engine";
    }

    /**
     * Gets the current power direction of the engine.
     *
     * @return the current {@code Direction} of power output
     */
    public Direction getFireDirection() {
        return fireDirection;
    }


    /**
     * Rotates the engine component and updates its firing direction.
     *
     * This method overrides the {@code rotate} method in the parent class to provide
     * additional functionality specific to the {@code Engine} component. It performs the following:
     * 1. Calls the parent class's {@code rotate} method to adjust the orientation of connectors.
     * 2. Updates the firing direction of the engine by shifting it 90 degrees clockwise
     *    using the {@code shiftDirection} utility method.
     */
    @Override
    public void rotate() {
        super.rotate();
        fireDirection = shiftDirection(fireDirection);
    }

    /**
     * Retrieves the label associated with the component.
     * The label is a short string identifier used to represent the component type.
     *
     * @return a string representing the component type label
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "ENG";
    }

    /**
     * Retrieves a string representation of the main attribute for the component.
     * The main attribute corresponds to the current firing direction of the engine.
     * It returns a shorthand notation of the direction as follows:
     * - "N" for NORTH
     * - "S" for SOUTH
     * - "W" for WEST
     * - "E" for EAST
     *
     * @return a string representing the main attribute, which corresponds to the current firing direction
     */
    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return switch (fireDirection) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
        };
    }

}
