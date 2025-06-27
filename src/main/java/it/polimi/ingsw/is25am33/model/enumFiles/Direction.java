package it.polimi.ingsw.is25am33.model.enumFiles;

/**
 * Represents the four cardinal directions used in the game.
 * These directions are primarily used for orienting and rotating fire directions.
 * The order of enum constants is significant and must be preserved for proper rotation mechanics.
 */
public enum Direction {
    /**
     * North direction.
     */
    NORTH,

    /**
     * East direction.
     */
    EAST,

    /**
     * South direction.
     */
    SOUTH,

    /**
     * West direction.
     */
    WEST; // Don't change direction's order. Used in rotating fireDirection
}