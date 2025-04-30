package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.List;
import java.util.Map;

/**
 * Represents a shield component, extending the {@code Component} class.
 * Implements the {@code Activable} and {@code Rotatable} interfaces.
 */
public class Shield extends Component implements Activable, Rotatable {

    /**
     * The directions in which the shield is active.
     * By default, it is set to {@code Direction.NORTH} and {@code Direction.EAST}.
     */
    private List<Direction> direction = List.of(Direction.NORTH, Direction.EAST);

    /**
     * Default constructor for {@code Shield}.
     */
    public Shield() {
        type = "Shield";
    }

    /**
     * Constructor that allows initializing the shield with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public Shield(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }

    @Override
    public String toString() {
        return "Shield{" +
                ", connectors = " + this.getConnectors() +
                ", direction = " + direction +
                '}';
    }


    /**
     * Gets the active shield directions.
     *
     * @return a list of {@code Direction} where the shield is active
     */
    public List<Direction> getDirections() {
        return direction;
    }

    /**
     * Rotates the shield's active directions based on its rotation.
     */
    public void setDirection() {
        for (int i = 0; i < getRotation() % 4; i++) {
            this.direction.set(0, shiftDirection(this.direction.get(0)));
            this.direction.set(1, shiftDirection(this.direction.get(1)));
        }
    }

    /**
     * Changes the shield's orientation and updates its active directions accordingly.
     */
    @Override
    public void changeOrientation() {
        super.changeOrientation();
        setDirection();
    }
}
