package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.component.*;

import it.polimi.ingsw.is25am33.model.enumFiles.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example JUnit 5 test class for ShipBoard, now adapted to use:
 *  - Cannon(Map<Direction, ConnectorType>, Direction)
 *  - Engine(Map<Direction, ConnectorType>)
 *  - Cabin(Map<Direction, ConnectorType>)
 */
public class ShipBoardTest {

    private ShipBoard shipBoard;

    @BeforeAll
    static void setupAll() {
        // Initialize valid positions
        //ShipBoard.initializeValidPositions(createDefaultValidPositions());
    }

    @BeforeEach
    void setup() {
        // Use whichever concrete subclass is appropriate (e.g., Level2ShipBoard)
        shipBoard = new Level2ShipBoard(PlayerColor.RED);
    }

    /**
     * Helper that returns a simple 12x12 matrix of "true", except for [0][0] which is "false"
     */
    private static boolean[][] createDefaultValidPositions() {
        boolean[][] positions = new boolean[ShipBoard.BOARD_DIMENSION][ShipBoard.BOARD_DIMENSION];
        for (int i = 0; i < ShipBoard.BOARD_DIMENSION; i++) {
            for (int j = 0; j < ShipBoard.BOARD_DIMENSION; j++) {
                positions[i][j] = true;
            }
        }
        positions[0][0] = false;
        return positions;
    }

    /**
     * Helper to create a minimal connector map:
     *  - For example, let's set each of the four main directions to SINGLE
     *    so the new component can connect on all sides if needed.
     *  - Adjust as you like for your tests.
     */
    private Map<Direction, ConnectorType> createSimpleConnectors() {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, ConnectorType.SINGLE);
        connectors.put(Direction.SOUTH, ConnectorType.SINGLE);
        connectors.put(Direction.EAST,  ConnectorType.SINGLE);
        connectors.put(Direction.WEST,  ConnectorType.SINGLE);
        return connectors;
    }

    //@Test
    @DisplayName("Test: initializeValidPositions throws if called twice")
    void testInitializeValidPositionsError() {
        boolean[][] dummy = new boolean[ShipBoard.BOARD_DIMENSION][ShipBoard.BOARD_DIMENSION];
        // Already called in setup(). Another call => must throw
//        assertThrows(IllegalStateException.class, () ->
//                ShipBoard.initializeValidPositions(dummy)
//        );
    }

    @Test
    @DisplayName("Test: isValidPosition returns false OOB, true in-bounds")
    void testIsValidPosition() {
        assertFalse(shipBoard.isValidPosition(-1, 0));
        assertFalse(shipBoard.isValidPosition(0, -1));
        assertFalse(shipBoard.isValidPosition(12, 5));
        assertFalse(shipBoard.isValidPosition(5, 12));
        assertFalse(shipBoard.isValidPosition(12, 12));

        assertFalse(shipBoard.isValidPosition(0, 0));

        assertTrue(shipBoard.isValidPosition(11, 11));
    }

    @Test
    @DisplayName("Test: placeComponentWithFocus throws if not connected to the ship")
    void testPlaceComponentWithFocusNotConnected() {
        // Build a Cannon with a simple connector map, facing NORTH
        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = cannon;

        // Board is empty => no adjacency => must throw
        assertThrows(IllegalArgumentException.class,
                () -> shipBoard.placeComponentWithFocus(4, 4)
        );
    }



    @Test
    @DisplayName("Test: isPositionConnectedToShip is true for adjacent cells near central cabin")
    void testIsPositionConnectedToShip() {
        int x = ShipBoard.STARTING_CABIN_POSITION[0];
        int y = ShipBoard.STARTING_CABIN_POSITION[1];

        assertTrue(shipBoard.isPositionConnectedToShip(x+1, y));
        assertTrue(shipBoard.isPositionConnectedToShip(x-1, y));
        assertTrue(shipBoard.isPositionConnectedToShip(x,   y+1));
        assertTrue(shipBoard.isPositionConnectedToShip(x,   y-1));

        // Not directly adjacent => false
        assertFalse(shipBoard.isPositionConnectedToShip(x+2, y));
    }

    @Test
    @DisplayName("Test: placeComponentWithFocus - placing next to center cabin should succeed")
    void testPlaceComponentWithFocus() {
        // e.g. Cannon with connectors, facing NORTH
        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = cannon;

        int x = ShipBoard.STARTING_CABIN_POSITION[0] + 1;
        int y = ShipBoard.STARTING_CABIN_POSITION[1];

        assertDoesNotThrow(() ->
                shipBoard.placeComponentWithFocus(x, y)
        );
        assertEquals(cannon, shipBoard.shipMatrix[x][y]);
        assertTrue(shipBoard.componentsPerType.get(cannon.getClass()).contains(cannon));
        assertNull(shipBoard.focusedComponent);
    }


    @Test
    @DisplayName("Test: releaseComponentWithFocus sets state to FREE & clears focus")
    void testReleaseComponentWithFocus() {
        // Create an Engine with a simple connector map
        Engine engine = new Engine(createSimpleConnectors());
        engine.setCurrState(ComponentState.BOOKED);
        shipBoard.focusedComponent = engine;

        //shipBoard.releaseComponentWithFocus();
        assertEquals(ComponentState.VISIBLE, engine.getCurrState());
        assertNull(shipBoard.focusedComponent);
    }

    @Test
    @DisplayName("Test: isEngineDirectionWrong returns true if engine is facing SOUTH")
    void testIsEngineDirectionWrong() {
        // Notice: Engine constructor sets powerDirection= SOUTH by default
        Engine eng = new Engine(createSimpleConnectors());
        assertTrue(shipBoard.isEngineDirectionWrong(eng)); // because it's SOUTH

        eng.rotate();
        eng.rotate();
        //eng.rotatePowerDirection();
        assertFalse(shipBoard.isEngineDirectionWrong(eng));
    }

    @Test
    @DisplayName("Test: removeAndRecalculateShipParts throws if cell is empty")
    void testRemoveAndRecalculateShipParts() {
        assertThrows(IllegalArgumentException.class,
                () -> shipBoard.removeAndRecalculateShipParts(4, 4)
        );
    }

    @Test
    @DisplayName("Test: removeAndRecalculateShipParts splits the ship into two isolated parts, which can then be removed")
    void testRemoveAndRecalculateShipPartsIsolatedParts() {
    /*
       We'll build a small chain of components in row=4:
         (4,4) - bridging - (4,5) - bridging - (4,6) - (4,7)
       Then removing (4,5) should yield two parts:
         Part A: (4,4)
         Part B: (4,6) and (4,7)
       We'll check that the method returns these two sets, and then
       remove one of them to confirm the board updates.
    */

        // Create simple connectors for each component
        Map<Direction, ConnectorType> connectors = createSimpleConnectors();

        // Place four components in a row
        // For brevity, let's use Cabin for all. You could use different types if you prefer.
        Cabin c1 = new Cabin(connectors); // at (4,4)
        Cabin c2 = new Cabin(connectors); // bridging piece at (4,5)
        Cabin c3 = new Cabin(connectors); // at (4,6)
        Cabin c4 = new Cabin(connectors); // at (4,7)

        shipBoard.shipMatrix[4][4] = c1;
        shipBoard.shipMatrix[4][5] = c2;
        shipBoard.shipMatrix[4][6] = c3;
        shipBoard.shipMatrix[4][7] = c4;

        // Now remove the piece at (4,5). This should split the ship into two groups:
        //   Part A: the single cell at (4,4)
        //   Part B: the two cells at (4,6) and (4,7)
        List<Set<List<Integer>>> parts = shipBoard.removeAndRecalculateShipParts(4, 5);

        // Let's check that c2 is now in notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(c2),
                "Removed component at (4,5) should be in notActiveComponents");
        assertNull(shipBoard.shipMatrix[4][5],
                "Cell (4,5) should now be empty");

        // We expect exactly 2 isolated parts
        assertEquals(2, parts.size(),
                "Should have 2 separate parts after removing the bridging component");

        // For clarity, let's find which set is (4,4) and which is (4,6)&(4,7).
        // E.g. we can sort by size or check coordinates explicitly.
        Set<List<Integer>> partA = parts.get(0).size() == 1 ? parts.get(0) : parts.get(1);
        Set<List<Integer>> partB = parts.get(0).size() == 1 ? parts.get(1) : parts.get(0);

        // partA should contain exactly (4,4)
        assertEquals(1, partA.size(),
                "One of the parts must be a singleton, presumably (4,4)");
        assertTrue(partA.contains(Arrays.asList(4, 4)), "Expected the single cell (4,4) in partA");

        // partB should contain (4,6) and (4,7)
        assertEquals(2, partB.size(),
                "Other part should contain 2 cells: (4,6) and (4,7)");
        assertTrue(partB.contains(Arrays.asList(4, 6)),
                "Should contain (4,6)");
        assertTrue(partB.contains(Arrays.asList(4, 7)),
                "Should contain (4,7)");

        // Now we simulate removing the entire partB from the board
        shipBoard.removeShipPart(partB);
        // That should remove c3 and c4 from the board
        assertNull(shipBoard.shipMatrix[4][6],
                "PartB cell (4,6) should be removed");
        assertNull(shipBoard.shipMatrix[4][7],
                "PartB cell (4,7) should be removed");

        // They should also appear in notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(c3),
                "c3 must be in notActiveComponents after removal");
        assertTrue(shipBoard.notActiveComponents.contains(c4),
                "c4 must be in notActiveComponents after removal");
    }


    @Test
    @DisplayName("Test: isThereACannon returns true if Cannon with that direction found in row/col")
    void testIsThereACannon() {
        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.shipMatrix[3][4] = cannon;
        // For direction NORTH, we interpret 'pos=4' as column=4
        // => check row i in [0..11], i=3 => found cannon => true
        assertTrue(shipBoard.isThereACannon(4, Direction.NORTH));
        // Wrong direction => false
        assertFalse(shipBoard.isThereACannon(4, Direction.SOUTH));
    }

    @Test
    @DisplayName("Test: isThereADoubleCannon returns true if DoubleCannon is in row/col with given direction")
    void testIsThereADoubleCannon() {
        // e.g. new DoubleCannon(...) => same constructor signature in your code
        DoubleCannon dbl = new DoubleCannon(createSimpleConnectors());
        shipBoard.shipMatrix[5][5] = dbl;

        assertTrue(shipBoard.isThereADoubleCannon(5, Direction.NORTH));
        assertFalse(shipBoard.isThereADoubleCannon(5, Direction.SOUTH));
    }

    @Test
    @DisplayName("Test: getOrderedComponentsInDirection returns correct order for a row/column")
    void testGetOrderedComponentsInDirection() {
        // Fill row=2 with random cannons
        for (int col = 0; col < ShipBoard.BOARD_DIMENSION; col++) {
            shipBoard.shipMatrix[2][col] = new Cannon(createSimpleConnectors());
        }
        Component[] fromWest = shipBoard.getOrderedComponentsInDirection(2, Direction.WEST);
        assertEquals(12, fromWest.length);
        // The first from West is (2,0), then (2,1)...
        assertEquals(shipBoard.shipMatrix[2][0], fromWest[0]);
        assertEquals(shipBoard.shipMatrix[2][1], fromWest[1]);


        // Fill col=7 with random cannons
        for (int row = 0; row < ShipBoard.BOARD_DIMENSION; row++) {
            shipBoard.shipMatrix[row][7] = new Cannon(createSimpleConnectors());
        }
        Component[] fromSouth = shipBoard.getOrderedComponentsInDirection(7, Direction.SOUTH);
        assertEquals(12, fromWest.length);
        // The first from West is (2,0), then (2,1)...
        assertEquals(shipBoard.shipMatrix[11][7], fromSouth[0]);
        assertEquals(shipBoard.shipMatrix[10][7], fromSouth[1]);
    }

    @Test
    @DisplayName("Test: isExposed returns false if row/col is empty")
    void testIsExposed() {
        assertFalse(shipBoard.isExposed(4, Direction.NORTH));
    }

    @Test
    @DisplayName("Test: countExposed is 0 on empty board")
    void testCountExposed() {
        assertEquals(0, shipBoard.countExposed());
    }

    @Test
    @DisplayName("Test: getCrewMembers returns all inhabitants from Cabins")
    void testGetCrewMembers() {
        Cabin cabin1 = new Cabin(createSimpleConnectors());
        cabin1.getInhabitants().add(CrewMember.HUMAN);

        Cabin cabin2 = new Cabin(createSimpleConnectors());
        cabin2.getInhabitants().add(CrewMember.HUMAN);

        Cabin cabin3 = new Cabin(createSimpleConnectors());
        cabin3.getInhabitants().add(CrewMember.HUMAN);

        shipBoard.focusedComponent = cabin1;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = cabin2;
        shipBoard.placeComponentWithFocus(6, 7);
        shipBoard.focusedComponent = cabin3;
        shipBoard.placeComponentWithFocus(7, 9);


        List<CrewMember> members = shipBoard.getCrewMembers();
        assertEquals(3, members.size());
    }

    @Test
    @DisplayName("Test: cabinWithNeighbors returns cabins with at least one adjacent cabin with inhabitants")
    void testCabinWithNeighbors() {
        Cabin c1 = new Cabin(createSimpleConnectors());
        c1.getInhabitants().add(CrewMember.HUMAN);

        Cabin c2 = new Cabin(createSimpleConnectors());
        c2.getInhabitants().add(CrewMember.HUMAN);

        Cabin c3 = new Cabin(createSimpleConnectors());


        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(7, 9);
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(7, 10);

        Set<Cabin> neighbors = shipBoard.cabinWithNeighbors();
        assertEquals(2, neighbors.size());
        assertTrue(neighbors.contains(c1));
        assertTrue(neighbors.contains(c2));

        c3.getInhabitants().add(CrewMember.HUMAN);
        neighbors = shipBoard.cabinWithNeighbors();
        assertEquals(3, neighbors.size());
        assertTrue(neighbors.contains(c1));
        assertTrue(neighbors.contains(c2));
        assertTrue(neighbors.contains(c3));
    }

    @Test
    @DisplayName("Test: isShipCorrect => true if no incorrectlyPositionedComponents")
    void testIsShipCorrect() {
        assertTrue(shipBoard.isShipCorrect());
        // If you forcibly add something to incorrectlyPositionedComponents,
        // you'd expect false.
    }

    @Test
    @DisplayName("Test: getDoubleCannons returns the DoubleCannon objects")
    void testGetDoubleCannons() {
        // Normal cannon
        Cannon c = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = c;
        shipBoard.placeComponentWithFocus(7, 8);

        // Double cannon
        DoubleCannon dc = new DoubleCannon(createSimpleConnectors());
        shipBoard.focusedComponent = dc;
        shipBoard.placeComponentWithFocus(7, 9);

        List<DoubleCannon> doubles = shipBoard.getDoubleCannons();
        assertEquals(1, doubles.size());
        assertEquals(dc, doubles.get(0));
    }

    @Test
    @DisplayName("Test: getAllCannons returns both single and double cannons")
    void testGetAllCannons() {
        Cannon c = new Cannon(createSimpleConnectors());
        DoubleCannon dc = new DoubleCannon(createSimpleConnectors());

        shipBoard.focusedComponent = c;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = dc;
        shipBoard.placeComponentWithFocus(7, 9);

        List<Cannon> cannons = shipBoard.getAllCannons();
        assertEquals(2, cannons.size());
        assertTrue(cannons.contains(c));
        assertTrue(cannons.contains(dc));
    }

    @Test
    @DisplayName("Test: getAllEngines returns Engine and DoubleEngine")
    void testGetAllEngines() {
        Engine e = new Engine(createSimpleConnectors());
        DoubleEngine de = new DoubleEngine(createSimpleConnectors());

        shipBoard.focusedComponent = e;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = de;
        shipBoard.placeComponentWithFocus(7, 9);

        List<Engine> engines = shipBoard.getAllEngines();
        assertEquals(2, engines.size());
        assertTrue(engines.contains(e));
        assertTrue(engines.contains(de));
    }

    @Test
    @DisplayName("Test: getStorages returns all Storage components")
    void testGetStorages() {
        Storage s1 = new StandardStorage(createSimpleConnectors(), 3);
        Storage s2 = new SpecialStorage(createSimpleConnectors(), 2);

        shipBoard.focusedComponent = s1;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = s2;
        shipBoard.placeComponentWithFocus(7, 9);

        List<Storage> storages = shipBoard.getStorages();
        assertEquals(2, storages.size());
        assertTrue(storages.contains(s1));
        assertTrue(storages.contains(s2));
    }

    @Test
    @DisplayName("Test: getBatteryBoxes returns BatteryBox components")
    void testGetBatteryBoxes() {
        BatteryBox b1 = new BatteryBox(createSimpleConnectors(), 3);
        BatteryBox b2 = new BatteryBox(createSimpleConnectors(), 2);

        shipBoard.focusedComponent = b1;
        shipBoard.placeComponentWithFocus(7, 8);
        shipBoard.focusedComponent = b2;
        shipBoard.placeComponentWithFocus(7, 9);

        List<BatteryBox> boxes = shipBoard.getBatteryBoxes();
        assertEquals(2, boxes.size());
        assertTrue(boxes.contains(b1));
        assertTrue(boxes.contains(b2));
    }

    @Test
    @DisplayName("Test: isDirectionCoveredByShield returns true if any Shield covers that direction")
    void testIsDirectionCoveredByShield() {
        Shield shield = new Shield(createSimpleConnectors());
        shipBoard.focusedComponent = shield;
        shipBoard.placeComponentWithFocus(7, 8);

        assertTrue(shipBoard.isDirectionCoveredByShield(Direction.NORTH));
        assertFalse(shipBoard.isDirectionCoveredByShield(Direction.SOUTH));
    }

    @Test
    @DisplayName("Test: countTotalFirePower with front vs side cannons (watch integer division!)")
    void testCountTotalFirePower() {
        // Based on your snippet, single front=1, single side=0.5, double front=2, double side=1, etc.
        // But your code might do integer math => side single might become 0, etc.
        // Create a single front cannon, a single side cannon, a double front cannon
        Cannon singleFront = new Cannon(createSimpleConnectors());
        Cannon singleSide  = new Cannon(createSimpleConnectors());
        singleSide.rotate();
        singleSide.rotate();
        //singleSide.rotateFireDirection();
        DoubleCannon doubleFront = new DoubleCannon(createSimpleConnectors());

        // Pretend we "activate" them all => pass them in a Stream
        List<Cannon> all = List.of(singleFront, singleSide, doubleFront);
        double total = shipBoard.countTotalFirePower(all);

        // Adjust your expected value depending on how your code handles half-points
        // We'll just expect 3 if side single is truncated to 0.
        // If your code is floating, you might expect 3.5, in which case you'd have to do a float/double compare
        assertEquals(3.5, total);
    }

    @Test
    @DisplayName("Test: countTotalEnginePower for single and double engines")
    void testCountTotalEnginePower() {
        Engine e = new Engine(createSimpleConnectors());             // single => +1
        DoubleEngine de = new DoubleEngine(createSimpleConnectors()); // double => +2

        int total = shipBoard.countTotalEnginePower(List.of(e, de));
        assertEquals(3, total);
    }

    @Test
    @DisplayName("Test: countSingleEnginePower returns how many single engines")
    void testCountSingleEnginePower() {
        Engine e = new Engine(createSimpleConnectors());
        DoubleEngine de = new DoubleEngine(createSimpleConnectors());

        // We'll pass all engines
        int count = shipBoard.countSingleEnginePower(List.of(e, de));
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Test: identifyShipParts does not crash and returns BFS partitions")
    void testIdentifyShipParts() {
        Cabin c1 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(6, 7);
        Cabin c2 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(6, 6);
        Cabin c3 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(7, 6);
        Cabin c4 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c4;
        shipBoard.placeComponentWithFocus(8, 7);
        Cabin c5 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c5;
        shipBoard.placeComponentWithFocus(8, 8);
        Cabin c6 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c6;
        shipBoard.placeComponentWithFocus(8, 9);
        Cabin c7 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c7;
        shipBoard.placeComponentWithFocus(9, 8);


        assertDoesNotThrow(() -> {
            List<Set<List<Integer>>> parts = shipBoard.identifyShipParts(4, 4);
        });
        assertEquals(
                List.of(
                        Set.of(
                                List.of(6, 7),
                                List.of(6, 6),
                                List.of(7, 6)
                        ), Set.of(
                                List.of(8, 7),
                                List.of(8, 8),
                                List.of(8, 9),
                                List.of(9, 8)
                        )
                ),
                        shipBoard.identifyShipParts(7, 7));
    }

    @Test
    @DisplayName("Test: removeShipPart removes all components in the set")
    void testRemoveShipPart() {
        Cabin c1 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(6, 7);
        Cabin c2 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(6, 6);
        Cabin c3 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(7, 6);
        Cabin c4 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c4;
        shipBoard.placeComponentWithFocus(8, 7);
        Cabin c5 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c5;
        shipBoard.placeComponentWithFocus(8, 8);
        Cabin c6 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c6;
        shipBoard.placeComponentWithFocus(8, 9);
        Cabin c7 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c7;
        shipBoard.placeComponentWithFocus(9, 8);

        Set<List<Integer>> toRemove = new HashSet<>();
        toRemove.add(Arrays.asList(8, 7));
        toRemove.add(Arrays.asList(8, 8));
        toRemove.add(Arrays.asList(8, 9));
        toRemove.add(Arrays.asList(9, 8));

        shipBoard.removeShipPart(toRemove);

        assertNull(shipBoard.shipMatrix[8][7]);
        assertNull(shipBoard.shipMatrix[8][8]);
        assertNull(shipBoard.shipMatrix[8][9]);
        assertNull(shipBoard.shipMatrix[9][8]);
        // Must appear in notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(c4));
        assertTrue(shipBoard.notActiveComponents.contains(c5));
        assertTrue(shipBoard.notActiveComponents.contains(c6));
        assertTrue(shipBoard.notActiveComponents.contains(c7));
    }
}
