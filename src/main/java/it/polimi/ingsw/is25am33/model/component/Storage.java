package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a storage component, extending the {@code Component} class.
 * This class provides functionality to store, remove, and check for cargo cubes.
 */
public abstract class Storage extends Component {

    /**
     * The maximum capacity of the storage.
     */
    private int maxCapacity;

    /**
     * The list of stocked cargo cubes currently in storage.
     */
    private List<CargoCube> stockedCubes = new ArrayList<>();

    /**
     * Default constructor for {@code Storage}.
     */
    public Storage() {}

    /**
     * Constructor that allows initializing the storage with specified connectors and maximum capacity.
     *
     * @param connectors a map associating a {@code Direction} with a {@code ConnectorType}
     * @param maxCapacity the maximum storage capacity
     */
    public Storage(Map<Direction, ConnectorType> connectors, int maxCapacity) {
        super(connectors);
        this.maxCapacity = maxCapacity;
        this.stockedCubes = new ArrayList<CargoCube>();
    }

    /**
     * Gets the maximum capacity of the storage.
     *
     * @return the maximum capacity of the storage
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Gets the list of stocked cargo cubes currently in storage.
     *
     * @return a list of {@code CargoCube}s in storage
     */
    public List<CargoCube> getStockedCubes() {
        return stockedCubes;
    }

    /**
     * Sets the maximum capacity of the storage.
     *
     * @param maxCapacity the new maximum capacity
     */
    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Adds a {@code CargoCube} to the storage.
     *
     * @param cube the {@code CargoCube} to add
     */
    public void addCube(CargoCube cube) {
        stockedCubes.add(cube);
    }

    /**
     * Removes a {@code CargoCube} from the storage.
     *
     * @param cube the {@code CargoCube} to remove
     * @throws IllegalArgumentException if the storage is empty
     */
    public void removeCube(CargoCube cube) throws IllegalArgumentException {
        if (stockedCubes.isEmpty()) throw new IllegalArgumentException("Empty storage");
        stockedCubes.remove(cube);
    }

    /**
     * Removes all instances of a specific {@code CargoCube} from the storage.
     *
     * @param cube the {@code CargoCube} to remove
     * @throws IllegalArgumentException if the cube is not present in the storage
     */
    public void removeAllCargoCubesOfType(CargoCube cube) throws IllegalArgumentException {
        if (!stockedCubes.contains(cube)) throw new IllegalArgumentException("cube not exist");
        stockedCubes.removeIf(cube::equals);
    }

    /**
     * Removes a specified number of instances of a specific {@code CargoCube} from the storage.
     *
     * @param cube the {@code CargoCube} to remove
     * @param n the number of cubes to remove
     * @throws IllegalArgumentException if there are not enough cubes to remove
     */
    public void removeCargoCubesOfType(CargoCube cube, int n) throws IllegalArgumentException {
        if (!stockedCubes.contains(cube)) throw new IllegalArgumentException("cube not exist");
        int count = 0;
        for (CargoCube c : stockedCubes) {
            if (cube.equals(c))
                count++;
        }
        if (count < n) throw new IllegalArgumentException("wrong number of cubes");

        for (int i = 0; i < n; i++) {
            stockedCubes.remove(cube);
        }
    }

    /**
     * Checks if the storage contains a specific {@code CargoCube}.
     *
     * @param cube the {@code CargoCube} to check for
     * @return true if the cube is in storage, false otherwise
     */
    public boolean containsCargoCube(CargoCube cube) {
        for (CargoCube c : stockedCubes) {
            if (cube.equals(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the storage is full.
     *
     * @return true if the storage is full, false otherwise
     */
    public boolean isFull() {
        return stockedCubes.size() == maxCapacity;
    }

    @Override
    public String getMainAttribute() {
        return Integer.toString(maxCapacity - stockedCubes.size());
    }
}
