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

    /**
     * Retrieves the name of this structural module component.
     *
     * @return the name of the component as a string, which is "StructuralModules"
     */
    @Override
    @JsonIgnore
    public String getComponentName() {
        return "StructuralModules";
    }

    /**
     * Retrieves the label associated with the component.
     * The label is a short string identifier used to represent the component type.
     *
     * @return the label of the component as a string
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "STR";
    }

    /**
     * Retrieves a string representation of the main attribute for a structural module component.
     * For this component, the main attribute is represented as a blank space.
     *
     * @return a string representing the main attribute, which is a blank space
     */
    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return " ";
    }

}
