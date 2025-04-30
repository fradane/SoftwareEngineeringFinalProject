package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

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
        return "StructuralModules{" +
                ", connectors = " + this.getConnectors() +
                '}';
    }

}
