package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.model.game.DTO;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;

public abstract class ShipBoard implements Serializable {

    protected GameContext gameContext;

    /**
     * The size of the board where components can be placed. Just one length as the board is squared.
     */
    public static final int BOARD_DIMENSION = 12;

    /**
     * The coordinate where the main cabin is initially placed.
     */
    public static final int[] STARTING_CABIN_POSITION = {6, 6};

    /**
     * A matrix indicating which board positions are valid for placing components.
     * If a cell is false, no component can be placed there.
     * This matrix is what differentiates a Level1ShipBoard from a Level2ShipBoard
     */
    protected boolean[][] validPositions;

    /**
     * The main matrix of components. Each cell represents a position on the ship.
     */
    protected final Component[][] shipMatrix = new Component[BOARD_DIMENSION][BOARD_DIMENSION];

    /**
     * A map that groups components based on their class (type).
     */
    protected Map<Class<?>, List<Object>> componentsPerType = new HashMap<>();

    /**
     * A list of components that are not currently active on the board (either removed or awaiting placement).
     */
    protected List<Component> notActiveComponents = new ArrayList<>();

    /**
     * The component the player is currently trying to place (focus).
     * Once placed, this reference is set to null.
     */
    protected Component focusedComponent;

    protected Player player;

    /**
     * A set of components marked as incorrectly placed.
     */
    private Set<Component> incorrectlyPositionedComponents = new HashSet<>();

    /**
     * Constructor that creates a ShipBoard with the main cabin placed at the initial coordinates.
     *
     * @param color The color associated with the player.
     */
    public ShipBoard(PlayerColor color) {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.SOUTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.WEST,  ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,  ConnectorType.UNIVERSAL);

        shipMatrix[STARTING_CABIN_POSITION[0]][STARTING_CABIN_POSITION[1]] = new MainCabin(connectors, color);
    }

    public Component[][] getShipMatrix() {
        synchronized (shipMatrix) {
            return shipMatrix;
        }
    }

    public Component getFocusedComponent() {
        return focusedComponent;
    }

    public void setGameContext(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    public void setFocusedComponent(Component focusedComponent) {
        this.focusedComponent = focusedComponent;
    }

//    /**
//     * Initializes the matrix of valid positions for placing components.
//     *
//     * @param positions Boolean matrix indicating valid or invalid positions.
//     * @throws IllegalStateException If the validPositions matrix has already been initialized.
//     */
//    public static void initializeValidPositions(boolean[][] positions) throws IllegalStateException {
//        if (validPositions == null) {
//            validPositions = positions;
//        } else {
//            throw new IllegalStateException("validPositions has already been initialized.");
//        }
//    }

    /**
     * Checks whether the specified coordinates are valid and allowed on the board.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if the position is within the board and validPositions[x][y] is true, otherwise false.
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < BOARD_DIMENSION && y >= 0 && y < BOARD_DIMENSION && validPositions[x][y];
    }

    /**
     * Attempts to place the focused component at the specified coordinates,
     * performing various validity and connectivity checks.
     * If the placed component does not violate an essential rule it is simply added to list of incorrectyle Positioned Components.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @throws IllegalArgumentException If the position is invalid or violates placement rules.
     */
    public void placeComponentWithFocus(int x, int y) throws IllegalArgumentException {
        synchronized (shipMatrix) {
            if(!isValidPosition(x, y))
                throw new IllegalArgumentException("Not a valid position");
            else if (!isPositionConnectedToShip(x, y))
                throw new IllegalArgumentException("Not connected to the ship");
            else if(!areEmptyConnectorsWellConnected(focusedComponent, x, y))
                throw new IllegalArgumentException("Empty connector not well connected");
            else {
                if(
                        !areConnectorsWellConnected(focusedComponent, x, y)
                                || isComponentInFireDirection(focusedComponent, x, y)
                                || isComponentInEngineDirection(focusedComponent, x, y)
                                || (!(focusedComponent instanceof Engine) || isEngineDirectionWrong((Engine)focusedComponent))
                )
                    incorrectlyPositionedComponents.add(focusedComponent);

                shipMatrix[x][y] = focusedComponent;

                focusedComponent.insertInComponentsMap(componentsPerType);

                focusedComponent = null;
            }
        }

        DTO dto = new DTO();
        dto.setPlayer(player);
        dto.setCoordinates(new Coordinates(x,y));

        BiConsumer<Observer,String> notifyPlacingComponent= Observer::notifyPlacedComponent;
        //gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "placeFocusedComponent", dto ), notifyPlacingComponent);

    }

    /**
     * Checks whether the direction of an engine is incorrect (e.g., if it's not SOUTH).
     *
     * @param componentToPlace The engine to check.
     * @return true if the direction is invalid, otherwise false.
     */
    public boolean isEngineDirectionWrong(Engine componentToPlace) {
        return componentToPlace.getPowerDirection() == SOUTH;
    }

    /**
     * Verifies whether placing a component at the specified coordinates would be adjacent
     * to at least one existing component, ensuring continuity of the ship.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if the placement is adjacent to an existing component, otherwise false.
     */
    public boolean isPositionConnectedToShip(int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int dir = 0; dir < 4; dir++) {
            int neighborX = x + dx[dir];
            int neighborY = y + dy[dir];

            if(isValidPosition(neighborX, neighborY) && shipMatrix[neighborX][neighborY] != null) return true;
        }

        return false;
    }

    /**
     * Ensures that if an adjacent component has an EMPTY connector,
     * the newly placed component also has the corresponding EMPTY connector, avoiding mismatches.
     *
     * @param componentToPlace The component to validate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if there are no conflicts, otherwise false.
     */
    public boolean areEmptyConnectorsWellConnected(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

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
     * Checks connector compatibility between the new component and adjacent components,
     * to avoid single vs double connector mismatches.
     *
     * @param componentToPlace The component to validate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if the connectors are compatible, otherwise false.
     */
    public boolean areConnectorsWellConnected(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

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
     * Checks whether a cannon in an adjacent cell is pointed at the cell
     * where the new component is being placed.
     *
     * @param componentToPlace The component being placed.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if there is a cannon aimed at this cell, otherwise false.
     */
    public boolean isComponentInFireDirection(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

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
     * Checks whether an engine in an adjacent cell is pointed at the cell
     * where the new component is being placed.
     *
     * @param componentToPlace The component being placed.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if there is an engine aimed at this cell, otherwise false.
     */
    public boolean isComponentInEngineDirection(Component componentToPlace, int x, int y) {
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};
        Direction[] myDirectionsToCheck = {NORTH, SOUTH, WEST, EAST};

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
     * Removes the component from the specified coordinates and recalculates any disconnected ship parts
     * that may result from the removal.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return A list of sets, where each set contains the coordinates of components in a disconnected part.
     * @throws IllegalArgumentException If there is no component at the specified position.
     */
    public List<Set<List<Integer>>> removeAndRecalculateShipParts(int x, int y) throws IllegalArgumentException {
        if(shipMatrix[x][y] == null)
            throw new IllegalArgumentException("No component in this position");

        notActiveComponents.add(shipMatrix[x][y]);
        incorrectlyPositionedComponents.remove(shipMatrix[x][y]);
        shipMatrix[x][y] = null;

        return identifyShipParts(x, y);
    }

    /**
     * Determines whether there is a cannon in the specified row or column that is pointed
     * in the given direction.
     *
     * @param pos The row or column index.
     * @param direction The direction to check.
     * @return true if there is a cannon firing in that direction, otherwise false.
     * @throws IllegalArgumentException If the position is invalid.
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

    /**
     * Determines whether there is a double cannon in the specified row or column that is pointed
     * in the given direction.
     *
     * @param pos The row or column index.
     * @param direction The direction to check.
     * @return true if there is a double cannon firing in that direction, otherwise false.
     * @throws IllegalArgumentException If the position is invalid.
     */
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

    /**
     * Checks whether a DangerousObj will actually hit the ship based on its trajectory.
     *
     * @param obj The DangerousObj to evaluate.
     * @return true if the object will impact the ship, otherwise false.
     */
    public boolean isItGoingToHitTheShip(DangerousObj obj){
        Component[] componentsInObjectDirection = getOrderedComponentsInDirection(obj.getCoordinate(), obj.getDirection());
        return componentsInObjectDirection.length != 0;
    }

    /**
     * Returns the components in a row or column in the order they would be hit by an object
     * (e.g., a projectile).
     *
     * @param pos The row or column index.
     * @param direction The direction of travel.
     * @return An array of Components in the order of impact.
     * @throws IllegalArgumentException If the position or direction is invalid.
     */
    public Component[] getOrderedComponentsInDirection(int pos, Direction direction) throws IllegalArgumentException {
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
     * Checks whether the first non-null component found in a row or column
     * has a non-empty connector facing outward.
     *
     * @param pos The row or column index.
     * @param direction The direction to check.
     * @return true if the first component found has a non-empty exposed connector, otherwise false.
     * @throws IllegalArgumentException If the position is invalid.
     */
    public boolean isExposed(int pos, Direction direction) throws IllegalArgumentException {
        if(pos < 0 || pos >= BOARD_DIMENSION )
            throw new IllegalArgumentException("Not a valid position");

        Component[] componentsInDirection = getOrderedComponentsInDirection(pos, direction);

        if(Arrays.stream(componentsInDirection).allMatch(Objects::isNull))
            return false;

        return Arrays.stream(componentsInDirection)
                .filter(Objects::nonNull)
                .findFirst()
                .map(component -> (Boolean) (component.getConnectors().get(direction) != EMPTY))
                .orElse(false);
    }

    /**
     * Calculates the total number of exposed connectors on the ship.
     * A connector is exposed if it points to an empty cell and is not EMPTY.
     *
     * @return The total number of exposed connectors.
     */
    public int countExposed() {
        int counter = 0;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        Direction[] neighborDirectionsToCheck = {SOUTH, NORTH, EAST, WEST};

        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {

                if(isValidPosition(i, j) && shipMatrix[i][j] == null){
                    for (int dir = 0; dir < 4; dir++) {
                        int neighborI = i + dx[dir];
                        int neighborJ = j + dy[dir];

                        if(isValidPosition(neighborI, neighborJ)
                                && shipMatrix[i][j] != null
                                && shipMatrix[neighborI][neighborJ].getConnectors().get(neighborDirectionsToCheck[dir]) != EMPTY)
                            counter++;
                    }
                }
            }
        }

        return counter;
    }

    /**
     * Retrieves all the crew members present in the ship's cabins.
     *
     * @return A list of CrewMember objects currently on the ship.
     */
    @JsonIgnore
    public List<CrewMember> getCrewMembers() {
        return componentsPerType.get(Cabin.class)
                .stream()
                .map(Cabin.class::cast)
                .map(Cabin::getInhabitants)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Finds all cabins occupied by at least one crew member
     * that are orthogonally adjacent to other occupied cabins.
     *
     * @return A set of cabins that meet the adjacency criterion.
     */
    public Set<Cabin> cabinWithNeighbors() {
        Set<Cabin> cabinsWithNeighbors = new HashSet<>();

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
     * Checks whether there are no incorrectly placed components on the ship.
     *
     * @return true if the ship has no placement errors, otherwise false.
     */
    public boolean isShipCorrect() {
        return incorrectlyPositionedComponents.isEmpty();
    }

    /**
     * Returns the list of DoubleCannon components present on the ship.
     *
     * @return A list of DoubleCannon objects.
     */
    @JsonIgnore
    public List<DoubleCannon> getDoubleCannons () {
        return componentsPerType.get(DoubleCannon.class)
                .stream()
                .map(DoubleCannon.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of single Cannon components present on the ship.
     *
     * @return A list of single Cannon objects.
     */
    @JsonIgnore
    public List<Cannon> getSingleCannons () {
        return componentsPerType.get(Cannon.class)
                .stream()
                .map(Cannon.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of all cannons (single and double) present on the ship.
     *
     * @return A list of all Cannon objects.
     */
    @JsonIgnore
    public List<Cannon> getAllCannons () {
        return Stream.concat(getSingleCannons().stream(), getDoubleCannons().stream()).collect(Collectors.toList());
    }

    /**
     * Calculates the total firepower of a list of cannons,
     * considering that cannons (single or double) directed NORTH are worth more.
     *
     * @param cannonsToCountFirePower A list of cannons to consider.
     * @return The total firepower.
     */
    public double countTotalFirePower(List<Cannon> cannonsToCountFirePower) {
        Stream<Cannon> singleCannons = cannonsToCountFirePower.stream().filter(cannon -> !(cannon instanceof DoubleCannon));
        Stream<DoubleCannon> doubleCannons = cannonsToCountFirePower.stream().filter(cannon -> cannon instanceof DoubleCannon).map(cannon -> (DoubleCannon) cannon);

        double singleCannonsFirePower = singleCannons.mapToDouble(cannon -> cannon.getFireDirection() == NORTH ? 1 : 0.5).sum();
        double doubleCannonsFirePower = doubleCannons.mapToDouble(cannon -> cannon.getFireDirection() == NORTH ? 2 : 1).sum();

        return singleCannonsFirePower + doubleCannonsFirePower;
    }

    /**
     * Returns the list of DoubleEngine components present on the ship.
     *
     * @return A list of DoubleEngine objects.
     */
    @JsonIgnore
    public List<DoubleEngine> getDoubleEngines () {
        return componentsPerType.get(DoubleEngine.class)
                .stream()
                .map(DoubleEngine.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of single Engine components present on the ship.
     *
     * @return A list of single Engine objects.
     */
    @JsonIgnore
    public List<Engine> getSingleEngines () {
        return componentsPerType.get(Engine.class)
                .stream()
                .map(Engine.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of all engines (single and double) present on the ship.
     *
     * @return A list of all Engine objects.
     */
    @JsonIgnore
    public List<Engine> getAllEngines () {
        return Stream.concat(getSingleEngines().stream(), getDoubleEngines().stream()).collect(Collectors.toList());
    }

    /**
     * Calculates the total power of the provided engines, considering that DoubleEngine counts as double.
     *
     * @param enginesToCountEnginePower The list of engines to consider.
     * @return The total engine power.
     */
    public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
        Stream<Engine> singleEngines = enginesToCountEnginePower.stream().filter(engine -> !(engine instanceof DoubleEngine));
        Stream<DoubleEngine> doubleEngines = enginesToCountEnginePower.stream().filter(engine -> engine instanceof DoubleEngine).map(engine -> (DoubleEngine) engine);

        int totalEnginePower = (int) (singleEngines.count() + 2 * doubleEngines.count());

        return totalEnginePower;
    }

    /**
     * Calculates the number of single engines in operation among the provided engines.
     *
     * @param enginesToCountEnginePower The list of engines to consider.
     * @return The number of single engines.
     */
    public int countSingleEnginePower(List<Engine> enginesToCountEnginePower) {
        Stream<Engine> singleEngines = enginesToCountEnginePower.stream().filter(engine -> !(engine instanceof DoubleEngine));
        return (int) singleEngines.count();
    }

    /**
     * Returns the list of StandardStorage components present on the ship.
     *
     * @return A list of StandardStorage objects.
     */
    @JsonIgnore
    public List<StandardStorage> getStandardStorages() {
        return componentsPerType.get(StandardStorage.class)
                .stream()
                .map(StandardStorage.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of SpecialStorage components present on the ship.
     *
     * @return A list of SpecialStorage objects.
     */
    @JsonIgnore
    public List<SpecialStorage> getSpecialStorages() {
        return componentsPerType.get(SpecialStorage.class)
                .stream()
                .map(SpecialStorage.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all Storage components (both Standard and Special).
     *
     * @return A list of Storage objects.
     */
    @JsonIgnore
    public List<Storage> getStorages() {
        return Stream.concat(getStandardStorages().stream(), getSpecialStorages().stream()).collect(Collectors.toList());
    }

    /**
     * Returns the list of BatteryBox components present on the ship.
     *
     * @return A list of BatteryBox objects.
     */
    @JsonIgnore
    public List<BatteryBox> getBatteryBoxes() {
        return componentsPerType.get(BatteryBox.class)
                .stream()
                .map(BatteryBox.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of Shield components present on the ship.
     *
     * @return A list of Shield objects.
     */
    @JsonIgnore
    public List<Shield> getShields() {
        return componentsPerType.get(Shield.class)
                .stream()
                .map(Shield.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Identifies contiguous parts of the ship stemming from the specified position.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return A list of sets, where each set contains the coordinates of components in a connected part.
     */
    public List<Set<List<Integer>>> identifyShipParts(int x, int y) {
        boolean[][] visited = new boolean[BOARD_DIMENSION][BOARD_DIMENSION];
        List<Set<List<Integer>>> shipParts = new ArrayList<>();

        visited[x][y] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int dir = 0; dir < 4; dir++) {
            int newX = x + dx[dir];
            int newY = y + dy[dir];

            if(!isValidPosition(newX, newY) || shipMatrix[newX][newY] == null || visited[newX][newY]) continue;

            Set<List<Integer>> currentPart = bfsCollectPart(newX, newY, visited);
            shipParts.add(currentPart);
        }
        return shipParts;
    }

    /**
     * Performs a breadth-first search (BFS) to gather all connected cells
     * starting from the provided coordinates.
     *
     * @param startX The starting x-coordinate.
     * @param startY The starting y-coordinate.
     * @param visited A matrix of visited nodes.
     * @return A set of coordinates forming a connected part of the ship.
     */
    public Set<List<Integer>> bfsCollectPart(int startX, int startY, boolean[][] visited) {
        Queue<List<Integer>> queue = new LinkedList<>();
        Set<List<Integer>> part = new HashSet<>();

        queue.add(Arrays.asList(startX, startY));
        visited[startX][startY] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            List<Integer> pos = queue.poll();
            part.add(pos);

            for (int dir=0; dir<4; dir++) {
                int newX = pos.get(0) + dx[dir];
                int newY = pos.get(1) + dy[dir];

                if (isValidPosition(newX, newY) && shipMatrix[newX][newY] != null && !visited[newX][newY]) {
                    visited[newX][newY] = true;
                    queue.add(Arrays.asList(newX, newY));
                }
            }
        }
        return part;
    }

    /**
     * Removes the specified set of components from the board and marks them as inactive.
     *
     * @param componentsPositions A set of coordinates of components to remove.
     */
    public void removeShipPart (Set<List<Integer>> componentsPositions) {
        for(List<Integer> componentPosition : componentsPositions) {
            Component currentComponent = shipMatrix[componentPosition.get(0)][componentPosition.get(1)];
            notActiveComponents.add(currentComponent);
            incorrectlyPositionedComponents.remove(currentComponent);
            shipMatrix[componentPosition.get(0)][componentPosition.get(1)] = null;
        }

        DTO dto = new DTO();
        dto.setShipBoard(this);

        BiConsumer<Observer,String> notifyShipBoard= Observer::notifyShipBoardUpdate;

        //virtualServer.notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "ShipBoardUpdate", dto ), notifyShipBoard);

    }

    /**
     * Finds the coordinates of the first non-null component in a given direction,
     * starting from a specified row or column index.
     *
     * @param pos The row or column index.
     * @param direction The direction to search in.
     * @return An array [x, y] with the coordinates of the first component found.
     * @throws IllegalArgumentException If the position is invalid.
     */
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

    /**
     * Checks whether there is at least one shield covering a specific direction.
     *
     * @param direction The direction to check.
     * @return true if the direction is covered by a shield, otherwise false.
     */
    public boolean isDirectionCoveredByShield(Direction direction) {
        return getShields()
                .stream()
                .flatMap(shield -> shield.getDirections().stream())
                .anyMatch(shieldDirection -> shieldDirection == direction);
    }

    /**
     * Handles the effect of a dangerous object (DangerousObj) on the ship.
     *
     * @param obj The dangerous object to handle.
     */
    public abstract void handleDangerousObject(DangerousObj obj);

    /**
     * Checks whether the ship can defend itself from a dangerous object using single cannons.
     *
     * @param obj The dangerous object.
     * @return true if the ship can defend itself, otherwise false.
     */
    public abstract boolean canDifendItselfWithSingleCannons(DangerousObj obj);

    public List<Component> getNotActiveComponents() {
        return notActiveComponents;
    }

    public Component getComponentAt(Coordinates coordinates) {
        return this.shipMatrix[coordinates.getX()][coordinates.getY()];
    }

    public Component releaseFocusedComponent() {
        Component component = getFocusedComponent();
        setFocusedComponent(null);
        return component;
    }

}
