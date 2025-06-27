package it.polimi.ingsw.is25am33.model.card;


import java.util.*;
import java.io.InputStream;
import java.io.FileNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Represents a deck of adventure cards used in the gameModel.
 * It manages different levels of cards, organizes them into decks,
 * and provides methods for drawing and shuffling.
 */
public class Deck {

    /**
     * A stack containing all adventure cards available in the game. This stack serves
     * as the primary collection of cards, loaded during setup, and is used to populate
     * various decks throughout the game.
     *
     * This field is initialized as a {@link Stack}, allowing for operations such as
     * shuffling, drawing, and organizing cards according to game rules. The cards
     * are instances of {@code AdventureCard} and represent distinct scenarios or events
     * within the game.
     *
     * The content of this stack is managed through various methods in the {@code Deck}
     * class, such as methods for loading cards, creating decks, and retrieving specific
     * subsets of cards.
     *
     * This list is immutable once initialized to ensure consistent management of the
     * game's adventure card collection.
     */
    private final List<AdventureCard> allCards = new Stack<>();
    /**
     * Represents a collection of "little" visible decks in the game, where each deck is a
     * list of adventure cards. Each inner list within this structure represents a distinct
     * subset of visible cards in the game. These decks are organized for specific purposes
     * in the game's mechanics and are initialized with three empty decks.
     *
     * This variable is immutable and meant to maintain a reference to the game's current
     * state of visible "little" decks, ensuring their consistent accessibility during play.
     * The decks are often manipulated and populated using various class methods, reflecting
     * game state changes as players interact with the system.
     */
    private final List<List<AdventureCard>> littleVisibleDecks = new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    /**
     * Represents a private deck of non-visible adventure cards within the game's little decks.
     * This deck consists of a subset of cards intentionally hidden from the players during gameplay.
     * It is used for managing the non-visible state of the little deck system in the game.
     */
    private final List<AdventureCard> littleNotVisibleDeck = new ArrayList<>();
    /**
     * Stores the string representations of the little visible decks.
     * Each inner list corresponds to a specific deck, and the strings
     * represent the cards within those decks. This variable is populated
     * to avoid transferring full card objects over the network, instead
     * providing a lightweight representation for communication purposes.
     */
    private final List<List<String>> littleVisibleDecksString = new ArrayList<>();
    /**
     * Represents the main game deck that holds a collection of adventure cards.
     * This deck is used in gameplay to draw and manage cards as part of the game flow.
     * The stack structure enforces a Last-In-First-Out (LIFO) mechanism, allowing cards
     * to be drawn from the top and added to the bottom.
     *
     * The deck is populated and managed through various methods of the containing class.
     * It can be shuffled, cards can be drawn based on specific game logic, and its state
     * can be queried or modified depending on the current stage of the game.
     */
    private final Stack<AdventureCard> gameDeck = new Stack<>();
    /**
     * A notifier instance responsible for handling communication and updates
     * between the game system and its clients during gameplay events.
     *
     * This component is used to facilitate notifications about game state
     * changes, ensuring that all connected clients remain synchronized and
     * informed of important updates that occur during the game's progression.
     */
    private GameClientNotifier gameClientNotifier;
    /**
     * Tracks the availability status of the "little decks" in the game.
     * Each element in the list corresponds to a specific "little deck". If the value
     * is {@code true}, the respective deck is free and available for use. If the value
     * is {@code false}, the deck is currently occupied or unavailable.
     *
     * This list is initialized with every "little deck" marked as available.
     * Updates to the list ensure synchronization with the game's state regarding
     * the availability of these decks.
     */
    private final List<Boolean> isLittleDeckFree = new ArrayList<>(List.of(true, true, true));
    /**
     * Represents whether the first card in the game deck has been drawn.
     * This variable is used to track the game state and determine if
     * specific actions or rules should apply during the card drawing process.
     *
     * Initially set to {@code false}, it is updated to {@code true} once
     * the first card is drawn from the game deck.
     */
    private boolean isFirstCardDrawn = false;
    /**
     * A boolean flag indicating whether the game is in test flight mode.
     * Test flight mode alters the behavior of certain game functionalities,
     * such as card loading and initial card drawing, to facilitate testing
     * or specific game scenarios.
     */
    private boolean isTestFlight;

    public void setGameClientNotifier(GameClientNotifier gameClientNotifier){
        this.gameClientNotifier = gameClientNotifier;
    }

    /**
     * Creates a game deck by combining specific card sets and shuffling them.
     * Depending on the provided parameter, this method determines whether to
     * include all cards or a subset of decks in the main game deck.
     *
     * @param isTestFlight A boolean value indicating whether all cards should
     *                     be added to the game deck (true) or only specific
     *                     "little" decks (false).
     */
    public void createGameDeck(boolean isTestFlight) {

        if (isTestFlight)
            gameDeck.addAll(allCards);
        else {
            gameDeck.addAll(littleNotVisibleDeck);
            littleVisibleDecks.forEach(gameDeck::addAll);
        }

        Collections.shuffle(gameDeck);
    }

    /**
     * Draws a card from the game deck with specific rules based on game state and player count.
     * <p>
     * First draw: Returns a level 2 card (level 1 in test mode) and sets it as START_CARD.
     * Single player: Cannot draw WarField cards.
     * Multiplayer: Draw any card from the deck.
     *
     * @param inGamePlayers The number of players in the game
     * @return The drawn AdventureCard with a state set to START_CARD
     * @throws EmptyStackException if the game deck is empty
     */
    public AdventureCard drawCard(int inGamePlayers) throws EmptyStackException {

        if (!isFirstCardDrawn) {
            isFirstCardDrawn = true;
            AdventureCard firstCard = gameDeck.pop();
            int cardLevel = firstCard.getLevel();

            while (cardLevel != (isTestFlight ? 1 : 2)) {
                gameDeck.push(firstCard);
                Collections.shuffle(gameDeck);
                firstCard = gameDeck.pop();
                cardLevel = firstCard.getLevel();
            }

            firstCard.setCurrState(CardState.START_CARD);
            return firstCard;
        }

        AdventureCard card;

        if (inGamePlayers == 1) {
            do {
                card = gameDeck.pop();
            } while (card instanceof WarField);
        } else {
            card = gameDeck.pop();
        }

        card.setCurrState(CardState.START_CARD);
        return card;
    }

    /**
     * Sets up the little decks for the given game model. This involves loading all adventure cards,
     * organizing them based on their levels, shuffling, populating the visible and non-visible little decks,
     * and notifying all clients of the current state of the little visible decks.
     *
     * @param gameModel The game model instance with which the little decks will be associated.
     */
    public void setUpLittleDecks(GameModel gameModel) {

        if (gameModel.isTestFlight()) {
            isTestFlight = true;
            loadCards(true);
            allCards.forEach(adventureCard -> adventureCard.setGame(gameModel));
            return;
        }

        isTestFlight = false;
        loadCards(false);
        allCards.forEach(adventureCard -> adventureCard.setGame(gameModel));
        List<AdventureCard> level1Cards = new ArrayList<>(allCards.stream().filter(c -> c.getLevel() == 1).collect(Collectors.toList()));
        List<AdventureCard> level2Cards = new ArrayList<>(allCards.stream().filter(c -> c.getLevel() == 2).collect(Collectors.toList()));

        Collections.shuffle(level1Cards);
        Collections.shuffle(level2Cards);

        try {
            littleVisibleDecks.forEach(littleDeck -> composeLittleDecks(level1Cards, level2Cards, littleDeck));
            composeLittleDecks(level1Cards, level2Cards, littleNotVisibleDeck);
        } catch (NoSuchElementException e) {
            System.out.println("Not enough cards");
            throw new NoSuchElementException();
        }

        List<List<ClientCard>> littleVisibleClientCardsDecks = littleVisibleDecks.stream()
                .map(innerList -> innerList.stream()
                        .map(AdventureCard::toClientCard)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        mapLittleDecksToString();

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyVisibleDeck(nicknameToNotify, littleVisibleClientCardsDecks);
        });

    }

    /**
     * Maps the content of the little visible decks to their string representations.
     * This method initializes the string representation lists for the little visible decks
     * and iterates over each card in the decks, converting them into their string form
     * using the {@code toString} method of the {@code AdventureCard} class.
     * The resulting string representations are stored in the {@code littleVisibleDecksString}.
     * All of this is done to avoid sending the whole {@code AdventureCard} instances over RMI.
     */
    private void mapLittleDecksToString() {

        IntStream.range(0, 3).forEach(_ -> littleVisibleDecksString.add(new ArrayList<>()));

        IntStream.range(0, 3).forEach(littleDeckIndex -> {
            IntStream.range(0, littleVisibleDecks.get(littleDeckIndex).size()).forEach(cardIndex -> {
                littleVisibleDecksString.get(littleDeckIndex).add(littleVisibleDecks.get(littleDeckIndex).get(cardIndex).toString());
            });
        });

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
     * Loads a set of adventure cards into the game, optionally filtering them based on the test flight flag.
     *
     * @param isTestFlight A boolean value indicating whether to load only test flight cards (if true)
     *                     or all available cards (if false).
     */
    private void loadCards(boolean isTestFlight) {

        List<AdventureCard> cards = new ArrayList<>();

        cards.addAll(Deck.loadAbandonedShipFromJson());
        cards.addAll(Deck.loadAbandonedStationFromJson());
        cards.addAll(Deck.loadFreeSpaceFromJson());
        cards.addAll(Deck.loadPiratesFromJson());
        cards.addAll(Deck.loadSlaveTradersFromJson());
        cards.addAll(Deck.loadSmugglersFromJson());
        cards.addAll(Deck.loadStardustFromJson());
        cards.addAll(Deck.loadWarFieldFromJson());
        cards.addAll(Deck.loadMeteoriteStormFromJson());
        cards.addAll(Deck.loadEpidemicFromJson());
        cards.addAll(Deck.loadPlanetsFromJson());

        cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());cards.addAll(Deck.loadSlaveTradersFromJson());

        if (isTestFlight)
            allCards.addAll(cards.stream().filter(AdventureCard::isTestFlightCard).toList());
        else
            allCards.addAll(cards);
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

    private static List<Epidemic> loadEpidemicFromJson() {
        return loadFromJson("Epidemy.json", Epidemic.class);
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
        List<WarField> warFields = new ArrayList<>(loadFromJson("WarField.json", WarField.class));
        warFields.forEach(WarField::convertIdsToShots);
        return warFields;
    }

    /**
     * Retrieves all the adventure cards available in the deck.
     *
     * @return a list containing all the AdventureCard objects in the deck.
     */
    public List<AdventureCard> getAllCards() {
        return allCards;
    }

    /**
     * Checks if a specified little deck is available to use and marks it as unavailable if it is free.
     * This method is thread-safe and ensures atomic updates to the little deck's availability state.
     *
     * @param littleDeckChoice The index (1-based) of the little deck to check for availability.
     * @return {@code true} if the specified little deck is available and has been marked as unavailable,
     *         {@code false} otherwise.
     */
    public boolean isLittleDeckAvailable(int littleDeckChoice) {
        synchronized (isLittleDeckFree) {
            if (!isLittleDeckFree.get(littleDeckChoice - 1)) return false;
            isLittleDeckFree.set(littleDeckChoice - 1, false);
            return true;
        }
    }

    /**
     * Marks the specified little deck as available for future use by updating its state in the
     * list that tracks the availability of little decks.
     *
     * @param littleDeckChoice The index of the little deck to be marked as free. The index value
     *                         should be greater than or equal to 1.
     */
    public void releaseLittleDeck(int littleDeckChoice) {
        synchronized (isLittleDeckFree) {
            isLittleDeckFree.set(littleDeckChoice - 1, true);
        }
    }

    /**
     * Retrieves the little visible decks, which consist of organized and accessible lists
     * of adventure cards that are part of the game's current state. The little visible
     * decks represent subsets of adventure cards that are intentionally visible to players
     * within the game context.
     *
     * @return A list of lists containing adventure cards, where each inner list represents
     *         a specific subset of visible adventure cards in the game's little decks.
     */
    public List<List<AdventureCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    /**
     * Retrieves the "little" non-visible deck of adventure cards.
     * This deck contains a subset of cards that are not currently visible in the playable game state.
     *
     * @return A list of {@link AdventureCard} objects representing the little non-visible deck.
     */
    public List<AdventureCard> getLittleNotVisibleDeck() {
        return littleNotVisibleDeck;
    }

    /**
     * Retrieves the string representations of the little visible decks.
     * This method returns a list of lists containing the string representation
     * of cards in the little visible decks. The representation is pre-mapped
     * to avoid transferring full card objects over the network.
     *
     * @return A list of lists of strings, where each inner list represents a deck.
     */
    public List<List<String>> getLittleVisibleDecksString() {
        return littleVisibleDecksString;
    }

    /**
     * Skips all cards in the game deck except the last one. This method retains
     * only the topmost card in the deck, removing all others.
     *
     * @throws IllegalStateException if the game deck contains only one or no cards.
     */
    public void skipToLastCard() {
        if (gameDeck.size() <= 1) {
            throw new IllegalStateException("Cannot skip cards: only one or no cards remaining in deck");
        }
        AdventureCard lastCard = gameDeck.pop();
        gameDeck.clear();
        gameDeck.push(lastCard);
    }

    /**
     * Checks if the game deck is empty, indicating that all cards have been finished.
     *
     * @return true if the game deck is empty, false otherwise
     */
    public boolean hasFinishedCards(){
        return gameDeck.isEmpty();
    }
}