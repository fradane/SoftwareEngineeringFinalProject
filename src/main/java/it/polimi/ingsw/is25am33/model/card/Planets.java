package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CubesRedistributionHandler;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Planets extends AdventureCard implements PlayerMover, CubesRedistributionHandler {
    /**
     * Represents the list of planets that are currently available for selection or interaction.
     * Each element in this list is an instance of the {@link Planet} class, which represents
     * a single planet with associated attributes and operations. The availability of planets
     * may change depending on specific game dynamics or interactions.
     */
    private List<Planet> availablePlanets;
    /**
     * Represents a mapping between player identifiers and the corresponding planets they are associated with.
     * Each entry in the map consists of a unique player identifier as the key (String)
     * and a {@link Planet} object as the value.
     *
     * This map is implemented as a thread-safe {@link ConcurrentHashMap}, ensuring safe
     * concurrent access and modification in a multi-threaded environment.
     *
     * Key Characteristics:
     * - The key is a {@code String} that uniquely identifies a player.
     * - The value is an instance of the {@code Planet} class, representing the associated planet.
     *
     * The map is final and cannot be reassigned, ensuring the integrity of its reference.
     */
    private final Map<String, Planet> playerPlanet = new ConcurrentHashMap<>();
    /**
     * Represents the number of steps the player wishes to move back in the current game state.
     * This field tracks backward movements based on player input or game logic.
     */
    private int stepsBack;
    /**
     * A list of predefined card states specific to the Planets card.
     * This list defines the sequence or possible states during the execution of the card's logic.
     * In this case, the card can transition through the specified states:
     * CHOOSE_PLANET and HANDLE_CUBES_REWARD.
     */
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_PLANET, CardState.HANDLE_CUBES_REWARD);
    /**
     * Represents the current planet associated with the ongoing game or player's state.
     * The `currentPlanet` variable holds a reference to a {@link Planet} object,
     * signifying the planet currently being interacted with or focused on.
     * This may serve as a key element during gameplay mechanics, such as traveling,
     * resource acquisition, or implementing specific actions related to the planet.
     */
    private Planet currentPlanet;

    /**
     * Constructs an instance of the Planets class. The constructor initializes
     * the cardName property to the class name of the instance.
     */
    public Planets() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Sets the number of steps the player has chosen to move backward.
     *
     * @param stepsBack the number of steps to move back. Must be a non-negative integer representing the steps to rewind.
     */
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    /**
     * Sets the list of available planets.
     *
     * @param availablePlanets a list of Planet objects to be set as the available planets
     */
    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    /**
     * Retrieves the list of available planets.
     *
     * @return a list of Planet objects that are currently available.
     */
    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    /**
     * Retrieves the number of steps back.
     *
     * @return the number of steps back as an integer.
     */
    public int getStepsBack() {
        return stepsBack;
    }

    /**
     * Retrieves the mapping of player identifiers to their respective assigned planets.
     *
     * @return a Map where the keys are Strings representing player identifiers and the values
     *         are Planet objects representing the planets assigned to those players.
     */
    public Map<String, Planet> getPlayerPlanet() {
        return playerPlanet;
    }

    /**
     * Sets the current planet for the player or game context.
     *
     * @param planet the Planet object to be set as the current planet
     */
    public void setCurrPlanet(Planet planet) {
        this.currentPlanet = planet;
    }

    /**
     * Converts the current object to an instance of ClientCard.
     *
     * @return a new ClientCard instance initialized with the current object's properties.
     */
    public ClientCard toClientCard() {
        return new ClientPlanets(cardName, imageName, availablePlanets, playerPlanet, stepsBack);
    }

    /**
     * Retrieves the first state from the list of card states.
     *
     * @return the first CardState object in the list
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the appropriate logic based on the current state of the game and the player's choices.
     *
     * @param playerChoices The data structure containing the player's choices, which may include
     *                      the chosen planet index, updates to storage, or selected cargo cube storage.
     * @throws UnknownStateException Thrown when the current state is unrecognized or invalid for this method.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_PLANET:
                try {
                    this.currPlayerWantsToVisit(playerChoices.getChosenPlanetIndex());
                } catch (IllegalIndexException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLE_CUBES_REWARD:

                this.handleStorageUpdates(playerChoices.getStorageUpdates().orElseThrow());

                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }
    }

    /**
     * Manages the action of the current player attempting to visit a selected planet.
     * Updates the planet's status and notifies all connected clients of the player's actions.
     * Throws an exception if the chosen planet is invalid or already marked as busy.
     *
     * @param chosenPlanetIndex the index of the planet chosen by the current player;
     *                          must be positive and within the range of available planets.
     * @throws IllegalIndexException if the chosen planet has already been selected by another player.
     * @throws IndexOutOfBoundsException if the chosen index is out of the range of available planets.
     */
    private void currPlayerWantsToVisit (int chosenPlanetIndex) throws IllegalIndexException, IndexOutOfBoundsException {

        if (chosenPlanetIndex != 0) {
            currentPlanet = availablePlanets.get(chosenPlanetIndex - 1);

            if (currentPlanet.isBusy())
                throw new IllegalIndexException("Planet has already been chosen");

            playerPlanet.put(gameModel.getCurrPlayer().getNickname(), currentPlanet);
            availablePlanets.get(chosenPlanetIndex - 1).setBusy(true);
            currentPlanet.setNoMoreAvailable();

            ClientCard clientCard = this.toClientCard();
            String currPlayerNickname = gameModel.getCurrPlayer().getNickname();

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerVisitedPlanet(nicknameToNotify, currPlayerNickname, clientCard);
            });

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCurrAdventureCardUpdate(nicknameToNotify, clientCard);
            });

            setCurrState(CardState.HANDLE_CUBES_REWARD);

        } else if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    /**
     * Handles updates to the storage based on the provided data structure. This includes
     * validation of input, updating the game model's state, notifying clients about
     * changes, and managing player actions such as movement and turn progression.
     * If invalid data is provided, the current player is notified of the error, and
     * the storage is not updated.
     *
     * @param storageUpdates a map where the key is a Coordinates object representing
     *                       the position on the shipBoard and the value is a list of
     *                       CargoCube objects to be stored at the corresponding
     *                       coordinates. The map represents the proposed updates
     *                       to the player's storage configuration.
     */
    private void handleStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
        try {
            validateStorageUpdates(storageUpdates, gameModel);
            applyStorageUpdates(storageUpdates, gameModel);

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });

            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyRankingUpdate(nicknameToNotify,
                        gameModel.getCurrPlayer().getNickname(),
                        gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
            });

            proceedToNextPlayerOrEndCard();

        } catch (IllegalArgumentException e) {
            String currentPlayer = gameModel.getCurrPlayer().getNickname();
            gameModel.getGameClientNotifier().notifyClients(
                Set.of(currentPlayer),
                (nickname, clientController) -> {
                    clientController.notifyStorageError(nickname, e.getMessage());
                }
            );

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });

        }
    }

    /**
     * Advances the game to the next player's turn or transitions to the end-of-card state
     * if there are no more players in the current turn sequence.
     *
     * This method determines if there is a next player available using the {@code hasNextPlayer}
     * method of the game model. If a next player exists, the turn is passed to that player by
     * invoking {@code nextPlayer}, and the card state is updated to {@code CardState.CHOOSE_PLANET}.
     *
     * If there are no more players in the current sequence, the method transitions the card
     * state to {@code CardState.END_OF_CARD}, resets the player iterator to its initial state
     * using {@code resetPlayerIterator}, and updates the game's current state to
     * {@code GameState.CHECK_PLAYERS}.
     *
     * This method ensures the proper flow of gameplay and handles state transitions effectively
     * between players and phases.
     */
    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

}