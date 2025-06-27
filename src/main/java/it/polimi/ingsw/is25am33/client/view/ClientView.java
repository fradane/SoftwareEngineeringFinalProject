package it.polimi.ingsw.is25am33.client.view;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.client.view.tui.MessageType;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.EVALUATE_CREW_MEMBERS_MENU;


public interface ClientView {

    /**
     * Retrieves the instance of the {@code ClientController} associated with this view.
     *
     * @return the {@code ClientController} instance managing the client-side logic
     */
    ClientController getClientController();

    /**
     * Initializes the client view component by setting up input handling mechanisms.
     * This method starts background threads responsible for reading user input and
     * processing it into appropriate actions via input queues and handlers.
     *
     * The method ensures smooth interaction between the client view and the
     * input processing system by continuously monitoring user inputs while running
     * in daemon threads, allowing proper thread termination when the main thread concludes.
     */
    void initialize();

    /**
     * Prompts the user with a question or description and displays an interrogation prompt
     * to gather an input from the user.
     *
     * @param questionDescription a brief description or context for the question being asked
     * @param interrogationPrompt the specific prompt intended to request input from the user
     * @return the string representation of the user's input
     */
    String askForInput(String questionDescription, String interrogationPrompt);

    /**
     * Displays a message to the user with a specific message type.
     *
     * @param message the message to be displayed to the user
     * @param type the type of the message, specifying how it should be handled or displayed
     */
    void showMessage(String message, MessageType type);

    /**
     * Displays an error message to the user.
     *
     * @param errorMessage the error message to be displayed
     */
    void showError(String errorMessage);

    /**
     * Prompts the user to input their nickname. This method is typically used
     * at the beginning of the client interaction to collect the player's
     * preferred username. It may include an interface for user input
     * and handle the process of storing or validating the provided nickname.
     */
    void askNickname();

    /**
     * Displays the main menu of the client interface to the user.
     * Provides access to various options and features available within
     * the application. Typically invoked after the user successfully
     * registers or needs to navigate the primary options.
     */
    void showMainMenu();

    /**
     * Notifies the client that a player has joined a game.
     *
     * @param nickname The nickname of the player who joined.
     * @param gameInfo Information about the game that the player has joined.
     */
    void notifyPlayerJoined(String nickname, GameInfo gameInfo);

    /**
     * Notifies the client that a game has been successfully created.
     *
     * @param gameId the unique identifier of the created game
     */
    void notifyGameCreated(String gameId);

    /**
     * Notifies the client that the game has started and provides the current game state.
     *
     * @param gameState the current state of the game
     */
    void notifyGameStarted(GameState gameState);

    /**
     * Converts an integer value to a corresponding PlayerColor enum value.
     * If the integer does not match any predefined color, the default color RED is returned.
     *
     * @param colorChoice the integer value representing the desired PlayerColor
     * @return the PlayerColor corresponding to the provided integer value,
     *         or RED if the value does not match any available PlayerColor
     */
    default PlayerColor intToPlayerColor(int colorChoice) {
        return switch (colorChoice) {
            case 1 -> PlayerColor.RED;
            case 2 -> PlayerColor.BLUE;
            case 3 -> PlayerColor.GREEN;
            case 4 -> PlayerColor.YELLOW;
            default -> PlayerColor.RED;
        };
    }

    /**
     * Displays the updated state of the game to the user.
     * This method should be called when the game state has changed, ensuring that the user interface
     * reflects the latest game conditions and provides the necessary information for the next steps.
     */
    void showNewGameState();

    /**
     * Updates the user interface to reflect the current state of the card in the game.
     * This method is typically invoked after the card state has been updated in the model.
     * It ensures that any relevant details regarding the new card state are visually or contextually
     * presented to the user in the application's view.
     */
    void showNewCardState();

    /**
     * Displays the current adventure card to the user.
     *
     * @param isFirstTime a boolean indicating whether this is the first time displaying the adventure card.
     */
    void showCurrAdventureCard(boolean isFirstTime);

    /**
     * Retrieves the ClientModel associated with this instance.
     * The ClientModel represents the current state and data of the client that interacts
     * with the application.
     *
     * @return the ClientModel instance tied to this instance of the client view
     */
    ClientModel getClientModel();

    /**
     * Displays the menu for building a ship board during the game setup process.
     * This method is intended to interact with the player, allowing them to select
     * ship components, configure their ship, and prepare for subsequent phases of the game.
     */
    void showBuildShipBoardMenu();

    /**
     * Displays the selected component along with a menu for the user.
     * This method is used to provide a visual and interactive context
     * after a component has been selected, enabling the user to take
     * appropriate actions or make further choices.
     */
    void showPickedComponentAndMenu();

    /**
     * Displays the ship board of a specific player along with the owner's nickname.
     *
     * @param shipBoard the ship board to be displayed
     * @param shipBoardOwnerNickname the nickname of the ship board's owner
     */
    void showShipBoard(ShipBoardClient shipBoard, String shipBoardOwnerNickname);

    /**
     * Displays the ship board along with additional customization and color mapping for the components.
     *
     * @param shipBoardClient the ship board to be displayed, which contains the state of the ship including its components
     * @param shipBoardOwnerNickname the nickname of the player who owns the displayed ship board
     * @param colorMap a map where each key corresponds to a color identifier, and each value is a set of coordinates
     *                 indicating the positions of components or sections of the ship board associated with that color
     */
    void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap);

    /**
     * Displays the provided visible components along with the relevant menu.
     *
     * @param visibleComponents a map containing the components to be displayed,
     *                          where the key represents an integer identifier for each component,
     *                          and the value is the corresponding {@code Component} object
     */
    void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents);

    /**
     * Displays the specified little deck based on the given choice.
     *
     * @param littleDeckChoice an integer representing the choice of the little deck to display.
     */
    void showLittleDeck(int littleDeckChoice);

    /**
     * Updates the remaining time and the number of flips left for the client view.
     *
     * @param timeLeft   the amount of time remaining in seconds
     * @param flipsLeft  the number of flips remaining
     */
    void updateTimeLeft(int timeLeft, int flipsLeft);

    /**
     * Notifies the client that the hourglass timer has ended.
     * This method is typically called when the timer reaches zero.
     *
     * @param flipsLeft the number of flips remaining for the hourglass timer
     */
    void notifyTimerEnded(int flipsLeft);

    /**
     * Notifies the client view that the hourglass timer has started.
     * Provides information about the remaining flips and the nickname
     * of the player associated with the hourglass.
     *
     * @param flipsLeft the number of flips remaining for the hourglass
     * @param nickname  the nickname of the player associated with the hourglass timer
     */
    void notifyHourglassStarted(int flipsLeft, String nickname);

    /**
     * Displays a message or interface indicating that the game is currently waiting for players to join.
     * This method is typically called while transitioning between game states, ensuring the user is informed
     * of the current status and expected action.
     */
    void showWaitingForPlayers();

    /**
     * Displays a prompt to the user to choose a reserved component.
     * This method will present the necessary information to the user
     * and allow them to make a selection related to their reserved components.
     */
    void showPickReservedComponentQuestion();

    /**
     * Displays a menu indicating that the ship board configuration is invalid.
     * This method typically provides the user with options to address or correct
     * the issues with the ship board setup, such as repositioning components
     * or reviewing improperly placed elements.
     */
    void showInvalidShipBoardMenu();

    /**
     * Displays the valid ship board menu to the user. This method is typically
     * invoked to allow the user to interact with a ship board that has passed
     * validation or does not contain any errors. The implementation of this
     * menu provides options related to further actions or configurations
     * the player can take regarding their validated ship board.
     */
    void showValidShipBoardMenu();

    /**
     * Displays a menu for the user to select which component they would like to remove
     * from their shipboard. This is typically used in scenarios where a player's shipboard
     * contains components that are incorrectly positioned or invalid within the game's rules.
     *
     * The method transitions the user interface to provide relevant options for removal,
     * enabling players to correct their shipboard setup.
     */
    void showChooseComponentToRemoveMenu();

    /**
     * Displays a menu where the user can choose ship parts. The provided list represents
     * different sets of ship parts, where each set contains coordinates of components
     * that form a part of the ship.
     *
     * @param shipParts a list of sets, where each set contains the coordinates of components
     *                  belonging to a specific ship part.
     */
    void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts);



    /**
     * Displays the menu for visiting a location during the game.
     * This method is typically invoked when the game state changes to a point
     * where the player needs to choose or interact with a specific location.
     *
     * This menu provides the available options related to visiting locations
     * and allows the user to make their selection accordingly.
     */
    void showVisitLocationMenu();

    /**
     * Displays the menu for rolling dice during a specific game phase.
     * This method is typically used when the player needs to perform an action
     * requiring a dice roll, such as determining outcomes tied to randomness.
     * It provides the user with the necessary options to proceed with the dice roll.
     */
    void showThrowDicesMenu();

    /**
     * Displays the menu to allow the player to choose a planet to visit.
     * This method is typically invoked during the selection phase in the game
     * where the player is required to select a planet for their next move.
     */
    void showChoosePlanetMenu();

    /**
     * Displays the menu for choosing available engines in the current game context.
     * This method is typically invoked when the game logic requires the player to
     * select engines, such as during configuration or gameplay events where engines
     * are relevant. The menu will present options to the player based on the current
     * state of the game and available resources.
     */
    void showChooseEnginesMenu();

    /**
     * Displays the menu for accepting a reward in the game.
     *
     * This method is invoked when the game's state transitions to the context
     * of handling reward acceptance. It provides the user with the appropriate
     * interface or prompts required to accept a reward. The specific details of
     * the reward and interaction are managed within the respective implementation.
     */
    void showAcceptTheRewardMenu();

    /**
     * Displays the menu for choosing cannons in the game's interface.
     * This method is used when the player is required to interact with the game
     * and make a decision related to available cannon options.
     *
     * It is typically called during a specific state in the game where a cannon-related
     * choice is needed, and it updates the interface to reflect the options
     * available to the player.
     */
    void showChooseCannonsMenu();

    /**
     * Displays the menu for handling small dangerous objects within the game.
     * This method is part of the graphical user interface and is invoked when the player
     * needs to address situations involving small hazards or risky objects.
     */
    void showSmallDanObjMenu();

    /**
     * Displays the menu related to handling a big meteorite event in the game.
     * This menu provides options or prompts for the user to respond to or manage the effects
     * of encountering a big meteorite during gameplay.
     */
    void showBigMeteoriteMenu();

    /**
     * Displays the menu for handling a "Big Shot" event during the game.
     * A "Big Shot" scenario typically involves a significant game event or challenge
     * where players need to make decisions related to this event.
     *
     * The method is invoked when the game state transitions to the "Big Shot" handling phase.
     */
    void showBigShotMenu();

    /**
     * Displays the menu for handling the removal of crew members.
     * This menu allows the user to make decisions regarding crew members
     * that may need to be removed under specific game conditions.
     * Typically invoked in scenarios where an evaluation of crew members
     * is required due to gameplay events, such as attacks or status changes.
     */
    void showHandleRemoveCrewMembersMenu();

    /**
     * Displays the menu for handling a cubes reward scenario during the game.
     * This menu allows the player to interact with the game mechanics related to
     * processing or managing rewards involving cubes.
     * Typically invoked when the game state transitions to a phase where such rewards
     * need to be addressed.
     */
    void showHandleCubesRewardMenu();

    /**
     * Displays the epidemic menu to the user. This menu is intended to handle
     * scenarios where epidemic-related gameplay actions are required.
     *
     * The method is typically used in scenarios where the game state transitions
     * to an epidemic-related event, requiring the user to make decisions or take
     * specific actions related to managing an epidemic within the game.
     */
    void showEpidemicMenu();

    /**
     * Displays the Stardust Menu in the client view.
     * This menu is used to handle interactions or options related to stardust
     * within the game environment. The context in which this menu is shown depends on the
     * current state of the game and the player's actions.
     */
    void showStardustMenu();

    /**
     * Displays the menu for handling cube maluses during gameplay.
     * This method is responsible for presenting options or information
     * related to cube penalties that might be applied to the player's
     * game state at this specific point of interaction.
     */
    void showHandleCubesMalusMenu();

    /**
     * Displays the menu to inspect the player's shipboard after an attack has occurred.
     * This method is triggered during post-attack scenarios to allow players to
     * assess the state of their shipboard and make necessary decisions based on damages
     * or other outcomes resulting from the attack.
     */
    void checkShipBoardAfterAttackMenu();

    /**
     * Displays the currently available information regarding the crew members on the ship.
     * This method is responsible for presenting details such as crew status, positions,
     * and any relevant updates about crew members to the user.
     */
    void showCrewMembersInfo();

    // ---------------------------------------------------


    /**
     * Displays a message or visual indicator to notify the user
     * about the first player who has entered or performed a specific action.
     * This method is typically called to inform or highlight the first participant
     * in a significant game-related event or action.
     */
    void showFirstToEnter();

    /**
     * Sets the test flight mode for the client view.
     *
     * @param isTestFlight a boolean value indicating whether the test flight mode is enabled (true)
     *                     or disabled (false)
     */
    void setIsTestFlight(boolean isTestFlight);

    /**
     * Displays the current ranking of players.
     *
     * This method is responsible for rendering the current ranking information
     * to the user, typically reflecting the latest updates in players' standings.
     * The ranking is updated based on game events and player actions.
     */
    void showCurrentRanking();

    /**
     * Displays the crew placement menu to the user.
     * This method is typically invoked during the crew placement phase of the game.
     * It allows the user to interact with and make decisions regarding crew placement on the ship.
     */
    void showCrewPlacementMenu();

    /**
     * Displays a menu of available prefab ships to the user.
     *
     * @param prefabShips the list of PrefabShipInfo objects containing information about the prefab ships
     */
    void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips);

    /**
     * Displays information about a component hit at the given coordinates on the shipboard.
     *
     * @param coordinates the coordinates of the component that was hit
     */
    void showComponentHitInfo(Coordinates coordinates);

    /**
     * Converts the given card state to a corresponding client state based on the game logic.
     * The method determines the client-side representation of the given card state,
     * taking into account additional context provided by the client model.
     *
     * @param cardState the current card state to be translated into a client state
     * @param clientModel the client model providing additional context for the state conversion
     * @return the corresponding client state derived from the given card state and client model
     */
    default ClientState cardStateToClientState(CardState cardState, ClientModel clientModel) {
        switch (cardState) {
            case VISIT_LOCATION:
                return ClientState.VISIT_LOCATION_MENU;
            case CHOOSE_PLANET:
                return ClientState.CHOOSE_PLANET_MENU;
            case CHOOSE_CANNONS:
                return ClientState.CHOOSE_CANNONS_MENU;
            case CHOOSE_ENGINES:
                return ClientState.CHOOSE_ENGINES_MENU;
            case THROW_DICES:
                return ClientState.THROW_DICES_MENU;
            case DANGEROUS_ATTACK:
                ClientDangerousObject obj = clientModel.getCurrDangerousObj();
                if (obj != null) {
                    String type = obj.getType();
                    if (type.contains("Small")) {
                        return ClientState.HANDLE_SMALL_DANGEROUS_MENU;
                    } else if (type.contains("bigMeteorite")) {
                        return ClientState.HANDLE_BIG_METEORITE_MENU;
                    } else if (type.contains("bigShot")) {
                        return ClientState.HANDLE_BIG_SHOT_MENU;
                    }
                }
                return ClientState.HANDLE_SMALL_DANGEROUS_MENU;// Default
            case ACCEPT_THE_REWARD:
                return ClientState.ACCEPT_REWARD_MENU;
            case HANDLE_CUBES_REWARD:
                return ClientState.HANDLE_CUBES_REWARD_MENU;
            case HANDLE_CUBES_MALUS:
                return ClientState.HANDLE_CUBES_MALUS_MENU;
            case REMOVE_CREW_MEMBERS:
                return ClientState.CHOOSE_CABIN_MENU;
            case EPIDEMIC:
                return ClientState.EPIDEMIC_MENU;
            case STARDUST:
                return ClientState.STARDUST_MENU;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                return ClientState.CHECK_SHIPBOARD_AFTER_ATTACK;
            case EVALUATE_CREW_MEMBERS:
                return EVALUATE_CREW_MEMBERS_MENU;
            default:
                return ClientState.PLAY_CARD;
        }
    }

    /**
     * Displays the appropriate menu based on the given client state.
     * This method maps a specific {@link ClientState} to its corresponding menu
     * and executes the related functionality.
     *
     * @param mappedState the current client state that determines which menu to show
     */
    default void showCardStateMenu(ClientState mappedState) {

        switch (mappedState) {
            case VISIT_LOCATION_MENU:
                showVisitLocationMenu();
                break;
            case CHOOSE_CABIN_MENU:
                showHandleRemoveCrewMembersMenu();
                break;
            case CHOOSE_PLANET_MENU:
                showChoosePlanetMenu();
                break;
            case CHOOSE_CANNONS_MENU:
                showChooseCannonsMenu();
                break;
            case CHOOSE_ENGINES_MENU:
                showChooseEnginesMenu();
                break;
            case THROW_DICES_MENU:
                showThrowDicesMenu();
                break;
            case ACCEPT_REWARD_MENU:
                showAcceptTheRewardMenu();
                break;
            case HANDLE_SMALL_DANGEROUS_MENU:
                showSmallDanObjMenu();
                break;
            case HANDLE_BIG_METEORITE_MENU:
                showBigMeteoriteMenu();
                break;
            case HANDLE_BIG_SHOT_MENU:
                showBigShotMenu();
                break;
            case HANDLE_CUBES_REWARD_MENU:
                showHandleCubesRewardMenu();
                break;
            case EPIDEMIC_MENU:
                showEpidemicMenu();
                break;
            case STARDUST_MENU:
                showStardustMenu();
                break;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                checkShipBoardAfterAttackMenu();
                break;
            case EVALUATE_CREW_MEMBERS_MENU:
                showCrewMembersInfo();
                break;
            case HANDLE_CUBES_MALUS_MENU:
                showHandleCubesMalusMenu();
                break;
        }
    }

    /**
     * Displays information about the cabins and their neighboring components where infected crew members have been removed.
     *
     * @param cabinWithNeighbors a set of coordinates representing the cabins and their adjacent areas affected by the removal of infected crew members
     */
    void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors);

    /**
     * Displays the end-game information including the final ranking of players and the nicknames of players
     * with the prettiest ship.
     *
     * @param finalRanking the final ranking of players represented as a list of PlayerFinalData objects
     * @param playersNicknamesWithPrettiestShip a list of nicknames of players who have the prettiest ship
     */
    void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip);

    /**
     * Displays a notification or message indicating that a player has landed early.
     *
     * @param nickname the nickname of the player who landed early
     */
    void showPlayerEarlyLanded(String nickname);

    /**
     * Displays the cubes associated with a specific player's shipboard.
     *
     * @param shipboardOf the shipboard client representing the player's current state
     * @param nickname the nickname of the player whose cubes are being displayed
     */
    void showCubes(ShipBoardClient shipboardOf, String nickname);

    /**
     * Updates the current game information displayed to the client.
     *
     * @param gameInfos a list of {@code GameInfo} objects containing the updated game information
     */
    void refreshGameInfos(List<GameInfo> gameInfos);

    /**
     * Displays a disconnect message to the user.
     *
     * @param message the message to be shown indicating the disconnect status
     */
    void showDisconnectMessage(String message);

    /**
     * Displays all components that are hidden, making them visible again.
     * This method ensures that any previously hidden UI components
     * or elements are revealed and visible to the user.
     *
     * It does not alter the state of components that are already visible
     * and does not perform any additional operations on other components.
     * This method is typically used to restore full visibility
     * within the user interface.
     */
    void showNoMoreHiddenComponents();

    /**
     * Displays the cube redistribution menu in the application.
     * This method is responsible for presenting the user interface
     * or options related to redistributing cubes. It may involve
     * interaction with the user to select or confirm redistribution choices.
     */
    void showCubeRedistributionMenu();

    void showStolenVisibleComponent();

    void notifyPlayerDisconnected(String disconnectedPlayerNickname);
}