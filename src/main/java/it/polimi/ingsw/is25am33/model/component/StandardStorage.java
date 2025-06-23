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

    /**
     * Retrieves the name of this component.
     *
     * @return the name of the component as a string
     */
    @Override
    @JsonIgnore
    public String getComponentName() {
        return "StandardStorage";
    }

    /**
     * Validates the specified CargoCube and ensures it adheres to the restrictions
     * of the StandardStorage. Throws an exception if the cube is not allowed.
     *
     * @param cube the CargoCube to be validated
     * @throws IllegalArgumentException if the cube is RED, as it is not permitted in StandardStorage
     */
    protected void validateCube(CargoCube cube) {
        if (cube == CargoCube.RED) {
            throw new IllegalArgumentException("Red cube in StandardStorage");
        }
    }

    /**
     * Retrieves the label associated with the component.
     * The label is a short, unique string designed to represent the specific type of component.
     *
     * @return the label of the component as a string
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "STS";
    }

}
