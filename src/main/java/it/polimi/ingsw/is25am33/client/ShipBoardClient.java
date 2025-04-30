package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.component.Component;

import java.util.ArrayList;
import java.util.List;

public class ShipBoardClient {
    private Component[][] shipBoardMatrix;
    private Component focusedComponent;
    private List<Component> bookedComponent = new ArrayList<>();

    public ShipBoardClient(Component[][] shipBoardMatrix, Component focusComponent, List<Component> bookedComponent){
        this.shipBoardMatrix=shipBoardMatrix;
        this.focusedComponent=focusComponent;
        this.bookedComponent=bookedComponent;
    }

    public List<Component> getBookedComponent() {
        return bookedComponent;
    }

    public void setBookedComponent(List<Component> bookedComponent) {
        this.bookedComponent = bookedComponent;
    }

    public Component getFocusedComponent() {
        return focusedComponent;
    }

    public void setFocusedComponent(Component focusedComponent) {
        this.focusedComponent = focusedComponent;
    }

    public void setShipBoardMatrix(Component[][] shipBoardMatrix) {
        this.shipBoardMatrix = shipBoardMatrix;
    }

    public Component[][] getShipBoardMatrix(){
        return shipBoardMatrix;
    }
}
