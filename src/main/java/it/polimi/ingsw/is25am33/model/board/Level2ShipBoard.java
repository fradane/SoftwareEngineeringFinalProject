package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.crew.CrewMember;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.polimi.ingsw.is25am33.model.Direction.NORTH;

public class Level2ShipBoard extends ShipBoard{

    public void book () {
        notActiveComponents.add(focusedComponent);
        focusedComponent = null;
    }




    public void handleDangerousObject(DangerousObj obj){

        int[] hitCoordinate = findFirstComponentInDirection(obj.getCoordinate(), obj.getDirection());

        removeComponent(hitCoordinate[0], hitCoordinate[1]);

    }

    boolean canDifendItselfWithSingleCannons(DangerousObj obj){
        if(obj.getDirection() == NORTH){
            if(!isThereACannon(obj.getCoordinate(), obj.getDirection()))
                return false;
        }else{
            if(
                    !isThereACannon(obj.coordinate, obj.getDirection())
                            && !isThereACannon(obj.getCoordinate() - 1, obj.getDirection())
                            && !isThereACannon(obj.getCoordinate() + 1, obj.getDirection())
            )
                return false;
        }
        return true;
    }


}
