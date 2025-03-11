package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.Direction;

public interface Rotatable {
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
