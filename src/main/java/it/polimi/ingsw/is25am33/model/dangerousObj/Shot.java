package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.card.interfaces.HowToDefend;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public abstract class Shot extends DangerousObj {

    /**
     * Constructs a Shot object with a specified direction.
     * Invokes the constructor of the DangerousObj class to initialize the direction
     * and sets the coordinate of the object to its default value.
     *
     * @param direction the initial direction of the Shot. Must be a valid direction
     *                  as defined in the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public Shot(Direction direction) {
        super(direction);
    }

    /**
     * Default constructor for the Shot class.
     * Initializes an instance of Shot without setting any specific direction or initial configuration.
     * This constructor inherits its behavior from the superclass DangerousObj.
     * It can be used to create a Shot object in scenarios where no initial direction or coordinate is required.
     */
    public Shot() {
        super();
    }

    /**
     * Initiates an attack using the specified player choices and shot sender card.
     *
     * @param playerChoices the data structure that contains the player's choices related to the attack
     * @param card the card representing the shot sender used for the attack
     */
    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, HowToDefend card);

}
