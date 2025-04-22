package it.polimi.ingsw.is25am33.model.board;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;
import it.polimi.ingsw.is25am33.model.game.DTO;

import java.util.function.BiConsumer;

import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.NORTH;

public class Level2ShipBoard extends ShipBoard{

    static boolean[][] level2ValidPositions = {
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, true, false, true, false, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, true, true, true, true, true, true, true, false, false},
            {false, false, false, true, true, true, true, true, true, true, false, false},
            {false, false, false, true, true, true, false, true, true, true, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false}
    };

    public Level2ShipBoard(PlayerColor playerColor) {
        super(playerColor);
        this.validPositions = level2ValidPositions;
    }

    public static boolean isOutsideShipboard(int x, int y) {
        return !level2ValidPositions[x][y];
    }

    public void book () {
        notActiveComponents.add(focusedComponent);
        focusedComponent.setCurrState(ComponentState.BOOKED);

        DTO dto = new DTO();
        dto.setPlayer(player);
        dto.setComponent(focusedComponent);
        dto.setComponentState(ComponentState.BOOKED);

        BiConsumer<Observer,String> notifyBookingComponent= Observer::notifyBookedComponent;
        //gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "BookFocusComponent", dto ), notifyBookingComponent);
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
