package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.crew.CrewMember;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Level2ShipBoard extends ShipBoard{

    public void book () {
        notActiveComponents.add(focusedComponent);
        focusedComponent = null;
    }




    public void handleDangerousObject(DangerousObj obj){

        int[] hitCoordinate = findFirstComponentInDirection(obj.coordinate, obj.direction);

        switch (obj) {
            case BigShot bigShot -> {
                removeComponent(hitCoordinate[0], hitCoordinate[1]);
            }
            case SmallShot smallShot -> {
                if (isDirectionCoveredByShield(obj.direction) && !game.getController().wantsToActivateShield())
                    removeComponent(hitCoordinate[0], hitCoordinate[1]);
            }
            case BigMeteorite bigMeteorite -> {
                if(obj.direction == NORTH){
                    if(!isThereACannon(obj.coordinate, obj.direction))
                        removeComponent(hitCoordinate[0], hitCoordinate[1]);
                }else{
                    if(
                            !isThereACannon(obj.coordinate, obj.direction)
                                    && !isThereACannon(obj.coordinate - 1, obj.direction)
                                    && !isThereACannon(obj.coordinate + 1, obj.direction)
                    )
                        removeComponent(hitCoordinate[0], hitCoordinate[1]);
                }
            }
            case SmallMeteorite smallMeteorite -> {
                if(isExposed(obj.coordinate, obj.direction) && isDirectionCoveredByShield(obj.direction) && !game.getController().wantsToActivateShield())
                    removeComponent(hitCoordinate[0], hitCoordinate[1]);
            }
        }
    }


}
