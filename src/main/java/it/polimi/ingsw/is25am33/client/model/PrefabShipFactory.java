package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.*;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType.SINGLE;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;
import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.WEST;

public class PrefabShipFactory {
    private static final Map<String, PrefabShipInfo> PREFAB_SHIPS = new HashMap<>();

    static {
        // Inizializza le navi prefabbricate disponibili
        PREFAB_SHIPS.put("basic_ship", new PrefabShipInfo(
                "basic_ship",
                "Basic Ship",
                "A simple ship with essential components",
                false
        ));

        PREFAB_SHIPS.put("basic_gui_shipboard", new PrefabShipInfo(
                "basic_gui_shipboard",
                "GUI ship",
                "A simple ship, but GUI friendly",
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
            case "basic_gui_shipboard" -> applyGuiBasicShip(shipBoard);
            default -> false;
        };
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
                    switch (imageName) {
                        case "GT-new_tiles_16_for_web143.jpg" -> addComponent(shipBoard, component, 7, 8);
                        case "GT-new_tiles_16_for_web51.jpg" -> addComponent(shipBoard, component, 7, 9);
                        case "GT-new_tiles_16_for_web47.jpg" -> addComponent(shipBoard, component, 7, 6);
                        case "GT-new_tiles_16_for_web141.jpg" -> addComponent(shipBoard, component, 7, 5);
                    }
                });

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