package it.polimi.ingsw.is25am33.model.board;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;

import java.util.Map;
import java.util.Set;

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

    public Level2ShipBoard(PlayerColor playerColor, GameClientNotifier gameClientNotifier, boolean isGui) {
        super(playerColor, gameClientNotifier, isGui);
        this.validPositions = level2ValidPositions;
    }

    public static boolean isOutsideShipboard(int x, int y) {
        return !level2ValidPositions[x][y];
    }

    public void book () {

        //TODO aggiungere il fatto che non si possono prenotare più di due componenti
        //TODO quando un componente riservato diventa focused non lo puoi rilasciare

        notActiveComponents.add(focusedComponent);
        focusedComponent.setCurrState(ComponentState.BOOKED);

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyBookedComponent(nicknameToNotify, player.getNickname(), focusedComponent);
        });

    }

    public int[] handleDangerousObject(DangerousObj obj){
        int[] hitCoordinate = findFirstComponentInDirection(obj.getCoordinate(), obj.getDirection());
        return hitCoordinate;
    }

    public boolean canDifendItselfWithSingleCannons(DangerousObj obj){
        if (obj.getDirection() == NORTH) {
            return isThereACannon(obj.getCoordinate(), obj.getDirection());
        } else {
            return isThereACannon(obj.getCoordinate(), obj.getDirection())
                    || isThereACannon(obj.getCoordinate() - 1, obj.getDirection())
                    || isThereACannon(obj.getCoordinate() + 1, obj.getDirection());
        }
    }

    /* TODO indice del reserved component da mettere in focus e accertarsi della notify,
        fatto da fra ma marco deve controllare e aggiungere cose :) */
    public void focusReservedComponent(int choice) {
        if (choice < 0 || choice >= getBookedComponents().size())
            return;

        Component reservedComponent = getBookedComponents().remove(choice);
        setFocusedComponent(reservedComponent);
    }

    @Override
    public Map<Coordinates, Set<ColorLifeSupport>> getCabinsWithLifeSupport() {
        // Utilizza l'implementazione della classe padre
        return super.getCabinsWithLifeSupport();
    }

    @Override
    public boolean canAcceptAlien(Coordinates coords, CrewMember alien) {
        // Utilizza l'implementazione della classe padre
        return super.canAcceptAlien(coords, alien);
    }
}
