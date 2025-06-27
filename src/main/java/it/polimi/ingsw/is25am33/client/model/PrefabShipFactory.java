package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.*;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.WEST;

public class PrefabShipFactory {
    private static final Map<String, PrefabShipInfo> PREFAB_SHIPS = new HashMap<>();

    //TODO aggiungere una nave prefabbricata per i testing durante l'esame. Quindi una nave completa per giocare e poter sfruttare tutte le carte e una scorretta per mostrare il controllo della shipboard
    static {
        // Inizializza le navi prefabbricate disponibili
        PREFAB_SHIPS.put("nave_completa", new PrefabShipInfo(
                "nave_completa",
                "Nave Completa da Gioco",
                "Una nave completa con tutti i tipi di componenti per testare il gioco",
                false,
                true
        ));

        PREFAB_SHIPS.put("nave_test_errori", new PrefabShipInfo(
                "nave_test_errori",
                "Nave Test Errori",
                "Una nave con tutti i tipi di errori per testare la validazione",
                false,
                true
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
            case "test_many_exposed" -> applyTestManyExposed(shipBoard);
            case "test_no_exposed" -> applyTestNoExposed(shipBoard);
            case "test_cargo_full" -> applyTestCargoFull(shipBoard);
            case "test_many_lost" -> applyTestManyLost(shipBoard);
            case "test_no_engines" -> applyTestNoEngines(shipBoard);
            case "test_no_humans" -> applyTestNoHumans(shipBoard);
            case "nave_completa" -> applyNaveCompleta(shipBoard);
            case "nave_test_errori" -> applyNaveTestErrori(shipBoard);
            case "storage_ship" -> applyStorageShip(shipBoard);
            case "gui_shipboard_meteorite" -> applyGuiMeteoriteShip(shipBoard);
            case "engine_batterybox_ship" -> applyEngineShip(shipBoard);
            case "nave_smugglers" -> applySmugglersShip(shipBoard);
            case "nave_test_errore_check_shipboard" -> applyCheckShipBoardShip(shipBoard);

            default -> false;
        };
    }

    private static boolean applyCheckShipBoardShip(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        addComponent(shipBoard, new Cabin(createCustomConnectors(EMPTY, SINGLE, EMPTY, EMPTY)), 7, 5);
        addComponent(shipBoard, new Engine(createCustomConnectors(EMPTY, EMPTY, SINGLE, EMPTY)), 7, 6);

        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, EMPTY, SINGLE, EMPTY)), 8, 5);
        addComponent(shipBoard, new Cabin(createCustomConnectors(EMPTY, EMPTY, SINGLE, SINGLE)), 8, 6);
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, EMPTY, EMPTY, SINGLE)), 8, 7);


        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applySmugglersShip(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 6, 6);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 6, 7);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 6, 8);

        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 7, 5);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 7, 6);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 7, 8);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 7, 9);

        Cannon cannon1 = new Cannon(createSimpleConnectors());
        cannon1.rotate();
        cannon1.rotate();
        cannon1.rotate();
        addComponent(shipBoard, cannon1, 8, 6);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 8, 7);
        Cannon cannon2 = new Cannon(createSimpleConnectors());
        cannon2.rotate();
        addComponent(shipBoard, cannon2, 8, 8);

        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 9, 5);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 9, 6);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 9, 8);
        addComponent(shipBoard, new Cannon(createSimpleConnectors()), 9, 9);

        shipBoard.checkShipBoard();
        return true;
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
                         "GT-new_tiles_16_for_web135.jpg",
                         "GT-new_tiles_16_for_web129.jpg",
                         "GT-new_tiles_16_for_web133.jpg",
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
                        case "GT-new_tiles_16_for_web135.jpg" ->  addComponent(shipBoard, component, 7, 10);
                        case "GT-new_tiles_16_for_web129.jpg" -> addComponent(shipBoard, component, 7, 5);
                        case "GT-new_tiles_16_for_web151.jpg" -> {
                            component.rotate();
                            component.rotate();
                            addComponent(shipBoard, component, 8, 7);
                        }
                        case "GT-new_tiles_16_for_web136.jpg" -> {
                            component.rotate();
                            addComponent(shipBoard, component, 8, 8);
                        }
                        case "GT-new_tiles_16_for_web133.jpg" -> {
                            component.rotate();
                            component.rotate();
                            component.rotate();
                            addComponent(shipBoard, component, 8, 6);
                        }
                        case "GT-new_tiles_16_for_web119.jpg" -> addComponent(shipBoard, component, 6, 8);
                        case "GT-new_tiles_16_for_web149.jpg" -> {
                            component.rotate();
                            component.rotate();
                            component.rotate();
                            addComponent(shipBoard, component, 6, 7);
                        }
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

    private static boolean applyTestManyExposed(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Nave con molti connettori esposti
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Engine(createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE)), 8, 8);
        addComponent(shipBoard, new Cannon(createCustomConnectors(EMPTY, SINGLE, SINGLE, SINGLE)), 7, 6);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 2), 6, 7);
        // Lascia molti connettori esposti non collegati

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyTestNoExposed(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Nave compatta con tutti i connettori ben collegati
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, SINGLE, SINGLE, EMPTY)), 7, 8);
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE)), 8, 8);
        addComponent(shipBoard, new Engine(createCustomConnectors(SINGLE, EMPTY, EMPTY, SINGLE)), 8, 7);
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(EMPTY, SINGLE, SINGLE, EMPTY)), 7, 6);

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyTestCargoFull(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Aggiungi storage pieni di cubi specifici
        StandardStorage storage1 = new StandardStorage(createSimpleConnectors(), 3);
        storage1.addCube(CargoCube.YELLOW);
        storage1.addCube(CargoCube.YELLOW);
        storage1.addCube(CargoCube.YELLOW);
        addComponent(shipBoard, storage1, 7, 8);

        SpecialStorage storage2 = new SpecialStorage(createSimpleConnectors(), 2);
        storage2.addCube(CargoCube.RED);
        storage2.addCube(CargoCube.GREEN);
        addComponent(shipBoard, storage2, 7, 6);

        StandardStorage storage3 = new StandardStorage(createSimpleConnectors(), 2);
        storage3.addCube(CargoCube.BLUE);
        storage3.addCube(CargoCube.BLUE);
        addComponent(shipBoard, storage3, 8, 7);

        // Aggiungi cabine per l'equipaggio
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 6, 7);
        addComponent(shipBoard, new Engine(createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE)), 6, 6);

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyTestManyLost(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Aggiungi componenti base
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Engine(createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE)), 8, 8);

        // Aggiungi molti componenti alla lista notActiveComponents per simulare perdite
        for(int i = 0; i < 5; i++) {
            shipBoard.getNotActiveComponents().add(new Cannon(createSimpleConnectors()));
        }

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyTestNoEngines(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Nave senza motori
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 6);
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 2), 8, 7);

        shipBoard.checkShipBoard();
        return true;
    }

    private static boolean applyTestNoHumans(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Nave con cabine ma senza umani (verranno rimossi nel test)
        addComponent(shipBoard, new Cabin(createSimpleConnectors()), 7, 8);
        addComponent(shipBoard, new Engine(createCustomConnectors(SINGLE, EMPTY, SINGLE, SINGLE)), 8, 8);

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

    private static boolean applyEngineShip(ShipBoard shipBoard) {

        clearShipBoard(shipBoard);

        //addComponent(shipBoard, new BatteryBox(createSimpleConnectors(), 3), 8, 7);
        SpecialStorage storage1 = new SpecialStorage(createSimpleConnectors(), 2);
        storage1.addCube(CargoCube.RED);
        storage1.addCube(CargoCube.YELLOW);
        addComponent(shipBoard, storage1, 6, 7, "GT-new_tiles_16_for_web64.jpg");

        SpecialStorage storage2 = new SpecialStorage(createSimpleConnectors(), 3);
        storage2.addCube(CargoCube.GREEN);
        storage2.addCube(CargoCube.RED);
        storage2.addCube(CargoCube.BLUE);
        addComponent(shipBoard, storage2, 7, 6, "GT-new_tiles_16_for_web64.jpg");
        addComponent(shipBoard, new StandardStorage(createSimpleConnectors(), 3), 7, 8, "GT-new_tiles_16_for_web18.jpg");
        addComponent(shipBoard, new BatteryBox(createSimpleConnectors(), 1), 6, 6, "GT-new_tiles_16_for_web12.jpg");

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
                    //shipBoard.getNotActiveComponents().add(component);
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

    private static void addComponent(ShipBoard shipBoard, Component component, int x, int y, String imageName) {
        // Passaggio da coordinate 1-based a 0-based
        int x_0_based = x-1;
        int y_0_based = y-1;

        Component[][] matrix = shipBoard.getShipMatrix();
        matrix[x_0_based][y_0_based] = component;
        component.insertInComponentsMap(shipBoard.getComponentsPerType());
        component.setImageName(imageName);
    }

    /**
     * Applica una nave completa con tutti i tipi di componenti per testare il gioco
     */
    private static boolean applyNaveCompleta(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Riga 6
        addComponent(shipBoard, new LifeSupport(createCustomConnectors(DOUBLE, EMPTY, DOUBLE, DOUBLE), ColorLifeSupport.PURPLE), 6, 6, "GT-new_tiles_16_for_web144.jpg");
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, SINGLE, EMPTY, UNIVERSAL)), 6, 7, "GT-new_tiles_16_for_web48.jpg");
        addComponent(shipBoard, new Cannon(createCustomConnectors(EMPTY, SINGLE, EMPTY, EMPTY)), 6, 8, "GT-new_tiles_16_for_web102.jpg");

        // Riga 7
        DoubleCannon doubleCannon7 = new DoubleCannon(createCustomConnectors(EMPTY, DOUBLE, SINGLE, SINGLE));
        doubleCannon7.rotate();
        doubleCannon7.rotate();
        doubleCannon7.rotate();
        addComponent(shipBoard, doubleCannon7, 7, 5, "GT-new_tiles_16_for_web131.jpg");
        addComponent(shipBoard, new BatteryBox(createCustomConnectors(EMPTY, EMPTY, UNIVERSAL, UNIVERSAL), 2), 7, 6, "GT-new_tiles_16_for_web10.jpg");
        SpecialStorage storage1 = new SpecialStorage(createCustomConnectors(SINGLE, SINGLE, SINGLE, UNIVERSAL), 1);
        storage1.addCube(CargoCube.RED);
        addComponent(shipBoard, storage1 , 7, 8, "GT-new_tiles_16_for_web64.jpg");
        addComponent(shipBoard, new Shield(createCustomConnectors(SINGLE, SINGLE, EMPTY, SINGLE)), 7, 9, "GT-new_tiles_16_for_web150.jpg");

        // Riga 8
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, SINGLE, DOUBLE, SINGLE)), 8, 5, "GT-new_tiles_16_for_web36.jpg");
        addComponent(shipBoard, new LifeSupport(createCustomConnectors(EMPTY, EMPTY, EMPTY, UNIVERSAL), ColorLifeSupport.BROWN), 8, 6, "GT-new_tiles_16_for_web139.jpg");
        addComponent(shipBoard, new DoubleEngine(createCustomConnectors(SINGLE, EMPTY, EMPTY, EMPTY)), 8, 7, "GT-new_tiles_16_for_web92.jpg");
        addComponent(shipBoard, new BatteryBox(createCustomConnectors(SINGLE, DOUBLE, EMPTY, EMPTY), 3), 8, 8, "GT-new_tiles_16_for_web12.jpg");
        Cannon cannon8 = new Cannon(createCustomConnectors(EMPTY, EMPTY, DOUBLE, UNIVERSAL));
        cannon8.rotate();
        addComponent(shipBoard, cannon8, 8, 9, "GT-new_tiles_16_for_web125.jpg");

        // Riga 9
        addComponent(shipBoard, new Cabin(createCustomConnectors(SINGLE, DOUBLE, DOUBLE, SINGLE)), 9, 5, "GT-new_tiles_16_for_web37.jpg");
        addComponent(shipBoard, new LifeSupport(createCustomConnectors(EMPTY, DOUBLE, EMPTY, UNIVERSAL), ColorLifeSupport.PURPLE), 9, 6, "GT-new_tiles_16_for_web146.jpg");
        addComponent(shipBoard, new Engine(createCustomConnectors(UNIVERSAL, EMPTY, DOUBLE, EMPTY)), 9, 8, "GT-new_tiles_16_for_web79.jpg");
        addComponent(shipBoard, new StandardStorage(createCustomConnectors(DOUBLE, SINGLE, EMPTY, DOUBLE), 3), 9, 9, "GT-new_tiles_16_for_web32.jpg");

        // Verifica la correttezza della nave
        shipBoard.checkShipBoard();
        return true;
    }

    /**
     * Applica una nave completamente scorretta con tutti i tipi di errori per testare la validazione
     */
    private static boolean applyNaveTestErrori(ShipBoard shipBoard) {
        clearShipBoard(shipBoard);

        // Riga 6
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 6, 6, "GT-new_tiles_16_for_web60.jpg");
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 6, 7, "GT-new_tiles_16_for_web60.jpg");
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 6, 8, "GT-new_tiles_16_for_web60.jpg");

        // Riga 7
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 7, 5, "GT-new_tiles_16_for_web60.jpg");
        addComponent(shipBoard, new LifeSupport(createCustomConnectors(DOUBLE, EMPTY, EMPTY, UNIVERSAL), ColorLifeSupport.PURPLE), 7, 6, "GT-new_tiles_16_for_web148.jpg");
        addComponent(shipBoard, new Cannon(createCustomConnectors(EMPTY, DOUBLE, EMPTY, UNIVERSAL)), 7, 8, "GT-new_tiles_16_for_web124.jpg");

        // Riga 8
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 8, 5, "GT-new_tiles_16_for_web60.jpg");
        Engine engine8 = new Engine(createCustomConnectors(SINGLE, EMPTY, EMPTY, EMPTY));
        engine8.rotate();
        addComponent(shipBoard, engine8, 8, 6, "GT-new_tiles_16_for_web74.jpg");
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 8, 7, "GT-new_tiles_16_for_web60.jpg");
        addComponent(shipBoard, new Shield(createCustomConnectors(EMPTY, UNIVERSAL, EMPTY, DOUBLE)), 8, 8, "GT-new_tiles_16_for_web152.jpg");

        // Riga 9
        addComponent(shipBoard, new StructuralModules(createCustomConnectors(UNIVERSAL, DOUBLE, DOUBLE, UNIVERSAL)), 9, 8, "GT-new_tiles_16_for_web60.jpg");
        addComponent(shipBoard, new Cannon(createCustomConnectors(EMPTY, EMPTY, EMPTY, SINGLE)), 9, 9, "GT-new_tiles_16_for_web112.jpg");

        // Verifica la correttezza della nave (dovrebbe trovare molti errori!)
        shipBoard.checkShipBoard();
        return true;
    }

}
