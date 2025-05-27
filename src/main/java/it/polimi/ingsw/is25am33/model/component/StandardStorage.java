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
            %s
            StandardStorage
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            MaxCapacity: %d
            """,imageName, north, west, east, south, getMaxCapacity());
    }

    /**
     * Adds a {@code CargoCube} to the storage ensuring that only non-red cubes are added. If the storage is full,
     * it will replace the least valuable cube regardless of value comparison.
     *
     * @param cube the {@code CargoCube} to add
     * @return the {@code CargoCube} that was removed to make space, or null if storage wasn't full
     *  @throws IllegalArgumentException if the cube is red, as red cubes cannot be stored in standard storage
     */
    public CargoCube addCube(CargoCube cube) throws IllegalArgumentException {
        if (cube == CargoCube.RED) {
            throw new IllegalArgumentException("Red cube in StandardStorage");
        }

        // Se lo storage non è pieno, aggiungi semplicemente il cubo
        if (!isFull()) {
            getStockedCubes().add(cube);
            return null;
        }

        // Se lo storage è pieno, trova e rimuovi il cubo di valore minore
        CargoCube leastValuableCube = findLeastValuableCube();

        if (leastValuableCube != null) {
            getStockedCubes().remove(leastValuableCube);
            getStockedCubes().add(cube);
            return leastValuableCube;
        }

        // Caso teoricamente impossibile se isFull() è true
        getStockedCubes().add(cube);
        return null;
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "STS";
    }

}
