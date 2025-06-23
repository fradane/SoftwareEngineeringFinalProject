package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.card.interfaces.HowToDefend;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class SmallShot extends Shot {

    /**
     * Constructs a SmallShot object with a specified direction.
     * This constructor invokes the superclass's constructor to initialize the direction
     * and sets the object type to "smallShot".
     *
     * @param direction the initial direction of the SmallShot. Must be a valid direction
     *                  as defined in the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public SmallShot(Direction direction) {
        super(direction);
        this.dangerousObjType = "smallShot";
    }

    /**
     * Default constructor for the SmallShot class.
     * Initializes a SmallShot object without setting a specific direction or initial configuration.
     * Sets the dangerous object type to "smallShot".
     */
    public SmallShot() {
        super();
        this.dangerousObjType = "smallShot";
    }

    /**
     * Initiates the attack by determining the player's choices for defense
     * using a shield and battery boxes, and communicates these choices
     * to the provided shot sender card.
     *
     * @param playerChoices the player's chosen defensive options, including
     *                      the selected shield and battery boxes
     * @param card          the shot sender card responsible for processing
     *                      the player's defensive decisions
     */
    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, HowToDefend card) {
        card.playerDecidedHowToDefendTheirSelvesFromSmallShot(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
    }

    /**
     * Retrieves the type of the dangerous object associated with this instance.
     * The value returned corresponds to a unique identifier for the object type
     * (e.g., "bigMeteorite", "smallMeteorite", "bigShot", "smallShot").
     *
     * @return a String representing the type of the dangerous object.
     */
    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
