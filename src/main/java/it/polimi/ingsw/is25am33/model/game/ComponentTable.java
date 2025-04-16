package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.ComponentLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ComponentTable {

    final int rows = 8;
    final int cols = 19;
    private final Component[][] componentTable = new Component[rows][cols];

    public ComponentTable() {
        List<Component> components = ComponentLoader.loadComponents();
        Collections.shuffle(components);
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (index == components.size() - 1) return;
                componentTable[i][j] = components.get(index);
                components.get(index).setTableCoordinates(new Coordinates(i, j));
                index++;
            }
        }
    }

    public Component getComponent(Coordinates coordinates){
        return componentTable[coordinates.getX()][coordinates.getY()];
    }

    public Stream<Component> getComponentsAsStream(){
        return Arrays.stream(componentTable).flatMap(Arrays::stream);
    }

}