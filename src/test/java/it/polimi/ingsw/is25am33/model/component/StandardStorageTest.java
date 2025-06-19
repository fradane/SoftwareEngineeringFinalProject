package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StandardStorageTest {
    private StandardStorage storage;
    @BeforeEach
    void setUp() {
        Map<Direction, ConnectorType> connectors = new LinkedHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.EMPTY);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);

        storage = new StandardStorage(connectors,2);
    }

    @Test
    void addCube() {
        storage.addCube(CargoCube.BLUE);
        storage.addCube(CargoCube.GREEN);
        List<CargoCube> cargoCubeExpected = new ArrayList<>();
        cargoCubeExpected.add(CargoCube.BLUE);
        cargoCubeExpected.add(CargoCube.GREEN);
        assertEquals(cargoCubeExpected,storage.getStockedCubes(), "stocked Cubes");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> storage.addCube(CargoCube.RED));
        assertEquals(exception.getMessage(),"Red cube in StandardStorage");

    }

    @Test
    void removeCube() {
        storage.addCube(CargoCube.BLUE);
        storage.addCube(CargoCube.GREEN);
        storage.removeCube(CargoCube.BLUE);

        List<CargoCube> cargoCubeExpected = new ArrayList<>();
        cargoCubeExpected.add(CargoCube.GREEN);
        assertEquals(cargoCubeExpected,storage.getStockedCubes(), "remove Cube");

        storage.removeCube(CargoCube.GREEN);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> storage.removeCube(CargoCube.BLUE));
        assertEquals(exception.getMessage(),"Empty storage");
    }


}