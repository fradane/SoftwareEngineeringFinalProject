package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.card.Deck;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComponentLoader {

    private static <T> List<T> loadFromJson(String filePath, Class<T> type) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> objects = new ArrayList<>();

        try {
            ClassLoader classLoader = Deck.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(filePath);

            if (inputStream == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            JsonNode rootNode = objectMapper.readTree(inputStream);

            for (JsonNode node : rootNode) {
                T obj = objectMapper.treeToValue(node, type);
                objects.add(obj);
            }

        } catch (IOException e) {
            Logger.getLogger(Deck.class.getName()).log(Level.SEVERE, "Error loading JSON file: " + filePath, e);
        }

        return objects;
    }

    private static List<BatteryBox> loadBatteryBoxes() {
        return ComponentLoader.loadFromJson("BatteryBox.json", BatteryBox.class);
    }

    public static List<Component> loadComponents() {

        List<Component> components = new ArrayList<>();

        //TODO aggiungere le altre tessere
        components.addAll(ComponentLoader.loadBatteryBoxes());

        return components;
    }


}
