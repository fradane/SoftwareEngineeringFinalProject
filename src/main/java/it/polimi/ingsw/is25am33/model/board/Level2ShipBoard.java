package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.GameContext;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;


import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.NORTH;

public class Level2ShipBoard extends ShipBoard implements ShipBoardClient {

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

    public Level2ShipBoard(PlayerColor playerColor, GameContext gameContext) {
        super(playerColor, gameContext);
        this.validPositions = level2ValidPositions;
    }

    public static boolean isOutsideShipboard(int x, int y) {
        return !level2ValidPositions[x][y];
    }

    public void book () {

        //TODO aggiungere il fatto che non si possono prenotare più di due componenti

        notActiveComponents.add(focusedComponent);
        focusedComponent.setCurrState(ComponentState.BOOKED);

        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyBookedComponent(nicknameToNotify, player.getNickname(), focusedComponent);
        });

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

    // TODO indice del reserved component da mettere in focus e accertarsi della notify
    public void focusReservedComponent(int choice) {

    }
}
