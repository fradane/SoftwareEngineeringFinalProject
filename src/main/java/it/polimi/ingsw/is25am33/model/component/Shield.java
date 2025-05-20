package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a shield component, extending the {@code Component} class.
 * Implements the {@code Activable} and {@code Rotatable} interfaces.
 */
public class Shield extends Component implements Activable, Rotatable {

    /**
     * The directions in which the shield is active.
     * By default, it is set to {@code Direction.NORTH} and {@code Direction.EAST}.
     */
    private final List<Direction> direction = new ArrayList<>(List.of(Direction.NORTH, Direction.EAST));

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
            Shield
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            fireDirection: %s
            """, imageName, north, west, east, south, direction);
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
     * Changes the shield's orientation and updates its active directions accordingly.
     */
    @Override
    public void rotate() {
        super.rotate();
        direction.replaceAll(this::shiftDirection);
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "SLD";
    }

    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return direction.stream()
                .map(direction -> switch (direction) {
                    case NORTH -> "N";
                    case SOUTH -> "S";
                    case WEST -> "W";
                    case EAST -> "E";
                })
                .collect(Collectors.joining());
    }

}
