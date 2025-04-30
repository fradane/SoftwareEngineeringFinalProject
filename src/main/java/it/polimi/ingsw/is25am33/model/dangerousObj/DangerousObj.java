package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.function.BiConsumer;
import java.io.Serializable;

public abstract class DangerousObj implements Serializable {

    private final Direction direction;
    private int coordinate;

    public DangerousObj(Direction direction) {
        this.direction = direction;
        this.coordinate = 0;
    }

    public DangerousObj() {
        this.direction = null;
        this.coordinate = 0;
    }

    public abstract String getDangerousObjType();

    public Direction getDirection() {
        return direction;
    }

    public int getCoordinate() {
        return coordinate;
    }

    public void setCoordinates(int coordinate) {
        this.coordinate = coordinate;
    }

    public abstract BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view);
}


