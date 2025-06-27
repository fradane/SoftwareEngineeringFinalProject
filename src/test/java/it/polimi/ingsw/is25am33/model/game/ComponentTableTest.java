package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.ThrowingBiConsumer;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTableTest {

    private ComponentTable componentTable;

    @BeforeEach
    void setUp() {
        componentTable = new ComponentTable();
        componentTable.setGameClientNotifier(new GameClientNotifier( new ConcurrentHashMap<>()));
    }


    @Test
    void testConstructorInitializesHiddenComponents() {
        ComponentTable table = new ComponentTable();
        assertNotNull(table.pickHiddenComponent(), "Hidden components should not be null after initialization");
    }

    // Test that pickHiddenComponent returns a valid component when called normally
    @Test
    void testPickHiddenComponentNormal() {
        Component component = componentTable.pickHiddenComponent();
        assertNotNull(component, "Should return a component when available");
    }

    @Test
    void testPickHiddenComponentUntilEmpty() {
        Component component;
        do {
            component = componentTable.pickHiddenComponent();
        } while (component != null);
        assertNull(componentTable.pickHiddenComponent(), "Should return null when no components remain");
    }

    @Test
    void testAddVisibleComponentAndPick() {

        Component chosenComponent = null;

        for (int i = 1; i < 11; i++) {
            Component randComponent = componentTable.pickHiddenComponent();
            if (i == 9) chosenComponent = randComponent;
            componentTable.addVisibleComponent(randComponent);
        }

        assertEquals(10, componentTable.getVisibleComponents().size(), "Should have 10 visible components");

        Component retrieved = componentTable.pickVisibleComponent(9);
        assertEquals(chosenComponent, retrieved, "Should retrieve the same ninth component");

        assertEquals(9, componentTable.getVisibleComponents().size(), "Should have 9 visible components after picking one");

        assertNull(componentTable.pickVisibleComponent(9), "Should return null when picking the same component twice");

        retrieved = componentTable.pickVisibleComponent(12);
        assertNull(retrieved, "Should return null when picking an invalid index");
        assertEquals(9, componentTable.getVisibleComponents().size(), "Should have 9 visible components after picking one");

    }

}