package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class DoubleCannon extends Cannon implements Activable {
    public DoubleCannon(Map<Direction, ConnectorType> connectors, Direction fireDirection) {
        super(connectors, fireDirection);
    }
}

