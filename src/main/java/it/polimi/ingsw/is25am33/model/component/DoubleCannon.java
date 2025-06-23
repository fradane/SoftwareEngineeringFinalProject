package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * Retrieves the name of the component.
     *
     * @return the name of the component as a string
     */
    public String getComponentName() {
        return "DoubleCannon";
    }

    /**
     * Retrieves the label associated with the component.
     * The label is a concise identifier representing the component type.
     *
     * @return the label of the component as a string
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "2CN";
    }

}

