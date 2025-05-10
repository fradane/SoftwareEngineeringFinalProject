package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a standard storage component, extending the {@code Storage} class.
 */
public class StandardStorage extends Storage {

    /**
     * Default constructor for {@code StandardStorage}.
     */
    public StandardStorage() {
        type = "StandardStorage";
    }

    /**
     * Constructor that allows initializing the standard storage with specified connectors and capacity.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param maxCapacity the maximum storage capacity
     */
    public StandardStorage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors, maxCapacity);
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
            StandardStorage
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            MaxCapacity: %d
            """, north, west, east, south, getMaxCapacity());
    }

    /**
     * Adds a {@code CargoCube} to the storage, ensuring that only non-red cubes are added.
     *
     * @param cube the {@code CargoCube} to add
     * @throws IllegalArgumentException if the cube is red, as red cubes cannot be stored in standard storage
     */
    public void addCube(CargoCube cube) throws IllegalArgumentException {
        if (cube == CargoCube.RED) {
            throw new IllegalArgumentException("Red cube in StandardStorage");
        }
        getStockedCubes().add(cube);
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "STS";
    }

}
