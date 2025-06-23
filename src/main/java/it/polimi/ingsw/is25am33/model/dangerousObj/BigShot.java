package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.card.interfaces.HowToDefend;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class BigShot extends Shot {

    /**
     * Constructs a BigShot object with a specified initial direction.
     * This constructor invokes the superclass's constructor to set the direction
     * and initializes the dangerous object type to "bigShot".
     *
     * @param direction the initial direction of the BigShot. Must be a valid direction
     *                  as defined in the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public BigShot(Direction direction) {
        super(direction);
        this.dangerousObjType = "bigShot";
    }

    /**
     * Default constructor for the BigShot class.
     * Initializes an instance of BigShot without setting any specific direction or initial configuration.
     * Sets the dangerous object type to "bigShot".
     */
    public BigShot() {
        super();
        this.dangerousObjType = "bigShot";
    }

    /**
     * Initiates an attack on a player using the current instance of a BigShot.
     *
     * @param playerChoices the structure containing the player's choices or actions
     * @param card the card instance representing the sender of the shot
     */
    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, HowToDefend card) {
        card.playerIsAttackedByABigShot();
    }

    /**
     * Retrieves the type of the dangerous object associated with this instance.
     * The returned value represents the specific type of dangerous object
     * (e.g., "bigShot", "smallShot", "bigMeteorite", "smallMeteorite").
     *
     * @return a String representing the type of the dangerous object.
     */
    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
