package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class Cannon extends Component implements Rotatable {
    private Direction fireDirection;

    public Cannon(Map<Direction, ConnectorType> connectors, Direction fireDirection) {
        super(connectors);
        this.fireDirection= fireDirection;
    }
    public Direction getFireDirection() {
        return fireDirection;
    }
    public void RotateFireDirection() {
        for(int i=0; i<getRotation()%4; i++)
            this.fireDirection=shiftDirection(this.fireDirection);
    }
}
