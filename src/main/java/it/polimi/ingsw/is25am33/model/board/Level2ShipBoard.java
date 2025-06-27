package it.polimi.ingsw.is25am33.model.board;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;

import java.util.Map;
import java.util.Set;

import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.NORTH;

/**
 * Represents a Level 2 ship board in the game.
 * This class defines the layout and specific behaviors of the ship board at Level 2,
 * including valid positions, defense capabilities, component booking, and life support systems.
 *
 * Level 2 is an intermediate ship configuration with enhanced functionality compared to Level 1:
 * - Can defend itself with single cannons based on direction
 * - Allows booking up to 2 components
 * - Inherits life support system and alien acceptance capabilities from the parent class
 */
public class Level2ShipBoard extends ShipBoard implements ShipBoardClient {

    /**
     * A 2D boolean array defining the valid positions on the Level 2 ship board.
     * {@code true} indicates a valid position, {@code false} indicates an invalid position.
     */
    static boolean[][] level2ValidPositions = {
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, true, false, true, false, false, false, false},
            {false, false, false, false, true, true, true, true, true, false, false, false},
            {false, false, false, true, true, true, true, true, true, true, false, false},
            {false, false, false, true, true, true, true, true, true, true, false, false},
            {false, false, false, true, true, true, false, true, true, true, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false, false, false, false}
    };

    /**
     * Constructs a new Level 2 ship board.
     *
     * @param playerColor        the player color associated with this ship board
     * @param gameClientNotifier the notifier used to update game clients
     * @param isGui              indicates whether this board is used in a GUI context
     */
    public Level2ShipBoard(PlayerColor playerColor, GameClientNotifier gameClientNotifier, boolean isGui) {
        super(playerColor, gameClientNotifier, isGui);
        this.validPositions = level2ValidPositions;
    }

    /**
     * Determines if the specified coordinates are outside the valid ship board area.
     *
     * @param x the x-coordinate to check
     * @param y the y-coordinate to check
     * @return {@code true} if the coordinates are outside the ship board, {@code false} otherwise
     */
    public static boolean isOutsideShipboard(int x, int y) {
        return !level2ValidPositions[x][y];
    }

    /**
     * Books the currently focused component if possible.
     * If there are already 2 or more inactive components, the booking fails
     * and an error notification is sent to the player.
     * Otherwise, the component is added to the inactive components list,
     * and all clients are notified of the successful booking.
     */
    public void book () {

        if(notActiveComponents.size() >= 2) {
            gameClientNotifier.notifyClients(Set.of(player.getNickname()), (nicknameToNotify, clientController) -> {
                clientController.notifyErrorWhileBookingComponent(nicknameToNotify, player.getNickname(), focusedComponent);
            });
            releaseFocusedComponent();
            return;
        }

        notActiveComponents.add(focusedComponent);

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyBookedComponent(nicknameToNotify, player.getNickname(), focusedComponent);
        });

        releaseFocusedComponent();
    }

    /**
     * Checks if this ship board can defend itself against a dangerous object using single cannons.
     * The defense capability depends on the direction of the incoming threat:
     * - For NORTH direction, checks if there's a cannon at the exact coordinate
     * - For other directions, checks if there's a cannon at the exact coordinate or adjacent positions
     *
     * @param obj the dangerous object that is threatening the ship
     * @return {@code true} if the ship can defend itself with single cannons, {@code false} otherwise
     */
    public boolean canDifendItselfWithSingleCannons(DangerousObj obj){
        if (obj.getDirection() == NORTH) {
            return isThereACannon(obj.getCoordinate(), obj.getDirection());
        } else {
            return isThereACannon(obj.getCoordinate(), obj.getDirection())
                    || isThereACannon(obj.getCoordinate() - 1, obj.getDirection())
                    || isThereACannon(obj.getCoordinate() + 1, obj.getDirection());
        }
    }

    /**
     * Sets focus on a reserved component from the list of booked components.
     * Does nothing if the provided index is out of bounds.
     *
     * @param choice the index of the reserved component to focus on
     */
    public void focusReservedComponent(int choice) {
        if (choice < 0 || choice >= getBookedComponents().size())
            return;

        //Component reservedComponent = getBookedComponents().remove(choice);
        Component reservedComponent = getBookedComponents().get(choice);
        setFocusedComponent(reservedComponent);
    }

    /**
     * Returns a map of coordinates to life support colors for all cabins with life support.
     * This implementation delegates to the parent class.
     *
     * @return a map associating cabin coordinates with their life support colors
     */
    @Override
    public Map<Coordinates, Set<ColorLifeSupport>> getCabinsWithLifeSupport() {
        return super.getCabinsWithLifeSupport();
    }

    /**
     * Checks if the specified coordinates can accept an alien crew member.
     * This implementation delegates to the parent class.
     *
     * @param coords the coordinates to check
     * @param alien  the alien crew member to potentially place
     * @return {@code true} if the alien can be accepted at the given coordinates, {@code false} otherwise
     */
    @Override
    public boolean canAcceptAlien(Coordinates coords, CrewMember alien) {
        return super.canAcceptAlien(coords, alien);
    }
}
