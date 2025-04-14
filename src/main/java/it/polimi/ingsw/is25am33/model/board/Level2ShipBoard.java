package it.polimi.ingsw.is25am33.model.board;
import it.polimi.ingsw.is25am33.model.ComponentState;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.ObserverManager;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;
import it.polimi.ingsw.is25am33.model.game.DTO;
import it.polimi.ingsw.is25am33.model.game.GameEvent;

import java.util.function.BiConsumer;

import static it.polimi.ingsw.is25am33.model.Direction.NORTH;

public class Level2ShipBoard extends ShipBoard{

    public Level2ShipBoard(PlayerColor playerColor) {
        super(playerColor);
    }

    public void book () {
        notActiveComponents.add(focusedComponent);
        focusedComponent.setCurrState(ComponentState.BOOKED);

        DTO dto = new DTO();
        dto.setPlayer(player);
        dto.setComponent(focusedComponent);
        dto.setComponentState(ComponentState.BOOKED);

        BiConsumer<Observer,String> notifyBookingComponent= Observer::notifyBookedComponent;
        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "BookFocusComponent", dto ), notifyBookingComponent);
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
