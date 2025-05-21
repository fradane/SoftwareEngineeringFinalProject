package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class SmallMeteorite extends Meteorite {

    public SmallMeteorite(Direction direction) {
        super(direction);
        this.dangerousObjType = "smallMeteorite";
    }

    public SmallMeteorite() {
        super();
        this.dangerousObjType = "smallMeteorite";
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card) {
        card.playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBox().orElseThrow());
    }

    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
