package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.ThrowingBiConsumer;
import it.polimi.ingsw.is25am33.model.component.Component;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTableTest {

    // Test that the constructor properly initializes the hidden components list
    // and pickHiddenComponent returns a non-null value right after initialization
    @Test
    void testConstructorInitializesHiddenComponents() {
        ComponentTable table = new ComponentTable();
        assertNotNull(table.pickHiddenComponent(), "Hidden components should not be null after initialization");
    }

    // Test that pickHiddenComponent returns a valid component when called normally
    @Test
    void testPickHiddenComponentNormal() {
        ComponentTable table = new ComponentTable();
        Component comp = table.pickHiddenComponent();
        assertNotNull(comp, "Should return a component when available");
    }

    // Test that pickHiddenComponent keeps returning components until the pool is empty,
    // and then returns null when no components remain
    @Test
    void testPickHiddenComponentUntilEmpty() {
        ComponentTable table = new ComponentTable();
        Component comp;
        do {
            comp = table.pickHiddenComponent();
        } while (comp != null);
        assertNull(table.pickHiddenComponent(), "Should return null when no components remain");
    }

    // Test the functionality of adding components to the visible list and retrieving them by index.
    // Also checks correct behavior when picking an already-picked or invalid index
    @Test
    void testAddVisibleComponentAndPick() {
        ComponentTable table = new ComponentTable();

        // Create a mock GameContext with a no-op notifyAllClients implementation
        GameClientNotifier mockContext = new GameClientNotifier(null, nullCCCC) {
            @Override
            public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {}
        };

        // Assign the mock context to the component table
        table.setGameClientNotifier(mockContext);

        // Add 10 components to the visible list and store the 9th one for later verification
        Component chosenComponent = null;

        for (int i = 1; i < 11; i++) {
            Component randComponent = table.pickHiddenComponent();
            if (i == 9) chosenComponent = randComponent;
            table.addVisibleComponent(randComponent);
        }

        // Verify that 10 components have been added to the visible list
        assertEquals(10, table.getVisibleComponents().size(), "Should have 10 visible components");

        // Pick the 9th component and verify it's the same one stored earlier
        Component retrieved = table.pickVisibleComponent(9);
        assertEquals(chosenComponent, retrieved, "Should retrieve the same ninth component");
        // Verify that the visible list now contains only 9 components
        assertEquals(9, table.getVisibleComponents().size(), "Should have 9 visible components after picking one");
        // Verify that picking the same index again returns null
        // Verify that picking an invalid index also returns null
        // Ensure the visible list size remains unchanged
        assertNull(table.pickVisibleComponent(9), "Should return null when picking the same component twice");

        retrieved = table.pickVisibleComponent(12);
        assertNull(retrieved, "Should return null when picking an invalid index");
        assertEquals(9, table.getVisibleComponents().size(), "Should have 9 visible components after picking one");

    }

}