package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Represents a Level 1 ship board in the game.
 * This class defines the layout and specific behaviors of the ship board at Level 1,
 * including valid positions, defense capabilities, and life support systems.
 *
 * Level 1 is a basic ship configuration with limited functionality:
 * - Cannot defend itself with single cannons
 * - Has no cabins with life support
 * - Cannot accept aliens
 */
public class Level1ShipBoard extends ShipBoard implements ShipBoardClient {

    /**
     * A 2D boolean array defining the valid positions on the Level 1 ship board.
     * {@code true} indicates a valid position, {@code false} indicates an invalid position.
     */
    static boolean[][] level1ValidPositions = {
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, true, false, false, false, false, false},
            {false, false, false, false, false, true, true, true, false, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, false, true, true, false, true, true, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false}
    };

    /**
     * Constructs a new Level 1 ship board.
     *
     * @param color              the player color associated with this ship board
     * @param gameClientNotifier the notifier used to update game clients
     * @param isGui              indicates whether this board is used in a GUI context
     */
    public Level1ShipBoard(PlayerColor color, GameClientNotifier gameClientNotifier, boolean isGui) {
        super(color, gameClientNotifier, isGui);
        this.validPositions = level1ValidPositions;
    }

    /**
     * Checks if this ship board can defend itself against a dangerous object using single cannons.
     * Level 1 ship boards cannot defend themselves with single cannons.
     *
     * @param obj the dangerous object that is threatening the ship
     * @return {@code false} as Level 1 ship boards cannot defend themselves with single cannons
     */
    @Override
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj) {
        return false;
    }

    /**
     * Determines if the specified coordinates are outside the valid ship board area.
     *
     * @param x the x-coordinate to check
     * @param y the y-coordinate to check
     * @return {@code true} if the coordinates are outside the ship board, {@code false} otherwise
     */
    public static Boolean isOutsideShipboard(int x, int y) {
        return !level1ValidPositions[x][y];
    }

    /**
     * Returns a map of coordinates to life support colors for all cabins with life support.
     * Level 1 ship boards have no cabins with life support in test flight mode.
     *
     * @return an empty map as there are no cabins with life support in Level 1
     */
    @Override
    public Map<Coordinates, Set<ColorLifeSupport>> getCabinsWithLifeSupport() {
        // in test flight, no cabin can have alien
        return Collections.emptyMap();
    }

    /**
     * Checks if the specified coordinates can accept an alien crew member.
     * Level 1 ship boards cannot accept aliens.
     *
     * @param coords the coordinates to check
     * @param alien  the alien crew member to potentially place
     * @return {@code false} as Level 1 ship boards cannot accept aliens
     */
    @Override
    public boolean canAcceptAlien(Coordinates coords, CrewMember alien) {
        return false;
    }

}
