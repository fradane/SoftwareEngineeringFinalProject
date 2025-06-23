package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class BigMeteorite extends Meteorite {

    /**
     * Constructs a BigMeteorite object with a specific initial direction.
     * This constructor invokes the superclass's constructor to set the initial direction
     * and initializes the dangerous object type to "bigMeteorite".
     *
     * @param direction the initial direction of the BigMeteorite. Must be a valid value
     *                  from the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public BigMeteorite(Direction direction) {
        super(direction);
        this.dangerousObjType = "bigMeteorite";
    }

    /**
     * Default constructor for the BigMeteorite class.
     * This constructor initializes an instance of BigMeteorite without requiring any direction.
     * It sets the dangerous object type of this instance to "bigMeteorite".
     */
    public BigMeteorite() {
        super();
        this.dangerousObjType = "bigMeteorite";
    }

    /**
     * Initiates the attack sequence for a Big Meteorite by determining the player's chosen
     * defenses and applying them to the MeteoriteStorm card.
     *
     * @param playerChoices The data structure containing the player's choices for defenses,
     *                      including double cannons and battery boxes.
     * @param card The MeteoriteStorm card that will execute the defensive measures for
     *             the Big Meteorite attack.
     */
    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card) {
        card.playerDecidedHowToDefendTheirSelvesFromBigMeteorite(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
    }

    /**
     * Retrieves the type of the dangerous object associated with this instance.
     * The returned value identifies the specific type of dangerous object
     * (e.g., "bigMeteorite", "smallMeteorite", "bigShot", "smallShot").
     *
     * @return a String representing the type of the dangerous object.
     */
    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
