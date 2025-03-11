package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    @Test
    void testEngine() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Engine engine= new Engine(connectors);
        engine.rotate();
        engine.rotate();
        engine.rotate();
        engine.changeOrientation();
        engine.RotatePowerDirection();

        Map<Direction, ConnectorType> connectorsExpected = new LinkedHashMap<>();
        connectorsExpected.put(Direction.NORTH,ConnectorType.DOUBLE);
        connectorsExpected.put(Direction.EAST,ConnectorType.EMPTY);
        connectorsExpected.put(Direction.SOUTH,ConnectorType.SINGLE);
        connectorsExpected.put(Direction.WEST,ConnectorType.UNIVERSAL);

        Direction powerDirectionExpected = Direction.EAST;

        assertEquals(3,engine.getRotation());
        assertEquals(connectorsExpected,engine.getConnectors(),"Different Connector");
        assertEquals(powerDirectionExpected,engine.getPowerDirection(), "Wrong PowerDirection");
    }

}