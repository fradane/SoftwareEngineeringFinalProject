package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;

public abstract class ShipBoard implements Serializable, ShipBoardClient {

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
    protected Component[][] shipMatrix = new Component[BOARD_DIMENSION][BOARD_DIMENSION];

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
    private Set<Coordinates> incorrectlyPositionedComponentsCoordinates = new HashSet<>();

    /**
     * Constructor that creates a ShipBoard with the main cabin placed at the initial coordinates.
     *
     * @param color The color associated with the player.
     */
    public ShipBoard(PlayerColor color, GameContext gameContext) {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.SOUTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.WEST,  ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,  ConnectorType.UNIVERSAL);
        shipMatrix[STARTING_CABIN_POSITION[0]][STARTING_CABIN_POSITION[1]] = new MainCabin(connectors, color);
        this.gameContext = gameContext;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Component[][] getShipMatrix() {
        synchronized (shipMatrix) {
            return shipMatrix;
        }
    }

    public Component getFocusedComponent() {
        return focusedComponent;
    }

    public Set<Coordinates> getIncorrectlyPositionedComponentsCoordinates() {
        return incorrectlyPositionedComponentsCoordinates;
    }

    public void setGameContext(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    public void setShipMatrix(Component[][] shipMatrix) {
        this.shipMatrix = shipMatrix;
    }

    public void setFocusedComponent(Component focusedComponent)  {
        this.focusedComponent = focusedComponent;
        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyFocusedComponent(nicknameToNotify, player.getNickname(), focusedComponent);;
        });
    }

    public List<Component> getBookedComponents(){
        return notActiveComponents;
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
     * Verifies that the component is properly connected to the ship through at least one non-EMPTY connector.
     * A component connected only through EMPTY connectors is not considered properly connected.
     *
     * @param componentToPlace The component to validate.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return true if the component is properly connected through at least one non-EMPTY connector, otherwise false.
     */
    public boolean isPositionConnectedToShip(Component componentToPlace, int x, int y) {
        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int neighborX = neighbor[0];
            int neighborY = neighbor[1];

            // Skip if position is invalid or empty
            if (!isValidPosition(neighborX, neighborY) || !isPositionOccupiedByComponent(neighborX, neighborY)) {
                continue;
            }

            Component neighborComponent = shipMatrix[neighborX][neighborY];
            Direction oppositeDirection = getOppositeDirection(direction);

            // Check if both connectors are non-EMPTY
            ConnectorType myConnector = componentToPlace.getConnectors().get(direction);
            ConnectorType neighborConnector = neighborComponent.getConnectors().get(oppositeDirection);

            if (myConnector != EMPTY && neighborConnector != EMPTY) {
                // Found at least one proper connection
                return true;
            }
        }

        // No proper (non-EMPTY) connections found
        return false;
    }

    // throws an exception if is not allowed to place the component in that position
    public void checkPosition(int x, int y) {
        if (!isValidPosition(x, y))
            throw new IllegalArgumentException("Not a valid position");
        else if (isPositionOccupiedByComponent(x, y))
            throw new IllegalArgumentException("Position already occupied");
        else if (!isPositionConnectedToShip(focusedComponent, x, y))
            throw new IllegalArgumentException("Not connected to the ship or connected only via empty connector");
        //else if (!areEmptyConnectorsWellConnected(focusedComponent, x, y))
            //throw new IllegalArgumentException("Empty connector not well connected");
    }

    /**
     * Checks the entire ship board for placement rule violations and updates the
     * incorrectlyPositionedComponentsCoordinates set with coordinates of any components
     * that violate the placement rules.
     *
     * This method iterates through all components on the ship board, applying the same
     * checks used during component placement to identify incorrectly positioned components.
     */
    public void checkShipBoard() {
        // Clear the current set of incorrectly positioned components
        incorrectlyPositionedComponentsCoordinates.clear();

        // Iterate through the entire ship matrix
        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                Component component = shipMatrix[i][j];

                // Skip empty cells or cells outside valid positions
                if (component == null || !isValidPosition(i, j)) {
                    continue;
                }

                // Check if this position is properly connected to the ship
                if (!(component instanceof MainCabin) && !isPositionConnectedToShip(component, i, j)) {
                    incorrectlyPositionedComponentsCoordinates.add(new Coordinates(i, j));
                    //TODO da togliere usato solo in debug della checkShipBoardPhase
                    System.out.println("x: " + (i+1) + " y: " + (j+1) + ". isPositionConnectedTOShip");
                    continue;
                }

                // Check if this component violates any placement rules
                if (!areConnectorsWellConnected(component, i, j)
                        || !areEmptyConnectorsWellConnected(component, i, j)
                        || isComponentInFireDirection(component, i, j)
                        || isComponentInEngineDirection(component, i, j)
                        || isEngineDirectionWrong(component)
                        || isAimingAComponent(component, i, j)) {
                    //TODO da togliere usato solo in debug della checkShipBoardPhase
                    if(isAimingAComponent(component, i, j)) {
                        System.out.println("x: " + (i+1) + " y: " + (j+1) + ". isAimingAComponent");
                    }

                    incorrectlyPositionedComponentsCoordinates.add(new Coordinates(i, j));
                }
            }
        }
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
//    public void placeComponentWithFocus(int x, int y) throws IllegalArgumentException {
//        synchronized (shipMatrix) {
//            //TODO uncommentare la checkPosition, serve solo per debugging checkShipboardPhase
//            //checkPosition(x, y); // throws an exception if is not allowed to place the component in that position
//            if (//TODO aggiungere controllo che se sto aggiungendo un cannone che punta verso un component già piazzato
//                    !areConnectorsWellConnected(focusedComponent, x, y)
//                            || !areEmptyConnectorsWellConnected(focusedComponent, x, y) //TODO da controllare se effettivamente va messo qui questo controllo oppure all'interno di checkPosition
//                            || isComponentInFireDirection(focusedComponent, x, y)
//                            || isComponentInEngineDirection(focusedComponent, x, y)
//                            || isEngineDirectionWrong(focusedComponent)
//                            || isAimingAComponent(focusedComponent, x, y)
//            ) {
//                incorrectlyPositionedComponentsCoordinates.add(new Coordinates(x, y));
//            }
//            shipMatrix[x][y] = focusedComponent;
//
//            focusedComponent.insertInComponentsMap(componentsPerType);
//
//                gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
//                        clientController.notifyComponentPlaced(nicknameToNotify, player.getNickname(), focusedComponent, new Coordinates(x, y));
//                });
//
//            focusedComponent = null;
//
//        }
//
//    }
    public void placeComponentWithFocus(int x, int y) throws IllegalArgumentException {
        synchronized (shipMatrix) {
          checkPosition(x, y); // throws an exception if is not allowed to place the component in that position

            shipMatrix[x][y] = focusedComponent;

            focusedComponent.insertInComponentsMap(componentsPerType);

            gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyComponentPlaced(nicknameToNotify, player.getNickname(), focusedComponent, new Coordinates(x, y));
            });

            focusedComponent = null;
        }
    }

    public boolean isAimingAComponent(Component componentToPlace, int x, int y) {
        Direction fireDirection;
        if (componentToPlace instanceof Cannon) {
            Cannon cannon = (Cannon) componentToPlace;
            fireDirection = cannon.getFireDirection();
        } else if (componentToPlace instanceof Engine) {
            Engine engine = (Engine) componentToPlace;
            fireDirection = engine.getFireDirection();
        } else {
            return false;
        }

        int[] neighbor = getNeighborCoordinates(x, y, fireDirection);
        int neighborX = neighbor[0];
        int neighborY = neighbor[1];

        return isValidPosition(neighborX, neighborY) && isPositionOccupiedByComponent(neighborX, neighborY);
    }

    /**
     * Checks whether the direction of an engine is incorrect (e.g., if it's not SOUTH).
     *
     * @param componentToPlace The engine to check.
     * @return true if the direction is invalid, otherwise false.
     */
    public boolean isEngineDirectionWrong(Component componentToPlace) {
        if(componentToPlace instanceof Engine)
            return ((Engine)componentToPlace).getFireDirection() != SOUTH;

        return false;
    }

    /**
     * Restituisce le coordinate del vicino nella direzione specificata
     *
     * @param x Coordinata x della cella di partenza
     * @param y Coordinata y della cella di partenza
     * @param direction La direzione in cui trovare il vicino
     * @return Un array di due elementi [newX, newY] con le coordinate del vicino
     */
    protected int[] getNeighborCoordinates(int x, int y, Direction direction) {
        switch (direction) {
            case NORTH: return new int[] {x - 1, y};
            case SOUTH: return new int[] {x + 1, y};
            case EAST:  return new int[] {x, y + 1};
            case WEST:  return new int[] {x, y - 1};
            default: throw new IllegalArgumentException("Direzione non valida: " + direction);
        }
    }

    /**
     * Restituisce la direzione opposta a quella specificata
     *
     * @param direction La direzione di cui trovare l'opposto
     * @return La direzione opposta
     */
    protected Direction getOppositeDirection(Direction direction) {
        switch (direction) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST:  return WEST;
            case WEST:  return EAST;
            default: throw new IllegalArgumentException("Direzione non valida: " + direction);
        }
    }

    public boolean isPositionOccupiedByComponent(int x, int y) {
        return shipMatrix[x][y] != null;
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

        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int neighborX = neighbor[0];
            int neighborY = neighbor[1];

            if (!isValidPosition(neighborX, neighborY) || !isPositionOccupiedByComponent(neighborX, neighborY)) {
                continue;
            }

            Component neighborComponent = shipMatrix[neighborX][neighborY];
            Direction oppositeDirection = getOppositeDirection(direction);

            // Controllo 1: Se il vicino ha un connettore EMPTY nella direzione opposta,
            // il nostro componente deve avere un connettore EMPTY nella direzione corrispondente
            if (neighborComponent.getConnectors().get(oppositeDirection) == EMPTY &&
                    componentToPlace.getConnectors().get(direction) != EMPTY) {
                //TODO da togliere usato solo in debug della checkShipBoardPhase
                System.out.println("x: " +( x+1) + " y: " + (y+1) + ". areEmptyConnectorsWellConnected");
                return false;
            }

            // Controllo 2: Se il nostro componente ha un connettore EMPTY in una direzione,
            // il vicino deve avere un connettore EMPTY nella direzione opposta
            if (componentToPlace.getConnectors().get(direction) == EMPTY &&
                    neighborComponent.getConnectors().get(oppositeDirection) != EMPTY) {
                //TODO da togliere usato solo in debug della checkShipBoardPhase
                System.out.println("x: " + (x+1) + " y: " + (y+1) + ". areEmptyConnectorsWellConnected");
                return false;
            }
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
        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int neighborX = neighbor[0];
            int neighborY = neighbor[1];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];
            Direction oppositeDirection = getOppositeDirection(direction);

            if (!areConnectorsCompatible(
                    componentToPlace.getConnectors().get(direction),
                    neighborComponent.getConnectors().get(oppositeDirection))) {
                //TODO da togliere usato solo in debug della checkShipBoardPhase
                System.out.println("x: " + (x+1) + " y: " + (y+1) + ". areConnectorsWellConnected");
                return false;
            }
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
        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int neighborX = neighbor[0];
            int neighborY = neighbor[1];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];
            Direction oppositeDirection = getOppositeDirection(direction);

            if (neighborComponent instanceof Cannon &&
                    ((Cannon) neighborComponent).getFireDirection() == oppositeDirection) {
                //TODO da togliere usato solo in debug della checkShipBoardPhase
                System.out.println("x: " + (x+1) + " y: " + (y+1) + ". isComponentInFireDirection");
                return true;
            }
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
        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int neighborX = neighbor[0];
            int neighborY = neighbor[1];

            if (!isValidPosition(neighborX, neighborY) || shipMatrix[neighborX][neighborY] == null) continue;

            Component neighborComponent = shipMatrix[neighborX][neighborY];
            Direction oppositeDirection = getOppositeDirection(direction);

            if (neighborComponent instanceof Engine &&
                    ((Engine) neighborComponent).getFireDirection() == SOUTH &&
                    ((Engine) neighborComponent).getFireDirection() == oppositeDirection) {
                //TODO da togliere usato solo in debug della checkShipBoardPhase
                System.out.println("x: " + (x+1) + " y: " + (y+1) + ". isComponentInEngineDirection");
                return true;
            }
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
    public Set<Set<Coordinates>> removeAndRecalculateShipParts(int x, int y) throws IllegalArgumentException {
        if(shipMatrix[x][y] == null)
            throw new IllegalArgumentException("No component in this position");

        Component componentToRemove = shipMatrix[x][y];
        notActiveComponents.add(componentToRemove);
        removeFromComponentsMap(componentToRemove);
        incorrectlyPositionedComponentsCoordinates.remove(new Coordinates(x, y));
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

        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                if (shipMatrix[i][j] != null) continue;

                for (Direction direction : Direction.values()) {
                    int[] neighbor = getNeighborCoordinates(i, j, direction);
                    int neighborX = neighbor[0];
                    int neighborY = neighbor[1];

                    if (isValidPosition(neighborX, neighborY) &&
                            shipMatrix[neighborX][neighborY] != null &&
                            shipMatrix[neighborX][neighborY].getConnectors().get(getOppositeDirection(direction)) != EMPTY)
                        counter++;
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
     * Returns the list of Cabin components present on the ship.
     *
     * @return A list of Cabin objects.
     */
    @JsonIgnore
    public List<Cabin> getCabin() {
        return componentsPerType.get(Cabin.class)
                .stream()
                .map(Cabin.class::cast)
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

        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                if (!isValidPosition(i, j)) continue;
                Component currentCabin = shipMatrix[i][j];

                if (currentCabin instanceof Cabin && ((Cabin) currentCabin).hasInhabitants()) {
                    for (Direction direction : Direction.values()) {
                        int[] neighbor = getNeighborCoordinates(i, j, direction);
                        int newX = neighbor[0];
                        int newY = neighbor[1];

                        if (!isValidPosition(newX, newY)) continue;

                        Component neighborComponent = shipMatrix[newX][newY];
                        if (neighborComponent instanceof Cabin && ((Cabin) neighborComponent).hasInhabitants()) {
                            cabinsWithNeighbors.add((Cabin) currentCabin);
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
        return incorrectlyPositionedComponentsCoordinates.isEmpty();
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
    public Set<Set<Coordinates>> identifyShipParts(int x, int y) {
        boolean[][] visited = new boolean[BOARD_DIMENSION][BOARD_DIMENSION];
        Set<Set<Coordinates>> shipParts = new HashSet<>();

        visited[x][y] = true;

        for (Direction direction : Direction.values()) {
            int[] neighbor = getNeighborCoordinates(x, y, direction);
            int newX = neighbor[0];
            int newY = neighbor[1];

            if (!isValidPosition(newX, newY) || shipMatrix[newX][newY] == null || visited[newX][newY]) continue;

            Set<Coordinates> currentPart = bfsCollectPart(newX, newY, visited);
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
    public Set<Coordinates> bfsCollectPart(int startX, int startY, boolean[][] visited) {
        Queue<Coordinates> queue = new LinkedList<>();
        Set<Coordinates> part = new HashSet<>();

        queue.add(new Coordinates(startX, startY));
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            Coordinates pos = queue.poll();
            part.add(pos);

            for (Direction direction : Direction.values()) {
                int[] neighbor = getNeighborCoordinates(pos.getX(), pos.getY(), direction);
                int newX = neighbor[0];
                int newY = neighbor[1];

                if (isValidPosition(newX, newY) && shipMatrix[newX][newY] != null && !visited[newX][newY]) {
                    visited[newX][newY] = true;
                    queue.add(new Coordinates(newX, newY));
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
    public void removeShipPart (Set<Coordinates> componentsPositions) {
        for(Coordinates componentPosition : componentsPositions) {
            Component currentComponent = shipMatrix[componentPosition.getX()][componentPosition.getY()];
            notActiveComponents.add(currentComponent);
            removeFromComponentsMap(currentComponent);
            incorrectlyPositionedComponentsCoordinates.remove(componentPosition);
            shipMatrix[componentPosition.getX()][componentPosition.getY()] = null;
        }
//        String playerNicknameToNotify = player != null ? player.getNickname() : "";
//        gameContext.notifyClients(Set.of(playerNicknameToNotify), (nicknameToNotify, clientController) -> {
//            try {
//                if(isShipCorrect())
//                    clientController.notifyValidShipBoard(nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
//                else
//                    clientController.notifyInvalidShipBoard(nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
//            } catch (RemoteException e) {
//                System.err.println("Remote Exception");
//            }
//        });

    }

    public void setIncorrectlyPositionedComponentsCoordinates(Set<Coordinates> incorrectlyPositionedComponentsCoordinates) {
        this.incorrectlyPositionedComponentsCoordinates = incorrectlyPositionedComponentsCoordinates;
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
        component.setCurrState(ComponentState.VISIBLE);
        setFocusedComponent(null);

        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyReleaseComponent(nicknameToNotify, player.getNickname());
        });

        return component;

    }

    /**
     * Removes a component from the componentsPerType map
     * @param component The component to remove
     */
    private void removeFromComponentsMap(Component component) {
        Class<?> componentClass = component.getClass();
        List<Object> componentsList = componentsPerType.get(componentClass);
        if (componentsList != null) {
            componentsList.remove(component);
            // Se la lista diventa vuota, potresti volerla rimuovere dalla mappa
            if (componentsList.isEmpty()) {
                componentsPerType.remove(componentClass);
            }
        }
    }

}
