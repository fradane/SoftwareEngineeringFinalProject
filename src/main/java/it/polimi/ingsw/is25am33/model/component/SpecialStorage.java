package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.Map;

/**
 * Represents a special storage component, extending the {@code Storage} class.
 */
public class SpecialStorage extends Storage {

    /**
     * Default constructor for {@code SpecialStorage}.
     */
    public SpecialStorage() {
        type = "SpecialStorage";
    }

    /**
     * Constructor that allows initializing the special storage with specified connectors and capacity.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param maxCapacity the maximum storage capacity
     */
    public SpecialStorage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors, maxCapacity);
    }
}
