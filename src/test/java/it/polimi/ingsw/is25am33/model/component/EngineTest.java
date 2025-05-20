package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {

        private Engine engine;

    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        engine = new Engine(connectors);
    }

    @Test
    void testRotate() {
        engine.rotate();
        assertEquals(1, engine.getRotation(), "wrong rotation ");
    }

    @Test
    void testChangeOrientation() {
        engine.rotate();
        engine.rotate();
        engine.rotate();

        engine.rotate();

        Map<Direction, ConnectorType> connectorsExpected = new LinkedHashMap<>();
        connectorsExpected.put(Direction.NORTH, ConnectorType.DOUBLE);
        connectorsExpected.put(Direction.EAST, ConnectorType.EMPTY);
        connectorsExpected.put(Direction.SOUTH, ConnectorType.SINGLE);
        connectorsExpected.put(Direction.WEST, ConnectorType.UNIVERSAL);

        assertEquals(connectorsExpected, engine.getConnectors(), "Different Connector");
    }

    @Test
    void testRotateFireDirection(){
        engine.rotate();
        engine.rotate();
        engine.rotate();

        //engine.rotateFireDirection();

        Direction fireDirectionExpected = Direction.EAST;
        assertEquals(fireDirectionExpected,engine.getFireDirection(), "Wrong FireDirection");

    }
}