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

    @Override
    public String toString() {
        String north = getConnectors().get(Direction.NORTH) != null
                ? String.valueOf(getConnectors().get(Direction.NORTH).fromConnectorTypeToValue())
                : " ";
        String south = getConnectors().get(Direction.SOUTH) != null
                ? String.valueOf(getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue())
                : " ";
        String west  = getConnectors().get(Direction.WEST) != null
                ? String.valueOf(getConnectors().get(Direction.WEST).fromConnectorTypeToValue())
                : " ";
        String east  = getConnectors().get(Direction.EAST) != null
                ? String.valueOf(getConnectors().get(Direction.EAST).fromConnectorTypeToValue())
                : " ";

        return String.format("""
            %s
            Engine
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            fireDirection: %s
            """,imageName, north, west, east, south, fireDirection);
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
     * Rotates the power direction based on the engine's rotation.
     */
   /*public void rotateFireDirection() {
        for (int i = 0; i < getRotation() % 4; i++) {
            this.fireDirection = shiftDirection(this.fireDirection);
        }
    }*/

    /**
     * Changes the engine's orientation and updates the power direction accordingly.
     */
    @Override
    public void rotate() {
        super.rotate();
        fireDirection = shiftDirection(fireDirection);
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "ENG";
    }

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
