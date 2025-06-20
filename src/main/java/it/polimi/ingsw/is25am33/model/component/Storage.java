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
     * Adds a {@code CargoCube} to the storage. If the storage is full,
     * it will replace the least valuable cube regardless of value comparison.
     *
     * @param cube the {@code CargoCube} to add
     * @return the {@code CargoCube} that was removed to make space, or null if storage wasn't full
     */
    public CargoCube addCube(CargoCube cube) {
        validateCube(cube);
        // Se lo storage non è pieno, aggiungi semplicemente il cubo
        if (!isFull()) {
            stockedCubes.add(cube);
            return null;
        }

        // Se lo storage è pieno, trova e rimuovi il cubo di valore minore
        CargoCube leastValuableCube = findLeastValuableCube();

        if (leastValuableCube != null) {
            stockedCubes.remove(leastValuableCube);
            stockedCubes.add(cube);
            return leastValuableCube;
        }

        // Caso teoricamente impossibile se isFull() è true
        stockedCubes.add(cube);
        return null;
    }

    /**
     * Finds the least valuable cube in the storage.
     *
     * @return the least valuable {@code CargoCube}, or null if storage is empty
     */
    @JsonIgnore
    protected CargoCube findLeastValuableCube() {
        if (stockedCubes.isEmpty()) {
            return null;
        }

        // Ordina i cubi per valore e prendi il primo (meno prezioso)
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

    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return maxCapacity - stockedCubes.size() + "";
    }

    // Questo metodo permette alle sottoclassi di aggiungere controlli
    protected void validateCube(CargoCube cube) {
        // Nessun controllo di default
    }

    @Override
    @JsonIgnore
    @NotNull
    public Integer getGuiHash() {
        return stockedCubes == null ?
                Objects.hash(imageName, getRotation()) :
                Objects.hash(imageName, stockedCubes, getRotation());
    }
}
