package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.crew.CrewMember;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.List;

public class Level1ShipBoard extends ShipBoard{


    @Override
    public void handleDangerousObject(DangerousObj obj) {

    }

    @Override
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
        return false;
    }
}
