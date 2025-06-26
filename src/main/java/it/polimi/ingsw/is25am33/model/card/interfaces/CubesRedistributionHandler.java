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
     * Validates the storage updates provided by checking their compliance with the game rules and constraints.
     *
     * @param storageUpdates a map where the keys are the coordinates of the storages to be updated,
     *                       and the values are lists of CargoCubes to be placed in the corresponding storages
     * @param gameModel the current state of the game, containing information about the player, their ship, and storages
     * @throws IllegalArgumentException if any of the following conditions are met:
     *         - No storage is found at the specified coordinates.
     *         - RED CargoCubes are attempted to be placed in a storage that is not of type SpecialStorage.
     *         - The number of cubes exceeds the maximum capacity of the specified storage.
     *         - The coordinates provided are outside of the valid shipboard matrix bounds.
     *         - The specified coordinates do not point to a storage component on the shipboard.
     */
    default void validateStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates, GameModel gameModel){
        Player currentPlayer = gameModel.getCurrPlayer();
        ShipBoard shipBoard = currentPlayer.getPersonalBoard();

        for (Map.Entry<Coordinates, List<CargoCube>> entry : storageUpdates.entrySet()) {
            Coordinates coord = entry.getKey();
            List<CargoCube> cubes = entry.getValue();

            // 1. Check if the storage exists
            Storage storage = shipBoard.getCoordinatesAndStorages().get(coord);
            if (storage == null) {
                throw new IllegalArgumentException("No storage found at coordinates: " + coord);
            }

            // 2. Ensure RED cubes are only placed in SpecialStorage
            for (CargoCube cube : cubes) {
                if (cube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
                    throw new IllegalArgumentException("RED cubes can only be placed in SpecialStorage");
                }
            }

            // 3. Check storage capacity
            if (cubes.size() > storage.getMaxCapacity()) {
                throw new IllegalArgumentException("Storage at coordinates " + coord +
                        " can hold at most " + storage.getMaxCapacity() +
                        " cubes, but " + cubes.size() + " were specified");
            }

            // 4. Validate coordinates
            Component[][] shipMatrix = shipBoard.getShipMatrix();
            if (coord.getX() < 0 || coord.getX() >= shipMatrix.length ||
                    coord.getY() < 0 || coord.getY() >= shipMatrix[0].length) {
                throw new IllegalArgumentException("Invalid coordinates: " + coord);
            }

            // 5. Check that the component is actually a storage
            Component component = shipMatrix[coord.getX()][coord.getY()];
            if (!(component instanceof Storage)) {
                throw new IllegalArgumentException("Component at coordinates " + coord +
                        " is not a storage");
            }
        }
    }

    /**
     * Applies the updates to the storages.
     * Updates the storages on a player's ship board with the provided storage updates.
     * The existing cargo cubes in each affected storage are cleared and replaced
     * with new cubes as specified in the storageUpdates map.
     *
     * @param storageUpdates map of updates to be applied
     * @param gameModel the current game model
     * @param storageUpdates a map where each key represents the coordinates of a storage,
     *                       and the value is a list of CargoCube objects to be added to that storage
     * @param gameModel the current game model containing the player's ship board and game state
     */
    default void applyStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates, GameModel gameModel) {
        Player currentPlayer = gameModel.getCurrPlayer();
        ShipBoard shipBoard = currentPlayer.getPersonalBoard();

        for (Map.Entry<Coordinates, List<CargoCube>> entry : storageUpdates.entrySet()) {
            Coordinates coord = entry.getKey();
            List<CargoCube> newCubes = entry.getValue();

            Storage storage = shipBoard.getCoordinatesAndStorages().get(coord);

            // Clear the storage
            storage.getStockedCubes().clear();

            // Add new cubes
            storage.getStockedCubes().addAll(newCubes);
        }
    }
}