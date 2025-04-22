package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class SmallShot extends Shot {

    public SmallShot(Direction direction) {
        super(direction);
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, Pirates pirates) {
        pirates.playerDecidedHowToDefendTheirSelvesFromSmallShot(playerChoices.getChosenShield().orElseThrow(), playerChoices.getChosenBatteryBox().orElseThrow());
    }

}
