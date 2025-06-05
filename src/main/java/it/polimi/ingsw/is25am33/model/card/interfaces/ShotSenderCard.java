package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.Shield;

import java.util.List;

public interface ShotSenderCard {

    void playerDecidedHowToDefendTheirSelvesFromSmallShot(List<Coordinates> chosenShieldsCoords, List<Coordinates> chosenBatteryBoxesCoords);

    public void playerIsAttackedByABigShot();

}
