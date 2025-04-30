package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatteryBoxTest {

    private BatteryBox batteryBox;

    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);


        batteryBox = new BatteryBox(connectors,2);
    }

    @Test
    void testMaxBatteryCapacity() {
        assertEquals(2, batteryBox.getMaxBatteryCapacity(), "max battery capacity");
    }
    @Test
    void testUseBattery(){

        batteryBox.useBattery();
        batteryBox.useBattery();
        assertEquals(0,batteryBox.getRemainingBatteries(),"Number of available battery");

        Exception exception= assertThrows(Exception.class,batteryBox::useBattery);
        assertEquals("empty battery box", exception.getMessage());

    }
}