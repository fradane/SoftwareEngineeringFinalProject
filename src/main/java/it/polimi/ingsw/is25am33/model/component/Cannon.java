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
     * Changes the cannon's orientation and updates the power direction accordingly.
     */
    @Override
    public void rotate() {
        super.rotate();
        fireDirection = shiftDirection(fireDirection);
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "CNN";
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
