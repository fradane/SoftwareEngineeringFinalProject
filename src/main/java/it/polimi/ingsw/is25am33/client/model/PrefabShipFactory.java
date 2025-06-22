package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.*;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.EMPTY;
import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.SINGLE;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.WEST;

public class PrefabShipFactory {
    private static final Map<String, PrefabShipInfo> PREFAB_SHIPS = new HashMap<>();

    static {
        // Inizializza le navi prefabbricate disponibili
        PREFAB_SHIPS.put("storage_ship", new PrefabShipInfo(
                "storage_ship",
                "storage_ship",
                "Ship for testing cube malus",
                false,
                false
        ));

        PREFAB_SHIPS.put("basic_ship", new PrefabShipInfo(
                "basic_ship",
                "Basic Ship",
                "A simple ship with essential components",
                false,
                false
        ));

        PREFAB_SHIPS.put("cargo_ship", new PrefabShipInfo(
                "cargo_ship",
                "cargo ship",
                "ship which can only store cargo cubes",
                false,
                true
        ));

        PREFAB_SHIPS.put("basic_gui_shipboard", new PrefabShipInfo(
                "basic_gui_shipboard",
                "GUI ship",
                "A simple ship, but GUI friendly",
                false,
                true
        ));

        PREFAB_SHIPS.put("gui_shipboard_meteorite", new PrefabShipInfo(
                "gui_shipboard_meteorite",
                "meteorite_GUI",
                "ship with shields and battery box",
                false,
                true
        ));

        PREFAB_SHIPS.put("ship_for_meteorites", new PrefabShipInfo(
                "ship_for_meteorites",
                "ship_for_meteorites",
                "A ship with cannon, double cannon and shield",
                false,
                false
        ));


        PREFAB_SHIPS.put("cargo_hauler", new PrefabShipInfo(
                "cargo_hauler",
                "Cargo Hauler",
                "A mid-sized cargo ship with 3 crew cabins and 2 storage units, perfect for freight missions",
                false,
                false
        ));

        PREFAB_SHIPS.put("nave_scorretta", new PrefabShipInfo(
                "nave_scorretta",
                "Nave Scorretta",
                "nave scorretta per fare controlli sulla nave",
                false,
                false
        ));

    }

    /**
     * Gets a list of all available prefab ships
     */
    public static List<PrefabShipInfo> getAvailablePrefabShips(boolean isTestFlight) {
        return PREFAB_SHIPS.values().stream()
                .filter(ship -> !ship.isForTestFlight() || isTestFlight)
                .collect(Collectors.toList());
    }

    /**
     * Gets information about a specific prefab ship
     */
    public static PrefabShipInfo getPrefabShipInfo(String prefabShipId) {
        return PREFAB_SHIPS.get(prefabShipId);
    }

    /**
     * Applies a prefab ship configuration to the given ship board
     */
    public static boolean applyPrefabShip(ShipBoard shipBoard, String prefabShipId) {
        return switch (prefabShipId) {
            case "basic_ship" -> applyBasicShip(shipBoard);
            case "cargo_ship" -> applyCargoShip(shipBoard);
            case "cargo_hauler" -> applyCargoHauler(shipBoard);
            case "nave_scorretta" -> applyNaveScorretta(shipBoard);
            case "basic_gui_shipboard" -> applyGuiBasicShip(shipBoard);
            case "ship_for_meteorites" -> applyMeteoriteShip(shipBoard);
            case "storage_ship" -> applyStorageShip(shipBoard);
            case "gui_shipboard_meteorite" -> applyGuiMeteoriteShip(shipBoard);

            default -> false;
        };
    }

    private static boolean applyGuiMeteoriteShip(ShipBoard shipBoard) {

        clearShipBoard(shipBoard);

        ComponentLoader.loadComponents()
                .stream()
                .filter(component -> switch (component.getImageName()) {
                    case "GT-new_tiles_16_for_web7.jpg",
                         "GT-new_tiles_16_for_web10.jpg",
                         "GT-new_tiles_16_for_web156.jpg",
                         "GT-new_tiles_16_for_web151.jpg",
                         "GT-new_tiles_16_for_web119.jpg",
                         "GT-new_tiles_16_for_web136.jpg",
                         "GT-new_tiles_16_for_web149.jpg" -> true;
                    default -> false;
                })
                .forEach(component -> {
                    String imageName = component.getImageName();
                    System.out.println(imageName);
                    switch (imageName) {
                        case "GT-new_tiles_16_for_web7.jpg" -> addComponent(shipBoard, component, 7, 8);
                        case "GT-new_tiles_16_for_web10.jpg" -> addComponent(shipBoard, component, 7, 6);
                        case "GT-new_tiles_16_for_web156.jpg" -> addComponent(shipBoard, component, 7, 9);
                        case "GT-new_tiles_16_for_web151.jpg" -> {
                            component.rotate();
                            component.rotate();
                            addComponent(shipBoard, component, 8, 7);
                        }
                        case "GT-new_tiles_16_for_web136.jpg" -> {
                            component.rotate();
                            addComponent(shipBoard, component, 8, 8);
                        }
                        case "GT-new_tiles_16_for_web119.jpg" -> addComponent(shipBoard, component, 6, 8);
                        case "GT-new_tiles_16_for_web149.jpg" -> addComponent(shipBoard, component, 6, 7);
                    }
                });

        return true;
    }

    private static boolean applyCargoShip(ShipBoard shipBoard) {

        clearShipBoard(shipBoard);

        ComponentLoader.loadComponents()
                .stream()
                .filter(component -> switch (component.getImageName()) {
                    case "GT-new_tiles_16_for_web62.jpg",
                         "GT-new_tiles_16_for_web31.jpg",
                         "GT-new_tiles_16_for_web64.jpg",
                         "GT-new_tiles_16_for_web18.jpg" -> true;
                    default -> false;
                })
                .forEach(component -> {
                    String imageName = component.getImageName();
                    System.out.println(imageName);
                    switch (imageName) {
                        case "GT-new_tiles_16_for_web62.jpg" -> addComponent(shipBoard, component, 7, 6);
                        case "GT-new_tiles_16_for_web31.jpg" -> addComponent(shipBoard, component, 8, 6);
                        case "GT-new_tiles_16_for_web64.jpg" -> addComponent(shipBoard, component, 6, 7);
                        case "GT-new_tiles_16_for_web18.jpg" -> addComponent(shipBoard, component, 8, 7);
                    }
                });

        return true;
    }

    private static boolean applyStorageShip(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        StandardStorage storageWithBluAndYellowCube = new StandardStorage(createSimpleConnectors(), 3);
        //storageWithBluAndYellowCube.addCube(CargoCube.BLUE);
        //storageWithBluAndYellowCube.addCube(CargoCube.YELLOW);

        StandardStorage storageWithGreenAndYellowCube = new StandardStorage(createSimpleConnectors(), 2);
       storageWithGreenAndYellowCube.addCube(CargoCube.GREEN);
//        storageWithGreenAndYellowCube.addCube(CargoCube.YELLOW);

        SpecialStorage storageWithRedAndBlueCube = new SpecialStorage(createSimpleConnectors(), 2);
        storageWithRedAndBlueCube.addCube(CargoCube.RED);
       // storageWithRedAndBlueCube.addCube(CargoCube.BLUE);

        addComponent(shipBoard, storageWithBluAndYellowCube, 7, 8);
        addComponent(shipBoard, new Shield(createSimpleConnectors()), 8, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 6, 7);
        addComponent(shipBoard, new BatteryBox(createSimpleConnectors(),3), 8, 7);
        addComponent(shipBoard, storageWithGreenAndYellowCube, 7, 5);
        //addComponent(shipBoard, new BatteryBox(createSimpleConnectors(),2), 7, 3);
        addComponent(shipBoard, storageWithRedAndBlueCube, 7, 4);
        addComponent(shipBoard, new DoubleEngine(createSimpleConnectors()), 8, 9);
        addComponent(shipBoard, new DoubleCannon(createSimpleConnectors()), 6, 5);

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyGuiBasicShip(ShipBoard shipBoard) {

        clearShipBoard(shipBoard);

        ComponentLoader.loadComponents()
                .stream()
                .filter(component -> switch (component.getImageName()) {
                    case "GT-new_tiles_16_for_web143.jpg",
                         "GT-new_tiles_16_for_web51.jpg",
                         "GT-new_tiles_16_for_web141.jpg",
                         "GT-new_tiles_16_for_web47.jpg" -> true;
                    default -> false;
                })
                .forEach(component -> {
                    String imageName = component.getImageName();
                    System.out.println(imageName);
                    switch (imageName) {
                        case "GT-new_tiles_16_for_web143.jpg" -> addComponent(shipBoard, component, 7, 8);
                        case "GT-new_tiles_16_for_web51.jpg" -> addComponent(shipBoard, component, 7, 9);
                        case "GT-new_tiles_16_for_web47.jpg" -> addComponent(shipBoard, component, 7, 6);
                        case "GT-new_tiles_16_for_web141.jpg" -> addComponent(shipBoard, component, 7, 5);
                    }
                });

        return true;
    }

    private static boolean applyMeteoriteShip(ShipBoard shipBoard){
        clearShipBoard(shipBoard);

        addComponent(shipBoard, new DoubleCannon(createCustomConnectors(EMPTY,SINGLE,SINGLE,SINGLE)), 7, 8);
        addComponent(shipBoard, new Shield(createSimpleConnectors()), 8, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);
        addComponent(shipBoard, new Cannon(createCustomConnectors(EMPTY,SINGLE,SINGLE,SINGLE)), 6, 7);
        addComponent(shipBoard, new BatteryBox(createSimpleConnectors(),3), 8, 7);
        addComponent(shipBoard, new BatteryBox(createSimpleConnectors(),2), 7, 5);
        addComponent(shipBoard, new DoubleCannon(createCustomConnectors(EMPTY,SINGLE,SINGLE,SINGLE)), 6, 5);
        addComponent(shipBoard, new DoubleCannon(createCustomConnectors(EMPTY,SINGLE,SINGLE,SINGLE)), 8, 9);
        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyNaveScorretta(ShipBoard shipBoard) {
        // Clear any existing components except the main cabin
        clearShipBoard(shipBoard);

        // Add components around the main cabin
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 6, 7);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 5, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 8, 5);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 8, 6);

        shipBoard.checkShipBoard();

        return true;
    }

    private static boolean applyBasicShip(ShipBoard shipBoard) {
        // Clear any existing components except the main cabin
        clearShipBoard(shipBoard);

        // Add components around the main cabin
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 8, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 6, 7);

        shipBoard.checkShipBoard();

        return true;
    }

    private static boolean applyCargoHauler(ShipBoard shipBoard) {
        // Clear any existing components except the main cabin
        clearShipBoard(shipBoard);

        // Add 3 Cabins around the main cabin
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);  // Right of main cabin
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 8, 6);  // Far right of main cabin
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 5);  // Above right cabin

        // Add 2 StandardStorage units
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 2), 6, 7);  // Below main cabin
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 7, 8);  // Below right cabin

        shipBoard.checkShipBoard();

        return true;
    }

    /**
     * Helper to create a minimal connector map:
     *  - For example, let's set each of the four main directions to SINGLE
     *    so the new component can connect on all sides if needed.
     *  - Adjust as you like for your tests.
     */
    private static Map<Direction, ConnectorType> createSimpleConnectors() {
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
    private static Map<Direction, ConnectorType> createCustomConnectors(ConnectorType north, ConnectorType south,
                                                                        ConnectorType east, ConnectorType west) {
        Map<Direction, ConnectorType> connectors = new EnumMap<>(Direction.class);
        connectors.put(NORTH, north);
        connectors.put(SOUTH, south);
        connectors.put(EAST, east);
        connectors.put(WEST, west);
        return connectors;
    }

    // Implementazione per le altre navi prefabbricate...

    /**
     * Pulisce la plancia rimuovendo tutti i componenti tranne la MainCabin.
     * I componenti rimossi vengono aggiunti alla lista notActiveComponents.
     *
     * @param shipBoard la nave da pulire
     */
    private static void clearShipBoard(ShipBoard shipBoard) {
        Component[][] matrix = shipBoard.getShipMatrix();
        Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();

        // Posizione della MainCabin
        int mainCabinX = ShipBoard.STARTING_CABIN_POSITION[0];
        int mainCabinY = ShipBoard.STARTING_CABIN_POSITION[1];

        // Salva la MainCabin
        Component mainCabin = matrix[mainCabinX][mainCabinY];

        // Pulisci la mappa componentsPerType mantenendo solo la MainCabin
        for (Map.Entry<Class<?>, List<Component>> entry : new HashMap<>(componentsPerType).entrySet()) {
            Class<?> componentClass = entry.getKey();
            List<Component> components = new ArrayList<>(entry.getValue());

            // Se non è la classe MainCabin, rimuovi tutti i componenti
            if (!MainCabin.class.equals(componentClass)) {
                for (Component component : components) {
                    // Aggiungi il componente alla lista dei componenti non attivi
                    shipBoard.getNotActiveComponents().add(component);
                    // Rimuovi il componente dalla lista del suo tipo
                    componentsPerType.get(componentClass).remove(component);
                }
                // Se la lista è vuota, rimuovi la chiave dalla mappa
                if (componentsPerType.get(componentClass).isEmpty()) {
                    componentsPerType.remove(componentClass);
                }
            } else {
                // Se è MainCabin, mantieni solo il componente nella posizione iniziale
                components.removeIf(component -> component != mainCabin);
            }
        }

        // Rimuovi i componenti dalla matrice (tranne la MainCabin)
        for (int i = 0; i < ShipBoard.BOARD_DIMENSION; i++) {
            for (int j = 0; j < ShipBoard.BOARD_DIMENSION; j++) {
                // Se non è la posizione della MainCabin e c'è un componente, rimuovilo
                if ((i != mainCabinX || j != mainCabinY) && matrix[i][j] != null) {
                    matrix[i][j] = null;
                }
            }
        }

        // Pulisci il set di componenti posizionati in modo errato
        shipBoard.getIncorrectlyPositionedComponentsCoordinates().clear();
    }

    private static void addComponent(ShipBoard shipBoard, Component component, int x, int y) {
        // Passaggio da coordinate 1-based a 0-based
        int x_0_based = x-1;
        int y_0_based = y-1;

        Component[][] matrix = shipBoard.getShipMatrix();
        matrix[x_0_based][y_0_based] = component;
        component.insertInComponentsMap(shipBoard.getComponentsPerType());
    }

    // Metodi per creare i vari tipi di componenti...


}
