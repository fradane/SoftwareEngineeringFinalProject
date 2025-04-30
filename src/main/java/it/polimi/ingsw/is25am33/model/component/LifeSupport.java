package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.Map;

/**
 * Represents a life support component, extending the {@code Component} class.
 */
public class LifeSupport extends Component {

    /**
     * The color associated with the life support system.
     */
    private ColorLifeSupport lifeSupportColor;

    /**
     * Default constructor for {@code LifeSupport}.
     */
    public LifeSupport() {
        type = "LifeSupport";
    }

    /**
     * Constructor that allows initializing the life support with specified connectors and color.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param color the {@code ColorLifeSupport} associated with this component
     */
    public LifeSupport(Map<Direction, ConnectorType> connectors, ColorLifeSupport color) {
        super(connectors);
        this.lifeSupportColor = color;
    }

    @Override
    public String toString() {
        return "LifeSupport {" +
                ", connectors = " + this.getConnectors() +
                ", lifeSupportColor = " + lifeSupportColor +
                '}';
    }


    /**
     * Gets the color of the life support system.
     *
     * @return the {@code ColorLifeSupport} of this component
     */
    public ColorLifeSupport getLifeSupportColor() {
        return lifeSupportColor;
    }

    /**
     * Sets the color of the life support system.
     *
     * @param color the new {@code ColorLifeSupport} to be assigned
     */
    public void setLifeSupportColor(ColorLifeSupport color) {
        this.lifeSupportColor = color;
    }
}

