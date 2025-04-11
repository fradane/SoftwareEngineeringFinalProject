package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.ObserverManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;

import java.util.Collections;
import java.util.List;

import static it.polimi.ingsw.is25am33.model.ComponentState.BOOKED;
import static it.polimi.ingsw.is25am33.model.ComponentState.FREE;

public class ComponentTable {

    int rows=12;
    int cols=13;
    private Component[][] componentTable = new Component[rows][cols];

    public ComponentTable(List<Component> components){
        Collections.shuffle(components);
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                componentTable[i][j] = components.get(index++);
                components.get(index++).setTableCoordinates(new Coordinates(i, j));
            }
        }
    }

    public Component getComponent(Coordinates coordinates){
        return componentTable[coordinates.getX()][coordinates.getY()];
    }
}
