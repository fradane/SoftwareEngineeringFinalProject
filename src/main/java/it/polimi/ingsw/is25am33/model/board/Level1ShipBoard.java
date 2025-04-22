package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

public class Level1ShipBoard extends ShipBoard{


    public Level1ShipBoard(PlayerColor color) {
        super(color);
    }

    @Override
    public void handleDangerousObject(DangerousObj obj) {

    }

    @Override
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
        return false;
    }
}
