package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.card.Deck;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    private static List<Cabin> loadCabin() {
        return ComponentLoader.loadFromJson("Cabin.json", Cabin.class);
    }

    private static List<Cannon> loadCannon() {
        return ComponentLoader.loadFromJson("Cannon.json", Cannon.class);
    }

    private static List<DoubleCannon> loadDoubleCannon() {
        return ComponentLoader.loadFromJson("DoubleCannon.json", DoubleCannon.class);
    }

    private static List<DoubleEngine> loadDoubleEngine() {
        return ComponentLoader.loadFromJson("DoubleEngine.json", DoubleEngine.class);
    }

    private static List<Engine> loadEngine() {
        return ComponentLoader.loadFromJson("Engine.json", Engine.class);
    }

    private static List<LifeSupport> loadLifeSupport() {
        return ComponentLoader.loadFromJson("LifeSupport.json", LifeSupport.class);
    }

    private static List<MainCabin> loadMainCabin() {
        return ComponentLoader.loadFromJson("MainCabin.json", MainCabin.class);
    }

    private static List<Shield> loadShield() {
        return ComponentLoader.loadFromJson("Shield.json", Shield.class);
    }

    private static List<SpecialStorage> loadSpecialStorage() {
        return ComponentLoader.loadFromJson("SpecialStorage.json", SpecialStorage.class);
    }

    private static List<StandardStorage> loadStandardStorage() {
        return ComponentLoader.loadFromJson("StandardStorage.json", StandardStorage.class);
    }

    private static List<StructuralModules> loadStructuralModules() {
        return ComponentLoader.loadFromJson("StructuralModules.json", StructuralModules.class);
    }

    public static List<Component> loadComponents() {

        List<Component> components = new ArrayList<>();
        //TODO unncommetare. commentati solo per dubug
          components.addAll(ComponentLoader.loadBatteryBoxes());
//        components.addAll(ComponentLoader.loadCabin());
//        components.addAll(ComponentLoader.loadCannon());
//        components.addAll(ComponentLoader.loadDoubleCannon());
        components.addAll(ComponentLoader.loadDoubleEngine());
        components.addAll(ComponentLoader.loadEngine());
//        components.addAll(ComponentLoader.loadLifeSupport());
//        components.addAll(ComponentLoader.loadShield());
//        components.addAll(ComponentLoader.loadSpecialStorage());
//        components.addAll(ComponentLoader.loadStandardStorage());
//        components.addAll(ComponentLoader.loadStructuralModules());

        //TODO capire se va uncommentato
        //components.addAll(ComponentLoader.loadMainCabin());

        return components;
    }


}


