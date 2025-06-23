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

    /**
     * Retrieves the name of the component.
     *
     * @return the name of the component as a string
     */
    public String getComponentName() {
        return "DoubleEngine";
    }

    /**
     * Retrieves the label associated with this component.
     * The label is a short identifier used to represent the component.
     *
     * @return the label of the component as a string
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "2EN";
    }

}