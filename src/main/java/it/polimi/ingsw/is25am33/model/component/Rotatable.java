package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.Direction;

/**
 * Represents a rotatable component that can change orientation.
 * This interface provides a method to shift a direction based on a 90-degree rotation.
 */
public interface Rotatable {

    /**
     * Shifts the given direction by 90 degrees clockwise.
     *
     * @param direction the direction to rotate
     * @return the new direction after rotation
     * @throws IllegalArgumentException if the direction is invalid (not one of the predefined directions)
     */
    default Direction shiftDirection(Direction direction) throws IllegalArgumentException {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };
    }
}

