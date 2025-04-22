package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class SmallMeteorite extends Meteorite {

    public SmallMeteorite(Direction direction) {
        super(direction);
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm meteoriteStorm) {
        meteoriteStorm
                .playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBox().orElseThrow());
    }

}
