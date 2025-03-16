package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class StandardStorage extends Storage {
    public StandardStorage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors, maxCapacity);
    }
    public void addCube(CargoCube cube) throws IllegalArgumentException {
        if(cube==CargoCube.RED){
            throw new IllegalArgumentException("Red cube in StandardStorage");
        }
        else getStockedCubes().add(cube);
    }
}
