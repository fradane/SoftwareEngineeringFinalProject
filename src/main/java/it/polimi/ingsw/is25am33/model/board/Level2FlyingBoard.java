package it.polimi.ingsw.is25am33.model.board;

import java.util.Map;

/**
 * The {@code Level2FlyingBoard} class extends {@code FlyingBoard} to represent
 * a specialized flying board with an additional feature of an hourglass position.
 * This class is used to manage the state and behavior of a level 2 flying board in the game.
 */
public class Level2FlyingBoard extends FlyingBoard {

    /**
     * The current position of the hourglass on this board.
     */
    private int hourglassPosition;

    /**
     * Constructs a new Level2FlyingBoard with a specified run length for the flying board.
     * Initializes the hourglass position to the starting point (0).
     */
    public Level2FlyingBoard() {
        super(13); // Pass the runLength to the superclass constructor
        this.hourglassPosition = 0; // Initialize the hourglass position
    }

}
