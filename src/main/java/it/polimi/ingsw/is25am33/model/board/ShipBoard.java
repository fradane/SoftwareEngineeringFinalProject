package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.CrewMember;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.polimi.ingsw.is25am33.model.ComponentState.*;
import static it.polimi.ingsw.is25am33.model.ConnectorType.*;
import static it.polimi.ingsw.is25am33.model.Direction.*;

public abstract class ShipBoard {

    // The board is represented as a 12x12 matrix.
    public static final int BOARD_DIMENSION = 12;

    // Defines the starting position for the central module.
    public static final int[] STARTING_CABIN_POSITION = {7, 7};

    /**
     * A matrix that indicates which positions on the board can potentially hold a component.
     * If an index in validPositions is false, you cannot place a component there.
     */
    static boolean[][] validPositions;

    /**
     * The main matrix of Components, each cell representing a position on the player's ship.
     */
    protected Component[][] shipMatrix = new Component[BOARD_DIMENSION][BOARD_DIMENSION];

    /**
     * A list of components that are currently removed from the board. In Level2ShipBoard this list is
     * used to store booked components too.
     */
    protected List<Component> notActiveComponents = new ArrayList<Component>();

    /**
     * The component that the player is currently trying to place (the "focus").
     * Once placed, this reference is cleared.
     */
    protected Component focusedComponent;

    /**
     * A collection of components marked as incorrectly placed, based on checks such as
     * orientation, connectors, or adjacency rules.
     */
    private Set<Component> incorrectlyPositionedComponents = new HashSet<Component>();



    /**
     * Initializes the static matrix representing valid positions on the board where components can be placed.
     * This method should be called exactly once to set up the board’s constraints before any component placement.
     *
     * @param positions a boolean matrix indicating valid positions on the board for placing components
     * @throws IllegalStateException if the validPositions matrix has already been initialized
     */
    public static void initializeValidPositions(boolean[][] positions) throws IllegalStateException {
        if (validPositions == null) {
            validPositions = positions;
        } else {
            throw new IllegalStateException("validPositions has already been initialized.");
        }
    }

    /**
     * Checks if the given coordinates (x, y) are valid board positions:
     *  - Within the board’s boundaries
     *  - Allowed by the validPositions matrix.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return true if (x, y) is a valid spot for placing components; false otherwise
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < BOARD_DIMENSION && y >= 0 && y < BOARD_DIMENSION && validPositions[x][y];
    }

    /**
     * Attempts to place the currently focused component onto the board at (x, y).
     * Performs a series of checks to verify:
     *   - The position is valid
     *   - The component is connected to the rest of the ship
     *   - Connectors match or do not improperly face empty connectors
     *   - Orientation rules for cannons/engines are respected
     *
     * If these rules are violated, it throws an exception or flags the component
     * as incorrectly positioned.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @throws IllegalArgumentException if the position is invalid or the component breaks placement rules
     */
    public void placeComponentWithFocus(int x, int y) throws IllegalArgumentException {
        // Preliminary validations: boundary and connectivity checks.
        if(!isValidPosition(x, y))
            throw new IllegalArgumentException("Not a valid position");
        else if (!isPositionConnectedToShip(x, y))
            throw new IllegalArgumentException("Not connected to the ship");
        else if(!areEmptyConnectorsWellConnected(focusedComponent, x, y))
            throw new IllegalArgumentException("Empty connector not well connected");
        else {
            // If additional rules (like connector compatibility or orientation) fail,
            // the component is marked incorrectly.
            if(
                    !areConnectorsWellConnected(focusedComponent, x, y)
                            || isComponentInFireDirection(focusedComponent, x, y)
                            || isComponentInEngineDirection(focusedComponent, x, y)
                            // If it's an engine, also check that it doesn't fire in a prohibited direction (e.g., not SOUTH).
                            || (!(focusedComponent instanceof Engine) || isEngineDirectionWrong((Engine)focusedComponent))
            )
                incorrectlyPositionedComponents.add(focusedComponent);

            // If all checks are passed (or the game allows it with a penalty),
            // place the component in the matrix and clear the focus.
            shipMatrix[x][y] = focusedComponent;
            focusedComponent = null;
        }
    }

    /**
     * Checks whether an engine is pointed in a "wrong" direction (e.g. not facing SOUTH).
     *
     * @param componentToPlace the component (engine) being placed
     * @return true if the engine's direction is invalid, false otherwise
     */
    public boolean isEngineDirectionWrong(Engine componentToPlace) {
        if(componentToPlace instanceof Engine)
            return componentToPlace.getPowerDirection() == SOUTH;
        return false;
    }

    /**
     * Determines if the given position is connected to at least one existing component.
     * This ensures the ship remains contiguous and no components are placed in isolation.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return true if the new component would be adjacent to an existing one, false otherwise
     */
    public boolean isPositionConnectedToShip(int x, int y) {
        // Offsets representing the four cardinal directions.
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        // Check if any neighbor cell has a component.
        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];

            if(isValidPosition(neighborX, neighborY) && shipMatrix[neighborX][neighborY] != null) return true;
        }

        return false;
    }

    /**
     * Ensures that if a neighboring component has an empty connector, the new component's
     * corresponding side is also empty. This prevents mismatched connectors
     * (e.g., an open connector facing a blank connector).
     *
     * @param componentToPlace the component to validate
     * @param x the x-coordinate of placement
     * @param y the y-coordinate of placement
     * @return false if there's a mismatch in empty connectors, true otherwise
     */
    public boolean areEmptyConnectorsWellConnected(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        // For each neighbor, check if there's a conflict in empty connectors.
        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];
            Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
            Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];

            if(
                    neighborComponent.getConnectors().get(neighborDirectionsToCheck[dir]) == EMPTY
                            && componentToPlace.getConnectors().get(myDirectionsToCheck[dir]) != EMPTY
            )
                return false;
        }

        return true;
    }


    /**
     * Checks if the connectors between the new component and neighboring components are
     * mutually compatible (i.e., no single connector facing a double connector).
     *
     * @param componentToPlace the component to validate
     * @param x the x-coordinate of placement
     * @param y the y-coordinate of placement
     * @return false if there is an incompatibility, true otherwise
     */
    public boolean areConnectorsWellConnected(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

        // Check each neighbor for connector compatibility using the static method in ConnectorType.
        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];

            if(
                    !areConnectorsCompatible(
                            componentToPlace.getConnectors().get(myDirectionsToCheck[dir]),
                            neighborComponent.getConnectors().get(neighborDirectionsToCheck[dir]))
            )
                return false;
        }

        return true;
    }

    /**
     * Checks if the component to be placed is targeted by a cannon in an adjacent tile.
     * For instance, if there's a cannon facing that direction, it might mean an illegal overlap
     * or friendly-fire scenario.
     *
     * @param componentToPlace the component being placed
     * @param x the x-coordinate of placement
     * @param y the y-coordinate of placement
     * @return true if there is a cannon pointing at this spot, false otherwise
     */
    public boolean isComponentInFireDirection(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

        // Scan around (x, y) to see if any adjacent component is a cannon that points here.
        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null ) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];

            if(
                    neighborComponent instanceof Cannon
                            && ((Cannon) neighborComponent).getFireDirection() == neighborDirectionsToCheck[dir]
            )
                return true;
        }

        return false;
    }

    /**
     * Similar to isComponentInFireDirection, but checks for engines that might be facing
     * into this component’s cell. If the engine is not point SOUTH it will be soon removed so
     * it doesn't represent a problem for the aimed component.
     *
     * @param componentToPlace the component being placed
     * @param x the x-coordinate of placement
     * @param y the y-coordinate of placement
     * @return true if there is an engine pointing toward this spot, false otherwise
     */
    public boolean isComponentInEngineDirection(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

        // Scan around (x, y) to see if any adjacent component is an engine that points here.
        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null ) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];

            if(
                    neighborComponent instanceof Engine
                            && ((Engine) neighborComponent).getPowerDirection() == SOUTH
                            && ((Engine) neighborComponent).getPowerDirection() == neighborDirectionsToCheck[dir]
            )
                return true;
        }

        return false;
    }

    /**
     * Releases the currently focused component, resetting its state to FREE.
     * This typically happens when a player decides not to place a component.
     */
    public void releaseComponentWithFocus() {
        focusedComponent.setCurrState(FREE);
        focusedComponent = null;
    };

    /**
     * ???
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @throws IllegalArgumentException if (x, y) is outside the board
     */
    public List<Set<int[]>> removeAndRecalculateShipParts(int x, int y) throws IllegalArgumentException{
        if(shipMatrix[x][y] == null)
            throw new IllegalArgumentException("No component in this position");

        notActiveComponents.add(shipMatrix[x][y]);
        shipMatrix[x][y] = null;


        List<Set<int[]>> shipParts = identifyShipParts(x, y);
        return shipParts;
    }

    /**
     * Checks if there is any cannon in the given row or column (depending on direction)
     * that faces in that direction.
     *
     * @param pos the index of the row or column
     * @param direction the direction (NORTH, SOUTH, EAST, WEST) to check
     * @return true if a cannon facing that direction exists, false otherwise
     */
    public boolean isThereACannon(int pos, Direction direction) {
        if(pos < 0 || pos >= BOARD_DIMENSION )
            throw new IllegalArgumentException("Not a valid position");

        return Arrays.stream(
                (direction == NORTH || direction == SOUTH) ?
                        IntStream.range(0, BOARD_DIMENSION)
                            .mapToObj(i -> shipMatrix[i][pos])
                            .toArray(Component[]::new)
                        : shipMatrix[pos] )
                .anyMatch(component -> component instanceof Cannon && ((Cannon) component).getFireDirection() == direction );
    }

    public boolean isThereADoubleCannon(int pos, Direction direction) {
        if(pos < 0 || pos >= BOARD_DIMENSION )
            throw new IllegalArgumentException("Not a valid position");

        return Arrays.stream(
                        (direction == NORTH || direction == SOUTH) ?
                                IntStream.range(0, BOARD_DIMENSION)
                                        .mapToObj(i -> shipMatrix[i][pos])
                                        .toArray(Component[]::new)
                                : shipMatrix[pos] )
                .anyMatch(component -> component instanceof DoubleCannon && ((Cannon) component).getFireDirection() == direction );
    }

    public boolean isItGoingToHitTheShip(DangerousObj obj){
        Component[] componentsInObjectDirection = getOrderedComponentsInDirection(obj.getCoordinate(), obj.getDirection());
        return componentsInObjectDirection.length != 0;
    }


    /**
     * Returns the components in a row/column in the order that a projectile or effect would encounter them.
     * For NORTH, we return top-to-bottom; for SOUTH, bottom-to-top; etc.
     *
     * @param pos the index of the row or column
     * @param direction the direction (NORTH, SOUTH, EAST, WEST)
     * @return an array of Components in the path
     * @throws IllegalArgumentException if pos is out of bounds or direction is invalid
     */
    public Component[] getOrderedComponentsInDirection(int pos, Direction direction) throws IllegalArgumentException{
        if(pos < 0 || pos >= BOARD_DIMENSION )
            throw new IllegalArgumentException("Not a valid position");

        return switch (direction)  {
            case NORTH -> IntStream.range(0, BOARD_DIMENSION)
                            .mapToObj(i -> shipMatrix[i][pos])
                            .toArray(Component[]::new);
            case SOUTH -> IntStream.range(0, BOARD_DIMENSION)
                            .map(i -> BOARD_DIMENSION - 1 - i)
                            .mapToObj(i -> shipMatrix[i][pos])
                            .toArray(Component[]::new);
            case EAST -> IntStream.range(0, BOARD_DIMENSION)
                            .map(i -> BOARD_DIMENSION - 1 - i)
                            .mapToObj(i -> shipMatrix[pos][i])
                            .toArray(Component[]::new);
            case WEST -> IntStream.range(0, BOARD_DIMENSION)
                            .mapToObj(i -> shipMatrix[pos][i])
                            .toArray(Component[]::new);
            default -> throw new IllegalArgumentException("Not a valid direction");

        };
    }

    /**
     * Determines if the first non-null component in the given row or column is "exposed",
     * meaning it has a non-empty connector facing outward.
     *
     * @param pos the row/column index
     * @param direction the direction to check
     * @return true if the first component in that direction has a non-empty connector, false otherwise
     * @throws IllegalArgumentException if pos is out of bounds
     */
    public boolean isExposed(int pos, Direction direction) throws IllegalArgumentException {
        if(pos < 0 || pos >= BOARD_DIMENSION )
            throw new IllegalArgumentException("Not a valid position");

        return Arrays.stream( getOrderedComponentsInDirection(pos, direction) )
                .findFirst()
                .map(component -> (Boolean) (component.getConnectors().get(direction) != EMPTY))
                .orElse(Boolean.valueOf(false));
    }

    /**
     * Counts how many exposed connectors are on the board.
     * An exposed connector is one that, if the adjacent cell is empty,
     * still has a non-empty connector facing outwards.
     *
     * @return the total number of exposed connectors across the whole board
     */
    public int countExposed() {
        int counter = 0;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};

        // For every empty cell, check if a neighbor's connector points into it and is not empty.
        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {

                if(isValidPosition(i, j) && shipMatrix[i][j] == null){

                    for (int dir = 0; dir < 4; dir++) {
                        int neighborI = i + dx[dir];
                        int neighborJ = j + dy[dir];

                        if(isValidPosition(neighborI, neighborJ)
                                && shipMatrix[neighborI][neighborJ].getConnectors().get(neighborDirectionsToCheck[dir]) != EMPTY)
                            counter++;

                    }

                }

            }
        }

        return counter;
    }

    /**
     * Retrieves all CrewMembers from all the cabins on the board.
     * In Galaxy Trucker, each cabin may contain one or more inhabitants.
     *
     * @return a list of all crew members present on this ship
     */
    public List<CrewMember> getCrewMembers() {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Cabin)
                .map(component -> (Cabin)component)
                .map(cabin -> cabin.getInhabitants())
                .filter(Objects::nonNull)
                .flatMap(inhabitants -> inhabitants.stream())
                .collect(Collectors.toList());
    }

    /**
     * Finds all cabins that have at least one neighboring cabin (orthogonally adjacent)
     * which also contains crew members.
     *
     * @return a set of occupied cabins that are directly adjacent to at least one other occupied cabin
     */
    public Set<Cabin> cabinWithNeighbors() {
        Set<Cabin> cabinsWithNeighbors = new HashSet<Cabin>();

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};


        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                if (!isValidPosition(i, j)) continue;
                Component currentCabin = shipMatrix[i][j];

                if(currentCabin instanceof Cabin && ((Cabin) currentCabin).hasInhabitants()){
                    for (int dir = 0; dir < 4; dir++) {
                        int newI = i + dx[dir];
                        int newJ = j + dy[dir];

                        if(!isValidPosition(newI, newJ)) continue;

                        Component neighbor = shipMatrix[newI][newJ];
                        if (neighbor instanceof Cabin && ((Cabin) neighbor).hasInhabitants()){
                            cabinsWithNeighbors.add((Cabin)currentCabin);
                            break;
                        }
                    }
                }
            }
        }

        return cabinsWithNeighbors;
    }

    /**
     * Checks if there are no incorrectly placed components on the board.
     * If the set of incorrectly positioned components is empty, we consider the ship to be "correct."
     * Components are added to this set during placement (`placeComponentWithFocus`)
     * if they violate any rules.
     *
     * @return true if no components violate placement rules, false otherwise
     */
    public boolean isShipCorrect() {
        if (incorrectlyPositionedComponents.isEmpty()) return true;
        return false;
    }

    public Stream<DoubleCannon> getDoubleCannons () {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof DoubleCannon)
                .map(component -> (DoubleCannon)component);
    }

    public Stream<Cannon> getAllCannons () {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Cannon)
                .map(component -> (Cannon)component);
    }

    /**
     * ???
     *
     * @return the sum of firepower from active cannons
     */
    //cannonsActivated include sia i singoli che i doppi attivati
    public int countTotalFirePower(Stream<Cannon> cannonsToCountFirePower) {
        // Convert all cannons on the board into a stream
        Stream<Cannon> cannonStream = Arrays.stream(shipMatrix)
                                        .flatMap(row -> Arrays.stream(row))
                                        .filter(Objects::nonNull)
                                        .filter(component -> component instanceof Cannon)
                                        .map(component -> (Cannon)component);

        // The controller asks the user how many double cannons wants to activate
        Stream<DoubleCannon> doubleCannonsActivated = cannonsToCountFirePower.filter(cannon -> cannon instanceof DoubleCannon).map(cannon -> (DoubleCannon)cannon);
        Stream <Cannon> singleCannons = cannonsToCountFirePower.filter(cannon -> !(cannon instanceof DoubleCannon));

        // Cannon aimed NORTH = 1 point, otherwise 1/2.
        // Double cannon aimed NORTH = 2 points, otherwise 1 point.
        int totalFirePower = singleCannons.mapToInt(cannon -> cannon.getFireDirection() == NORTH ? 1 : 1/2).sum()
                + doubleCannonsActivated.mapToInt(cannon -> cannon.getFireDirection() == NORTH ? 2 : 1).sum();

        return totalFirePower;
    }

    public Stream<Engine> getAllEngines () {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Engine)
                .map(component -> (Engine)component);
    }

    /**
     * ???
     *
     * @return the total engine power of the ship
     */
    public int countTotalEnginePower(Stream<Engine> enginesToCountEnginePower) {
        Stream<Engine> engineStream = Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Engine)
                .map(component -> (Engine)component);

        Stream<DoubleEngine> doubleEnginesActivated = enginesToCountEnginePower.filter(engine -> engine instanceof DoubleEngine).map(engine -> (DoubleEngine)engine);
        Stream <Engine> singleEngines = enginesToCountEnginePower.filter(engine -> !(engine instanceof DoubleEngine));


        int totalEnginePower = (int) (singleEngines.count() + 2 * doubleEnginesActivated.count());

        return totalEnginePower;
    }

    public int countSingleEnginePower(Stream<Engine> enginesToCountEnginePower) {
        Stream<Engine> singleEngineStream = Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Engine)
                .filter(engine -> !(engine instanceof DoubleEngine))
                .map(component -> (Engine)component);


        return (int) singleEngineStream.count();
    }

    /**
     * Retrieves all the storage components on the board.
     * Used to calculate resource capacity or to decide where to store cargoCubes.
     *
     * @return a list of all Storage components
     */
    public List<Storage> getStorages() {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Storage)
                .map(component -> (Storage)component)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all the battery boxes (energy storage) on the board.
     * Used in powering double cannons/engines.
     *
     * @return a list of BatteryBox components
     */
    public List<BatteryBox> getBatteryBoxes() {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof BatteryBox)
                .map(component -> (BatteryBox)component)
                .collect(Collectors.toList());
    }

    public List<Set<int[]>> identifyShipParts(int x, int y) {
        boolean[][] visited = new boolean[BOARD_DIMENSION][BOARD_DIMENSION];
        List<Set<int[]>> shipParts = new ArrayList<>();

        visited[x][y] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int dir = 0; dir < 4; dir++) {
            int newX = x + dx[dir];
            int newY = y + dy[dir];

            if(!isValidPosition(newX, newY)) continue;

            Set<int[]> currentPart = bfsCollectPart(x, y, visited);
            shipParts.add(currentPart);
        }
        return shipParts;
    }

    public Set<int[]> bfsCollectPart(int startX, int startY, boolean[][] visited) {
        Queue<int[]> queue = new LinkedList<>();
        Set<int[]> part = new HashSet<>();

        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            part.add(pos);

            for (int dir=0; dir<4; dir++) {
                int newX = pos[0] + dx[dir];
                int newY = pos[1] + dy[dir];

                if (isValidPosition(newX, newY) && shipMatrix[newX][newY] != null && !visited[newX][newY]) {
                    visited[newX][newY] = true;
                    queue.add(new int[]{newX, newY});
                }
            }
        }
        return part;
    }

    public void removeShipPart (Set<int[]> componentsPositions) {
        for(int[] componentPosition : componentsPositions) {
            Component currentComponent = shipMatrix[componentPosition[0]][componentPosition[1]];
            notActiveComponents.add(currentComponent);
            shipMatrix[componentPosition[0]][componentPosition[1]] = null;
        }
    }

    public int[] findFirstComponentInDirection(int pos, Direction direction) {
        int x, y;

        if( pos < 0 || pos >= BOARD_DIMENSION)
            throw new IllegalArgumentException("Invalid position: " + pos);

        switch (direction) {
            case NORTH:
                y = pos;
                x = 0;
                break;
            case SOUTH:
                y = pos;
                x = 12;
                break;
            case EAST:
                x = pos;
                y = 12;
                break;
            case WEST:
                x = pos;
                y = 0;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + direction);
        }

        while (shipMatrix[x][y] == null) {

            switch (direction) {
                case NORTH:
                    x ++;
                    break;
                case SOUTH:
                    x --;
                    break;
                case EAST:
                    y --;
                    break;
                case WEST:
                    y ++;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + direction);
            }

        }

        return new int[]{x, y};
    }

    public boolean isDirectionCoveredByShield(Direction direction) {
        return Arrays.stream(shipMatrix)
                .flatMap(row -> Arrays.stream(row))
                .filter(Objects::nonNull)
                .filter(component -> component instanceof Shield)
                .map(component -> ((Shield)component))
                .flatMap(shield -> shield.getDirections().stream())
                .anyMatch(dir -> dir == direction);
    }

    public abstract void handleDangerousObject(DangerousObj obj);
    public abstract boolean canDifendItselfWithSingleCannons(DangerousObj obj);
}