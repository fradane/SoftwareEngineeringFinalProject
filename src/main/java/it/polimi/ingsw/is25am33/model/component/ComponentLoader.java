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

    /**
     * Loads a list of objects of the specified type from a JSON file.
     *
     * @param filePath the path to the JSON file to be loaded
     * @param type the class type of the objects contained in the JSON file
     * @return a list of objects of the specified type; an empty list is returned if the file is not found or an error occurs
     */
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

    /**
     * Loads a list of BatteryBox components from a JSON file.
     *
     * @return a list of BatteryBox objects; an empty list is returned if the file is not found or an error occurs
     */
    private static List<BatteryBox> loadBatteryBoxes() {
        return ComponentLoader.loadFromJson("BatteryBox.json", BatteryBox.class);
    }

    /**
     * Loads a list of {@code Cabin} objects from the JSON file "Cabin.json".
     *
     * @return a list of {@code Cabin} objects loaded from the "Cabin.json" file;
     * an empty list is returned if the file is not found or an error occurs
     */
    private static List<Cabin> loadCabin() {
        return ComponentLoader.loadFromJson("Cabin.json", Cabin.class);
    }

    /**
     * Loads a list of Cannon objects from the "Cannon.json" file.
     * The method utilizes the {@code ComponentLoader.loadFromJson} utility to parse the JSON file
     * and create instances of the {@code Cannon} class.
     *
     * @return a list of {@code Cannon} objects loaded from the file; returns an empty list if the file is not found
     *         or an error occurs during the loading process.
     */
    private static List<Cannon> loadCannon() {
        return ComponentLoader.loadFromJson("Cannon.json", Cannon.class);
    }

    /**
     * Loads a list of {@code DoubleCannon} objects from a predefined JSON file.
     * This method utilizes the {@code ComponentLoader.loadFromJson} utility
     * to parse the JSON file and map its contents to a list of {@code DoubleCannon} instances.
     *
     * @return a list of {@code DoubleCannon} objects; an empty list if the file
     *         is not found or an error occurs during the loading process
     */
    private static List<DoubleCannon> loadDoubleCannon() {
        return ComponentLoader.loadFromJson("DoubleCannon.json", DoubleCannon.class);
    }

    /**
     * Loads a list of {@code DoubleEngine} objects from a JSON file.
     *
     * The method utilizes the {@code loadFromJson} method of the {@code ComponentLoader} class
     * to read and parse a JSON file named "DoubleEngine.json". The parsed data is converted into
     * instances of the {@code DoubleEngine} class.
     *
     * @return a list of {@code DoubleEngine} objects; an empty list is returned if the file is not found
     *         or an error occurs during the loading process.
     */
    private static List<DoubleEngine> loadDoubleEngine() {
        return ComponentLoader.loadFromJson("DoubleEngine.json", DoubleEngine.class);
    }

    /**
     * Loads a list of {@code Engine} objects from a predefined JSON file.
     * The method fetches the data from "Engine.json" and converts it into a list
     * of {@code Engine} instances using the utility provided by the {@code ComponentLoader}.
     *
     * @return a list of {@code Engine} objects loaded from the JSON file;
     *         an empty list is returned if the file is not found or if an error occurs during loading
     */
    private static List<Engine> loadEngine() {
        return ComponentLoader.loadFromJson("Engine.json", Engine.class);
    }

    /**
     * Loads a list of life support components from the "LifeSupport.json" file.
     *
     * This method leverages the {@code ComponentLoader.loadFromJson} utility to parse
     * and load all entries of type {@code LifeSupport} present in a JSON file named "LifeSupport.json".
     * If the file is not found or any error occurs during the loading process, an empty list is returned.
     *
     * @return a list of {@code LifeSupport} objects; returns an empty list if loading fails
     */
    private static List<LifeSupport> loadLifeSupport() {
        return ComponentLoader.loadFromJson("LifeSupport.json", LifeSupport.class);
    }

    /**
     * Loads a list of {@code MainCabin} objects from the "MainCabin.json" file.
     * This method utilizes a JSON loader to deserialize the contents of the file
     * into a list of {@code MainCabin} instances.
     *
     * @return a list of {@code MainCabin} objects; returns an empty list if the file is not found
     *         or if an error occurs during deserialization
     */
    private static List<MainCabin> loadMainCabin() {
        return ComponentLoader.loadFromJson("MainCabin.json", MainCabin.class);
    }

    /**
     * Loads a list of {@code Shield} objects from the "Shield.json" file using the {@code loadFromJson} method.
     *
     * @return a list of {@code Shield} objects; an empty list is returned if the file is not found or an error occurs
     */
    private static List<Shield> loadShield() {
        return ComponentLoader.loadFromJson("Shield.json", Shield.class);
    }

    /**
     * Loads a list of {@code SpecialStorage} objects from the JSON file "SpecialStorage.json".
     *
     * Internally, this method utilizes the {@code ComponentLoader.loadFromJson} method
     * to deserialize the contents of the specified JSON file into a list of {@code SpecialStorage} instances.
     *
     * @return a {@code List} of {@code SpecialStorage} objects; if the file is missing or an error occurs,
     * an empty list is returned
     */
    private static List<SpecialStorage> loadSpecialStorage() {
        return ComponentLoader.loadFromJson("SpecialStorage.json", SpecialStorage.class);
    }

    /**
     * Loads a list of {@code StandardStorage} objects from a JSON file.
     *
     * @return a list of {@code StandardStorage} objects; an empty list is returned if the file is not found or an error occurs
     */
    private static List<StandardStorage> loadStandardStorage() {
        return ComponentLoader.loadFromJson("StandardStorage.json", StandardStorage.class);
    }

    /**
     * Loads a list of {@code StructuralModules} from a JSON file named "StructuralModules.json".
     * This method utilizes the {@code ComponentLoader.loadFromJson()} utility to load and
     * parse the data into a list of {@code StructuralModules}.
     *
     * @return a list of {@code StructuralModules} objects; returns an empty list if the file
     *         is not found or an error occurs during loading.
     */
    private static List<StructuralModules> loadStructuralModules() {
        return ComponentLoader.loadFromJson("StructuralModules.json", StructuralModules.class);
    }

    /**
     * Loads and combines various components from different categories into a single list.
     * This method aggregates components such as battery boxes, cabins, life support systems,
     * cannons, engines, shields, storage modules, and structural modules.
     *
     * @return a list of {@code Component} objects containing all loaded components
     */
    public static List<Component> loadComponents() {

        List<Component> components = new ArrayList<>();
//        components.addAll(ComponentLoader.loadBatteryBoxes());
//        components.addAll(ComponentLoader.loadCabin());
//        components.addAll(ComponentLoader.loadLifeSupport());
//        components.addAll(ComponentLoader.loadCannon());
//        components.addAll(ComponentLoader.loadDoubleCannon());
//        components.addAll(ComponentLoader.loadDoubleEngine());
//        components.addAll(ComponentLoader.loadEngine());
//        components.addAll(ComponentLoader.loadShield());
//        components.addAll(ComponentLoader.loadSpecialStorage());
//        components.addAll(ComponentLoader.loadStandardStorage());
//        components.addAll(ComponentLoader.loadStructuralModules());

        components.addAll(ComponentLoader.loadCabin());
        components.addAll(ComponentLoader.loadCabin());
        components.addAll(ComponentLoader.loadCabin());
        components.addAll(ComponentLoader.loadCabin());

        return components;
    }


}


