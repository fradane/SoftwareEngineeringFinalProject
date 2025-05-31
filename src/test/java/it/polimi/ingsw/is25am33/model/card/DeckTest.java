package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.ThrowingBiConsumer;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked") // Reflection is used to access private fields, which is not recommended in production code.
class DeckTest {

    private Deck deck;
    private GameModel dummyGameModel;

    @BeforeEach
    void setUp() {
        deck = new Deck();
        dummyGameModel = new GameModel(null, 2, false);
        GameClientNotifier dummyGameClientNotifier = new GameClientNotifier(null,null) {
            @Override
            public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {}
        };
        deck.setGameContext(dummyGameClientNotifier);
        dummyGameModel.setGameContext(dummyGameClientNotifier);
    }

    @Test
    void testConstructorInitializesAllFieldsCorrectly() {
        Deck localDeck = new Deck();

        // allCards should be empty
        assertNotNull(localDeck.getAllCards());
        assertTrue(localDeck.getAllCards().isEmpty(), "allCards should be empty");

        // Access private fields using reflection to test internal state
        try {
            var littleVisibleDecksField = Deck.class.getDeclaredField("littleVisibleDecks");
            var littleNotVisibleDeckField = Deck.class.getDeclaredField("littleNotVisibleDeck");
            var littleVisibleDecksStringField = Deck.class.getDeclaredField("littleVisibleDecksString");
            var gameDeckField = Deck.class.getDeclaredField("gameDeck");
            var isLittleDeckFreeField = Deck.class.getDeclaredField("isLittleDeckFree");

            littleVisibleDecksField.setAccessible(true);
            littleNotVisibleDeckField.setAccessible(true);
            littleVisibleDecksStringField.setAccessible(true);
            gameDeckField.setAccessible(true);
            isLittleDeckFreeField.setAccessible(true);

            List<List<?>> littleVisibleDecks = (List<List<?>>) littleVisibleDecksField.get(localDeck);
            List<?> littleNotVisibleDeck = (List<?>) littleNotVisibleDeckField.get(localDeck);
            List<List<String>> littleVisibleDecksString = (List<List<String>>) littleVisibleDecksStringField.get(localDeck);
            List<?> gameDeck = (List<?>) gameDeckField.get(localDeck);
            List<Boolean> isLittleDeckFree = (List<Boolean>) isLittleDeckFreeField.get(localDeck);

            assertEquals(3, littleVisibleDecks.size(), "Should have 3 little visible decks");
            littleVisibleDecks.forEach(list -> assertTrue(list.isEmpty(), "Each little visible deck should be empty"));

            assertNotNull(littleNotVisibleDeck);
            assertTrue(littleNotVisibleDeck.isEmpty(), "littleNotVisibleDeck should be empty");

            assertNotNull(littleVisibleDecksString);
            assertTrue(littleVisibleDecksString.isEmpty(), "littleVisibleDecksString should be empty");

            assertNotNull(gameDeck);
            assertTrue(gameDeck.isEmpty(), "gameDeck should be empty");

            assertEquals(List.of(true, true, true), isLittleDeckFree, "All little decks should be marked as free");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection access failed: " + e.getMessage());
        }
    }

    @Test
    void testSetUpLittleDecksInitializesDecksCorrectly() {

        // Execute method to test
        deck.setUpLittleDecks(dummyGameModel);

        // Check that all littleVisibleDecks contain 3 cards
        assertEquals(3, deck.getLittleVisibleDecks().size());
        deck.getLittleVisibleDecks().forEach(deckList -> assertEquals(3, deckList.size()));

        // Check that the not visible deck contains 3 cards
        assertEquals(3, deck.getLittleNotVisibleDeck().size());

        // Check that littleVisibleDecksString has been populated
        assertEquals(3, deck.getLittleVisibleDecksString().size());
        deck.getLittleVisibleDecksString().forEach(list -> assertFalse(list.isEmpty()));

        // Check that each string deck contains 3 elements
        deck.getLittleVisibleDecksString().forEach(list -> assertEquals(3, list.size(), "Each string deck must contain 3 elements"));

        // Check correspondence between cards and their string representations
        for (int i = 0; i < 3; i++) {
            List<?> cardDeck = deck.getLittleVisibleDecks().get(i);
            List<String> stringDeck = deck.getLittleVisibleDecksString().get(i);
            for (int j = 0; j < 3; j++) {
                assertEquals(cardDeck.get(j).toString(), stringDeck.get(j), "The string representation of the card must match");
            }
        }

    }

    @Test
    void testMergeIntoGameDeckShufflesAndCombinesCards() {

        // Prepare the decks
        deck.setUpLittleDecks(dummyGameModel);

        int expectedTotalCards = 3 * 4; // 3 visible decks and 1 non-visible deck, each with 3 cards

        // Act
        deck.createGameDeck(false);

        // Check
        List<AdventureCard> gameDeckField = null;
        try {
            var gameDeckReflect = Deck.class.getDeclaredField("gameDeck");
            gameDeckReflect.setAccessible(true);
            gameDeckField = (List<AdventureCard>) gameDeckReflect.get(deck);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection access failed: " + e.getMessage());
        }

        assertNotNull(gameDeckField, "gameDeck should not be null after merging");
        assertEquals(expectedTotalCards, gameDeckField.size(), "gameDeck should contain all cards from little decks");

        // Ensure cards are from the original decks
        List<AdventureCard> originalCards = deck.getLittleNotVisibleDeck();
        deck.getLittleVisibleDecks().forEach(originalCards::addAll);
        assertTrue(gameDeckField.containsAll(originalCards), "gameDeck should contain all cards from visible and non-visible decks");
    }

    @Test
    void testDrawCardFromGameDeck() {

        // Prepare the decks
        deck.setUpLittleDecks(dummyGameModel);
        deck.createGameDeck(false);

        // Draw a card
        AdventureCard drawnCard = deck.drawCard();

        assertNotNull(drawnCard, "Drawn card should not be null");
        // Ensure the card's state was set correctly
        assertEquals(CardState.START_CARD, drawnCard.getCurrState(), "Card state should be START_CARD");

        // Check that the size of the game deck decreased by one
        List<AdventureCard> gameDeckField = null;
        try {
            var gameDeckReflect = Deck.class.getDeclaredField("gameDeck");
            gameDeckReflect.setAccessible(true);
            gameDeckField = (List<AdventureCard>) gameDeckReflect.get(deck);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection access failed: " + e.getMessage());
        }

        assertNotNull(gameDeckField);
        assertEquals(11, gameDeckField.size(), "After drawing, the game deck should have one less card");
    }

    @Test
    void testDrawCardThrowsExceptionWhenDeckIsEmpty() {

        // Prepare the decks and empty the game deck manually
        deck.setUpLittleDecks(dummyGameModel);
        deck.createGameDeck(false);

        // Draw all cards
        for (int i = 0; i < 12; i++) {
            deck.drawCard();
        }

        // Now the deck should be empty; drawing one more should throw EmptyStackException
        assertThrows(java.util.EmptyStackException.class, deck::drawCard, "Expected EmptyStackException when drawing from an empty deck");
    }



    @Test
    void testIsLittleDeckAvailableAndRelease() {
        // Initially, all decks should be available
        assertTrue(deck.isLittleDeckAvailable(1), "Deck 1 should be available initially");
        assertFalse(deck.isLittleDeckAvailable(1), "Deck 1 should not be available after being claimed");

        // Release deck 1 and check again
        deck.releaseLittleDeck(1);
        assertTrue(deck.isLittleDeckAvailable(1), "Deck 1 should be available again after release");
        deck.releaseLittleDeck(1); // Release again, should not throw an exception

        // Test all decks
        for (int i = 1; i <= 3; i++) {
            assertTrue(deck.isLittleDeckAvailable(i), "Deck " + i + " should be available initially");
            assertFalse(deck.isLittleDeckAvailable(i), "Deck " + i + " should not be available after being claimed");
            deck.releaseLittleDeck(i);
            assertTrue(deck.isLittleDeckAvailable(i), "Deck " + i + " should be available again after release");
            deck.releaseLittleDeck(i);
        }

        // Test that requesting one deck does not affect another
        assertTrue(deck.isLittleDeckAvailable(1), "Deck 1 should be available");
        assertFalse(deck.isLittleDeckAvailable(1), "Deck 1 should not be available after being claimed");

        // Deck 2 should still be available
        assertTrue(deck.isLittleDeckAvailable(2), "Deck 2 should be available even if Deck 1 was claimed");
        assertFalse(deck.isLittleDeckAvailable(2), "Deck 2 should not be available after being claimed");
    }

    @Test
    void testSetUpLittleDecksWithTestFlightTrueSkipsSetup() {
        GameModel testFlightGameModel = new GameModel(null, 2, true); // isTestFlight = true
        deck.setUpLittleDecks(testFlightGameModel);

        // Since isTestFlight is true, little decks should remain empty
        assertTrue(deck.getLittleVisibleDecks().stream().allMatch(List::isEmpty), "All little visible decks should be empty for test flight");
        assertTrue(deck.getLittleNotVisibleDeck().isEmpty(), "Little not visible deck should be empty for test flight");
        assertTrue(deck.getLittleVisibleDecksString().isEmpty(), "Little visible decks string should be empty for test flight");
    }

    @Test
    void testCreateGameDeckWithTestFlightAddsAllCards() {
        GameModel testFlightGameModel = new GameModel(null, 2, true); // isTestFlight = true
        deck.setGameContext(dummyGameModel.getGameContext());
        deck.setUpLittleDecks(testFlightGameModel);
        deck.createGameDeck(true); // Force add all loaded cards

        // The game deck should contain all cards loaded
        List<AdventureCard> allCards = deck.getAllCards();
        assertFalse(allCards.isEmpty(), "All cards should be loaded for test flight");

        List<AdventureCard> gameDeckCards;
        try {
            var gameDeckField = Deck.class.getDeclaredField("gameDeck");
            gameDeckField.setAccessible(true);
            gameDeckCards = (List<AdventureCard>) gameDeckField.get(deck);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection access failed: " + e.getMessage());
            return;
        }

        assertEquals(allCards.size(), gameDeckCards.size(), "Game deck should contain all loaded cards in test flight mode");
        assertTrue(gameDeckCards.containsAll(allCards), "Game deck should contain exactly all loaded cards");
    }

}


