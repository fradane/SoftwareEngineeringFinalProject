package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.Shield;

public interface ShotSenderCard {

    void playerDecidedHowToDefendTheirSelvesFromSmallShot(Shield chosenShield, BatteryBox chosenBatteryBox);

    public void playerIsAttackedByABigShot();

}
