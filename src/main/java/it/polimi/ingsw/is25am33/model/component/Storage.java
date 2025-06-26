package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * Adds a cargo cube to the storage. If the storage is not full, the cube is added directly.
     * If the storage is full, the least valuable cube is removed to make space for the new cube,
     * and the removed cube is returned. If no cube is removed, the new cube is simply added.
     *
     * @param cube the {@code CargoCube} to add to the storage
     * @return the removed {@code CargoCube} if the storage was full and a cube was replaced,
     *         or {@code null} if the cube was added without replacement
     */
    public CargoCube addCube(CargoCube cube) {
        validateCube(cube);

        if (!isFull()) {
            stockedCubes.add(cube);
            return null;
        }

        CargoCube leastValuableCube = findLeastValuableCube();

        if (leastValuableCube != null) {
            stockedCubes.remove(leastValuableCube);
            stockedCubes.add(cube);
            return leastValuableCube;
        }

        stockedCubes.add(cube);
        return null;
    }


    /**
     * Identifies and retrieves the least valuable {@code CargoCube} from the storage.
     * If the storage is empty, the method returns {@code null}.
     * The cubes are sorted based on their value, and the one with the lowest value is selected.
     *
     * @return the least valuable {@code CargoCube}, or {@code null} if storage is empty
     */
    @JsonIgnore
    protected CargoCube findLeastValuableCube() {
        if (stockedCubes.isEmpty()) {
            return null;
        }

        // Sort the cubes by value and take the first one (least valuable)
        List<CargoCube> sortedCubes = new ArrayList<>(stockedCubes);
        sortedCubes.sort(CargoCube.byValue);
        return sortedCubes.getFirst();
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
     * Checks if the storage is full.
     *
     * @return true if the storage is full, false otherwise
     */
    @JsonIgnore
    public boolean isFull() {
        return stockedCubes.size() == maxCapacity;
    }

    /**
     * Retrieves the main attribute of the storage.
     * The main attribute is calculated as the difference between the maximum capacity
     * and the size of the stocked cubes, represented as a string.
     *
     * @return a string representing the calculated main attribute of the storage
     */
    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return maxCapacity - stockedCubes.size() + "";
    }

    /**
     * Validates the given {@code CargoCube}.
     * This method ensures that the specified cargo cube meets the necessary
     * conditions or constraints for storage, such as value validation or compatibility
     * with the storage requirements.
     *
     * @param cube the {@code CargoCube} to be validated
     *             This parameter represents the cargo cube that needs to be checked
     *             for validity before processing or adding to the storage.
     */
    protected void validateCube(CargoCube cube) {}

    /**
     * Generates a hash code for the GUI representation of the storage component.
     * The hash code is computed based on the storage's image name, rotation,
     * and its list of stocked cargo cubes (if not null).
     *
     * @return the hash code used for GUI representation
     */
    @Override
    @JsonIgnore
    @NotNull
    public Integer getGuiHash() {
        return stockedCubes == null ?
                Objects.hash(imageName, getRotation()) :
                Objects.hash(imageName, stockedCubes, getRotation());
    }
}
