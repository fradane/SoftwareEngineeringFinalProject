package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class SmallMeteorite extends Meteorite {

    /**
     * Constructs a SmallMeteorite object with a specified direction.
     * This constructor initializes the direction of the meteorite
     * and sets the dangerous object type to "smallMeteorite".
     *
     * @param direction the initial direction of the SmallMeteorite. Must be a valid direction
     *                  as defined in the {@link Direction} enum (NORTH, EAST, SOUTH, WEST).
     */
    public SmallMeteorite(Direction direction) {
        super(direction);
        this.dangerousObjType = "smallMeteorite";
    }

    /**
     * Default constructor for the SmallMeteorite class.
     * This constructor initializes a SmallMeteorite object without specifying a direction or initial configuration.
     * The dangerous object type is set to "smallMeteorite", indicating its specific categorization.
     */
    public SmallMeteorite() {
        super();
        this.dangerousObjType = "smallMeteorite";
    }

    /**
     * Initiates the attack on the player by determining the defenses selected
     * by the player to counteract the small meteorite.
     *
     * @param playerChoices the player's chosen defenses, including shield and battery boxes
     * @param card the meteorite storm instance handling the defense resolution
     */
    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card) {
        card.playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
    }

    /**
     * Retrieves the type of the dangerous object associated with this instance.
     * The returned value identifies the specific type of the object
     * (e.g., "bigMeteorite", "smallMeteorite", "bigShot", "smallShot").
     *
     * @return a String representing the type of the dangerous object.
     */
    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
