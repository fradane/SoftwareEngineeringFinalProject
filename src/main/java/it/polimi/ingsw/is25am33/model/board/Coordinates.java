package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the coordinates of a component on the shipBoard.
 * This class stores a pair of integer values corresponding to the X and Y positions.
 * Coordinates are immutable once created.
 */
public class Coordinates {

    private List<Integer> coordinates;

    /**
     * Constructs a new ShipBoardCoordinates object with the specified X and Y coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public Coordinates(int x, int y) {
        coordinates = Arrays.asList(x, y);
    }

    /**
     * Empty constructor needed for Jackson
     */
    public Coordinates() {}

    /**
     * Sets the coordinates
     *
     * @param coordinates
     */
    public void setCoordinates(List<Integer> coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Returns the X coordinate.
     *
     * @return the X coordinate
     */
    @JsonIgnore
    public int getX() {
        return coordinates.get(0);
    }

    /**
     * Returns the Y coordinate.
     *
     * @return the Y coordinate
     */
    @JsonIgnore
    public int getY() {
        return coordinates.get(1);
    }

    /**
     * Returns the List containing the coordinates
     *
     * @return the coordinates
     */
    public List<Integer> getCoordinates() {
        return coordinates;
    }

    /**
     * Checks if both X and Y coordinates are non-negative.
     *
     * @return true if both coordinates are greater than or equal to 0; false otherwise
     */
    public boolean isPositive() {
        return getX() >= 0 && getY() >= 0;
    }

    /**
     * Determines whether the current coordinates are valid for use in a ComponentTable.
     * The coordinates must be non-negative, within the bounds of the table,
     * and correspond to a non-null component in the table.
     *
     * @param componentTable the ComponentTable to validate against
     * @return true if the coordinates are valid for the given ComponentTable; false otherwise
     */
    public boolean isValidForComponentTable(ComponentTable componentTable) {

        if (!isPositive() || componentTable == null) return false;

        if (getX() >= 13 || getY() >= 12) return false;

        return componentTable.getComponent(this) != null;

    }

}