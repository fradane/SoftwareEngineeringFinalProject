package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Level1ShipBoard extends ShipBoard implements ShipBoardClient {

    public Level1ShipBoard(PlayerColor color, GameClientNotifier gameContext) {
        super(color,gameContext);
    }

    @Override
    public void handleDangerousObject(DangerousObj obj) {

    }

    @Override
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
        return false;
    }


    @Override
    public Map<Coordinates, Set<ColorLifeSupport>> getCabinsWithLifeSupport() {
        // In modalità test flight, nessuna cabina può avere alieni
        return Collections.emptyMap();
    }

    @Override
    public boolean canAcceptAlien(Coordinates coords, CrewMember alien) {
        return false;
    }

}
