package it.polimi.ingsw.is25am33.model.component;
import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import  java.util.List;
import java.util.Map;

public abstract class Storage extends Component {
    private final int maxCapacity;
    private List<CargoCube> stockedCubes;

    public Storage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors);
        this.maxCapacity = maxCapacity;
    }
    public int getMaxCapacity() {
        return maxCapacity;
    }
    public List<CargoCube> getStockedCubes() {
        return stockedCubes;
    }
    public void addCube(CargoCube cube) {
        stockedCubes.add(cube);
    }
    public void removeCube(CargoCube cube) {
        stockedCubes.remove(cube);
    }
    public void removeAllCargoCubesOfType(CargoCube cube) {
        for (CargoCube c : stockedCubes) {
            if (cube.equals(c)) {
                stockedCubes.remove(c);
            }
        }
    }
    public void removeCargoCubesOfType(CargoCube cube, int n) {
        int i=0;
        for (CargoCube c : stockedCubes) {
            if (i==n){
                return;
            }
            else if (cube.equals(c)) {
                stockedCubes.remove(c);
                i++;
            }
        }
    }
    public boolean containsCargoCube(CargoCube cube) {
        for (CargoCube c : stockedCubes) {
            if (cube.equals(c)) {
                return true;
            }
        }
        return false;
    }
}
