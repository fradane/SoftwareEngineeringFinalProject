package it.polimi.ingsw.is25am33.model.component;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class ComponentLoaderTest {

    @Test
    void loadComponentsReturnNonNullList() {
        List<Component> components = ComponentLoader.loadComponents();

        // Verifica che la lista non sia null
        assertNotNull(components, "loadComponents deve restituire una lista non null");
    }

    @Test
    void loadComponentsReturnNonEmptyListIfFilesExist() {
        List<Component> components = ComponentLoader.loadComponents();

        // Se i file JSON ci sono nel classpath, la lista dovrebbe essere non vuota
        assertFalse(components.isEmpty(), "La lista dovrebbe contenere componenti se i file JSON esistono");
    }

}