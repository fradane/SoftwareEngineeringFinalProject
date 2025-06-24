package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.StandardStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Classe utilitaria per la gestione delle scelte di storage durante la fase di reward.
 * Permette di tenere traccia e validare le scelte degli storage per i cubi.
 */
public class StorageSelectionManager {

    private final List<Coordinates> selectedStorages = new ArrayList<>();
    private final List<CargoCube> cubeRewards;
    private final int cubeMalus;
    private final ShipBoardClient shipBoard;
    private final List<Storage> availableStorages;
    private final Map<CargoCube, List<Storage>> compatibleStoragesMap;

    /**
     * Costruttore per il gestore delle selezioni di storage.
     *
     * @param cubeRewards La lista di cubi reward da assegnare agli storage
     * @param shipBoard La ship board del giocatore contenente gli storage
     */
    public StorageSelectionManager(List<CargoCube> cubeRewards, int cubeMalus, ShipBoardClient shipBoard) {
        this.cubeRewards = new ArrayList<>(cubeRewards);
        this.shipBoard = shipBoard;
        this.availableStorages = new ArrayList<>();
        this.compatibleStoragesMap = new HashMap<>();
        this.cubeMalus = cubeMalus;

        // Inizializza la lista degli storage disponibili
        initializeAvailableStorages();

        // Costruisce la mappa di compatibilità tra cubi e storage
        buildCompatibilityMap();
    }

    public Map<CargoCube, List<Storage>> getCompatibleStoragesMap() {
        return compatibleStoragesMap;
    }

    /**
     * Inizializza la lista degli storage disponibili sulla ship board.
     */
    private void initializeAvailableStorages() {
        // Recupera tutti gli storage dalla ship board
        List<Storage> storages = shipBoard.getStorages();
        if (storages != null) {
            this.availableStorages.addAll(storages);
        }
    }

    /**
     * Costruisce una mappa che associa ogni tipo di CargoCube agli storage compatibili.
     */
    private void buildCompatibilityMap() {
        // Inizializza le liste per ogni tipo di cubo
        for (CargoCube cubeType : CargoCube.values()) {
            compatibleStoragesMap.put(cubeType, new ArrayList<>());
        }

        // Popola la mappa
        for (Storage storage : availableStorages) {
            // Gli storage speciali possono contenere qualsiasi tipo di cubo
            if (storage instanceof SpecialStorage) {
                for (CargoCube cubeType : CargoCube.values()) {
                    compatibleStoragesMap.get(cubeType).add(storage);
                }
            } else {
                // Gli storage standard possono contenere tutti i cubi tranne quelli ROSSI
                for (CargoCube cubeType : CargoCube.values()) {
                    if (cubeType != CargoCube.RED) {
                        compatibleStoragesMap.get(cubeType).add(storage);
                    }
                }
            }
        }
    }

    /**
     * Verifica se il giocatore ha almeno uno storage disponibile.
     *
     * @return true se il giocatore ha almeno uno storage, false altrimenti
     */
    public boolean hasAnyStorage() {
        return !availableStorages.isEmpty();
    }

    /**
     * Aggiunge uno storage alla selezione per il cubo corrente.
     *
     * @param storageCoords Le coordinate dello storage selezionato
     * @return true se lo storage è stato aggiunto con successo, false altrimenti
     */
    public boolean addStorageSelection(Coordinates storageCoords) {
        // Verifico se abbiamo già selezionato tutti gli storage necessari
        if (selectedStorages.size() >= cubeRewards.size()) {
            return false;
        }

        // Il giocatore non vuole salvare questo cubo
        if(storageCoords.isCoordinateInvalid()){
            selectedStorages.add(storageCoords);
            return true;
        }

        // Ottengo il componente alle coordinate specificate
        Storage storage = getStorageAtCoordinates(storageCoords);
        if (storage == null) {
            return false;
        }

        // Controllo se il cubo corrente è rosso e lo storage è di tipo standard
        int currentCubeIndex = selectedStorages.size();
        CargoCube currentCube = cubeRewards.get(currentCubeIndex);

        if (currentCube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
            return false;  // Cubi rossi possono essere messi solo in storage speciali
        }

        // Verifico se lo storage è pieno e mostra informazioni sul cubo che verrebbe sostituito
        if (storage.isFull()) {
            List<CargoCube> storedCubes = storage.getStockedCubes();
            if (!storedCubes.isEmpty()) {
                // Ordina i cubi per valore (il meno prezioso per primo)
                storedCubes.sort(CargoCube.byValue);
                CargoCube leastValuableCube = storedCubes.getFirst();

                // Se il cubo meno prezioso ha un valore maggiore del cubo corrente, avvisa l'utente
                if (leastValuableCube.getValue() > currentCube.getValue()) {
                    //TODO farlo con showMessage
                    System.out.println("ATTENZIONE: Il cubo meno prezioso nello storage (" +
                            leastValuableCube + ", valore: " + leastValuableCube.getValue() +
                            ") è più prezioso del cubo corrente (" +
                            currentCube + ", valore: " + currentCube.getValue() + ")");
                }
            }
        }

        storage.addCube(currentCube);

        // Aggiungo le coordinate alla lista delle selezioni
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
     * Verifica se il giocatore può accettare il cubo corrente.
     *
     * @return true se il giocatore ha almeno uno storage compatibile per il cubo corrente
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
     * Verifica se sono stati selezionati tutti gli storage necessari.
     *
     * @return true se tutti i cubi hanno uno storage assegnato, false altrimenti
     */
    public boolean isSelectionComplete() {
        return selectedStorages.size() == cubeRewards.size();
    }

    /**
     * Ottiene la lista di coordinate degli storage selezionati.
     *
     * @return La lista delle coordinate degli storage selezionati
     */
    public List<Coordinates> getSelectedStorageCoordinates() {
        return new ArrayList<>(selectedStorages);
    }

    /**
     * Restituisce il cubo corrente da assegnare.
     *
     * @return Il cubo corrente o null se tutti i cubi sono stati assegnati
     */
    public CargoCube getCurrentCube() {
        if (selectedStorages.size() >= cubeRewards.size()) {
            return null;
        }
        return cubeRewards.get(selectedStorages.size());
    }

    /**
     * Ottiene lo storage alle coordinate specificate.
     *
     * @param coordinates Le coordinate dello storage
     * @return L'oggetto Storage alle coordinate specificate o null se non c'è uno storage
     */
    private Storage getStorageAtCoordinates(Coordinates coordinates) {
        if (coordinates == null || coordinates.isCoordinateInvalid()) {
            return null;
        }

        try {
            // Ottiene il componente e verifica se è uno storage
            Object component = shipBoard.getComponentAt(coordinates);
            if (component instanceof Storage) {
                return (Storage) component;
            }
        } catch (Exception e) {
            // Gestisce l'eccezione se le coordinate sono fuori dai limiti
        }

        return null;
    }

    /**
     * Controlla se uno storage è già pieno e fornisce informazioni sul cubo meno prezioso.
     *
     * @param storageCoords Le coordinate dello storage da controllare
     * @return Una stringa che descrive lo stato dello storage, o null se non è uno storage valido
     */
    public String checkStorageStatus(Coordinates storageCoords) {
        Storage storage = getStorageAtCoordinates(storageCoords);
        if (storage == null) {
            return null;
        }

        if (!storage.isFull()) {
            // Non possiamo usare getMaxCubes() e getCurrentCubes() perché non esistono
            // Utilizziamo il metodo getStockedCubes() per ottenere i cubi correnti
            List<CargoCube> storedCubes = storage.getStockedCubes();
            // Non possiamo determinare esattamente lo spazio libero, quindi diamo un messaggio generico
            return "Storage disponibile, contiene " + storedCubes.size() + " cubi";
        } else {
            // Ordino i cubi per valore e prendo il meno prezioso
            List<CargoCube> storedCubes = storage.getStockedCubes();
            if (storedCubes.isEmpty()) {
                return "Storage pieno, ma non ci sono cubi (stato anomalo)";
            } else {
                storedCubes.sort(CargoCube.byValue);
                CargoCube leastValuableCube = storedCubes.get(0);
                return "Storage pieno, il cubo meno prezioso è " + leastValuableCube +
                        " (valore: " + leastValuableCube.getValue() + ")";
            }
        }
    }

    /**
     * Restituisce il numero totale di cubi reward da gestire.
     *
     * @return Il numero totale di cubi reward
     */
    public int getTotalCubesCount() {
        return cubeRewards.size();
    }

    /**
     * Resetta tutte le selezioni.
     */
    public void reset() {
        selectedStorages.clear();
    }

    public Map<CargoCube, List<Coordinates>> whereAreCube() {

        //ottengo una lista degli storage che contengono un cubo di quel colore con ripetizioni

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

    public Set<Coordinates> getSelectableCoordinates() {
        CargoCube currentCube = getCurrentCube();
        List<Storage> selectableStorages = compatibleStoragesMap.get(currentCube);

        return shipBoard.getCoordinatesOfComponents(selectableStorages);
    }

    /**
     * Adds a storage to the selection for the current cube by creating a copy of the storage.
     * This solves the ObservableProperty issue that doesn't detect internal changes.
     *
     * @param storageCoords The coordinates of the selected storage
     * @return true if the storage was successfully added, false otherwise
     */
    public boolean addStorageSelectionWithCopy(Coordinates storageCoords) {
        // Check if we have already selected all necessary storages
        if (selectedStorages.size() >= cubeRewards.size()) {
            return false;
        }

        // The player doesn't want to save this cube
        if (storageCoords.isCoordinateInvalid()) {
            selectedStorages.add(storageCoords);
            return true;
        }

        // Get the component at the specified coordinates
        Storage originalStorage = getStorageAtCoordinates(storageCoords);
        if (originalStorage == null) {
            return false;
        }

        // Check if the current cube is red and the storage is standard type
        int currentCubeIndex = selectedStorages.size();
        CargoCube currentCube = cubeRewards.get(currentCubeIndex);

        if (currentCube == CargoCube.RED && !(originalStorage instanceof SpecialStorage)) {
            return false;  // Red cubes can only be placed in special storages
        }

        // Create a copy of the storage
        Storage storageClone = cloneStorage(originalStorage);

        // Add the cube to the copy
        CargoCube removedCube = storageClone.addCube(currentCube);

        // If a cube was removed, inform the user
        if (removedCube != null) {
            // TODO: use showMessage
            System.out.println("The cube " + removedCube + " was removed to make room for the new cube " + currentCube);
        }

        // Replace the storage in the shipboard
        replaceStorageInShipBoard(storageCoords, originalStorage, storageClone);

        // Add the coordinates to the selection list
        selectedStorages.add(storageCoords);

        return true;
    }

    /**
     * Creates a deep copy of the storage maintaining the correct type.
     *
     * @param original The original storage to clone
     * @return A new instance of the storage with the same attributes
     */
    private Storage cloneStorage(Storage original) {
        Storage clone = getClonedStorage(original);

        // Copy common attributes from Component
        clone.setCurrState(original.getCurrState());
        clone.setImageName(original.getImageName());
        for (int i = 0; i < original.getRotation(); i++)
            clone.rotate();

        // Copy existing cubes
        for (CargoCube cube : original.getStockedCubes()) {
            clone.addCube(cube);
        }

        return clone;
    }

    private static @NotNull Storage getClonedStorage(Storage original) {
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

}