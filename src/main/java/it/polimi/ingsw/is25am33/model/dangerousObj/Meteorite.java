package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public abstract class Meteorite extends DangerousObj {

    /**
     * Default constructor for the Meteorite class.
     * Initializes an instance of Meteorite without setting any specific direction or initial configuration.
     * This constructor inherits its behavior from the superclass DangerousObj.
     * It can be used to create a Meteorite object in scenarios where no initial direction or coordinate is required.
     */
    public Meteorite() {
        super();
    }

    /**
     * Constructs a Meteorite object with a specified direction.
     * This constructor invokes the superclass's constructor to initialize
     * the direction of the Meteorite.
     *
     * @param direction the initial direction of the Meteorite. Must be a valid
     *                  value from the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public Meteorite(Direction direction) {
        super(direction);
    }

    /**
     * Initiates an attack using the provided player choices and a specific meteorite storm card.
     *
     * @param playerChoices The data structure containing the player's choices for the attack.
     * @param card The meteorite storm card that represents the attack being initiated.
     */
    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card);
}
