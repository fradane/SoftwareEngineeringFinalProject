package it.polimi.ingsw.is25am33.model.card;


import java.util.*;
import java.io.InputStream;
import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a deck of adventure cards used in the game.
 * It manages different levels of cards, organizes them into decks,
 * and provides methods for drawing and shuffling.
 */
public class Deck {

    private final List<AdventureCard> allCards;
    private final List<List<AdventureCard>> littleVisibleDecks;
    private final List<AdventureCard> littleNotVisibleDeck;
    private final Stack<AdventureCard> gameDeck;

    /**
     * Constructs a new Deck instance, initializing all internal card lists.
     */
    public Deck() {
        allCards = new Stack<>();
        littleVisibleDecks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            littleVisibleDecks.add(new ArrayList<>());
        }

        littleNotVisibleDeck = new ArrayList<>();
        gameDeck = new Stack<>();
    }

    /**
     * Merges the smaller decks into the main game deck and shuffles it.
     */
    public void mergeIntoGameDeck() {
        gameDeck.addAll(littleNotVisibleDeck);
        littleVisibleDecks.forEach(gameDeck::addAll);
        Collections.shuffle(gameDeck);
    }

    /**
     * Draws a card from the game deck.
     *
     * @return The top AdventureCard from the deck.
     * @throws EmptyStackException if the game deck is empty.
     */
    public AdventureCard drawCard() throws EmptyStackException {
        return gameDeck.pop();
    }

    /**
     * Allows players to view a specific visible deck by its index.
     *
     * @param index The index of the visible deck (0-2).
     * @return The list of AdventureCards in the selected deck.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public List<AdventureCard> watchVisibleDeck(int index) throws IndexOutOfBoundsException {
        return littleVisibleDecks.get(index);
    }

    /**
     * Sets up the smaller decks by categorizing cards into levels,
     * shuffling them, and distributing them into the visible and non-visible decks.
     */
    public void setUpLittleDecks(Game game) {
        loadCards();
        allCards.forEach(adventureCard -> adventureCard.setGame(game));
        List<AdventureCard> level1Cards = new ArrayList<>(allCards.stream().filter(c -> c.getLevel() == 1).toList());
        List<AdventureCard> level2Cards = new ArrayList<>(allCards.stream().filter(c -> c.getLevel() == 2).toList());

        Collections.shuffle(level1Cards);
        Collections.shuffle(level2Cards);

        littleVisibleDecks.forEach(l -> composeLittleDecks(level1Cards, level2Cards, l));
        composeLittleDecks(level1Cards, level2Cards, littleNotVisibleDeck);
    }

    /**
     * Composes a small deck with a predefined number of level 1 and level 2 cards.
     *
     * @param level1Cards The list of available level 1 cards.
     * @param level2Cards The list of available level 2 cards.
     * @param littleDeck The deck to be populated with the cards.
     */
    public void composeLittleDecks(List<AdventureCard> level1Cards, List<AdventureCard> level2Cards, List<AdventureCard> littleDeck) {
        littleDeck.add(level1Cards.removeFirst());
        littleDeck.add(level2Cards.removeFirst());
        littleDeck.add(level2Cards.removeFirst());
    }

    /**
     * Loads all adventure cards into the deck from their respective JSON files.
     * The deck is populated with the following types of adventure cards:
     * - Abandoned Ships
     * - Abandoned Stations
     * - Free Space
     * - Meteorite Storms
     * - Pirates
     * - Planets
     * - Slave Traders
     * - Smugglers
     * - Stardust
     * - War Fields
     */
    public void loadCards() {
        allCards.addAll(Deck.loadAbandonedShipFromJson());
        allCards.addAll(Deck.loadAbandonedStationFromJson());
        allCards.addAll(Deck.loadFreeSpaceFromJson());
        allCards.addAll(Deck.loadMeteoriteStormFromJson());
        allCards.addAll(Deck.loadPiratesFromJson());
        allCards.addAll(Deck.loadPlanetsFromJson());
        allCards.addAll(Deck.loadSlaveTradersFromJson());
        allCards.addAll(Deck.loadSmugglersFromJson());
        allCards.addAll(Deck.loadStardustFromJson());
        //TODO allCards.addAll(Deck.loadWarFieldFromJson());
    }

    /**
     * Loads a list of objects from a JSON file.
     * This method is generic and can be used to load any type of adventure card.
     *
     * @param fileName The path to the JSON file.
     * @param type The class type of the objects to be deserialized.
     * @return A list of objects of the specified type.
     */
    private static <T> List<T> loadFromJson(String fileName, Class<T> type) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> objects = new ArrayList<>();

        try {
            ClassLoader classLoader = Deck.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new FileNotFoundException("File not found: " + fileName);
            }

            JsonNode rootNode = objectMapper.readTree(inputStream);

            for (JsonNode node : rootNode) {
                T obj = objectMapper.treeToValue(node, type);
                objects.add(obj);
            }

        } catch (IOException e) {
            Logger.getLogger(Deck.class.getName()).log(Level.SEVERE, "Error loading JSON file: " + fileName, e);
        }

        return objects;
    }

    /**
     * Loads a list of Abandoned Ship cards from the JSON file.
     *
     * @return A list of AbandonedShip objects.
     */
    private static List<AbandonedShip> loadAbandonedShipFromJson() {
        return loadFromJson("AbandonedShip.json", AbandonedShip.class);
    }

    /**
     * Loads a list of Abandoned Station cards from the JSON file.
     *
     * @return A list of AbandonedStation objects.
     */
    private static List<AbandonedStation> loadAbandonedStationFromJson() {
        return loadFromJson("AbandonedStation.json", AbandonedStation.class);
    }

    /**
     * Loads a list of Free Space cards from the JSON file.
     *
     * @return A list of FreeSpace objects.
     */
    private static List<FreeSpace> loadFreeSpaceFromJson() {
        return loadFromJson("FreeSpace.json", FreeSpace.class);
    }

    /**
     * Loads a list of Meteorite Storm cards from the JSON file.
     *
     * @return A list of MeteoriteStorm objects.
     */
    private static List<MeteoriteStorm> loadMeteoriteStormFromJson() {

        List<MeteoriteStorm> meteoriteStorms = new ArrayList<>(loadFromJson("MeteoriteStorm.json", MeteoriteStorm.class));
        meteoriteStorms.forEach(MeteoriteStorm::convertIdsToMeteorites);
        return meteoriteStorms;
    }

    /**
     * Loads a list of Pirates cards from the JSON file.
     *
     * @return A list of Pirates objects.
     */
    private static List<Pirates> loadPiratesFromJson() {
        List<Pirates> pirates = new ArrayList<>(loadFromJson("Pirates.json", Pirates.class));
        pirates.forEach(Pirates::convertIdsToShots);
        return pirates;
    }

    /**
     * Loads a list of Planets cards from the JSON file.
     *
     * @return A list of Planets objects.
     */
    private static List<Planets> loadPlanetsFromJson() {
        return loadFromJson("Planets.json", Planets.class);
    }

    /**
     * Loads a list of Slave Traders cards from the JSON file.
     *
     * @return A list of SlaveTraders objects.
     */
    private static List<SlaveTraders> loadSlaveTradersFromJson() {
        return loadFromJson("SlaveTraders.json", SlaveTraders.class);
    }

    /**
     * Loads a list of Smugglers cards from the JSON file.
     *
     * @return A list of Smugglers objects.
     */
    private static List<Smugglers> loadSmugglersFromJson() {
        return loadFromJson("Smugglers.json", Smugglers.class);
    }

    /**
     * Loads a list of Stardust cards from the JSON file.
     *
     * @return A list of Stardust objects.
     */
    private static List<Stardust> loadStardustFromJson() {
        return loadFromJson("Stardust.json", Stardust.class);
    }

    /**
     * Loads a list of War Field cards from the JSON file.
     *
     * @return A list of WarField objects.
     */
    private static List<WarField> loadWarFieldFromJson() {
        return loadFromJson("WarField.json", WarField.class);
    }

    public List<AdventureCard> getAllCards() {
        return allCards;
    }

}