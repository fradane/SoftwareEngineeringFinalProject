package it.polimi.ingsw.is25am33.model.dangerousObj;

public abstract class DangerousObj {
    private Direction direction;
    private int coordinate;

    public DangerousObj(Direction direction) {
        this.direction = direction;
        this.coordinate = 0;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getCoordinate() {
        return coordinate;
    }

    public void setCoordinates(int coordinate) {
        this.coordinate = coordinate;
    }
}


