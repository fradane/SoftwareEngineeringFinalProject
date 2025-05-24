package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitaria per la gestione delle scelte di storage durante la fase di reward.
 * Permette di tenere traccia e validare le scelte degli storage per i cubi.
 */
public class StorageSelectionManager {

    private final List<Coordinates> selectedStorages = new ArrayList<>();
    private final List<CargoCube> cubeRewards;
    private final ShipBoardClient shipBoard;
    private final List<Storage> availableStorages;
    private final Map<CargoCube, List<Storage>> compatibleStoragesMap;

    /**
     * Costruttore per il gestore delle selezioni di storage.
     *
     * @param cubeRewards La lista di cubi reward da assegnare agli storage
     * @param shipBoard La ship board del giocatore contenente gli storage
     */
    public StorageSelectionManager(List<CargoCube> cubeRewards, ShipBoardClient shipBoard) {
        this.cubeRewards = new ArrayList<>(cubeRewards);
        this.shipBoard = shipBoard;
        this.availableStorages = new ArrayList<>();
        this.compatibleStoragesMap = new HashMap<>();

        // Inizializza la lista degli storage disponibili
        initializeAvailableStorages();

        // Costruisce la mappa di compatibilità tra cubi e storage
        buildCompatibilityMap();
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
     * Verifica se il giocatore può accettare tutti i cubi reward.
     *
     * @return true se il giocatore ha storage sufficienti per tutti i cubi, false altrimenti
     */
    public boolean canAcceptAllCubes() {
        // Conta quanti cubi di ogni tipo ci sono nei reward
        Map<CargoCube, Integer> cubeCount = new HashMap<>();
        for (CargoCube cube : cubeRewards) {
            cubeCount.put(cube, cubeCount.getOrDefault(cube, 0) + 1);
        }

        // Verifica se ci sono storage compatibili sufficienti per ogni tipo di cubo
        for (Map.Entry<CargoCube, Integer> entry : cubeCount.entrySet()) {
            CargoCube cubeType = entry.getKey();
            int requiredCount = entry.getValue();

            // Se non ci sono abbastanza storage compatibili per questo tipo di cubo
            if (compatibleStoragesMap.getOrDefault(cubeType, new ArrayList<>()).size() < requiredCount) {
                return false;
            }
        }

        return true;
    }

    /**
     * Ottiene una mappa che indica quanti cubi di ogni tipo il giocatore non può accettare.
     *
     * @return Una mappa con il tipo di cubo e il numero di cubi che non possono essere accettati
     */
    public Map<CargoCube, Integer> getUnacceptableCubesCount() {
        Map<CargoCube, Integer> cubeCount = new HashMap<>();
        Map<CargoCube, Integer> unacceptableCubes = new HashMap<>();

        // Conta quanti cubi di ogni tipo ci sono nei reward
        for (CargoCube cube : cubeRewards) {
            cubeCount.put(cube, cubeCount.getOrDefault(cube, 0) + 1);
        }

        // Verifica quanti cubi di ogni tipo non possono essere accettati
        for (Map.Entry<CargoCube, Integer> entry : cubeCount.entrySet()) {
            CargoCube cubeType = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = compatibleStoragesMap.getOrDefault(cubeType, new ArrayList<>()).size();

            if (availableCount < requiredCount) {
                unacceptableCubes.put(cubeType, requiredCount - availableCount);
            }
        }

        return unacceptableCubes;
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
                CargoCube leastValuableCube = storedCubes.get(0);

                // Se il cubo meno prezioso ha un valore maggiore del cubo corrente, avvisa l'utente
                if (leastValuableCube.getValue() > currentCube.getValue()) {
                    System.out.println("ATTENZIONE: Il cubo meno prezioso nello storage (" +
                            leastValuableCube + ", valore: " + leastValuableCube.getValue() +
                            ") è più prezioso del cubo corrente (" +
                            currentCube + ", valore: " + currentCube.getValue() + ")");
                }
            }
        }

        // Aggiungo le coordinate alla lista delle selezioni
        selectedStorages.add(storageCoords);
        return true;
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

        // Conta quanti cubi di questo tipo sono già stati assegnati
        int alreadyAssigned = 0;
        for (int i = 0; i < selectedStorages.size(); i++) {
            if (cubeRewards.get(i) == currentCube) {
                alreadyAssigned++;
            }
        }

        // Verifica se ci sono storage compatibili rimanenti
        return compatibleStoragesMap.getOrDefault(currentCube, new ArrayList<>()).size() > alreadyAssigned;
    }

    /**
     * Rimuove l'ultima selezione di storage.
     *
     * @return true se una selezione è stata rimossa, false se la lista era già vuota
     */
    public boolean removeLastSelection() {
        if (selectedStorages.isEmpty()) {
            return false;
        }

        selectedStorages.remove(selectedStorages.size() - 1);
        return true;
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
     * Restituisce il numero di storage ancora da selezionare.
     *
     * @return Il numero di storage ancora da selezionare
     */
    public int getRemainingSelectionsCount() {
        return cubeRewards.size() - selectedStorages.size();
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
     * Genera una stringa che descrive quali cubi possono essere accettati e quali no.
     *
     * @return Una stringa informativa sulla compatibilità dei cubi
     */
    public String getStorageCompatibilityInfo() {
        if (!hasAnyStorage()) {
            return "Non hai storage disponibili. Non puoi accettare nessun cubo.";
        }

        Map<CargoCube, Integer> unacceptableCubes = getUnacceptableCubesCount();
        if (unacceptableCubes.isEmpty()) {
            return "Hai storage sufficienti per accettare tutti i cubi reward.";
        }

        StringBuilder info = new StringBuilder("Non hai abbastanza storage per accettare tutti i cubi:\n");
        for (Map.Entry<CargoCube, Integer> entry : unacceptableCubes.entrySet()) {
            info.append("- ").append(entry.getValue()).append(" cubi ").append(entry.getKey())
                    .append(" non possono essere accettati.\n");
        }

        return info.toString();
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
     * Restituisce il numero di cubi che possono effettivamente essere accettati
     * dal giocatore considerando i suoi storage disponibili.
     *
     * @return Il numero di cubi che possono essere accettati
     */
    public int getAcceptableCubesCount() {
        Map<CargoCube, Integer> cubeCount = new HashMap<>();

        // Conta quanti cubi di ogni tipo ci sono nei reward
        for (CargoCube cube : cubeRewards) {
            cubeCount.put(cube, cubeCount.getOrDefault(cube, 0) + 1);
        }

        int acceptableCount = 0;

        // Per ogni tipo di cubo, calcola quanti possono essere accettati
        for (Map.Entry<CargoCube, Integer> entry : cubeCount.entrySet()) {
            CargoCube cubeType = entry.getKey();
            int requiredCount = entry.getValue();
            int availableStorageCount = compatibleStoragesMap.getOrDefault(cubeType, new ArrayList<>()).size();

            // Il numero accettabile è il minimo tra richiesto e disponibile
            acceptableCount += Math.min(requiredCount, availableStorageCount);
        }

        return acceptableCount;
    }

    /**
     * Restituisce una descrizione dettagliata di quali cubi possono essere accettati.
     *
     * @return Una stringa che descrive lo stato di accettazione dei cubi
     */
    public String getDetailedAcceptabilityStatus() {
        if (!hasAnyStorage()) {
            return "Nessuno storage disponibile. Non puoi accettare nessun cubo.";
        }

        Map<CargoCube, Integer> cubeCount = new HashMap<>();
        for (CargoCube cube : cubeRewards) {
            cubeCount.put(cube, cubeCount.getOrDefault(cube, 0) + 1);
        }

        StringBuilder status = new StringBuilder("Stato accettazione cubi:\n");

        for (Map.Entry<CargoCube, Integer> entry : cubeCount.entrySet()) {
            CargoCube cubeType = entry.getKey();
            int requiredCount = entry.getValue();
            int availableStorageCount = compatibleStoragesMap.getOrDefault(cubeType, new ArrayList<>()).size();
            int acceptableCount = Math.min(requiredCount, availableStorageCount);

            status.append("- ").append(cubeType).append(": ")
                    .append(acceptableCount).append("/").append(requiredCount)
                    .append(" (").append(availableStorageCount).append(" storage compatibili)")
                    .append("\n");
        }

        return status.toString();
    }

    /**
     * Verifica se almeno un cubo può essere accettato dal giocatore.
     *
     * @return true se almeno un cubo può essere accettato, false altrimenti
     */
    public boolean canAcceptAnyCube() {
        return getAcceptableCubesCount() > 0;
    }

    /**
     * Aggiunge una selezione di storage anche se le coordinate sono invalide.
     * Questo permette di gestire il caso in cui un cubo non può essere salvato.
     *
     * @param storageCoords Le coordinate dello storage selezionato (possono essere invalide)
     * @return true se la selezione è stata registrata, false altrimenti
     */
    public boolean addStorageSelectionAllowInvalid(Coordinates storageCoords) {
        // Verifico se abbiamo già selezionato tutti gli storage necessari
        if (selectedStorages.size() >= cubeRewards.size()) {
            return false;
        }

        // Se le coordinate sono invalide, le aggiungiamo comunque per indicare
        // che questo cubo viene scartato
        if (storageCoords.isCoordinateInvalid()) {
            selectedStorages.add(storageCoords);
            return true;
        }

        // Altrimenti usiamo la logica normale
        return addStorageSelection(storageCoords);
    }
    /**
     * Resetta tutte le selezioni.
     */
    public void reset() {
        selectedStorages.clear();
    }
}