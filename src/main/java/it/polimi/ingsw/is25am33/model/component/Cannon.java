package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

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

    /**
     * Returns the current firing direction of the cannon.
     *
     * @return the cannon's firing direction
     */
    public Direction getFireDirection() {
        return fireDirection;
    }

    /**
     * Rotates the firing direction of the cannon based on its rotation state.
     * <p>
     * The direction is shifted clockwise once for each rotation step modulo 4.
     * </p>
     */
    public void rotateFireDirection() {
        for (int i = 0; i < getRotation() % 4; i++)
            this.fireDirection = shiftDirection(this.fireDirection);
    }

    /**
     * Changes the cannon's orientation and updates the power direction accordingly.
     */
    @Override
    public void changeOrientation() {
        super.changeOrientation();
        rotateFireDirection();
    }
}
