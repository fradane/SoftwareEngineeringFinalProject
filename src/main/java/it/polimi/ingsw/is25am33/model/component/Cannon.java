package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a Cannon component capable of firing in a specified direction.
 * The firing direction can be rotated based on component orientation.
 */
public class Cannon extends Component implements Rotatable {

    /** The current direction in which this cannon fires. */
    private Direction fireDirection = Direction.NORTH;

    /**
     * Default constructor for {@code Cannon}.
     */
    public Cannon() {
        type = "Cannon";
    }

    /**
     * Constructs a Cannon with specified connectors and initial firing direction.
     *
     * @param connectors    a map associating directions with connector types
     */
    public Cannon(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }

    public String getComponentName() {
        return "Cannon";
    }

    /**
     * Returns the current firing direction of the cannon.
     *
     * @return the cannon's firing direction
     */
    public Direction getFireDirection() {
        return fireDirection;
    }


    /**
     * Rotates the cannon's orientation and adjusts its firing direction accordingly.
     *
     * This method first invokes the {@link Component#rotate()} method to adjust the connectors
     * based on the component's new rotation. Then, it shifts the cannon's firing direction
     * 90 degrees clockwise using the {@link #shiftDirection(Direction)} method.
     *
     * The {@code fireDirection} field is updated to reflect the new angular orientation
     * after the rotation.
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
     * @return the label of the component as a string
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "CNN";
    }

    /**
     * Retrieves a string representation of the main attribute for the component.
     * For this component, it returns the shorthand notation of the current
     * firing direction as follows:
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
