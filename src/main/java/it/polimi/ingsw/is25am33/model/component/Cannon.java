package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class Cannon extends Component implements Rotatable {

    private Direction fireDirection;

    public Cannon(Map<Direction, ConnectorType> connectors) {
        super(connectors);
        this.fireDirection= Direction.NORTH;
    }

    public Direction getFireDirection() {
        return fireDirection;
    }

    public void rotateFireDirection() {
        for(int i=0; i<getRotation()%4; i++)
            this.fireDirection=shiftDirection(this.fireDirection);
    }

    @Override
    public void changeOrientation() {
        super.changeOrientation();
        rotateFireDirection();
    }
}
