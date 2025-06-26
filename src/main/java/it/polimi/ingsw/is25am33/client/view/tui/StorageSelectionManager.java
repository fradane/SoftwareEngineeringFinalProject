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

    // Nuovi campi per la ridistribuzione
    private List<CargoCube> availableCubes = new ArrayList<>();
    private Map<Coordinates, List<CargoCube>> finalUpdates = new HashMap<>();
    private boolean redistributionMode = false;
    private int selectedCubeIndex = -1;

    // Nuovi campi per la gestione cube malus
    private Map<CargoCube, List<Coordinates>> cubeLocationMap = new HashMap<>();
    private int remainingCubesToRemove;
    private List<Coordinates> selectedStoragesForRemoval = new ArrayList<>();
    private boolean malusMode = false;

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
        this.malusMode = false;

        // Inizializza la lista degli storage disponibili
        initializeAvailableStorages();

        // Costruisce la mappa di compatibilità tra cubi e storage
        buildCompatibilityMap();
    }

    /**
     * Costruttore specifico per cube malus.
     *
     * @param cubeMalus Numero di cubi da rimuovere
     * @param shipBoard La ship board del giocatore contenente gli storage
     */
    public StorageSelectionManager(int cubeMalus, ShipBoardClient shipBoard) {
        this.cubeRewards = new ArrayList<>(); // Lista vuota per malus
        this.cubeMalus = cubeMalus;
        this.shipBoard = shipBoard;
        this.availableStorages = new ArrayList<>();
        this.compatibleStoragesMap = new HashMap<>();
        this.malusMode = true;
        this.remainingCubesToRemove = cubeMalus;

        // Inizializza la lista degli storage disponibili
        initializeAvailableStorages();

        // Costruisce la mappa di compatibilità tra cubi e storage
        buildCompatibilityMap();

        // Inizializza per modalità malus
        initializeForMalusMode();
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

    // ======== METODI PER LA RIDISTRIBUZIONE ========

    /**
     * Inizializza la modalità ridistribuzione con i cubi bonus.
     * 
     * @param newCubes cubi bonus da aggiungere
     */
    public void startRedistribution(List<CargoCube> newCubes) {
        this.redistributionMode = true;
        this.availableCubes.addAll(newCubes);
        this.finalUpdates.clear();
        this.selectedCubeIndex = -1;
        
        // Inizializza finalUpdates con lo stato attuale degli storage
        initializeFinalUpdatesWithCurrentState();
    }

    /**
     * Inizializza la mappa degli aggiornamenti con lo stato attuale degli storage.
     * Include TUTTI gli storage nella mappa, anche quelli vuoti, per garantire
     * che il server riceva lo stato completo.
     */
    private void initializeFinalUpdatesWithCurrentState() {
        Map<Coordinates, Storage> coordsAndStorages = shipBoard.getCoordinatesAndStorages();
        for (Map.Entry<Coordinates, Storage> entry : coordsAndStorages.entrySet()) {
            Storage storage = entry.getValue();
            // SEMPRE aggiungi tutti gli storage, anche se vuoti
            finalUpdates.put(entry.getKey(), new ArrayList<>(storage.getStockedCubes()));
        }
    }

    /**
     * Seleziona un cubo tramite indice.
     * 
     * @param index indice del cubo da selezionare
     * @return true se la selezione è avvenuta con successo
     */
    public boolean selectCubeByIndex(int index) {
        if (index >= 0 && index < availableCubes.size()) {
            selectedCubeIndex = index;
            return true;
        }
        return false;
    }

    /**
     * Ottiene il cubo attualmente selezionato.
     * 
     * @return il cubo selezionato o null se nessuno è selezionato
     */
    public CargoCube getSelectedCube() {
        if (selectedCubeIndex >= 0 && selectedCubeIndex < availableCubes.size()) {
            return availableCubes.get(selectedCubeIndex);
        }
        return null;
    }

    /**
     * Ottiene la lista dei cubi disponibili.
     * 
     * @return copia della lista dei cubi disponibili
     */
    public List<CargoCube> getAvailableCubes() {
        return new ArrayList<>(availableCubes);
    }

    /**
     * Ottiene l'indice del cubo selezionato.
     * 
     * @return indice del cubo selezionato o -1 se nessuno è selezionato
     */
    public int getSelectedCubeIndex() {
        return selectedCubeIndex;
    }

    /**
     * Aggiunge il cubo selezionato allo storage specificato.
     * 
     * @param coord coordinate dello storage
     * @return true se l'operazione è riuscita
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

        // Validazione cubo rosso
        if (selectedCube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
            return false;
        }

        // Aggiorna visivamente usando la logica esistente
        Storage clone = cloneStorage(storage);
        CargoCube displacedCube = clone.addCube(selectedCube);
        replaceStorageInShipBoard(coord, storage, clone);

        // Se un cubo è stato espulso, riaggiungilo agli availableCubes
        if (displacedCube != null) {
            availableCubes.add(displacedCube);
        }

        // Rimuovi dai disponibili
        availableCubes.remove(selectedCubeIndex);
        selectedCubeIndex = -1;

        // Aggiorna mappa finale
        updateFinalUpdates(coord, clone.getStockedCubes());
        
        return true;
    }

    /**
     * Rimuove un cubo dallo storage specificato (lo riaggiunge ai disponibili).
     * 
     * @param coord coordinate dello storage
     */
    public void removeCubeFromStorage(Coordinates coord) {
        Storage storage = getStorageAt(coord);
        if (storage == null || storage.getStockedCubes().isEmpty()) {
            return;
        }

        CargoCube removedCube = storage.getStockedCubes().get(storage.getStockedCubes().size() - 1);
        
        // Rimuovi visivamente
        Storage clone = cloneStorage(storage);
        clone.getStockedCubes().remove(removedCube);
        replaceStorageInShipBoard(coord, storage, clone);
        
        // Riaggiunge ai disponibili
        availableCubes.add(removedCube);
        
        // Aggiorna mappa finale
        updateFinalUpdates(coord, clone.getStockedCubes());
    }

    /**
     * Verifica se la ridistribuzione è completa.
     * 
     * @return true se tutti i cubi sono stati posizionati
     */
    public boolean isRedistributionComplete() {
        return availableCubes.isEmpty();
    }

    /**
     * Ottiene la mappa finale degli aggiornamenti per l'invio al server.
     * 
     * @return mappa degli aggiornamenti
     */
    public Map<Coordinates, List<CargoCube>> getFinalUpdates() {
        return new HashMap<>(finalUpdates);
    }

    /**
     * Helper per ottenere uno storage alle coordinate specificate.
     * 
     * @param coord coordinate dello storage
     * @return storage alle coordinate o null se non trovato
     */
    private Storage getStorageAt(Coordinates coord) {
        return shipBoard.getCoordinatesAndStorages().get(coord);
    }

    /**
     * Helper per aggiornare la mappa finale degli aggiornamenti.
     * IMPORTANTE: Include sempre le coordinate nella mappa, anche se la lista è vuota,
     * per notificare al server che lo storage è stato modificato.
     * 
     * @param coord coordinate dello storage
     * @param cubes lista dei cubi nello storage
     */
    private void updateFinalUpdates(Coordinates coord, List<CargoCube> cubes) {
        // SEMPRE metti le coordinate nella mappa, anche se lista vuota
        // Questo è essenziale per notificare al server che lo storage è stato modificato
        finalUpdates.put(coord, new ArrayList<>(cubes));
    }

    /**
     * Reset per nuovo utilizzo.
     */
    public void resetRedistribution() {
        availableCubes.clear();
        finalUpdates.clear();
        redistributionMode = false;
        selectedCubeIndex = -1;
    }

    /**
     * Verifica se è in modalità ridistribuzione.
     * 
     * @return true se in modalità ridistribuzione
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
    public boolean hasRemainingCubesToRemove() {
        return remainingCubesToRemove > 0;
    }

    /**
     * Gets the number of cubes still to remove.
     * Called by ClientCLIView to display progress information to the user.
     * 
     * @return Number of cubes still to remove
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
    public List<Coordinates> getSelectedStoragesForRemoval() {
        return new ArrayList<>(selectedStoragesForRemoval);
    }

    /**
     * Checks if batteries are needed to complete the malus.
     * Called by ClientCLIView to determine if transition to battery selection is needed.
     * 
     * @return true if batteries are needed
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
    public int getRequiredBatteriesCount() {
        return Math.max(0, remainingCubesToRemove);
    }

    /**
     * Checks if there are cubes available for removal.
     * Called by ClientCLIView at malus start to determine if cube selection or battery selection should begin.
     * 
     * @return true if cubes are available
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
    public boolean isInMalusMode() {
        return malusMode;
    }

}
