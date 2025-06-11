package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.io.Serializable;

public class ClientDangerousObject implements Serializable {
    private String type;
    private Direction direction;
    private int coordinate;

    public ClientDangerousObject() {}

    public ClientDangerousObject(String type, Direction direction, int coordinate) {

        this.type = type;
        this.direction = direction;
        this.coordinate = coordinate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Direction getDirection() {
        return direction;
    }
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(int coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("A "+type+" from "+direction).toString();
        if(direction==Direction.EAST||direction==Direction.WEST)
            builder.append(" on the row number "+(coordinate+1));
        else
            builder.append(" on the column number "+(coordinate+1));
        return builder.toString();
    }
}
