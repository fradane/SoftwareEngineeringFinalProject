package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CannonTest {
    private Cannon cannon;

    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.EMPTY);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        cannon = new Cannon(connectors);
    }


    @Test
    void getMainAttribute() {

            assertEquals("N", cannon.getMainAttribute());
        cannon.rotate();
            assertEquals("E", cannon.getMainAttribute());
        cannon.rotate();
            assertEquals("S", cannon.getMainAttribute());
        cannon.rotate();
            assertEquals("W", cannon.getMainAttribute());
    }
}