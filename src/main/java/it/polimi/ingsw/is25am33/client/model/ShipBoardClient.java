package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface defining all client-side operations for managing a ship board
 */
public interface ShipBoardClient {

    /**
     * Sets the player associated with this ship board
     *
     * @param player The player to associate with the ship board
     */
    void setPlayer(Player player);

    void setIncorrectlyPositionedComponentsCoordinates(Set<Coordinates> incorrectlyPositionedComponentsCoordinates);

    /**
     * Gets the ship matrix representing the board state
     *
     * @return The 2D component matrix
     */
    Component[][] getShipMatrix();

    /**
     * Sets the ship matrix representing the board state
     *
     * @param shipMatrix The 2D component matrix
     */
    void setShipMatrix(Component[][] shipMatrix);

    /**
     * Gets the currently focused component
     *
     * @return The focused component
     */
    Component getFocusedComponent();

    /**
     * Gets the currently booked components
     *
     * @return The booked components
     */
    List<Component> getBookedComponents();

    Set<Coordinates> getIncorrectlyPositionedComponentsCoordinates();

    /**
     * Sets the game context
     *
     * @param gameClientNotifier The game context to set
     */
    void setGameClientNotifier(GameClientNotifier gameClientNotifier);

    /**
     * Sets the focused component
     *
     * @param focusedComponent The component to focus
     */
    void setFocusedComponent(Component focusedComponent);

    /**
     * Checks whether the specified coordinates are valid and allowed on the board
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return true if the position is valid, otherwise false
     */
    boolean isValidPosition(int x, int y);

    /**
     * Attempts to place the focused component at the specified coordinates
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @throws IllegalArgumentException If the position is invalid or violates placement rules
     */
    void placeComponentWithFocus(int x, int y) throws IllegalArgumentException;

    /**
     * Checks whether the direction of an engine is incorrect
     *
     * @param componentToPlace The engine to check
     * @return true if the direction is invalid, otherwise false
     */
    boolean isEngineDirectionWrong(Component componentToPlace);

    /**
     * Verifies that the component is properly connected to the ship through at least one non-EMPTY connector.
     * A component connected only through EMPTY connectors is not considered properly connected.
     *
     * @param componentToPlace The component to validate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if the component is properly connected through at least one non-EMPTY connector, otherwise false.
     */
    boolean isPositionConnectedToShip(Component componentToPlace, int x, int y);

    /**
     * Ensures that if an adjacent component has an EMPTY connector,
     * the newly placed component also has the corresponding EMPTY connector
     *
     * @param componentToPlace The component to validate
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return true if there are no conflicts, otherwise false
     */
    boolean areEmptyConnectorsWellConnected(Component componentToPlace, int x, int y);

    /**
     * Checks connector compatibility between the new component and adjacent components
     *
     * @param componentToPlace The component to validate
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return true if the connectors are compatible, otherwise false
     */
    boolean areConnectorsWellConnected(Component componentToPlace, int x, int y);

    boolean isAimingAComponent(Component componentToPlace, int x, int y);

    /**
     * Checks whether a cannon in an adjacent cell is pointed at the cell
     * where the new component is being placed
     *
     * @param componentToPlace The component being placed
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return true if there is a cannon aimed at this cell, otherwise false
     */
    boolean isComponentInFireDirection(Component componentToPlace, int x, int y);

    /**
     * Checks whether an engine in an adjacent cell is pointed at the cell
     * where the new component is being placed
     *
     * @param componentToPlace The component being placed
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return true if there is an engine aimed at this cell, otherwise false
     */
    boolean isComponentInEngineDirection(Component componentToPlace, int x, int y);

    /**
     * Removes the component from the specified coordinates and recalculates any disconnected ship parts
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return A list of sets, where each set contains the coordinates of components in a disconnected part
     * @throws IllegalArgumentException If there is no component at the specified position
     */
    Set<Set<Coordinates>> removeAndRecalculateShipParts(int x, int y) throws IllegalArgumentException;

    /**
     * Determines whether there is a cannon in the specified row or column that is pointed
     * in the given direction
     *
     * @param pos The row or column index
     * @param direction The direction to check
     * @return true if there is a cannon firing in that direction, otherwise false
     * @throws IllegalArgumentException If the position is invalid
     */
    boolean isThereACannon(int pos, Direction direction);

    /**
     * Determines whether there is a double cannon in the specified row or column that is pointed
     * in the given direction
     *
     * @param pos The row or column index
     * @param direction The direction to check
     * @return true if there is a double cannon firing in that direction, otherwise false
     * @throws IllegalArgumentException If the position is invalid
     */
    boolean isThereADoubleCannon(int pos, Direction direction);

    /**
     * Checks whether a DangerousObj will actually hit the ship based on its trajectory
     *
     * @param obj The DangerousObj to evaluate
     * @return true if the object will impact the ship, otherwise false
     */
    boolean isItGoingToHitTheShip(DangerousObj obj);

    /**
     * Returns the components in a row or column in the order they would be hit by an object
     *
     * @param pos The row or column index
     * @param direction The direction of travel
     * @return An array of Components in the order of impact
     * @throws IllegalArgumentException If the position or direction is invalid
     */
    Component[] getOrderedComponentsInDirection(int pos, Direction direction) throws IllegalArgumentException;

    /**
     * Checks whether the first non-null component found in a row or column
     * has a non-empty connector facing outward
     *
     * @param pos The row or column index
     * @param direction The direction to check
     * @return true if the first component found has a non-empty exposed connector, otherwise false
     * @throws IllegalArgumentException If the position is invalid
     */
    boolean isExposed(int pos, Direction direction) throws IllegalArgumentException;

    /**
     * Calculates the total number of exposed connectors on the ship
     *
     * @return The total number of exposed connectors
     */
    int countExposed();

    /**
     * Retrieves all the crew members present in the ship's cabins
     *
     * @return A list of CrewMember objects currently on the ship
     */
    List<CrewMember> getCrewMembers();

    /**
     * Returns the list of Cabin components present on the ship
     *
     * @return A list of Cabin objects
     */
    List<Cabin> getCabin();

    /**
     * Finds all cabins occupied by at least one crew member
     * that are orthogonally adjacent to other occupied cabins
     *
     * @return A set of cabins that meet the adjacency criterion
     */
    Set<Coordinates> getCabinCoordinatesWithNeighbors();

    /**
     * Checks whether there are no incorrectly placed components on the ship
     *
     * @return true if the ship has no placement errors, otherwise false
     */
    boolean isShipCorrect();

    /**
     * Returns the list of DoubleCannon components present on the ship
     *
     * @return A list of DoubleCannon objects
     */
    List<DoubleCannon> getDoubleCannons();

    /**
     * Returns the list of single Cannon components present on the ship
     *
     * @return A list of single Cannon objects
     */
    List<Cannon> getSingleCannons();

    /**
     * Returns the list of all cannons (single and double) present on the ship
     *
     * @return A list of all Cannon objects
     */
    List<Cannon> getAllCannons();

    /**
     * Calculates the total firepower of a list of cannons
     *
     * @param cannonsToCountFirePower A list of cannons to consider
     * @return The total firepower
     */
    double countTotalFirePower(List<Cannon> cannonsToCountFirePower);

    /**
     * Returns the list of DoubleEngine components present on the ship
     *
     * @return A list of DoubleEngine objects
     */
    List<DoubleEngine> getDoubleEngines();

    /**
     * Returns the list of single Engine components present on the ship
     *
     * @return A list of single Engine objects
     */
    List<Engine> getSingleEngines();

    /**
     * Returns the list of all engines (single and double) present on the ship
     *
     * @return A list of all Engine objects
     */
    List<Engine> getAllEngines();

    /**
     * Calculates the total power of the provided engines
     *
     * @param enginesToCountEnginePower The list of engines to consider
     * @return The total engine power
     */
    int countTotalEnginePower(List<Engine> enginesToCountEnginePower);

    /**
     * Calculates the number of single engines in operation among the provided engines
     *
     * @param enginesToCountEnginePower The list of engines to consider
     * @return The number of single engines
     */
    int countSingleEnginePower(List<Engine> enginesToCountEnginePower);

    /**
     * Returns the list of StandardStorage components present on the ship
     *
     * @return A list of StandardStorage objects
     */
    List<StandardStorage> getStandardStorages();

    /**
     * Returns the list of SpecialStorage components present on the ship
     *
     * @return A list of SpecialStorage objects
     */
    List<SpecialStorage> getSpecialStorages();

    /**
     * Returns a list of all Storage components (both Standard and Special)
     *
     * @return A list of Storage objects
     */
    List<Storage> getStorages();

    /**
     * Returns the list of BatteryBox components present on the ship
     *
     * @return A list of BatteryBox objects
     */
    List<BatteryBox> getBatteryBoxes();

    /**
     * Returns the list of Shield components present on the ship
     *
     * @return A list of Shield objects
     */
    List<Shield> getShields();

    /**
     * Identifies contiguous parts of the ship stemming from the specified position
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return A list of sets, where each set contains the coordinates of components in a connected part
     */
    Set<Set<Coordinates>> identifyShipParts(int x, int y);

    /**
     * Performs a breadth-first search (BFS) to gather all connected cells
     * starting from the provided coordinates
     *
     * @param startX The starting x-coordinate
     * @param startY The starting y-coordinate
     * @param visited A matrix of visited nodes
     * @return A set of coordinates forming a connected part of the ship
     */
    Set<Coordinates> bfsCollectPart(int startX, int startY, boolean[][] visited);

    /**
     * Removes the specified set of components from the board and marks them as inactive
     *
     * @param componentsPositions A set of coordinates of components to remove
     */
    void removeShipPart(Set<Coordinates> componentsPositions);

    /**
     * Finds the coordinates of the first non-null component in a given direction,
     * starting from a specified row or column index
     *
     * @param pos The row or column index
     * @param direction The direction to search in
     * @return An array [x, y] with the coordinates of the first component found
     * @throws IllegalArgumentException If the position is invalid
     */
    int[] findFirstComponentInDirection(int pos, Direction direction);

    /**
     * Checks whether there is at least one shield covering a specific direction
     *
     * @param direction The direction to check
     * @return true if the direction is covered by a shield, otherwise false
     */
    boolean isDirectionCoveredByShield(Direction direction);

    /**
     * Handles the effect of a dangerous object (DangerousObj) on the ship
     *
     * @param obj The dangerous object to handle
     */
    int[] handleDangerousObject(DangerousObj obj);

    /**
     * Checks whether the ship can defend itself from a dangerous object using single cannons
     *
     * @param obj The dangerous object
     * @return true if the ship can defend itself, otherwise false
     */
    boolean canDifendItselfWithSingleCannons(DangerousObj obj);

    /**
     * Gets the list of components that are not currently active on the board
     *
     * @return A list of inactive components
     */
    List<Component> getNotActiveComponents();

    /**
     * Gets the component at the specified coordinates
     *
     * @param coordinates The coordinates to check
     * @return The component at the given coordinates
     */
    Component getComponentAt(Coordinates coordinates);

    /**
     * Releases the currently focused component
     *
     * @return The component that was released
     */
    Component releaseFocusedComponent();

    int getTotalAvailableBattery();

    void checkPosition(int x, int y);

    void setComponentsPerType(Map<Class<?>, List<Component>> componentsPerType);

    Map<Coordinates, Storage> getCoordinatesAndStorages();

    /**
     * Returns a map where keys are coordinates and values are Cabin components that have crew members.
     * Only includes coordinates that have Cabin components with at least one inhabitant.
     *
     * @return A map of coordinates to Cabin objects with crew
     */
    Map<Coordinates, Cabin> getCoordinatesAndCabinsWithCrew();

    /**
     * Restituisce una mappa di cabine connesse a moduli di supporto vitale.
     */
    Map<Coordinates, Set<ColorLifeSupport>> getCabinsWithLifeSupport();

    /**
     * Verifica se un alieno può essere posizionato in una cabina.
     */
    boolean canAcceptAlien(Coordinates coords, CrewMember alien);

    Set<Coordinates> getCoordinatesOfComponents(List<? extends Component> components);

    Map<Class<?>, List<Component>> getComponentsPerType();

    List<CargoCube> getCargoCubes();

    void setNotActiveComponents(List<Component> notActiveComponents);

}