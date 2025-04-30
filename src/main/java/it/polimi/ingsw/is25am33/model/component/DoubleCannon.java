package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a double engine, extending the {@code Cannon} class.
 * Implements the {@code Activable} interface.
 */

public class DoubleCannon extends Cannon implements Activable {

    /**
     * Default constructor for {@code DoubleCannon}.
     */
    public DoubleCannon() {
        type = "DoubleCannon";
    }

    /**
     * Constructor that allows initializing the double cannon
     * with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public DoubleCannon(Map<Direction, ConnectorType> connectors) {
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
        String west = getConnectors().get(Direction.WEST) != null
                ? String.valueOf(getConnectors().get(Direction.WEST).fromConnectorTypeToValue())
                : " ";
        String east = getConnectors().get(Direction.EAST) != null
                ? String.valueOf(getConnectors().get(Direction.EAST).fromConnectorTypeToValue())
                : " ";

        return String.format("""
                DoubleCannon
                +---------+
                |    %s    |
                | %s     %s |
                |    %s    |
                +---------+
                fireDirection: %s
                """, north, west, east, south, getFireDirection());
    }

    @Override
    public String getLabel() {
        return "2CN";
    }

}

