package it.polimi.ingsw.is25am33.model.board;

import java.util.Map;

public class Level2FlyingBoard extends FlyingBoard {

    private int hourglassPosition;

    // Constructor to initialize runLenght in the superclass e initialize the hourglass position
    public Level2FlyingBoard(int runLenght) {
        super(runLenght); // Pass the runLenght to the superclass constructor
        this.hourglassPosition = 0;
    }

}
