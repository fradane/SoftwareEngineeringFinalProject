package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class Engine extends Component implements Rotatable{
    private Direction powerDirection;

    public Engine(Map<Direction, ConnectorType> connectors){
        super(connectors);
        this.powerDirection= Direction.SOUTH ;
    }
    public Direction getPowerDirection() {
        return powerDirection;
    }
    public void rotatePowerDirection() {
        for(int i=0; i<getRotation()%4; i++)
            this.powerDirection=shiftDirection(this.powerDirection);
    }

    @Override
    public void changeOrientation() {
        super.changeOrientation();
        rotatePowerDirection();
    }
}

