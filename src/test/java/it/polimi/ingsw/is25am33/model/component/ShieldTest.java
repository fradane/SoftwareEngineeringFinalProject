package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShieldTest {
    private Shield shield;

    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.EMPTY);

        List<Direction> coveredDirections = new ArrayList<>();
        coveredDirections.add(Direction.SOUTH);
        coveredDirections.add(Direction.WEST);

        shield= new Shield(connectors,coveredDirections);
    }

    @Test
    void ChangeOrientation() {
        shield.rotate();
        shield.rotate();
        shield.rotate();
        shield.rotate();
        shield.rotate();

        shield.changeOrientation();

        Map<Direction, ConnectorType> connectorsExpected = new LinkedHashMap<>();
        connectorsExpected.put(Direction.NORTH, ConnectorType.EMPTY);
        connectorsExpected.put(Direction.EAST, ConnectorType.UNIVERSAL);
        connectorsExpected.put(Direction.SOUTH, ConnectorType.DOUBLE);
        connectorsExpected.put(Direction.WEST, ConnectorType.EMPTY);

        assertEquals(connectorsExpected, shield.getConnectors(), "Different Connector");

    }

    @Test
    void setDirection() {
        shield.rotate();
        shield.rotate();
        shield.rotate();
        shield.rotate();
        shield.rotate();

        shield.setDirection();

        List<Direction> coveredDirectionsExpected = new ArrayList<>();
        coveredDirectionsExpected.add(Direction.WEST);
        coveredDirectionsExpected.add(Direction.NORTH);

        assertEquals(coveredDirectionsExpected, shield.getDirections(), "Different direction covered");

    }

    @Test
    void turnOn(){
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.EMPTY);
        BatteryBox batteryBox=new BatteryBox(connectors,2);

        shield.turnOn(batteryBox);

        assertEquals(1, batteryBox.getAvailableBattery(), "Available battery");

    }

}