package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class BigMeteorite extends Meteorite {

    public BigMeteorite(Direction direction) {
        super(direction);
        this.dangerousObjType = "bigMeteorite";
    }

    public BigMeteorite() {
        super();
        this.dangerousObjType = "bigMeteorite";
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card) {
        card.playerDecidedHowToDefendTheirSelvesFromBigMeteorite(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
    }

    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

}
