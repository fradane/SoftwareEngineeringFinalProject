package it.polimi.ingsw.is25am33.model.component;
import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.ArrayList;
import java.util.LinkedList;
import  java.util.List;
import java.util.Map;

public abstract class Storage extends Component {
    private final int maxCapacity;
    private List<CargoCube> stockedCubes;

    public Storage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors);
        this.maxCapacity = maxCapacity;
        this.stockedCubes = new ArrayList<CargoCube>();
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
    public void removeCube(CargoCube cube) throws  IllegalArgumentException {
        if(stockedCubes.isEmpty()) throw new IllegalArgumentException("Empty storage");
        stockedCubes.remove(cube);
    }
    public void removeAllCargoCubesOfType(CargoCube cube) throws IllegalArgumentException {
        if(!stockedCubes.contains(cube)) throw new IllegalArgumentException("cube not exist");
        stockedCubes.removeIf(cube::equals);
    }
    public void removeCargoCubesOfType(CargoCube cube, int n) throws IllegalArgumentException {
        if(!stockedCubes.contains(cube)) throw new IllegalArgumentException("cube not exist");
        int count=0;
        for (CargoCube c : stockedCubes){
            if(cube.equals(c))
                count++;
        }
        if(count<n) throw new IllegalArgumentException("wrong number of cubes");

        for(int i=0;i<n;i++){
            stockedCubes.remove(cube);
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

    public boolean isfull(){
        if(stockedCubes.size()==maxCapacity) return true;
        return false;
    }
}
