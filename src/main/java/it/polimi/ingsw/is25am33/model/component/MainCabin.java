package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents the main cabin of a structure, extending the {@code Cabin} class.
 */
public class MainCabin extends Cabin implements Serializable {

    /**
     * The color associated with the main cabin, representing the player's color.
     */
    private PlayerColor color;

    /**
     * Default constructor for {@code MainCabin}.
     */
    public MainCabin() {
        type = "MainCabin";
    }

    /**
     * Constructor that allows initializing the main cabin with specified connectors and player color.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param color the {@code PlayerColor} associated with this main cabin
     */
    public MainCabin(Map<Direction, ConnectorType> connectors, PlayerColor color) {
        super(connectors);
        this.color = color;
    }

    @Override
    public String toString() {
        return "MainCabin {" +
                ", connectors = " + this.getConnectors() +
                ", MainCabinColor = " + color +
                '}';
    }


    /**
     * Gets the color of the main cabin.
     *
     * @return the {@code PlayerColor} of this main cabin
     */
    public PlayerColor getColor() {
        return color;
    }
}
