package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.*;

import it.polimi.ingsw.is25am33.model.enumFiles.*;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Example JUnit 5 test class for ShipBoard, now adapted to use:
 *  - Cannon(Map<Direction, ConnectorType>, Direction)
 *  - Engine(Map<Direction, ConnectorType>)
 *  - Cabin(Map<Direction, ConnectorType>)
 */
public class ShipBoardTest {

    private ShipBoard shipBoard;
    // MainCabin position for more readable tests
    private int cabinX;
    private int cabinY;

    @BeforeAll
    static void setupAll() {
        // Initialize valid positions
        //ShipBoard.initializeValidPositions(createDefaultValidPositions());
    }

    @BeforeEach
    void setup() {
        // Use whichever concrete subclass is appropriate (e.g., Level2ShipBoard)
        shipBoard = new Level2ShipBoard(PlayerColor.RED, new GameClientNotifier(null, new ConcurrentHashMap<>()), false);

        // Store MainCabin coordinates for cleaner test code
        cabinX = ShipBoard.STARTING_CABIN_POSITION[0];
        cabinY = ShipBoard.STARTING_CABIN_POSITION[1];
    }

    /**
     * Helper to create a minimal connector map:
     *  - For example, let's set each of the four main directions to SINGLE
     *    so the new component can connect on all sides if needed.
     *  - Adjust as you like for your tests.
     */
    private Map<Direction, ConnectorType> createSimpleConnectors() {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(NORTH, SINGLE);
        connectors.put(SOUTH, SINGLE);
        connectors.put(Direction.EAST,  SINGLE);
        connectors.put(Direction.WEST,  SINGLE);
        return connectors;
    }

    /**
     * Helper per creare una mappa di connettori personalizzati
     */
    private Map<Direction, ConnectorType> createCustomConnectors(ConnectorType north, ConnectorType south,
                                                                 ConnectorType east, ConnectorType west) {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(NORTH, north);
        connectors.put(SOUTH, south);
        connectors.put(EAST, east);
        connectors.put(WEST, west);
        return connectors;
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

        // Test using a position relative to MainCabin (cabinX + 1, cabinY + 2)
        assertTrue(shipBoard.isValidPosition(cabinX + 1, cabinY + 2));
    }

    @Test
    @DisplayName("Test: placeComponentWithFocus throws if not connected to the ship")
    void testPlaceComponentWithFocusNotConnected() {
        // Build a Cannon with a simple connector map, facing NORTH
        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = cannon;

        // Use position far away from MainCabin
        int farX = cabinX - 2;
        int farY = cabinY - 2;

        // Board is empty => no adjacency => must throw
        assertThrows(IllegalArgumentException.class,
                () -> shipBoard.placeComponentWithFocus(farX, farY)
        );
    }

    @Test
    @DisplayName("Test: motore che punta verso la cabina principale (SOUTH)")
    void testEnginePointingTowardMainCabin() {
        // Creo un motore (che punta verso SOUTH di default)
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Engine engine = new Engine(connectors);

        // Verifico che la direzione di default sia SOUTH
        assertEquals(SOUTH, engine.getFireDirection());

        // Posiziono il motore sopra la cabina principale
        shipBoard.focusedComponent = engine;

        // La posizione sarebbe cabinX-1, cabinY (sopra la cabina)
        shipBoard.placeComponentWithFocus(cabinX-1, cabinY);

        shipBoard.checkShipBoard();

        // Verifico che il posizionamento sia avvenuto ma che sia marcato come errato
        Set<Coordinates> incorrectCoords = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        assertTrue(incorrectCoords.contains(new Coordinates(cabinX-1, cabinY)));
    }

    @Test
    @DisplayName("Test: componente con connettore EMPTY che punta verso la cabina principale")
    void testComponentWithEmptyConnectorTowardMainCabin() {
        // Creo un componente con connettore EMPTY verso SUD (che punterà sulla cabina)
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE);
        Cabin cabin = new Cabin(connectors);

        // Posiziono il componente sopra la cabina principale
        shipBoard.focusedComponent = cabin;

        // La posizione sarebbe cabinX-1, cabinY
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> shipBoard.placeComponentWithFocus(cabinX-1, cabinY));

        // Verifico che l'eccezione sia quella prevista per connettori incompatibili
        assertEquals("Not connected to the ship or connected only via empty connector", exception.getMessage());
    }

    @Test
    @DisplayName("Test: isPositionConnectedToShip is true for adjacent cells near central cabin")
    void testIsPositionConnectedToShip() {
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cannon cannon = new Cannon(connectors);
        assertTrue(shipBoard.isPositionConnectedToShip(cannon, cabinX+1, cabinY));
        assertTrue(shipBoard.isPositionConnectedToShip(cannon, cabinX-1, cabinY));
        assertTrue(shipBoard.isPositionConnectedToShip(cannon, cabinX, cabinY+1));
        assertTrue(shipBoard.isPositionConnectedToShip(cannon, cabinX, cabinY-1));

        // Not directly adjacent => false
        assertFalse(shipBoard.isPositionConnectedToShip(cannon, cabinX+2, cabinY));
    }

    @Test
    @DisplayName("Test: cannone che punta verso la cabina principale")
    void testCannonPointingTowardMainCabin() {
        // Creo un cannone che punta verso SUD (che punterà sulla cabina)
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cannon cannon = new Cannon(connectors);

        // Il cannone punta di default verso NORTH, devo rotarlo due volte per puntare a SUD
        cannon.rotate();  // NORTH -> EAST
        cannon.rotate();  // EAST -> SOUTH

        // Posiziono il cannone sopra la cabina principale
        shipBoard.focusedComponent = cannon;

        // La posizione sarebbe cabinX-1, cabinY
        shipBoard.placeComponentWithFocus(cabinX-1, cabinY);

        shipBoard.checkShipBoard();

        // Verifico che il posizionamento sia avvenuto ma che sia marcato come errato
        Set<Coordinates> incorrectCoords = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        assertTrue(incorrectCoords.contains(new Coordinates(cabinX-1, cabinY)));
    }

    @Test
    @DisplayName("Test: isAimingAComponent verifica se un cannone punta a un altro componente")
    void testIsAimingAComponent() {
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);

        // Posiziono un componente a nord della cabina principale
        Cabin targetCabin = new Cabin(connectors);
        shipBoard.focusedComponent = targetCabin;
        int x1 = cabinX - 1;
        int y1 = cabinY;
        shipBoard.placeComponentWithFocus(x1, y1);

        // Posiziono un cannone a ovest del targetCabin, che punta verso est (quindi verso mainCabin)
        Cannon eastCannon = new Cannon(connectors);
        eastCannon.rotate(); // NORTH -> EAST

        shipBoard.focusedComponent = eastCannon;
        int x2 = cabinX;
        int y2 = cabinY - 1;
        shipBoard.placeComponentWithFocus(x2, y2);

        // Il cannone dovrebbe puntare verso targetCabin
        assertTrue(shipBoard.isAimingAComponent(eastCannon, x2, y2));

        // Ora posiziono un cannone che non punta verso alcun componente
        Cannon eastCannon2 = new Cannon(connectors);
        eastCannon2.rotate(); // NORTH -> EAST

        shipBoard.focusedComponent = eastCannon2;
        int x3 = cabinX;
        int y3 = cabinY + 1;
        shipBoard.placeComponentWithFocus(x3, y3);

        // Questo cannone non dovrebbe puntare verso alcun componente
        assertFalse(shipBoard.isAimingAComponent(eastCannon2, x3, y3));
    }

    @Test
    @DisplayName("Test: placeComponentWithFocus - placing next to center cabin should succeed")
    void testPlaceComponentWithFocus() {
        // e.g. Cannon with connectors, facing NORTH
        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = cannon;

        int x = cabinX + 1;
        int y = cabinY;

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

        shipBoard.releaseFocusedComponent();
        assertEquals(ComponentState.VISIBLE, engine.getCurrState());
        assertNull(shipBoard.focusedComponent);
    }

    @Test
    @DisplayName("Test: isEngineDirectionWrong returns true if engine is facing SOUTH")
    void testIsEngineDirectionWrong() {
        // Notice: Engine constructor sets fireDirection= SOUTH by default
        Engine eng = new Engine(createSimpleConnectors());
        assertFalse(shipBoard.isEngineDirectionWrong(eng)); // because it's SOUTH

        eng.rotate();
        eng.rotate();
        //eng.rotateFireDirection();
        assertTrue(shipBoard.isEngineDirectionWrong(eng));
    }

    @Test
    @DisplayName("Test: removeAndRecalculateShipParts throws if cell is empty")
    void testRemoveAndRecalculateShipParts() {
        // Use position relative to MainCabin but not adjacent
        int farX = cabinX - 2;
        int farY = cabinY - 2;

        assertThrows(IllegalArgumentException.class,
                () -> shipBoard.removeAndRecalculateShipParts(farX, farY)
        );
    }

    @Test
    @DisplayName("Test: removeAndRecalculateShipParts splits the ship into two isolated parts, which can then be removed")
    void testRemoveAndRecalculateShipPartsIsolatedParts() {
        /*
         * Costruiremo una struttura a L partendo dalla MainCabin, rispettando le posizioni valide:
         *
         *         MainCabin(6,6) --- c1(6,7) --- c2(6,8)
         *              |
         *   c4(7,5) - c3(7,6)
         *
         * Poi rimuoveremo c1, isolando c2, e verificheremo che vengano identificate due parti separate
         */

        // Verifichiamo innanzitutto le posizioni valide vicino alla MainCabin
        // La MainCabin è in (6,6)
        assertTrue(shipBoard.isValidPosition(cabinX, cabinY + 1)); // (6,7)
        assertTrue(shipBoard.isValidPosition(cabinX, cabinY + 2)); // (6,8)
        assertTrue(shipBoard.isValidPosition(cabinX + 1, cabinY)); // (7,6)
        assertTrue(shipBoard.isValidPosition(cabinX + 1, cabinY - 1)); // (7,5)

        // Creiamo i componenti con connettori semplici
        Map<Direction, ConnectorType> connectors = createSimpleConnectors();

        // Posiziono c1 a EST della MainCabin (6,7)
        Cabin c1 = new Cabin(connectors);
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        // Posiziono c2 a EST di c1 (6,8)
        Cabin c2 = new Cabin(connectors);
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        // Posiziono c3 a SUD della MainCabin (7,6)
        Cabin c3 = new Cabin(connectors);
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY);

        // Posiziono c4 a OVEST di c3 (7,5)
        Cabin c4 = new Cabin(connectors);
        shipBoard.focusedComponent = c4;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY - 1);

        // Verifichiamo che tutti i componenti siano stati posizionati correttamente
        assertEquals(c1, shipBoard.shipMatrix[cabinX][cabinY + 1]);
        assertEquals(c2, shipBoard.shipMatrix[cabinX][cabinY + 2]);
        assertEquals(c3, shipBoard.shipMatrix[cabinX + 1][cabinY]);
        assertEquals(c4, shipBoard.shipMatrix[cabinX + 1][cabinY - 1]);

        // Ora rimuoviamo c1, che dovrebbe separare c2 dal resto della nave
        Set<Set<Coordinates>> parts = shipBoard.removeAndRecalculateShipParts(cabinX, cabinY + 1);

        // Verifichiamo che c1 sia stato rimosso e aggiunto ai componenti non attivi
        assertNull(shipBoard.shipMatrix[cabinX][cabinY + 1]);
        assertTrue(shipBoard.notActiveComponents.contains(c1));

        // Dovremmo avere esattamente 2 parti: il componente c2 isolato e il resto della nave
        assertEquals(2, parts.size());

        // Troviamo quale parte è il componente isolato (c2) e quale è il corpo principale
        Set<Coordinates> isolatedPart = null;
        Set<Coordinates> mainPart = null;

        for (Set<Coordinates> part : parts) {
            if (part.size() == 1) {
                isolatedPart = part;
            } else {
                mainPart = part;
            }
        }

        assertNotNull(isolatedPart, "Dovrebbe esserci una parte isolata con un solo componente");
        assertNotNull(mainPart, "Dovrebbe esserci una parte principale con più componenti");

        // La parte isolata dovrebbe contenere solo c2
        assertTrue(isolatedPart.contains(new Coordinates(cabinX, cabinY + 2)));
        assertEquals(1, isolatedPart.size());

        // La parte principale dovrebbe contenere la MainCabin, c3 e c4
        assertTrue(mainPart.contains(new Coordinates(cabinX, cabinY))); // MainCabin
        assertTrue(mainPart.contains(new Coordinates(cabinX + 1, cabinY))); // c3
        assertTrue(mainPart.contains(new Coordinates(cabinX + 1, cabinY - 1))); // c4
        assertEquals(3, mainPart.size());

        // Ora rimuoviamo la parte isolata (c2)
        shipBoard.removeShipPart(isolatedPart);

        // Verifichiamo che c2 sia stato rimosso dalla matrice e aggiunto ai componenti non attivi
        assertNull(shipBoard.shipMatrix[cabinX][cabinY + 2]);
        assertTrue(shipBoard.notActiveComponents.contains(c2));

        // Gli altri componenti dovrebbero essere ancora nella matrice
        assertNotNull(shipBoard.shipMatrix[cabinX][cabinY]); // MainCabin
        assertNotNull(shipBoard.shipMatrix[cabinX + 1][cabinY]); // c3
        assertNotNull(shipBoard.shipMatrix[cabinX + 1][cabinY - 1]); // c4
    }

    @Test
    @DisplayName("Test: isThereACannon returns true if Cannon with that direction found in row/col")
    void testIsThereACannon() {
        // Place cannon at position relative to MainCabin
        int x = cabinX - 3;
        int y = cabinY - 2;

        Cannon cannon = new Cannon(createSimpleConnectors());
        shipBoard.shipMatrix[x][y] = cannon;

        // For direction NORTH, we interpret 'pos=y' as column=y
        assertTrue(shipBoard.isThereACannon(y, NORTH));
        // Wrong direction => false
        assertFalse(shipBoard.isThereACannon(y, SOUTH));
    }

    @Test
    @DisplayName("Test: isThereADoubleCannon returns true if DoubleCannon is in row/col with given direction")
    void testIsThereADoubleCannon() {
        // Place DoubleCannon at position relative to MainCabin
        int x = cabinX - 1;
        int y = cabinY - 1;

        DoubleCannon dbl = new DoubleCannon(createSimpleConnectors());
        shipBoard.shipMatrix[x][y] = dbl;

        assertTrue(shipBoard.isThereADoubleCannon(y, NORTH));
        assertFalse(shipBoard.isThereADoubleCannon(y, SOUTH));
    }

    @Test
    @DisplayName("Test: getOrderedComponentsInDirection returns correct order for a row/column")
    void testGetOrderedComponentsInDirection() {
        // Use a row relative to the MainCabin
        int row = cabinX - 4;

        // Fill row with random cannons
        for (int col = 0; col < ShipBoard.BOARD_DIMENSION; col++) {
            shipBoard.shipMatrix[row][col] = new Cannon(createSimpleConnectors());
        }
        Component[] fromWest = shipBoard.getOrderedComponentsInDirection(row, Direction.WEST);
        assertEquals(12, fromWest.length);
        // The first from West is (row,0), then (row,1)...
        assertEquals(shipBoard.shipMatrix[row][0], fromWest[0]);
        assertEquals(shipBoard.shipMatrix[row][1], fromWest[1]);

        // Use a column relative to the MainCabin
        int col = cabinY + 1;

        // Fill col with random cannons
        for (int r = 0; r < ShipBoard.BOARD_DIMENSION; r++) {
            shipBoard.shipMatrix[r][col] = new Cannon(createSimpleConnectors());
        }
        Component[] fromSouth = shipBoard.getOrderedComponentsInDirection(col, SOUTH);
        assertEquals(12, fromWest.length);
        // The first from South is (11,col), then (10,col)...
        assertEquals(shipBoard.shipMatrix[11][col], fromSouth[0]);
        assertEquals(shipBoard.shipMatrix[10][col], fromSouth[1]);
    }

    @Test
    @DisplayName("Test: isExposed returns false if row/col is empty")
    void testIsExposed() {
        // Use position relative to MainCabin
        int col = cabinY - 2;
        assertFalse(shipBoard.isExposed(col, NORTH));
    }

    @Test
    @DisplayName("Test: countExposed is 4 on empty board due to the MainCabin")
    void testCountExposed() {
        assertEquals(4, shipBoard.countExposed());
    }

    @Test
    @DisplayName("Test: countExposed calculates the correct number of exposed connectors including MainCabin")
    void testCountExposedWithComponents() {
        // Prima di aggiungere qualsiasi componente, controlliamo quanti connettori esposti ha la MainCabin
        // La MainCabin ha connettori UNIVERSAL in tutte le direzioni
        // All'inizio ogni lato della MainCabin è esposto (ha celle vuote adiacenti)

        // Calcolo manuale: la MainCabin ha 4 lati con connettori UNIVERSAL tutti esposti
        int expectedInitialExposed = 4;
        assertEquals(expectedInitialExposed, shipBoard.countExposed(),
                "La MainCabin dovrebbe avere 4 connettori esposti all'inizio");

        // Ora aggiungiamo alcuni componenti intorno alla MainCabin

        // Creiamo un componente C1 con connettore SINGLE a SUD
        // e lo posizionamo a NORD della MainCabin
        Map<Direction, ConnectorType> c1Connectors = createCustomConnectors(EMPTY, SINGLE, EMPTY, EMPTY);
        Cabin c1 = new Cabin(c1Connectors);
        shipBoard.shipMatrix[cabinX-1][cabinY] = c1;

        // Situazione attuale:
        //    C1 (EMPTY a NORD, SINGLE a SUD)
        //    |
        // MainCabin (UNIVERSAL in tutte le direzioni)

        // Dopo aver posizionato C1 a NORD della MainCabin:
        // - Il connettore NORD della MainCabin non è più esposto (collegato a C1)
        // - I connettori EST, SUD e OVEST della MainCabin sono ancora esposti
        // - C1 ha un connettore SINGLE a SUD collegato alla MainCabin (non esposto)
        // - C1 ha connettori EMPTY nelle altre direzioni (non contano come esposti)

        // Quindi ora abbiamo 3 connettori esposti (EST, SUD, OVEST della MainCabin)
        int expectedExposedAfterC1 = 3;
        assertEquals(expectedExposedAfterC1, shipBoard.countExposed(),
                "Dopo aver posizionato C1 a NORD, dovrebbero esserci 3 connettori esposti");

        // Aggiungiamo C2 a EST della MainCabin con connettori SINGLE a OVEST e NORD
        Map<Direction, ConnectorType> c2Connectors = createCustomConnectors(SINGLE, EMPTY, EMPTY, SINGLE);
        Cabin c2 = new Cabin(c2Connectors);
        shipBoard.shipMatrix[cabinX][cabinY+1] = c2;

        // Situazione attuale:
        //    C1 (EMPTY a NORD, SINGLE a SUD)
        //    |
        // MainCabin (UNIVERSAL in tutte le direzioni) -- C2 (SINGLE a OVEST, SINGLE a NORD)

        // Dopo aver posizionato C2 a EST della MainCabin:
        // - Il connettore EST della MainCabin non è più esposto (collegato a C2)
        // - I connettori SUD e OVEST della MainCabin sono ancora esposti
        // - C2 ha un connettore SINGLE a OVEST collegato alla MainCabin (non esposto)
        // - C2 ha un connettore SINGLE a NORD che è esposto (non c'è nulla in quella posizione)

        // Quindi ora abbiamo 3 connettori esposti (SUD e OVEST della MainCabin + NORD di C2)
        int expectedExposedAfterC2 = 3;
        assertEquals(expectedExposedAfterC2, shipBoard.countExposed(),
                "Dopo aver posizionato C2 a EST, dovrebbero esserci 3 connettori esposti");

        // Aggiungiamo C3 a SUD della MainCabin con connettori DOUBLE in tutte le direzioni
        Map<Direction, ConnectorType> c3Connectors = createCustomConnectors(DOUBLE, DOUBLE, DOUBLE, DOUBLE);
        Cabin c3 = new Cabin(c3Connectors);
        shipBoard.shipMatrix[cabinX+1][cabinY] = c3;

        // Situazione attuale:
        //    C1 (EMPTY a NORD, SINGLE a SUD)
        //    |
        // MainCabin (UNIVERSAL in tutte le direzioni) -- C2 (SINGLE a OVEST, SINGLE a NORD)
        //    |
        //    C3 (DOUBLE in tutte le direzioni)

        // Dopo aver posizionato C3 a SUD della MainCabin:
        // - Il connettore SUD della MainCabin non è più esposto (collegato a C3)
        // - Il connettore OVEST della MainCabin è ancora esposto
        // - C3 ha connettori DOUBLE a EST, SUD e OVEST che sono tutti esposti

        // Quindi ora abbiamo 4 connettori esposti (OVEST della MainCabin + NORD di C2 + EST, SUD, OVEST di C3)
        int expectedExposedAfterC3 = 5;
        assertEquals(expectedExposedAfterC3, shipBoard.countExposed(),
                "Dopo aver posizionato C3 a SUD, dovrebbero esserci 5 connettori esposti");

        // Aggiungiamo C4 a OVEST della MainCabin con connettori SINGLE in tutte le direzioni
        Map<Direction, ConnectorType> c4Connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cabin c4 = new Cabin(c4Connectors);
        shipBoard.shipMatrix[cabinX][cabinY-1] = c4;

        // Situazione finale:
        //              C1 (EMPTY a NORD, SINGLE a SUD)
        //              |
        // C4 (SINGLE ovunque) -- MainCabin (UNIVERSAL in tutte le direzioni) -- C2 (SINGLE a OVEST, SINGLE a NORD)
        //              |
        //              C3 (DOUBLE in tutte le direzioni)

        // Dopo aver posizionato C4 a OVEST della MainCabin:
        // - Tutti i connettori della MainCabin sono collegati (nessuno esposto)
        // - C4 ha connettori SINGLE a NORD, SUD e OVEST che sono tutti esposti

        // Quindi ora abbiamo 7 connettori esposti (NORD di C2 + EST, SUD, OVEST di C3 + NORD, SUD, OVEST di C4)
        int expectedExposedFinal = 7;
        assertEquals(expectedExposedFinal, shipBoard.countExposed(),
                "Nella configurazione finale, dovrebbero esserci 7 connettori esposti");
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

        // Place cabins at positions relative to MainCabin
        shipBoard.focusedComponent = cabin1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = cabin2;
        shipBoard.placeComponentWithFocus(cabinX - 1, cabinY);
        shipBoard.focusedComponent = cabin3;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

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

        // Place cabins at positions relative to MainCabin
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 3);

//        Set<Cabin> neighbors = shipBoard.getCabinCoordinatesWithNeighbors();
//        assertEquals(2, neighbors.size());
//        assertTrue(neighbors.contains(c1));
//        assertTrue(neighbors.contains(c2));
//
//        c3.getInhabitants().add(CrewMember.HUMAN);
//        neighbors = shipBoard.getCabinCoordinatesWithNeighbors();
//        assertEquals(3, neighbors.size());
//        assertTrue(neighbors.contains(c1));
//        assertTrue(neighbors.contains(c2));
//        assertTrue(neighbors.contains(c3));
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
        // Normal cannon at position relative to MainCabin
        Cannon c = new Cannon(createSimpleConnectors());
        shipBoard.focusedComponent = c;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        // Double cannon at position relative to MainCabin
        DoubleCannon dc = new DoubleCannon(createSimpleConnectors());
        shipBoard.focusedComponent = dc;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        List<DoubleCannon> doubles = shipBoard.getDoubleCannons();
        assertEquals(1, doubles.size());
        assertEquals(dc, doubles.get(0));
    }

    @Test
    @DisplayName("Test: getAllCannons returns both single and double cannons")
    void testGetAllCannons() {
        // Place cannons at positions relative to MainCabin
        Cannon c = new Cannon(createSimpleConnectors());
        DoubleCannon dc = new DoubleCannon(createSimpleConnectors());

        shipBoard.focusedComponent = c;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = dc;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        List<Cannon> cannons = shipBoard.getAllCannons();
        assertEquals(2, cannons.size());
        assertTrue(cannons.contains(c));
        assertTrue(cannons.contains(dc));
    }

    @Test
    @DisplayName("Test: getAllEngines returns Engine and DoubleEngine")
    void testGetAllEngines() {
        // Place engines at positions relative to MainCabin
        Engine e = new Engine(createSimpleConnectors());
        DoubleEngine de = new DoubleEngine(createSimpleConnectors());

        shipBoard.focusedComponent = e;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = de;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        List<Engine> engines = shipBoard.getAllEngines();
        assertEquals(2, engines.size());
        assertTrue(engines.contains(e));
        assertTrue(engines.contains(de));
    }

    @Test
    @DisplayName("Test: getStorages returns all Storage components")
    void testGetStorages() {
        // Place storages at positions relative to MainCabin
        Storage s1 = new StandardStorage(createSimpleConnectors(), 3);
        Storage s2 = new SpecialStorage(createSimpleConnectors(), 2);

        shipBoard.focusedComponent = s1;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY);
        shipBoard.focusedComponent = s2;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY + 1);

        List<Storage> storages = shipBoard.getStorages();
        assertEquals(2, storages.size());
        assertTrue(storages.contains(s1));
        assertTrue(storages.contains(s2));
    }

    @Test
    @DisplayName("Test: getBatteryBoxes returns BatteryBox components")
    void testGetBatteryBoxes() {
        // Place battery boxes at positions relative to MainCabin
        BatteryBox b1 = new BatteryBox(createSimpleConnectors(), 3);
        BatteryBox b2 = new BatteryBox(createSimpleConnectors(), 2);

        shipBoard.focusedComponent = b1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = b2;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        List<BatteryBox> boxes = shipBoard.getBatteryBoxes();
        assertEquals(2, boxes.size());
        assertTrue(boxes.contains(b1));
        assertTrue(boxes.contains(b2));
    }

    @Test
    @DisplayName("Test: isDirectionCoveredByShield returns true if any Shield covers that direction")
    void testIsDirectionCoveredByShield() {
        // Place shield at position relative to MainCabin
        Shield shield = new Shield(createSimpleConnectors());
        shipBoard.focusedComponent = shield;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        assertTrue(shipBoard.isDirectionCoveredByShield(NORTH));
        assertFalse(shipBoard.isDirectionCoveredByShield(SOUTH));
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

        shipBoard.focusedComponent = e;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = de;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        int total = shipBoard.countTotalEnginePower(List.of(de));
        assertEquals(3, total);
    }

    @Test
    @DisplayName("Test: countSingleEnginePower returns how many single engines")
    void testCountSingleEnginePower() {
        Engine e = new Engine(createSimpleConnectors());
        DoubleEngine de = new DoubleEngine(createSimpleConnectors());

        shipBoard.focusedComponent = e;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);
        shipBoard.focusedComponent = de;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        // We'll pass all engines
        int count = shipBoard.countSingleEnginePower(List.of(e, de));
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Test: identifyShipParts does not crash and returns BFS partitions")
    void testIdentifyShipParts() {
        // Place cabins at positions relative to MainCabin
        Cabin c1 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        Cabin c2 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(cabinX, cabinY - 1);

        Cabin c3 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(cabinX - 1, cabinY - 1);

        Cabin c4 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c4;
        shipBoard.placeComponentWithFocus(cabinX - 1, cabinY - 2);

        Cabin c5 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c5;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY - 1);

        Cabin c6 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c6;
        shipBoard.placeComponentWithFocus(cabinX + 2, cabinY - 1);

        Cabin c7 = new Cabin(createSimpleConnectors());
        shipBoard.focusedComponent = c7;
        shipBoard.placeComponentWithFocus(cabinX + 2, cabinY - 2);

        // Test with position not adjacent to any component
        int testX = cabinX + 1;
        int testY = cabinY + 2;

        assertDoesNotThrow(() -> {
            Set<Set<Coordinates>> parts = shipBoard.identifyShipParts(testX, testY);
        });

        // Test with position between the two groups
        int betweenX = cabinX;
        int betweenY = cabinY - 1;

        assertEquals(
                Set.of(
                        Set.of(
                                new Coordinates(cabinX, cabinY),
                                new Coordinates(cabinX, cabinY + 1)
                        ), Set.of(
                                new Coordinates(cabinX - 1, cabinY - 1),
                                new Coordinates(cabinX - 1, cabinY - 2)
                        ), Set.of(
                                new Coordinates(cabinX + 1, cabinY - 1),
                                new Coordinates(cabinX + 2, cabinY - 1),
                                new Coordinates(cabinX + 2, cabinY - 2)
                        )
                ),
                shipBoard.identifyShipParts(betweenX, betweenY));
    }

    @Test
    @DisplayName("Test: removeShipPart removes all components in the set")
    void testRemoveShipPart() {
        /*
         * Costruiremo una struttura che rispetta le posizioni valide:
         *
         *          c1(6,7) --- c2(6,8)
         *          |
         * MainCabin(6,6)
         *          |
         *          c3(7,6) --- c4(7,7)
         *                       |
         *                      c5(8,7)
         *
         * E poi rimuoveremo c3, c4 e c5 verificando che siano stati rimossi correttamente
         */

        // Verifichiamo innanzitutto che le posizioni scelte siano valide
        assertTrue(shipBoard.isValidPosition(cabinX, cabinY + 1));  // (6,7)
        assertTrue(shipBoard.isValidPosition(cabinX, cabinY + 2));  // (6,8)
        assertTrue(shipBoard.isValidPosition(cabinX + 1, cabinY));  // (7,6)
        assertTrue(shipBoard.isValidPosition(cabinX + 1, cabinY + 1)); // (7,7)
        assertTrue(shipBoard.isValidPosition(cabinX + 2, cabinY + 1)); // (8,7)

        // Creiamo i componenti con connettori semplici
        Map<Direction, ConnectorType> connectors = createSimpleConnectors();

        // Posiziono c1 a EST della MainCabin (6,7)
        Cabin c1 = new Cabin(connectors);
        shipBoard.focusedComponent = c1;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        // Posiziono c2 a EST di c1 (6,8)
        Cabin c2 = new Cabin(connectors);
        shipBoard.focusedComponent = c2;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 2);

        // Posiziono c3 a SUD della MainCabin (7,6)
        Cabin c3 = new Cabin(connectors);
        shipBoard.focusedComponent = c3;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY);

        // Posiziono c4 a EST di c3 (7,7)
        Cabin c4 = new Cabin(connectors);
        shipBoard.focusedComponent = c4;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY + 1);

        // Posiziono c5 a SUD di c4 (8,7)
        Cabin c5 = new Cabin(connectors);
        shipBoard.focusedComponent = c5;
        shipBoard.placeComponentWithFocus(cabinX + 2, cabinY + 1);

        // Verifichiamo che tutti i componenti siano stati posizionati correttamente
        assertEquals(c1, shipBoard.shipMatrix[cabinX][cabinY + 1]);
        assertEquals(c2, shipBoard.shipMatrix[cabinX][cabinY + 2]);
        assertEquals(c3, shipBoard.shipMatrix[cabinX + 1][cabinY]);
        assertEquals(c4, shipBoard.shipMatrix[cabinX + 1][cabinY + 1]);
        assertEquals(c5, shipBoard.shipMatrix[cabinX + 2][cabinY + 1]);

        // Creiamo il set di coordinate che vogliamo rimuovere (c3, c4, c5)
        Set<Coordinates> toRemove = new HashSet<>();
        toRemove.add(new Coordinates(cabinX + 1, cabinY));      // c3
        toRemove.add(new Coordinates(cabinX + 1, cabinY + 1));  // c4
        toRemove.add(new Coordinates(cabinX + 2, cabinY + 1));  // c5

        // Rimuoviamo la parte specifica della nave
        shipBoard.removeShipPart(toRemove);

        // Verifichiamo che le celle siano ora vuote
        assertNull(shipBoard.shipMatrix[cabinX + 1][cabinY]);      // c3
        assertNull(shipBoard.shipMatrix[cabinX + 1][cabinY + 1]);  // c4
        assertNull(shipBoard.shipMatrix[cabinX + 2][cabinY + 1]);  // c5

        // Verifichiamo che i componenti rimossi siano stati aggiunti a notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(c3));
        assertTrue(shipBoard.notActiveComponents.contains(c4));
        assertTrue(shipBoard.notActiveComponents.contains(c5));

        // Le altre celle non dovrebbero essere state modificate
        assertNotNull(shipBoard.shipMatrix[cabinX][cabinY]);     // MainCabin
        assertNotNull(shipBoard.shipMatrix[cabinX][cabinY + 1]); // c1
        assertNotNull(shipBoard.shipMatrix[cabinX][cabinY + 2]); // c2
        assertEquals(c1, shipBoard.shipMatrix[cabinX][cabinY + 1]);
        assertEquals(c2, shipBoard.shipMatrix[cabinX][cabinY + 2]);
    }

    @Test
    @DisplayName("Test: mismatch tra connettori SINGLE e DOUBLE")
    void testSingleDoubleConnectorMismatch() {
        // Posiziono un componente con connettori DOUBLE accanto alla cabina principale
        Map<Direction, ConnectorType> doubleConnectors = createCustomConnectors(DOUBLE, DOUBLE, DOUBLE, DOUBLE);
        Cabin cabin = new Cabin(doubleConnectors);

        int adjacentX = cabinX;
        int adjacentY = cabinY + 1;

        shipBoard.focusedComponent = cabin;
        shipBoard.placeComponentWithFocus(adjacentX, adjacentY);

        // Ora provo a collegare un altro componente con connettori SINGLE
        Map<Direction, ConnectorType> singleConnectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cabin cabin2 = new Cabin(singleConnectors);

        shipBoard.focusedComponent = cabin2;

        // Il posizionamento dovrebbe essere possibile ma il componente sarà marcato come errato
        shipBoard.placeComponentWithFocus(adjacentX, adjacentY + 1);

        shipBoard.checkShipBoard();

        // Verifico che il posizionamento sia avvenuto ma che sia marcato come errato
        Set<Coordinates> incorrectCoords = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        assertTrue(incorrectCoords.contains(new Coordinates(adjacentX, adjacentY + 1)));
    }

    @Test
    @DisplayName("Test: mismatch tra connettori")
    void testConnectorMismatch() {
        Map<Direction, ConnectorType> customConnectors1 = createCustomConnectors(SINGLE, SINGLE, EMPTY, SINGLE);
        Cabin cabin = new Cabin(customConnectors1);

        int adjacentX = cabinX;
        int adjacentY = cabinY + 1;

        shipBoard.focusedComponent = cabin;
        shipBoard.placeComponentWithFocus(adjacentX, adjacentY);

        // Ora provo a collegare un altro componente con connettori SINGLE
        Map<Direction, ConnectorType> customConnectors2 = createCustomConnectors(SINGLE, EMPTY, DOUBLE, DOUBLE);
        Cabin cabin2 = new Cabin(customConnectors2);
        cabin2.rotate();

        shipBoard.focusedComponent = cabin2;

        // Il posizionamento dovrebbe essere possibile ma il componente sarà marcato come errato
        shipBoard.placeComponentWithFocus(adjacentX - 1, adjacentY);

        shipBoard.checkShipBoard();

        // Verifico che il posizionamento sia avvenuto ma che sia marcato come errato
        Set<Coordinates> incorrectCoords = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        assertTrue(incorrectCoords.contains(new Coordinates(adjacentX - 1, adjacentY)));
    }

    @Test
    @DisplayName("Test: posizionamento in una cella valida ma non adiacente alla nave")
    void testPlacementInNonAdjacentValidCell() {
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cabin cabin = new Cabin(connectors);

        shipBoard.focusedComponent = cabin;

        // Scelgo un punto della griglia lontano dalla cabina principale
        int farX = cabinX + 1;
        int farY = cabinY - 2;

        // Verifico che la posizione sia valida ma non adiacente alla nave
        assertTrue(shipBoard.isValidPosition(farX, farY));
        assertFalse(shipBoard.isPositionConnectedToShip(cabin, farX, farY));

        // Il posizionamento dovrebbe lanciare un'eccezione
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> shipBoard.placeComponentWithFocus(farX, farY));

        assertEquals("Not connected to the ship or connected only via empty connector", exception.getMessage());
    }

    @Test
    @DisplayName("Test: posizionamento in una posizione già occupata")
    void testPlacementInOccupiedPosition() {
        // Uso direttamente la posizione della cabina principale
        Map<Direction, ConnectorType> connectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);
        Cabin cabin = new Cabin(connectors);

        shipBoard.focusedComponent = cabin;

        // Il posizionamento dovrebbe lanciare un'eccezione
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> shipBoard.placeComponentWithFocus(cabinX, cabinY));

        assertEquals("Position already occupied", exception.getMessage());
    }

    @Test
    @DisplayName("Test: verifica della compatibilità tra connettori")
    void testAreConnectorsCompatible() {
        // Per testare indirettamente, possiamo verificare che i connettori UNIVERSAL siano compatibili con tutto
        Map<Direction, ConnectorType> universalConnectors = createCustomConnectors(UNIVERSAL, UNIVERSAL, UNIVERSAL, UNIVERSAL);
        Map<Direction, ConnectorType> singleConnectors = createCustomConnectors(SINGLE, SINGLE, SINGLE, SINGLE);

        Cabin universalCabin = new Cabin(universalConnectors);
        Cabin singleCabin = new Cabin(singleConnectors);

        // Posiziono la cabina universal vicino alla main cabin
        int adjacentX = cabinX;
        int adjacentY = cabinY + 1;

        shipBoard.focusedComponent = universalCabin;
        shipBoard.placeComponentWithFocus(adjacentX, adjacentY);

        // Posiziono la cabina single vicino alla universal
        shipBoard.focusedComponent = singleCabin;

        // Il posizionamento dovrebbe avvenire senza errori
        assertDoesNotThrow(() -> shipBoard.placeComponentWithFocus(adjacentX, adjacentY + 1));

        // E il componente non dovrebbe essere marcato come errato
        Set<Coordinates> incorrectCoords = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        assertFalse(incorrectCoords.contains(new Coordinates(adjacentX, adjacentY + 1)));
    }

    @Test
    @DisplayName("Test: componentsPerType map is correctly updated on component addition and removal")
    void testComponentsPerTypeMapUpdates() {

        // ========================
        // TEST AGGIUNTA COMPONENTI
        // ========================

        Map<Direction, ConnectorType> connectors = createSimpleConnectors();

        // Aggiungo una cabina
        Cabin cabin = new Cabin(connectors);
        shipBoard.focusedComponent = cabin;
        shipBoard.placeComponentWithFocus(cabinX, cabinY + 1);

        // Verifica aggiunta alla mappa
        assertNotNull(shipBoard.componentsPerType.get(Cabin.class));
        assertEquals(1, shipBoard.componentsPerType.get(Cabin.class).size());
        assertTrue(shipBoard.componentsPerType.get(Cabin.class).contains(cabin));

        // Aggiungo un cannone
        Cannon cannon = new Cannon(connectors);
        shipBoard.focusedComponent = cannon;
        shipBoard.placeComponentWithFocus(cabinX + 1, cabinY);

        // Verifica aggiunta alla mappa
        assertNotNull(shipBoard.componentsPerType.get(Cannon.class));
        assertEquals(1, shipBoard.componentsPerType.get(Cannon.class).size());
        assertTrue(shipBoard.componentsPerType.get(Cannon.class).contains(cannon));

        // Aggiungo un motore
        Engine engine = new Engine(connectors);
        shipBoard.focusedComponent = engine;
        shipBoard.placeComponentWithFocus(cabinX - 1, cabinY);

        // Verifica aggiunta alla mappa
        assertNotNull(shipBoard.componentsPerType.get(Engine.class));
        assertEquals(1, shipBoard.componentsPerType.get(Engine.class).size());
        assertTrue(shipBoard.componentsPerType.get(Engine.class).contains(engine));

        // ==============================
        // TEST RIMOZIONE CON removeAndRecalculateShipParts
        // ==============================

        // Rimuovo la cabina
        shipBoard.removeAndRecalculateShipParts(cabinX, cabinY + 1);

        // Verifica rimozione dalla mappa (la lista può essere vuota o null)
        List<Component> cabinList = shipBoard.componentsPerType.get(Cabin.class);
        assertTrue(cabinList == null || cabinList.isEmpty());
        assertTrue(cabinList == null || !cabinList.contains(cabin));

        // Verifica che sia in notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(cabin));

        // ==============================
        // TEST RIMOZIONE CON removeShipPart
        // ==============================

        // Creo set con coordinate da rimuovere
        Set<Coordinates> toRemove = new HashSet<>();
        toRemove.add(new Coordinates(cabinX + 1, cabinY));  // cannon
        toRemove.add(new Coordinates(cabinX - 1, cabinY));  // engine

        // Rimuovo i componenti
        shipBoard.removeShipPart(toRemove);

        // Verifica rimozione dalla mappa (le liste possono essere vuote o null)
        List<Component> cannonList = shipBoard.componentsPerType.get(Cannon.class);
        List<Component> engineList = shipBoard.componentsPerType.get(Engine.class);

        assertTrue(cannonList == null || cannonList.isEmpty());
        assertTrue(engineList == null || engineList.isEmpty());
        assertTrue(cannonList == null || !cannonList.contains(cannon));
        assertTrue(engineList == null || !engineList.contains(engine));

        // Verifica che siano in notActiveComponents
        assertTrue(shipBoard.notActiveComponents.contains(cannon));
        assertTrue(shipBoard.notActiveComponents.contains(engine));
        assertEquals(3, shipBoard.notActiveComponents.size()); // cabin + cannon + engine
    }
}