package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.List;
import java.util.Map;

public interface CubesRedistributionHandler {
    /**
     * Valida gli aggiornamenti degli storage.
     *
     * @param storageUpdates mappa degli aggiornamenti da validare
     * @param gameModel
     * @throws IllegalArgumentException se la validazione fallisce
     */
    default void validateStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates, GameModel gameModel) {
        Player currentPlayer = gameModel.getCurrPlayer();
        ShipBoard shipBoard = currentPlayer.getPersonalBoard();

        for (Map.Entry<Coordinates, List<CargoCube>> entry : storageUpdates.entrySet()) {
            Coordinates coord = entry.getKey();
            List<CargoCube> cubes = entry.getValue();

            // 1. Controllo esistenza storage
            Storage storage = shipBoard.getCoordinatesAndStorages().get(coord);
            if (storage == null) {
                throw new IllegalArgumentException("Storage non trovato alle coordinate: " + coord);
            }

            // 2. Controllo cubi rossi solo in SpecialStorage
            for (CargoCube cube : cubes) {
                if (cube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
                    throw new IllegalArgumentException("I cubi rossi possono essere posizionati solo in SpecialStorage");
                }
            }

            // 3. Controllo capacità storage
            if (cubes.size() > storage.getMaxCapacity()) {
                throw new IllegalArgumentException("Storage alle coordinate " + coord +
                        " può contenere massimo " + storage.getMaxCapacity() +
                        " cubi, ma ne sono stati specificati " + cubes.size());
            }

            // 4. Controllo coordinate valide
            Component[][] shipMatrix = shipBoard.getShipMatrix();
            if (coord.getX() < 0 || coord.getX() >= shipMatrix.length ||
                    coord.getY() < 0 || coord.getY() >= shipMatrix[0].length) {
                throw new IllegalArgumentException("Coordinate non valide: " + coord);
            }

            // 5. Controllo che sia effettivamente uno storage
            Component component = shipMatrix[coord.getX()][coord.getY()];
            if (!(component instanceof Storage)) {
                throw new IllegalArgumentException("Il componente alle coordinate " + coord +
                        " non è uno storage");
            }
        }
    }

    /**
     * Applica gli aggiornamenti degli storage.
     *
     * @param storageUpdates mappa degli aggiornamenti da applicare
     * @param gameModel
     */
    default void applyStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates, GameModel gameModel) {
        Player currentPlayer = gameModel.getCurrPlayer();
        ShipBoard shipBoard = currentPlayer.getPersonalBoard();

        for (Map.Entry<Coordinates, List<CargoCube>> entry : storageUpdates.entrySet()) {
            Coordinates coord = entry.getKey();
            List<CargoCube> newCubes = entry.getValue();

            Storage storage = shipBoard.getCoordinatesAndStorages().get(coord);

            // Svuota storage
            storage.getStockedCubes().clear();

            // Aggiungi nuovi cubi
            storage.getStockedCubes().addAll(newCubes);
        }
    }
}
