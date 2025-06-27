package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AdventureCard {

    /**
     * Represents the name of the AdventureCard.
     * This field stores the unique identifier or name associated with the card,
     * which may be used for identification, display, or game-related operations.
     */
    protected String cardName;
    /**
     * Represents the level of the adventure card, which can be used to determine its rank,
     * difficulty, or progression step within the game's logic.
     */
    protected int level;
    /**
     * Represents the current state of the AdventureCard in its lifecycle.
     * The state defines what action or phase the card is in during the game.
     * This variable is an instance of the {@link CardState} enumeration, which
     * contains various possible states such as START_CARD, CHOOSE_CANNONS,
     * EPIDEMIC, END_OF_CARD, and more.
     *
     * This variable is intended to be modified by the card's state transitions
     * during gameplay. The current state influences the behavior of the card
     * and the available actions for the player.
     *
     * The {@code currState} serves as a key to control the flow of gameplay
     * related to this AdventureCard.
     */
    protected CardState currState;
    /**
     * Represents the game model associated with the AdventureCard.
     * This variable links the card to the broader game logic and state,
     * providing necessary context and integration with the overall game functionality.
     */
    protected GameModel gameModel;
    /**
     * The name of the image associated with this AdventureCard.
     * This variable holds the filename or unique identifier for the image representation
     * that can be displayed in the game's user interface or other visual elements.
     */
    protected String imageName;
    /**
     * Indicates whether the AdventureCard is designated as a test flight card.
     * This boolean flag can be used to differentiate specific functionality
     * or characteristics associated with test flight cards.
     */
    protected boolean isTestFlightCard;

    /**
     * A static map that associates string identifiers with factory functions
     * for creating specific types of {@link Meteorite} objects.
     * <p>
     * This is used by cards that contain meteorites, allowing them to dynamically
     * instantiate the appropriate meteorite object based on a given ID (e.g., "big_north").
     */
    protected static Map<String, Callable<Meteorite>> meteoriteCreator = Map.of(
            "big_north", () -> new BigMeteorite(Direction.NORTH),
            "big_east", () -> new BigMeteorite(Direction.EAST),
            "big_west", () -> new BigMeteorite(Direction.WEST),
            "big_south", () -> new BigMeteorite(Direction.SOUTH),
            "small_north", () -> new SmallMeteorite(Direction.NORTH),
            "small_south", () -> new SmallMeteorite(Direction.SOUTH),
            "small_west", () -> new SmallMeteorite(Direction.WEST),
            "small_east", () -> new SmallMeteorite(Direction.EAST)
    );

    /**
     * A static map that associates string identifiers with factory functions
     * for creating specific types of {@link Shot} objects.
     * <p>
     * This is used by cards that involve shots, allowing them to dynamically
     * instantiate the correct shot object based on a given ID (e.g., "small_east").
     */
    protected static Map<String, Callable<Shot>> shotCreator = Map.of(
            "big_north", () -> new BigShot(Direction.NORTH),
            "big_east", () -> new BigShot(Direction.EAST),
            "big_west", () -> new BigShot(Direction.WEST),
            "big_south", () -> new BigShot(Direction.SOUTH),
            "small_north", () -> new SmallShot(Direction.NORTH),
            "small_south", () -> new SmallShot(Direction.SOUTH),
            "small_west", () -> new SmallShot(Direction.WEST),
            "small_east", () -> new SmallShot(Direction.EAST)
    );

    /**
     * Sets the name of the image associated with this AdventureCard.
     *
     * @param imageName the name of the image to set
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     * Retrieves the name of the image associated with the card.
     *
     * @return the name of the image as a String
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Sets the level of the AdventureCard.
     *
     * @param level the new level to be set for the AdventureCard
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Retrieves the name of the card.
     *
     * @return the card name as a String
     */
    public String getCardName() {
        return cardName;
    }

    /**
     * Retrieves the level of the AdventureCard.
     *
     * @return the current level of the card
     */
    public int getLevel() {
        return level;
    }

    /**
     * Determines if the card is a Test Flight card.
     *
     * @return true if the card is a Test Flight card, false otherwise.
     */
    public boolean isTestFlightCard() {
        return isTestFlightCard;
    }

    /**
     * Sets the flag indicating whether this card is a Test Flight card.
     *
     * @param testFlightCard a boolean value representing whether the card is a Test Flight card.
     *                       True if it is; false otherwise.
     */
    public void setIsTestFlightCard(boolean testFlightCard) {
        isTestFlightCard = testFlightCard;
    }

    /**
     * Retrieves the first state of the card.
     *
     * @return the initial {@code CardState} of the card.
     */
    @JsonIgnore
    public abstract CardState getFirstState();

    /**
     * Updates the current state of the card and notifies all connected clients about the state change.
     *
     * @param currState the new state to set for this card.
     */
    @JsonIgnore
    public void setCurrState(CardState currState)  {
        this.currState = currState;

        try {
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCardState(nicknameToNotify, currState);
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the current state of the card.
     *
     * @return the current state of the card as an instance of {@code CardState}.
     */
    @JsonIgnore
    public CardState getCurrState() {
        return currState;
    }

    /**
     * Sets the game model for this instance.
     *
     * @param gameModel the GameModel object to be set
     */
    public void setGame(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    /**
     * Executes the gameplay logic associated with this AdventureCard. The specific implementation
     * of the gameplay will depend on the subclass and its unique behavior.
     *
     * @param playerChoices the data structure containing the player's choices and interactions
     *                       necessary for executing the card's gameplay logic.
     * @throws UnknownStateException if the card's current state cannot be determined or is invalid
     *                                during the execution of the method.
     */
    public abstract void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException;

    /**
     * Converts the current instance of an AdventureCard into its corresponding ClientCard representation.
     * This method is intended to provide a client-side representation of the AdventureCard,
     * including necessary details for rendering or interacting with the card in a user interface.
     *
     * @return a ClientCard instance representing the client-side equivalent of the current AdventureCard.
     */
    public abstract ClientCard toClientCard();

    /**
     * Sets the name of the card.
     *
     * @param cardName the name to set for the card
     */
    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

}

