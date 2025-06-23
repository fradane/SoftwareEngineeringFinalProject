package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the coordinates of a component on the shipBoard.
 * This class stores a pair of integer values corresponding to the X and Y positions.
 * Coordinates are immutable once created.
 */
public class Coordinates implements Serializable {

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
     * The values are set to negative integers for testing purpose only
     */
    public Coordinates() {
        this.coordinates = List.of(-1, -1);
    }

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
     * Returns a string representation of the Coordinates object in the format "{x = X, y = Y}",
     * where X is the value of the X coordinate and Y is the value of the Y coordinate.
     *
     * @return a string representation of the coordinates
     */
    @Override
    public String toString() {
        return "{x = " + getX() + ", y = " + getY() + "}";
    }

    /**
     * Compares the specified object with this Coordinates object for equality.
     * Two Coordinates objects are considered equal if their X and Y values are the same.
     *
     * @param obj the object to be compared for equality with this Coordinates object
     * @return true if the specified object is equal to this Coordinates object; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coordinates) {
            Coordinates other = (Coordinates) obj;
            return other.getX() == getX() && other.getY() == getY();
        }
        return false;
    }

    /**
     * Checks if the coordinates are invalid. Coordinates are considered invalid
     * if both the X and Y values are set to -1.
     *
     * @return true if the coordinates are invalid, false otherwise
     */
    @JsonIgnore
    public boolean isCoordinateInvalid() {
        return coordinates.getFirst() == -1 && coordinates.getLast() == -1;
    }


    /**
     * Computes the hash code for this Coordinates object using the X and Y coordinates.
     *
     * @return the hash code value for this Coordinates object
     */
    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

}