package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.StandardStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;

import java.util.*;

/**
 * Utility class for managing storage selections during the reward phase.
 * Allows tracking and validating storage choices for cargo cubes.
 */
public class StorageSelectionManager {

    private final List<Coordinates> selectedStorages = new ArrayList<>();
    private final List<CargoCube> cubeRewards;
    private final int cubeMalus;
    private final ShipBoardClient shipBoard;
    private final List<Storage> availableStorages;
    private final Map<CargoCube, List<Storage>> compatibleStoragesMap;

    // Nuovi campi per la ridistribuzione
    private final List<CargoCube> availableCubes = new ArrayList<>();
    private final Map<Coordinates, List<CargoCube>> finalUpdates = new HashMap<>();
    private boolean redistributionMode = false;
    private int selectedCubeIndex = -1;

    // Nuovi campi per la gestione cube malus
    private Map<CargoCube, List<Coordinates>> cubeLocationMap = new HashMap<>();
    private int remainingCubesToRemove;
    private List<Coordinates> selectedStoragesForRemoval = new ArrayList<>();
    private boolean malusMode = false;

    /**
     * Constructor for the storage selection manager.
     *
     * @param cubeRewards The list of reward cubes to assign to storages
     * @param shipBoard The player's ship board containing the storages
     */
    public StorageSelectionManager(List<CargoCube> cubeRewards, int cubeMalus, ShipBoardClient shipBoard) {
        this.cubeRewards = new ArrayList<>(cubeRewards);
        this.shipBoard = shipBoard;
        this.availableStorages = new ArrayList<>();
        this.compatibleStoragesMap = new HashMap<>();
        this.cubeMalus = cubeMalus;
        this.malusMode = false;

        // Inizializza la lista degli storage disponibili
        initializeAvailableStorages();

        // Costruisce la mappa di compatibilità tra cubi e storage
        buildCompatibilityMap();
    }


    /**
     * Constructor specifically for cube malus handling.
     * Initializes the manager in malus mode for removing cubes from storages.
     *
     * @param cubeMalus number of cubes to remove
     * @param shipBoard the player's ship board containing the storages
     */
    public StorageSelectionManager(int cubeMalus, ShipBoardClient shipBoard) {
        this.cubeRewards = new ArrayList<>(); // Lista vuota per malus
        this.cubeMalus = cubeMalus;
        this.shipBoard = shipBoard;
        this.availableStorages = new ArrayList<>();
        this.compatibleStoragesMap = new HashMap<>();
        this.malusMode = true;
        this.remainingCubesToRemove = cubeMalus;

        // Initialize the list of available storages
        initializeAvailableStorages();

        // Build the compatibility map for each cube type
        buildCompatibilityMap();

        // Inizializza per modalità malus
        initializeForMalusMode();
    }

    /**
     * Gets the map of compatible storages for each cube type.
     *
     * @return map where key is cube type and value is list of compatible storages
     */
    public Map<CargoCube, List<Storage>> getCompatibleStoragesMap() {
        return compatibleStoragesMap;
    }


    private void initializeAvailableStorages() {
        // Retrieve all storages from the ship board
        List<Storage> storages = shipBoard.getStorages();
        if (storages != null) {
            this.availableStorages.addAll(storages);
        }
    }


    private void buildCompatibilityMap() {
        // Initialize lists for each cube type
        for (CargoCube cubeType : CargoCube.values()) {
            compatibleStoragesMap.put(cubeType, new ArrayList<>());
        }

        // Populate the map
        for (Storage storage : availableStorages) {
            // Special storages can hold any type of cube
            if (storage instanceof SpecialStorage) {
                for (CargoCube cubeType : CargoCube.values()) {
                    compatibleStoragesMap.get(cubeType).add(storage);
                }
            } else {
                // Standard storages can hold all cubes except RED ones
                for (CargoCube cubeType : CargoCube.values()) {
                    if (cubeType != CargoCube.RED) {
                        compatibleStoragesMap.get(cubeType).add(storage);
                    }
                }
            }
        }
    }

    /**
     * Checks if the player has at least one available storage.
     *
     * @return true if the player has at least one storage, false otherwise
     */
    public boolean hasAnyStorage() {
        return !availableStorages.isEmpty();
    }

    /**
     * Adds a storage to the selection for the current cube.
     *
     * @param storageCoords The coordinates of the selected storage
     * @return true if the storage was successfully added, false otherwise
     */
    public boolean addStorageSelection(Coordinates storageCoords) {
        // Check if we have already selected all necessary storages
        if (selectedStorages.size() >= cubeRewards.size()) {
            return false;
        }

        // The player does not want to save this cube
        if(storageCoords.isCoordinateInvalid()){
            selectedStorages.add(storageCoords);
            return true;
        }

        // Get the component at the specified coordinates
        Storage storage = getStorageAtCoordinates(storageCoords);
        if (storage == null) {
            return false;
        }

        // Check if the current cube is red and the storage is standard type
        int currentCubeIndex = selectedStorages.size();
        CargoCube currentCube = cubeRewards.get(currentCubeIndex);

        if (currentCube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
            return false;  // Red cubes can only be placed in special storages
        }

        // Check if the storage is full and show information about the cube that would be replaced
        if (storage.isFull()) {
            List<CargoCube> storedCubes = storage.getStockedCubes();
            if (!storedCubes.isEmpty()) {
                // Sort cubes by value (least valuable first)
                storedCubes.sort(CargoCube.byValue);
                CargoCube leastValuableCube = storedCubes.getFirst();

                // If the least valuable cube has a higher value than the current cube, warn the user
                if (leastValuableCube.getValue() > currentCube.getValue()) {
                    System.out.println("WARNING: The least valuable cube in the storage (" +
                            leastValuableCube + ", value: " + leastValuableCube.getValue() +
                            ") is more valuable than the current cube (" +
                            currentCube + ", value: " + currentCube.getValue() + ")");
                }
            }
        }

        storage.addCube(currentCube);

        // Add the coordinates to the selection list
        selectedStorages.add(storageCoords);

        return true;
    }

    /**
     * Skips the current cube and moves to the next one.
     *
     * @return true if the cube was skipped, false if there are no more cubes to skip
     */
    public boolean skipCurrentCube() {
        if (selectedStorages.size() >= cubeRewards.size()) {
            return false;  // No more cubes to skip
        }

        selectedStorages.add(new Coordinates(-1, -1));
        return true;
    }

    /**
     * Checks if the current cube can be accepted and explains why if it can't.
     *
     * @return A string explaining why the cube can't be accepted, or null if it can be accepted
     */
    public String getCurrentCubeImpossibilityReason() {
        CargoCube currentCube = getCurrentCube();
        if (currentCube == null) {
            return "No more cubes to accept";
        }

        if (currentCube == CargoCube.RED && compatibleStoragesMap.getOrDefault(CargoCube.RED, new ArrayList<>()).isEmpty()) {
            return "Questo cubo ROSSO non può essere accettato perché non hai storage speciali disponibili";
        }

        // Count how many cubes of this type have already been assigned
        int alreadyAssigned = 0;
        for (int i = 0; i < selectedStorages.size(); i++) {
            if (cubeRewards.get(i) == currentCube) {
                alreadyAssigned++;
            }
        }

        int availableCount = compatibleStoragesMap.getOrDefault(currentCube, new ArrayList<>()).size();
        if (availableCount <= alreadyAssigned) {
            return "Non hai abbastanza storage compatibili per questo cubo " + currentCube;
        }

        return null; // Cube can be accepted
    }

    /**
     * Checks if the player can accept the current cube.
     *
     * @return true if the player has at least one compatible storage for the current cube
     */
    public boolean canAcceptCurrentCube() {
        CargoCube currentCube = getCurrentCube();
        if (currentCube == null) {
            return false;
        }

        // Verifica se ci sono storage compatibili
        return !compatibleStoragesMap.getOrDefault(currentCube, new ArrayList<>()).isEmpty();
    }

    /**
     * Checks if all necessary storages have been selected.
     *
     * @return true if all cubes have a storage assigned, false otherwise
     */
    public boolean isSelectionComplete() {
        return selectedStorages.size() == cubeRewards.size();
    }

    /**
     * Gets the list of coordinates of the selected storages.
     *
     * @return The list of coordinates of the selected storages
     */
    public List<Coordinates> getSelectedStorageCoordinates() {
        return new ArrayList<>(selectedStorages);
    }

    /**
     * Returns the current cube to be assigned.
     *
     * @return The current cube or null if all cubes have been assigned
     */
    public CargoCube getCurrentCube() {
        if (selectedStorages.size() >= cubeRewards.size()) {
            return null;
        }
        return cubeRewards.get(selectedStorages.size());
    }


    private Storage getStorageAtCoordinates(Coordinates coordinates) {
        if (coordinates == null || coordinates.isCoordinateInvalid()) {
            return null;
        }

        try {
            // Get the component and check if it is a storage
            Object component = shipBoard.getComponentAt(coordinates);
            if (component instanceof Storage) {
                return (Storage) component;
            }
        } catch (Exception e) {
            // Handles the exception if the coordinates are out of bounds
        }

        return null;
    }

    /**
     * Checks if a storage is already full and provides information about the least valuable cube.
     *
     * @param storageCoords The coordinates of the storage to check
     * @return A string describing the status of the storage, or null if it's not a valid storage
     */
    public String checkStorageStatus(Coordinates storageCoords) {
        Storage storage = getStorageAtCoordinates(storageCoords);
        if (storage == null) {
            return null;
        }

        if (!storage.isFull()) {
            // We can't use getMaxCubes() and getCurrentCubes() because they don't exist
            // Use getStockedCubes() to get the current cubes
            List<CargoCube> storedCubes = storage.getStockedCubes();
            // We can't determine the exact free space, so give a generic message
            return "Available storage, contains " + storedCubes.size() + " cubes";
        } else {
            // Sort cubes by value and take the least valuable one
            List<CargoCube> storedCubes = storage.getStockedCubes();
            if (storedCubes.isEmpty()) {
                return "Storage is full, but there are no cubes (anomalous state)";
            } else {
                storedCubes.sort(CargoCube.byValue);
                CargoCube leastValuableCube = storedCubes.get(0);
                return "Storage is full, the least valuable cube is " + leastValuableCube +
                        " (value: " + leastValuableCube.getValue() + ")";
            }
        }
    }

    /**
     * Returns the total number of reward cubes to manage.
     *
     * @return The total number of reward cubes
     */
    public int getTotalCubesCount() {
        return cubeRewards.size();
    }

    /**
     * Resets all selections.
     */
    public void reset() {
        selectedStorages.clear();
    }

    /**
     * Gets the location of all cubes on the ship board.
     *
     * @return map where key is cube type and value is list of coordinates where cubes are stored
     */
    public Map<CargoCube, List<Coordinates>> whereAreCube() {

        // Get a list of storages that contain a cube of that color, with repetitions

        Map<CargoCube,List<Coordinates>> storageAndCubes = new HashMap<>();
        storageAndCubes.put(CargoCube.RED, new ArrayList<>());
        storageAndCubes.put(CargoCube.YELLOW, new ArrayList<>());
        storageAndCubes.put(CargoCube.GREEN, new ArrayList<>());
        storageAndCubes.put(CargoCube.BLUE, new ArrayList<>());

        shipBoard.getCoordinatesAndStorages().forEach((coordinate, storage) -> {
            for(CargoCube cube: storage.getStockedCubes()) {
                if (cube == CargoCube.RED)
                    storageAndCubes.get(CargoCube.RED).add(coordinate);
                else if (cube == CargoCube.BLUE)
                    storageAndCubes.get(CargoCube.BLUE).add(coordinate);
                else if (cube == CargoCube.YELLOW)
                    storageAndCubes.get(CargoCube.YELLOW).add(coordinate);
                else
                    storageAndCubes.get(CargoCube.GREEN).add(coordinate);
            }
        });

        return storageAndCubes;
    }

    /**
     * Gets a list of coordinates containing the most precious cubes up to cubeMalus amount.
     * Searches in order: RED > YELLOW > GREEN > BLUE.
     *
     * @return list of coordinates containing the most precious cubes
     */
    public List<Coordinates> mostPreciousCube() {
        List<Coordinates> mostPrecious = new ArrayList<>();
        Map<CargoCube,List<Coordinates>> storageAndCubes = whereAreCube();

        for (Coordinates coordinate : storageAndCubes.get(CargoCube.RED)) {
            mostPrecious.add(coordinate);
            if (mostPrecious.size() == cubeMalus)
                return mostPrecious;
        }
        for (Coordinates coordinate : storageAndCubes.get(CargoCube.YELLOW)) {
            mostPrecious.add(coordinate);
            if (mostPrecious.size() == cubeMalus)
                return mostPrecious;
        }
        for (Coordinates coordinate : storageAndCubes.get(CargoCube.GREEN)) {
            mostPrecious.add(coordinate);
            if (mostPrecious.size() == cubeMalus)
                return mostPrecious;
        }
        for (Coordinates coordinate : storageAndCubes.get(CargoCube.BLUE)) {
            mostPrecious.add(coordinate);
            if (mostPrecious.size() == cubeMalus)
                return mostPrecious;
        }

        return mostPrecious;
    }



    /**
     * Gets the set of coordinates where a cube of the specified type can be placed.
     *
     * @param cubeType the type of cube to check placement for
     * @return set of valid coordinates where the cube can be placed
     */
    public Set<Coordinates> getSelectableCoordinates(CargoCube cubeType) {
        if (cubeType == null)
            return Collections.emptySet();

        List<Storage> selectableStorages = compatibleStoragesMap.get(cubeType);
        return shipBoard.getCoordinatesOfComponents(selectableStorages);
    }


    /**
     * Adds a storage to the selection by creating a copy to handle ObservableProperty issues.
     *
     * @param storageCoords coordinates of the storage to add
     * @param cubeType      type of cube to place in the storage
     * @return true if storage was successfully added, false otherwise
     */
    public boolean addStorageSelectionWithCopy(Coordinates storageCoords, CargoCube cubeType) {

        // Get the component at the specified coordinates
        Storage originalStorage = getStorageAtCoordinates(storageCoords);
        if (originalStorage == null) {
            return false;
        }

        // Check if the current cube is red and the storage is standard type
        if (cubeType == CargoCube.RED && !(originalStorage instanceof SpecialStorage)) {
            return false;  // Red cubes can only be placed in special storages
        }

        // Create a copy of the storage
        Storage storageClone = cloneStorage(originalStorage);

        // Add the cube to the copy
        CargoCube removedCube = storageClone.addCube(cubeType);

        // If a cube was removed, inform the user
        if (removedCube != null) {
            availableCubes.add(removedCube);
        }

        // Replace the storage in the shipboard
        replaceStorageInShipBoard(storageCoords, originalStorage, storageClone);

        availableCubes.remove(cubeType);

        // Add the coordinates to the selection list
        selectedStorages.add(storageCoords);
        updateFinalUpdates(storageCoords, storageClone.getStockedCubes());

        return true;
    }

    private Storage cloneStorage(Storage original) {
        Storage clone = getClonedStorage(original);

        // Copy common attributes from Component
        clone.setImageName(original.getImageName());
        for (int i = 0; i < original.getRotation(); i++)
            clone.rotate();

        // Copy existing cubes
        for (CargoCube cube : original.getStockedCubes()) {
            clone.addCube(cube);
        }

        return clone;
    }

    private static Storage getClonedStorage(Storage original) {
        Storage clone;

        // Create the correct instance based on type
        if (original instanceof SpecialStorage) {
            clone = new SpecialStorage(new EnumMap<>(original.getConnectors()), original.getMaxCapacity());
        } else if (original instanceof StandardStorage) {
            clone = new StandardStorage(new EnumMap<>(original.getConnectors()), original.getMaxCapacity());
        } else {
            throw new IllegalArgumentException("Unknown storage type: " + original.getClass());
        }

        return clone;
    }

    /**
     * Replaces the storage in the shipboard and updates the componentsPerType map.
     *
     * @param coords The coordinates of the storage
     * @param oldStorage The original storage to replace
     * @param newStorage The new storage that replaces the old one
     */
    private void replaceStorageInShipBoard(Coordinates coords, Storage oldStorage, Storage newStorage) {
        // Replace it in the matrix
        Component[][] shipMatrix = shipBoard.getShipMatrix();
        shipMatrix[coords.getX()][coords.getY()] = newStorage;

        // Update the componentsPerType map
        Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();

        // Remove the old storage from its type list
        Class<?> storageClass = oldStorage.getClass();
        List<Component> storageList = componentsPerType.get(storageClass);
        if (storageList != null) {
            storageList.remove(oldStorage);
            storageList.add(newStorage);
        }

        updateCompatibilityMapAfterReplacement(oldStorage, newStorage);
    }

    /**
     * Updates compatibility map after replacing the new storage
     */
    private void updateCompatibilityMapAfterReplacement(Storage oldStorage, Storage newStorage) {
        for (Map.Entry<CargoCube, List<Storage>> entry : compatibleStoragesMap.entrySet()) {
            List<Storage> storageList = entry.getValue();

            // Trova e sostituisci il vecchio storage con il nuovo
            int index = storageList.indexOf(oldStorage);
            if (index != -1) {
                storageList.set(index, newStorage);
            }
        }
    }

    // ======== METHODS FOR REDISTRIBUTION ========

    /**
     * Initializes redistribution mode with bonus cubes.
     *
     * @param newCubes bonus cubes to add
     */
    public void startRedistribution(List<CargoCube> newCubes) {
        this.redistributionMode = true;
        this.availableCubes.addAll(newCubes);
        this.finalUpdates.clear();
        this.selectedCubeIndex = -1;

        // Initialize finalUpdates with the current state of the storages
        initializeFinalUpdatesWithCurrentState();
    }

    /**
     * Initializes the update map with the current state of the storages.
     * ALWAYS includes all storages in the map, even if empty, to ensure the server receives the complete state.
     */
    private void initializeFinalUpdatesWithCurrentState() {
        Map<Coordinates, Storage> coordsAndStorages = shipBoard.getCoordinatesAndStorages();
        for (Map.Entry<Coordinates, Storage> entry : coordsAndStorages.entrySet()) {
            Storage storage = entry.getValue();
            // ALWAYS add all storages, even if empty
            finalUpdates.put(entry.getKey(), new ArrayList<>(storage.getStockedCubes()));
        }
    }

    /**
     * Selects a cube by index.
     *
     * @param index index of the cube to select
     * @return true if the selection was successful
     */
    public boolean selectCubeByIndex(int index) {
        if (index >= 0 && index < availableCubes.size()) {
            selectedCubeIndex = index;
            return true;
        }
        return false;
    }

    /**
     * Gets the currently selected cube.
     *
     * @return the selected cube or null if none is selected
     */
    public CargoCube getSelectedCube() {
        if (selectedCubeIndex >= 0 && selectedCubeIndex < availableCubes.size()) {
            return availableCubes.get(selectedCubeIndex);
        }
        return null;
    }

    /**
     * Gets the list of available cubes.
     *
     * @return copy of the list of available cubes
     */
    /**
     * Gets the list of cubes currently available for placement.
     *
     * @return copy of the list of available cubes
     */
    public List<CargoCube> getAvailableCubes() {
        return new ArrayList<>(availableCubes);
    }

    /**
     * Gets the index of the selected cube.
     *
     * @return index of the selected cube or -1 if none is selected
     */
    /**
     * Gets the index of the currently selected cube.
     *
     * @return index of selected cube or -1 if none selected
     */
    public int getSelectedCubeIndex() {
        return selectedCubeIndex;
    }

    /**
     * Adds the selected cube to the specified storage.
     *
     * @param coord coordinates of the storage
     * @return true if the operation succeeded
     */
    /**
     * Adds the currently selected cube to the specified storage.
     *
     * @param coord coordinates of the target storage
     * @return true if cube was successfully added, false otherwise
     */
    public boolean addSelectedCubeToStorage(Coordinates coord) {
        if (selectedCubeIndex < 0 || selectedCubeIndex >= availableCubes.size()) {
            return false;
        }

        CargoCube selectedCube = availableCubes.get(selectedCubeIndex);
        Storage storage = getStorageAt(coord);

        if (storage == null) {
            return false;
        }

        // Validate red cube
        if (selectedCube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
            return false;
        }

        // Visually update using the existing logic
        Storage clone = cloneStorage(storage);
        CargoCube displacedCube = clone.addCube(selectedCube);
        replaceStorageInShipBoard(coord, storage, clone);

        // If a cube was displaced, re-add it to availableCubes
        if (displacedCube != null) {
            availableCubes.add(displacedCube);
        }

        // Remove from available cubes
        availableCubes.remove(selectedCubeIndex);
        selectedCubeIndex = -1;

        // Update final updates map
        updateFinalUpdates(coord, clone.getStockedCubes());

        return true;
    }

    /**
     * Rimuove un cubo dallo storage specificato (lo riaggiunge ai disponibili).
     *
     * @param coords coordinate dello storage
     */
    /**
     * Removes a cube from the specified storage and adds it back to available cubes.
     * Creates a copy of the storage to handle ObservableProperty issues.
     *
     * @param coords coordinates of the storage to remove cube from
     */
    public void removeCubeFromStorageWithCopy(Coordinates coords) {
        Storage originalStorage = getStorageAt(coords);
        if (originalStorage == null || originalStorage.getStockedCubes().isEmpty()) {
            return;
        }

        CargoCube removedCube = originalStorage.getStockedCubes().getLast();

        // Rimuovi visivamente
        Storage storageClone = cloneStorage(originalStorage);
        storageClone.getStockedCubes().remove(removedCube);
        replaceStorageInShipBoard(coords, originalStorage, storageClone);

        // Riaggiunge ai disponibili
        availableCubes.add(removedCube);

        // Aggiorna mappa finale
        updateFinalUpdates(coords, storageClone.getStockedCubes());
    }

    /**
     * Removes a cube from the specified storage (re-adds it to available cubes).
     *
     * @param coord coordinates of the storage
     */
    /**
     * Removes a cube from the specified storage and adds it back to available cubes.
     *
     * @param coord coordinates of the storage to remove cube from
     */
    public void removeCubeFromStorage(Coordinates coord) {
        Storage storage = getStorageAt(coord);
        if (storage == null || storage.getStockedCubes().isEmpty()) {
            return;
        }

        CargoCube removedCube = storage.getStockedCubes().get(storage.getStockedCubes().size() - 1);

        // Visually remove
        Storage clone = cloneStorage(storage);
        clone.getStockedCubes().remove(removedCube);
        replaceStorageInShipBoard(coord, storage, clone);

        // Re-add to available cubes
        availableCubes.add(removedCube);

        // Update final updates map
        updateFinalUpdates(coord, clone.getStockedCubes());
    }

    /**
     * Checks if redistribution is complete.
     *
     * @return true if all cubes have been placed
     */
    /**
     * Checks if the redistribution process is complete.
     *
     * @return true if all cubes have been placed, false otherwise
     */
    public boolean isRedistributionComplete() {
        return availableCubes.isEmpty();
    }

    /**
     * Gets the final updates map for sending to the server.
     *
     * @return updates map
     */
    /**
     * Gets the final map of storage updates for sending to server.
     *
     * @return map of coordinates to final cube configurations
     */
    public Map<Coordinates, List<CargoCube>> getFinalUpdates() {
        return new HashMap<>(finalUpdates);
    }

    /**
     * Helper to get a storage at the specified coordinates.
     *
     * @param coord coordinates of the storage
     * @return storage at the coordinates or null if not found
     */
    private Storage getStorageAt(Coordinates coord) {
        return shipBoard.getCoordinatesAndStorages().get(coord);
    }

    /**
     * Helper to update the final updates map.
     * IMPORTANT: Always include the coordinates in the map, even if the list is empty,
     * to notify the server that the storage has been modified.
     *
     * @param coord coordinates of the storage
     * @param cubes list of cubes in the storage
     */
    private void updateFinalUpdates(Coordinates coord, List<CargoCube> cubes) {
        // ALWAYS put the coordinates in the map, even if the list is empty
        // This is essential to notify the server that the storage has been modified
        finalUpdates.put(coord, new ArrayList<>(cubes));
    }

    /**
     * Reset for reuse.
     */
    /**
     * Resets the redistribution state for reuse.
     * Clears available cubes, updates map and resets mode flags.
     */
    public void resetRedistribution() {
        availableCubes.clear();
        finalUpdates.clear();
        redistributionMode = false;
        selectedCubeIndex = -1;
    }

    /**
     * Checks if it is in redistribution mode.
     *
     * @return true if in redistribution mode
     */
    /**
     * Checks if the manager is currently in redistribution mode.
     *
     * @return true if in redistribution mode, false otherwise
     */
    public boolean isInRedistributionMode() {
        return redistributionMode;
    }

    // ======== METODI PER LA GESTIONE CUBE MALUS ========

    /**
     * Initializes the malus mode by building the cube-storage location map.
     * Called by the constructor when malusMode is true.
     */
    public void initializeForMalusMode() {
        if (!malusMode) {
            return;
        }
        buildCubeLocationMap();
    }

    /**
     * Builds a map that associates each cube type with the coordinates of storages containing it.
     * Called by initializeForMalusMode() and whenever the cube locations need to be refreshed.
     */
    /**
     * Builds a map associating cube types with storage locations containing them.
     * Called when cube locations need to be refreshed.
     */
    public void buildCubeLocationMap() {
        cubeLocationMap.clear();

        // Initialize empty lists for each cube type
        for (CargoCube cubeType : CargoCube.values()) {
            cubeLocationMap.put(cubeType, new ArrayList<>());
        }

        // Populate the map by scanning all storages
        shipBoard.getCoordinatesAndStorages().forEach((coords, storage) -> {
            for (CargoCube cube : storage.getStockedCubes()) {
                cubeLocationMap.get(cube).add(coords);
            }
        });
    }

    /**
     * Determines the most precious cube available following priority RED > YELLOW > GREEN > BLUE.
     * Called by validateStorageSelectionWithDetails() and UI methods to know which cube should be removed next.
     *
     * @return The most precious cube type available or null if no cubes exist
     */
    /**
     * Gets the most precious cube type currently available.
     * Follows priority: RED > YELLOW > GREEN > BLUE
     *
     * @return most precious available cube type or null if none exist
     */
    public CargoCube getMostPreciousCubeAvailable() {
        // Priority order: RED > YELLOW > GREEN > BLUE
        CargoCube[] priorityOrder = {CargoCube.RED, CargoCube.YELLOW, CargoCube.GREEN, CargoCube.BLUE};

        for (CargoCube cubeType : priorityOrder) {
            if (!cubeLocationMap.get(cubeType).isEmpty()) {
                return cubeType;
            }
        }
        return null; // No cubes available
    }

    /**
     * Validates if a storage selection is valid for cube removal.
     * Called by ClientCLIView when user selects storage coordinates for malus removal.
     *
     * @param coords Coordinates of the selected storage
     * @return true if selection is valid, false otherwise
     */
    /**
     * Validates if a storage can be selected for cube removal.
     *
     * @param coords coordinates to validate
     * @return true if selection is valid, false otherwise
     */
    public boolean isValidStorageSelection(Coordinates coords) {
        // Check if there's a storage at the coordinates
        Storage storage = getStorageAtCoordinates(coords);
        if (storage == null) {
            return false;
        }

        // Determine the most precious cube available
        CargoCube mostPreciousCube = getMostPreciousCubeAvailable();
        if (mostPreciousCube == null) {
            return false;
        }

        // Check if this storage contains the most precious cube
        List<Coordinates> validStorages = cubeLocationMap.get(mostPreciousCube);
        return validStorages.contains(coords);
    }

    /**
     * Gets error message for invalid storage selection.
     * Called by ClientCLIView to display appropriate error message.
     *
     * @param coords Coordinates that were invalid
     * @return Error message string
     */
    /**
     * Gets error message explaining why a storage selection was invalid.
     *
     * @param coords coordinates that were invalid
     * @return error message string
     */
    public String getValidationErrorMessage(Coordinates coords) {
        Storage storage = getStorageAtCoordinates(coords);
        if (storage == null) {
            return "No storage at these coordinates.";
        }

        CargoCube mostPreciousCube = getMostPreciousCubeAvailable();
        if (mostPreciousCube == null) {
            return "No cubes available to remove.";
        }

        return "ATTENTION! There are more precious cubes available!\nYou must remove " +
               mostPreciousCube.name() + " cubes first!";
    }

    /**
     * Selects a storage for cube removal, updating internal state and counters.
     * Called by ClientCLIView after successful validation to register the storage selection.
     *
     * @param coords Coordinates of the storage to select
     * @return true if selection was successful, false otherwise
     */
    /**
     * Selects a storage for cube removal during malus mode.
     *
     * @param coords coordinates of storage to select
     * @return true if selection was successful, false otherwise
     */
    public boolean selectStorageForRemoval(Coordinates coords) {
        if (!isValidStorageSelection(coords)) {
            return false;
        }

        // Add to selected storages list
        selectedStoragesForRemoval.add(coords);

        // Get the cube type that will be removed (most precious available)
        CargoCube removedCube = getMostPreciousCubeAvailable();
        updateCubeLocationMapAfterSelection(coords, removedCube);

        Storage currStorage = getStorageAtCoordinates(coords);
        currStorage.removeCube(removedCube);

        // Decrease the counter
        remainingCubesToRemove--;

        return true;
    }

    /**
     * Updates the cube location map after a storage selection for removal.
     * Called by selectStorageForRemoval() to maintain map consistency after cube removal.
     *
     * @param coords Storage coordinates
     * @param removedCube Type of cube being removed
     */
    /**
     * Updates the cube location map after removing a cube from storage.
     *
     * @param coords      storage coordinates where cube was removed
     * @param removedCube type of cube that was removed
     */
    public void updateCubeLocationMapAfterSelection(Coordinates coords, CargoCube removedCube) {
        List<Coordinates> locations = cubeLocationMap.get(removedCube);
        locations.remove(coords);
    }

    /**
     * Checks if there are still cubes to remove.
     * Called by ClientCLIView to determine if malus selection should continue.
     *
     * @return true if there are still cubes to remove
     */
    /**
     * Checks if there are still cubes that need to be removed in malus mode.
     *
     * @return true if there are remaining cubes to remove
     */
    public boolean hasRemainingCubesToRemove() {
        return remainingCubesToRemove > 0;
    }

    /**
     * Gets the number of cubes still to remove.
     * Called by ClientCLIView to display progress information to the user.
     *
     * @return Number of cubes still to remove
     */
    /**
     * Gets the number of cubes that still need to be removed in malus mode.
     *
     * @return number of remaining cubes to remove
     */
    public int getRemainingCubesToRemove() {
        return remainingCubesToRemove;
    }

    /**
     * Gets the list of storage coordinates selected for removal.
     * Called by ClientCLIView when malus selection is complete to send to server.
     *
     * @return List of coordinates of selected storages
     */
    /**
     * Gets the list of storage coordinates selected for cube removal.
     *
     * @return list of selected storage coordinates
     */
    public List<Coordinates> getSelectedStoragesForRemoval() {
        return new ArrayList<>(selectedStoragesForRemoval);
    }

    /**
     * Checks if batteries are needed to complete the malus.
     * Called by ClientCLIView to determine if transition to battery selection is needed.
     *
     * @return true if batteries are needed
     */
    /**
     * Checks if batteries are needed to complete the malus removal.
     *
     * @return true if batteries are needed, false otherwise
     */
    public boolean needsBatteries() {
        return malusMode && remainingCubesToRemove > 0;
    }

    /**
     * Calculates how many batteries are needed to complete the malus.
     * Called by ClientCLIView to determine exact number of batteries to select.
     *
     * @return Number of batteries needed
     */
    /**
     * Calculates how many batteries are needed to complete the malus removal.
     *
     * @return number of batteries required
     */
    public int getRequiredBatteriesCount() {
        return Math.max(0, remainingCubesToRemove);
    }

    /**
     * Checks if there are cubes available for removal.
     * Called by ClientCLIView at malus start to determine if cube selection or battery selection should begin.
     *
     * @return true if cubes are available
     */
    /**
     * Checks if there are any cubes available for removal in malus mode.
     *
     * @return true if cubes are available for removal
     */
    public boolean hasAvailableCubes() {
        if (!malusMode) {
            return false;
        }

        // Check if at least one cube type has available storages
        for (List<Coordinates> locations : cubeLocationMap.values()) {
            if (!locations.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets coordinates of storages containing the most precious cube.
     * Called by ClientCLIView to display valid options when user makes invalid selection.
     *
     * @return List of valid coordinates or empty list if no cubes exist
     */
    /**
     * Gets coordinates of storages containing the most precious available cube.
     *
     * @return list of valid storage coordinates or empty list if no cubes exist
     */
    public List<Coordinates> getValidStorageOptionsForMostPreciousCube() {
        CargoCube mostPrecious = getMostPreciousCubeAvailable();
        if (mostPrecious == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cubeLocationMap.get(mostPrecious));
    }

    /**
     * Resets the state for malus mode.
     * Called by ClientCLIView when malus selection is complete or cancelled.
     */
    /**
     * Resets the malus mode state.
     * Clears selected storages, cube locations and counters.
     */
    public void resetMalusState() {
        selectedStoragesForRemoval.clear();
        cubeLocationMap.clear();
        remainingCubesToRemove = 0;
    }

    /**
     * Checks if currently in malus mode.
     * Called by various methods to ensure malus-specific operations are only performed in malus mode.
     *
     * @return true if in malus mode
     */
    /**
     * Checks if the manager is currently in malus mode.
     *
     * @return true if in malus mode, false otherwise
     */
    public boolean isInMalusMode() {
        return malusMode;
    }

}
