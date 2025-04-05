package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

/**
 * Represents a life support component, extending the {@code Component} class.
 */
public class LifeSupport extends Component {

    /**
     * The color associated with the life support system.
     */
    private ColorLifeSupport color;

    /**
     * Default constructor for {@code LifeSupport}.
     */
    public LifeSupport() {}

    /**
     * Constructor that allows initializing the life support with specified connectors and color.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param color the {@code ColorLifeSupport} associated with this component
     */
    public LifeSupport(Map<Direction, ConnectorType> connectors, ColorLifeSupport color) {
        super(connectors);
        this.color = color;
    }

    /**
     * Gets the color of the life support system.
     *
     * @return the {@code ColorLifeSupport} of this component
     */
    public ColorLifeSupport getLifeSupportColor() {
        return color;
    }

    /**
     * Sets the color of the life support system.
     *
     * @param color the new {@code ColorLifeSupport} to be assigned
     */
    public void setLifeSupportColor(ColorLifeSupport color) {
        this.color = color;
    }
}

