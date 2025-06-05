package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

public class Level1ShipBoard extends ShipBoard implements ShipBoardClient {

    static boolean[][] level1ValidPositions = {
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, true, false, false, false, false, false},
            {false, false, false, false, false, true, true, true, false, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, false, true, true, false, true, true, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false}
    };

    public Level1ShipBoard(PlayerColor color, GameClientNotifier gameClientNotifier, boolean isGui) {
        super(color, gameClientNotifier, isGui);
        this.validPositions = level1ValidPositions;
    }

    @Override
    public void handleDangerousObject(DangerousObj obj) {

    }

    @Override
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
        return false;
    }


    public static Boolean isOutsideShipboard(int x, int y) {
        return !level1ValidPositions[x][y];
    }
}
