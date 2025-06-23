package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a shield component, extending the {@code Component} class.
 * Implements the {@code Activable} and {@code Rotatable} interfaces.
 */
public class Shield extends Component implements Activable, Rotatable {

    /**
     * The directions in which the shield is active.
     * By default, it is set to {@code Direction.NORTH} and {@code Direction.EAST}.
     */
    private final List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.EAST));

    /**
     * Default constructor for {@code Shield}.
     */
    public Shield() {
        type = "Shield";
    }

    /**
     * Constructor that allows initializing the shield with specified connectors.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     */
    public Shield(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }

    /**
     * Retrieves the name of the component.
     *
     * @return the name of the component as a string
     */
    public String getComponentName() {
        return "Shield";
    }

    /**
     * Gets the active shield directions.
     *
     * @return a list of {@code Direction} where the shield is active
     */
    public List<Direction> getDirections() {
        return directions;
    }


    /**
     * Rotates the shield component by 90 degrees clockwise.
     *
     * This method updates the shield's active directions by using {@link #shiftDirection(Direction)}
     * to compute the new directions after rotation. It also invokes the base implementation
     * of {@code rotate} in the {@code Component} class to adjust the orientation of connectors.
     */
    @Override
    public void rotate() {
        super.rotate();
        directions.replaceAll(this::shiftDirection);
    }

    /**
     * Retrieves the label associated with the shield component.
     * The label is a short string identifier used to represent the component type.
     *
     * @return the label of the shield component as a string ("SLD")
     */
    @Override
    @JsonIgnore
    public String getLabel() {
        return "SLD";
    }

    /**
     * Retrieves a string representation of the main attribute for the shield component.
     * The main attribute corresponds to a shorthand notation of the active directions,
     * concatenated together. Each direction is represented as:
     * - "N" for NORTH
     * - "S" for SOUTH
     * - "W" for WEST
     * - "E" for EAST
     *
     * @return a string representing the main attribute of the shield,
     *         concatenating all active directions' shorthand notations.
     */
    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return directions.stream()
                .map(direction -> switch (direction) {
                    case NORTH -> "N";
                    case SOUTH -> "S";
                    case WEST -> "W";
                    case EAST -> "E";
                })
                .collect(Collectors.joining());
    }

}
