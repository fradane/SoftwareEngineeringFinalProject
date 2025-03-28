package it.polimi.ingsw.is25am33.model.board;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;

import static it.polimi.ingsw.is25am33.model.Direction.NORTH;

public class Level2ShipBoard extends ShipBoard{

    public void book () {
        notActiveComponents.add(focusedComponent);
        focusedComponent = null;
    }

    public void handleDangerousObject(DangerousObj obj){

        int[] hitCoordinate = findFirstComponentInDirection(obj.getCoordinate(), obj.getDirection());

        removeAndRecalculateShipParts(hitCoordinate[0], hitCoordinate[1]);

    }

    public boolean canDifendItselfWithSingleCannons(DangerousObj obj){
        if(obj.getDirection() == NORTH){
            if(!isThereACannon(obj.getCoordinate(), obj.getDirection()))
                return false;
        }else{
            if(
                    !isThereACannon(obj.getCoordinate(), obj.getDirection())
                            && !isThereACannon(obj.getCoordinate() - 1, obj.getDirection())
                            && !isThereACannon(obj.getCoordinate() + 1, obj.getDirection())
            )
                return false;
        }
        return true;
    }


}
