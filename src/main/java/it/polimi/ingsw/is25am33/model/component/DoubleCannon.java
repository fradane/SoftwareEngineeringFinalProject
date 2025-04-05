package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

/**
 * Represents a double engine, extending the {@code Cannon} class.
 * Implements the {@code Activable} interface.
 */

public class DoubleCannon extends Cannon implements Activable {

    /**
     * Default constructor for {@code DoubleCannon}.
     */
    public DoubleCannon() {}

    /**
     * Constructor that allows initializing the double cannon
     * with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public DoubleCannon(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }
}
