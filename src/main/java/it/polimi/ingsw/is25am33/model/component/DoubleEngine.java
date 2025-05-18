package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a double engine, extending the {@code Engine} class.
 * Implements the {@code Activable} interface.
 */
public class DoubleEngine extends Engine implements Activable {

    /**
     * Default constructor for {@code DoubleEngine}.
     */
    public DoubleEngine() {
        type = "DoubleEngine";
    }

    /**
     * Constructor that allows initializing the double engine
     * with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public DoubleEngine(Map<Direction, ConnectorType> connectors) {
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
            DoubleEngine
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            fireDirection: %s
            """,imageName, north, west, east, south, getPowerDirection());
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "2EN";
    }

}