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

    private final List<AdventureCard> allCards = new Stack<>();
    private final List<List<AdventureCard>> littleVisibleDecks = new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    private final List<AdventureCard> littleNotVisibleDeck = new ArrayList<>();
    private final List<List<String>> littleVisibleDecksString = new ArrayList<>();
    private final Stack<AdventureCard> gameDeck = new Stack<>();
    private GameClientNotifier gameClientNotifier;
    private final List<Boolean> isLittleDeckFree = new ArrayList<>(List.of(true, true, true));
    private boolean isFirstCardDrawn = false;
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

    public List<AdventureCard> getAllCards() {
        return allCards;
    }

    public boolean isLittleDeckAvailable(int littleDeckChoice) {
        synchronized (isLittleDeckFree) {
            if (!isLittleDeckFree.get(littleDeckChoice - 1)) return false;
            isLittleDeckFree.set(littleDeckChoice - 1, false);
            return true;
        }
    }

    public void releaseLittleDeck(int littleDeckChoice) {
        synchronized (isLittleDeckFree) {
            isLittleDeckFree.set(littleDeckChoice - 1, true);
        }
    }

    public List<List<AdventureCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    public List<AdventureCard> getLittleNotVisibleDeck() {
        return littleNotVisibleDeck;
    }

    public List<List<String>> getLittleVisibleDecksString() {
        return littleVisibleDecksString;
    }

    public void skipToLastCard() {
        if (gameDeck.size() <= 1) {
            throw new IllegalStateException("Cannot skip cards: only one or no cards remaining in deck");
        }
        AdventureCard lastCard = gameDeck.pop();
        gameDeck.clear();
        gameDeck.push(lastCard);
    }

    public boolean hasFinishedCards(){
        return gameDeck.isEmpty();
    }
}