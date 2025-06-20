package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    private DoubleCannon doubleCannon;
    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.EMPTY);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);
        doubleCannon = new DoubleCannon(connectors);
        doubleCannon.setImageName("TestImage");
    }

    @Test
    void testToString() {

        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.EMPTY);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);

        // Costruiamo l'expected string
        String expected = String.format("""
            TestImage
            DoubleCannon
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            """,

                connectors.get(Direction.NORTH).fromConnectorTypeToValue(),
                connectors.get(Direction.WEST).fromConnectorTypeToValue(),
                connectors.get(Direction.EAST).fromConnectorTypeToValue(),
                connectors.get(Direction.SOUTH).fromConnectorTypeToValue()
        );

        // Verifica
        assertEquals(expected, doubleCannon.toString());
    }
}