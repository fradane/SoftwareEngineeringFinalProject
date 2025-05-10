package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a structural module component, extending the {@code Component} class.
 * This class is used to define the basic properties of a structural module in the system.
 */
public class StructuralModules extends Component {

    /**
     * Default constructor for {@code StructuralModules}.
     */
    public StructuralModules() {
        type = "StructuralModules";
    }

    /**
     * Constructor that allows initializing the structural module with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public StructuralModules(Map<Direction, ConnectorType> connectors) {
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
            StructuralModules
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            MaxCapacity: %d
            """, north, west, east, south);
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "STR";
    }

    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return " ";
    }

}
