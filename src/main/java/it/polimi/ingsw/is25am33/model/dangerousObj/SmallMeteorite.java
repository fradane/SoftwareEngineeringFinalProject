package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

import java.util.function.BiConsumer;

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
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm meteoriteStorm) {
        meteoriteStorm
                .playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBox().orElseThrow());
    }

    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
        return view.showSmallDanObjMenu();
    }

}
