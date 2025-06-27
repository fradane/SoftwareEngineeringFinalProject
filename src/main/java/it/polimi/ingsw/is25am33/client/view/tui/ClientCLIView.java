package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.PlayerClientData;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level1ShipBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import org.jetbrains.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.*;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;

/**
 * The ClientCLIView class represents the Command-Line Interface (CLI) view for the client in the application.
 * It serves as the primary interface for interacting with the user, handling input and displaying output.
 * The class provides various methods for rendering user menus, game updates, and notifications, and for
 * gathering user input during gameplay.
 *
 * This class manages the state of the client, including interactions with the client model and controller,
 * as well as the formatting and display of game-related information. It uses color formatting where applicable
 * to enhance the user's experience.
 *
 * Key responsibilities:
 * - Display game state information and updates to the console.
 * - Ask users for specific inputs based on game prompts.
 * - Handle user interaction during various gameplay phases, including setup, decision-making, and resolution.
 * - Display notifications, errors, and game alerts in a structured and formatted manner.
 * - Provide helper methods for formatting and managing internal representations of game entities.
 *
 * This class interacts with the ClientModel and ClientController to coordinate game logic updates and display
 * information to the user.
 */
public class ClientCLIView implements ClientView {

    private final Scanner scanner;
    private volatile boolean waitingForGameStart = false;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean waitingForInput = false;
    private static final String INPUT_INTERRUPT = "%";
    private ClientModel clientModel;
    private ClientController clientController;
    private final Object consoleLock = new Object();
    private ClientState clientState = REGISTER;
    BlockingQueue<String> stringQueue = new LinkedBlockingQueue<>();
    private boolean isTestFlight;

    // Class-level variables to track selection state
    private final List<Coordinates> selectedEngines = new ArrayList<>();
    private final List<Coordinates> selectedCabins = new ArrayList<>();
    private final List<Coordinates> selectedCannons = new ArrayList<>();
    private final List<Coordinates> selectedBatteries = new ArrayList<>();
    private final List<Coordinates> selectedShields = new ArrayList<>();
    private Coordinates currentSelection = null;
    private Coordinates hitComponent = null;
    private StorageSelectionManager storageManager = null;
    private final Map<Integer, Coordinates> crewPlacementCoordinatesMap = new HashMap<>();
    private final Map<Coordinates, CrewMember> crewChoices = new HashMap<>();
    private int cubeMalus;
    private List<Coordinates> mostPreciousCube;
    private final List<Coordinates> selectedStorage = new ArrayList<>();

    // Definizione dei colori ANSI (funziona nei terminali che supportano i colori ANSI).
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private String currentInterrogationPrompt = "";

    /**
     * Constructs a new ClientCLIView instance with input stream initialization.
     * Initializes the Scanner to read input from the standard input stream.
     *
     * @throws RemoteException if a remote communication error occurs.
     */
    public ClientCLIView() throws RemoteException {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Sets the client model reference for this view.
     * This model provides access to game state and data used for rendering the UI.
     *
     * @param clientModel The client-side game model instance.
     */
    public void setClientModel(ClientModel clientModel) {
        this.clientModel = clientModel;
    }

    /**
     * Sets the client controller instance used by this view to communicate with the game logic.
     *
     * @param clientController The controller managing the client-side logic.
     */
    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
    }

    /**
     * Sets whether the current game is a test flight mode.
     * Affects game rules such as scoring and available features.
     *
     * @param isTestFlight true if the game is a test flight; false otherwise.
     */
    @Override
    public void setIsTestFlight(boolean isTestFlight) {
        this.isTestFlight = isTestFlight;
    }

    /**
     * Retrieves the current client model.
     * Provides access to the client-side representation of the game state.
     *
     * @return the current ClientModel instance.
     */
    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    /**
     * Updates the client-side interface with the current list of available game sessions.
     * This method is called to refresh the game lobby or join menu after receiving new game info.
     *
     * @param gameInfos A list of available games retrieved from the server.
     */
    @Override
    public void refreshGameInfos(List<GameInfo> gameInfos) {}

    /**
     * Initializes the view by starting background threads for input handling.
     * Creates and starts daemon threads for reading user input and processing input messages.
     */
    @Override
    public void initialize() {
        // Avvia il thread di input
        Thread inputThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Leggi l'input e mettilo nella coda
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine();
                        inputQueue.put(input);
                    }
                    // Piccola pausa per evitare uso eccessivo della CPU
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread handlerInputMessage = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ClientCLIView.this.handleInput(inputQueue.take());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        handlerInputMessage.setDaemon(true);
        inputThread.setDaemon(true); // Termina quando il thread principale termina
        inputThread.start();
        handlerInputMessage.start();
    }

    /**
     * Prompts the user for input with a specific question and waits for their response.
     * This method blocks until input is received or interrupted.
     *
     * @param questionDescription a description of what information is being requested
     * @param interrogationPrompt the specific prompt to display to the user
     * @return the user's input as a String, or INPUT_INTERRUPT if the operation was interrupted
     */
    public String askForInput(String questionDescription, String interrogationPrompt) {

        synchronized (consoleLock) {
            currentInterrogationPrompt = interrogationPrompt;
            showMessage(questionDescription, INPUT);
            showMessage(interrogationPrompt, INPUT);
            waitingForInput = true;
        }

        try {
            String input = null;
            // Controlla periodicamente se è arrivato input
            while (input == null && waitingForInput) {
                input = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                // Qui puoi inserire codice per gestire notifiche dal server
            }

            return input != null ? input : INPUT_INTERRUPT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return INPUT_INTERRUPT;
        } finally {
            waitingForInput = false;
        }

    }

    /**
     * Retrieves the client controller associated with this view.
     * This controller manages interactions and logic on the client side.
     *
     * @return the current ClientController instance.
     */
    @Override
    public ClientController getClientController() {
        return clientController;
    }

    /**
     * Displays a message to the user based on the provided type.
     * The behavior and formatting of the output are determined by the {@code MessageType}.
     *
     * @param message the message to be displayed. The content and format of the message depend
     *                on the caller's implementation and should align with the specified {@code type}.
     * @param type the type of the message to be displayed. It determines how the message
     *             is presented to the user. Valid types include:
     *             - {@code STANDARD}: Prints the message to the console with a newline.
     *             - {@code ASK}: Prints the message to the console without a newline, expecting it
     *               to be formatted as a question.
     *             - {@code ERROR}: Prints an error message prefixed with "Error:" in red.
     *             - {@code NOTIFICATION_INFO}: Prints an informational message prefixed with "Info:" in blue.
     *             - {@code NOTIFICATION_CRITICAL}: Prints a critical notification message prefixed with "Important:"
     *               in yellow, and resets the input waiting state if active.
     */
    @Override
    public void showMessage(String message, MessageType type) {

        switch (type){
            case STANDARD :
                System.out.println(message);
                break;
            case ASK :
                // if ASK, this method expects that the message is formatted like a question; hence it does not go to the next line
                System.out.print(message);
                break;
            case ERROR :
                System.out.println(ANSI_RED + "Error: " + message + ANSI_RESET);
                System.out.print("> ");
                break;
            case NOTIFICATION_INFO:
                System.out.print("\n" + ANSI_BLUE + "Info: " + message + ANSI_RESET + "\n> ");
                break;
            case NOTIFICATION_CRITICAL:
                System.out.print("\n" + ANSI_YELLOW + "Important: " + message + ANSI_RESET + "\n> ");
                if (waitingForInput) {
                    waitingForInput = false;
                }
                break;
        }

    }


    /**
     * Displays an error message to the user.
     * Uses red color and a standard error format for visibility in the terminal.
     *
     * @param errorMessage The error message to be displayed.
     */
    @Override
    public void showError(String errorMessage) {
        showMessage(errorMessage, MessageType.ERROR);
    }

    /**
     * Prompts the user to enter their nickname.
     * This method is typically called during the registration phase.
     */
    @Override
    public void askNickname() {
        showMessage("Please enter your nickname: ", ASK);
    }

    /**
     * Displays the list of available games that the user can join.
     * If no games are found, it informs the user and returns to the main menu.
     *
     * @param games An iterable of GameInfo objects representing available game sessions.
     */
    public void showAvailableGames(Iterable<GameInfo> games) {
        boolean hasGames = false;

        StringBuilder output = new StringBuilder();

        output.append("Available games:\n");
        for (GameInfo game : games) {
            hasGames = true;
            output.append(String.format("ID: " + game.getGameId() +
                    " | Players: " + game.getConnectedPlayersNicknames().size() + "/" + game.getMaxPlayers() +
                    " | Test Flight: " + (game.isTestFlight() ? "Yes" : "No") + "\n"));
        }

        output.append("> ");
        showMessage(output.toString(), ASK);

        if (!hasGames) {
            System.out.println("No games available.");
            clientState = ClientState.MAIN_MENU;
            showMainMenu();
        }
    }

    /**
     * Displays a prompt asking the user to choose a color from a predefined list.
     * Typically called during the setup or joining phase of a game.
     */
    public void showColorQuestion() {
        String colorMenu = """
                Choose your color:
                1. RED
                2. BLUE
                3. GREEN
                4. YELLOW
                >\s""";
        showMessage(colorMenu, ASK);
    }

    /**
     * Displays the list of available colors for a specific game session.
     * Informs the user to choose from the remaining options.
     *
     * @param gameID The identifier of the game session.
     */
    public void showAvailableColorsQuestion(String gameID) {
        List<PlayerColor> occupiedColors = clientController.getGames().stream()
                .filter(gameInfo -> gameInfo.getGameId().equals(gameID))
                .flatMap(game -> game.getConnectedPlayers().values().stream())
                .toList();
        List<PlayerColor> availableColors = Arrays.stream(PlayerColor.values())
                .filter(currColor -> !occupiedColors.contains(currColor))
                .toList();

        StringBuilder colorMenu = new StringBuilder("\nChoose your color:\n");
        for (PlayerColor color : availableColors) {
            colorMenu.append(color.getNumber()).append(". ").append(color.name()).append("\n");
        }
        colorMenu.append("> ");
        showMessage(colorMenu.toString(), ASK);
    }

    /**
     * Displays the main menu options to the user.
     * Typically includes options such as creating or joining a game, or exiting the application.
     */
    @Override
    public void showMainMenu() {
        clientState = ClientState.MAIN_MENU;
        String menu = """
                \nChoose an option:
                1. Create a new game
                2. Join a game
                >\s""";
        showMessage(menu, ASK);
    }

    /**
     * Notifies that a player has joined the game and displays updated game information.
     *
     * @param nickname the nickname of the player who joined
     * @param gameInfo the updated game information after the player joined
     */
    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {
        showMessage(nickname + " joined the game with color "+ gameInfo.getConnectedPlayers().get(nickname) + ". Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers(), NOTIFICATION_INFO);
    }

    /**
     * Notifies that a new game has been created and displays the game ID.
     *
     * @param gameId the ID of the newly created game
     */
    public void notifyGameCreated(String gameId) {
        showMessage("Game created! ID: " + gameId, NOTIFICATION_INFO);
    }

    /**
     * Displays a message to the user without adding any formatting or additional UI prompts.
     *
     * @param message The plain message to be shown.
     */
    /**
     * Informs the user that the game is waiting for other players to join before starting.
     * Displays a simple waiting message in the CLI.
     */
    @Override
    public void showWaitingForPlayers() {
        String menu = """
                Successfully joined game!
                Enter "exit" to leave the game.
                Waiting for the game to start...
                """;
        showMessage(menu, STANDARD);
    }

    /**
     * Notifies the user that the game has officially started.
     * Usually displayed after all players are ready and the game setup is complete.
     */
    @Override
    public void notifyGameStarted(GameState gameState) {
        waitingForGameStart = false;
        showMessage("""
                The game is now in progress...
                
                """, STANDARD);
    }

    /**
     * Informs the user that the game has entered the build phase.
     * Prompts the player to start building their ship or components.
     */
    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {
        if (isFirstTime) showMessage("The card has been drawn from the deck.\n", STANDARD);
    /**
     * Informs the user that the game has entered the rebuild phase.
     * The player can now perform repairs or rearrangements as allowed.
     */

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card == null) {
            showMessage("No current card available.", STANDARD);
            return;
        }

        // Use the card's toString() method for display
        showMessage(card.toString(), STANDARD);
    }

    /**
     * Helper method to display card-specific information
     */
    private void displayCardSpecificInfo(ClientCard card, StringBuilder output) {
        switch (card.getCardType()) {
            case "Planets":
                displayPlanetsInfo((ClientPlanets) card, output);
                break;
            case "AbandonedShip":
                displayAbandonedShipInfo((ClientAbandonedShip) card, output);
                break;
            case "Pirates":
                displayPiratesInfo((ClientPirates) card, output);
                break;
            case "SlaveTraders":
                displaySlaveTradersInfo((ClientSlaveTraders) card, output);
                break;
            case "AbandonedStation":
                displayAbandonedStationInfo((ClientAbandonedStation) card, output);
                break;
            case "Epidemic":
                displayEpidemicInfo((ClientEpidemic) card, output);
                break;
            case "SMUGGLERS":
                displaySmugglersInfo((ClientSmugglers) card, output);
                break;
            case "MeteoriteStorm":
                displayMeteoriteStormInfo((ClientMeteoriteStorm) card, output);
                break;
            case "FreeSpace":
                displayFreeSpaceInfo((ClientFreeSpace) card, output);
               break;
            case "StarDust":
                displayStardustInfo((ClientStarDust) card, output);
                break;
            case "WarField":
                displayWarFieldInfo((ClientWarField) card, output);
                break;
            default:
                output.append("Unknown card type\n");
        }
    }

    private void displayAbandonedShipInfo(ClientAbandonedShip ship, StringBuilder output) {
        output.append("Crew Required: ").append(ship.getCrewMalus()).append("\n");
        output.append("Reward: ").append(ship.getReward()).append(" credits\n");
        output.append("Steps Back: ").append(ship.getStepsBack()).append("\n");

        output.append("\nYou can accept the reward if you have enough crew members to sacrifice.");
        output.append("\nIf you accept, you'll lose ").append(ship.getCrewMalus())
                .append(" crew members, gain ").append(ship.getReward())
                .append(" credits, and move back ").append(ship.getStepsBack())
                .append(" spaces.");
    }

    private void displayAbandonedStationInfo(ClientAbandonedStation station, StringBuilder output) {
        output.append("Crew Required: ").append(station.getRequiredCrewMembers()).append("\n");
        output.append("Steps Back: ").append(station.getStepsBack()).append("\n");

        output.append("Rewards: ");
        if (station.getReward() != null && !station.getReward().isEmpty()) {
            station.getReward().forEach(cube -> output.append(cube.name()).append(" "));
        } else {
            output.append("None");
        }
        output.append("\n");

        output.append("\nYou can accept the reward if you have enough crew members.");
        output.append("\nIf you accept, you'll gain cargo cubes and move back ")
              .append(station.getStepsBack()).append(" spaces.");
    }

    private void displayEpidemicInfo(ClientEpidemic epidemic, StringBuilder output) {
        output.append("Epidemic Card\n");
        output.append("Effect: Each occupied cabin connected to another occupied cabin will lose one crew member.\n");
        output.append("\nThe epidemic spreads through connected living quarters, affecting crew members in adjacent cabins.\n");
        output.append("Wait to see the results of the epidemic spread...");
    }

    private void displayPlanetsInfo(ClientPlanets planets, StringBuilder output) {
        output.append("Available Planets: ").append(planets.getPlanetCount()).append("\n");
        output.append("Steps Back: ").append(planets.getStepsBack()).append("\n");

        output.append("\nPlanet Rewards:\n");
        for (int i = 0; i < planets.getAvailablePlanets().size(); i++) {
            Planet planet = planets.getAvailablePlanets().get(i);
            if (!planet.isBusy()) {
                output.append("  Planet ").append(i + 1).append(": ");
                planet.getReward().forEach(cube -> output.append(cube.name()).append(" "));
                output.append("\n");
            }
        }
    }

    /**
     * Displays a message indicating that a specific component has been hit.
     * Also updates the last hit component coordinates for reference.
     *
     * @param coordinates The coordinates of the component that was hit.
     */
    public void showComponentHitInfo(Coordinates coordinates) {
            showMessage("The component at coordinates " + coordinates.getX() + "-"+ coordinates.getY() + " has been hit", STANDARD);
            hitComponent = coordinates;
    }



    /**
     * Displays information about the crew members of the current user and other players.
     * It first shows the number of crew members belonging to the current user.
     * Then, it iterates through the sorted ranking of all players and displays
     * the number of crew members for each player except the current user.
     * Concludes with a message prompting the user to proceed with the next phase.
     *
     * This method retrieves data from the client model, including the user's nickname,
     * the shipboard associated with each player, and the sorted rankings of all players.
     */
    public void showCrewMembersInfo() {
        showMessage("You have " + clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size() + " crew members", STANDARD);
        for (String player : clientModel.getSortedRanking()) {
            if (!player.equals(clientModel.getMyNickname()))
                showMessage(player + "has" + clientModel.getShipboardOf(player).getCrewMembers().size() + " crew members", STANDARD);
        }

        showMessage("Press any key to start this phase...", ASK);
    }

    private void displayPiratesInfo(ClientPirates pirates, StringBuilder output) {
        output.append("Required Fire Power: ").append(pirates.getRequiredFirePower()).append("\n");
        output.append("Reward: ").append(pirates.getReward()).append(" credits\n");
        output.append("Steps Back: ").append(pirates.getStepsBack()).append("\n");
        output.append("Shots: ").append(pirates.getDangerousObjCount()).append("\n");

        if (!pirates.getShots().isEmpty()) {
            output.append("\nShot Details:\n");
            for (int i = 0; i < pirates.getShots().size(); i++) {
                ClientDangerousObject shot = pirates.getShots().get(i);
                output.append("  Shot ").append(i + 1).append(": ").append(shot.getType()).append("\n");
            }
        }
    }

    private void displaySlaveTradersInfo(ClientSlaveTraders slaveTraders, StringBuilder output) {
        output.append("Fire Power Required: ").append(slaveTraders.getRequiredFirePower()).append("\n");
        output.append("Crew Malus: ").append(slaveTraders.getCrewMalus()).append("\n");
        output.append("Reward: ").append(slaveTraders.getReward()).append(" credits\n");
        output.append("Steps Back: ").append(slaveTraders.getStepsBack()).append("\n");
    }

    private void displaySmugglersInfo(ClientSmugglers smugglers, StringBuilder output) {
        output.append("Fire Power Required: ").append(smugglers.getRequiredFirePower()).append("\n");
        output.append("Cube Malus: ").append(smugglers.getCubeMalus()).append("\n");
        output.append("Steps Back: ").append(smugglers.getStepsBack()).append("\n");
        output.append("Rewards: ");
        smugglers.getReward().forEach(cube -> output.append(cube.name()).append(" "));
        output.append("\n");
    }

    private void displayMeteoriteStormInfo(ClientMeteoriteStorm storm, StringBuilder output) {
        output.append("ATTENTION! There are ").append(storm.getDangerousObjCount()).append(" meteorites.").append("\n");

        if (!storm.getMeteorites().isEmpty()) {
            output.append("\nMeteorite Details:\n");
            for (int i = 0; i < storm.getMeteorites().size(); i++) {
                ClientDangerousObject meteorite = storm.getMeteorites().get(i);
                output.append("  Meteorite ").append(i + 1).append(": ").append(meteorite.getType()).append(" "+ meteorite.getDirection()).append("\n");
            }
        }
    }

    private void displayFreeSpaceInfo(ClientFreeSpace freeSpace, StringBuilder output) {
        output.append("Free movement through space using engines\n");
    }

    private void displayStardustInfo(ClientStarDust stardust, StringBuilder output) {
        output.append("Stardust field! Ships will move back based on exposed connectors.\n");
    }

    private void displayWarFieldInfo(ClientWarField warField, StringBuilder output) {
        output.append("War Zone! Multiple evaluation phases:\n");
        output.append("Cube Malus: ").append(warField.getCubeMalus()).append("\n");
        output.append("Crew Malus: ").append(warField.getCrewMalus()).append("\n");
        output.append("Steps Back: ").append(warField.getStepsBack()).append("\n");
        output.append("Shots: ").append(warField.getShots().size()).append("\n");
    }

    /**
     * Returns the coordinates of the last component that was hit.
     * This information is updated whenever a component is struck.
     *
     * @return The coordinates of the most recently hit component.
     */
    public Coordinates getLastHitComponent() {
        return hitComponent;
    }

    /**
     * Displays a formatted message regarding the new game state to the user.
     * The method retrieves the current game state from the client model,
     * formats it into a predefined template, and outputs it with a standard message type.
     * This is primarily used to update users about the latest game state changes.
     */
    @Override
    public void showNewGameState() {
        showMessage(String.format("""
                        \n===================================
                        📢  [Game Update]
                        🎮  New Game State: %s
                        ===================================
                        """, clientModel.getGameState().toString()), STANDARD);
    }

    /**
     * Updates the client interface to reflect the new state of the card.
     * This method retrieves the current card state from the client model, maps it to
     * a client state, and updates the client's state accordingly.
     * It also resets any previous selection states for engines, cannons, batteries, and shields,
     * and clears the current selection.
     *
     * The method generates and displays a message summarizing the new card state and
     * then shows the corresponding card state menu.
     */
    @Override
    public void showNewCardState() {
        CardState currentCardState = clientModel.getCurrCardState();
        ClientState mappedState = cardStateToClientState(currentCardState, clientModel);
        setClientState(mappedState);

        // Reset selection state
        selectedEngines.clear();
        selectedCannons.clear();
        selectedBatteries.clear();
        selectedShields.clear();
        currentSelection = null;

        showMessage(String.format("""
                    \n===================================
                    🃏  [Card Update]
                    🆕  New Card State: %s
                    ===================================
                    """, currentCardState), STANDARD);
        showCardStateMenu(mappedState);
    }

    /**
     * Displays the menu for the Build Ship Board phase of the game.
     *
     * This method provides the user with a list of menu options to select during
     * the shipboard building phase. Based on the current game state, different
     * options will be available:
     *
     * Options include:
     *  - Building a prefabricated ship.
     *  - Selecting a random covered component from the table.
     *  - Picking a visible component from the table.
     *  - Ending the shipboard construction process.
     *  - Additional options such as restarting the hourglass, watching the little deck,
     *    and placing reserved components if the game is not in a test flight state.
     *
     * Players can also observe another player's shipboard by typing "show [nickname]".
     *
     * The method ensures the player receives a prompt highlighting their options and
     * expects an input response.
     */
    @Override
    public void showBuildShipBoardMenu() {
        clientState = BUILDING_SHIPBOARD_MENU;

        StringBuilder output = new StringBuilder();

        output.append("""
                \nChoose an option:
                0. Build a prefabricated ship
                1. Pick a random covered component from the table
                2. Pick a visible component from the table
                3. End ship board construction
                """);

        if (!isTestFlight)
            output.append("""
                    4. Restart hourglass
                    5. Watch a little deck
                    6. Place a reserved component
                    """);

        output.append("""
                ("show [nickname]" to watch other's player ship board)
                >\s""");

        showMessage(output.toString(), ASK);
    }

    /**
     * Notifies that the hourglass timer has started or restarted, providing the number of flips left
     * and the nickname of the player or entity responsible for the action.
     *
     * @param flipsLeft the number of flips remaining for the hourglass timer
     * @param nickname the nickname associated with the player initiating the timer action or "game" if it is the first time
     */
    @Override
    public void notifyHourglassStarted(int flipsLeft, String nickname) {

        if (nickname.equals("game")) {
            showMessage("Hourglass started!!! There will be " + flipsLeft + " flips left at the end of this timer.", NOTIFICATION_INFO);
            return;
        }

        if (flipsLeft == 0)
            showMessage(nickname + " flipped the hourglass!!! There will be no flips left at the end of this timer.", NOTIFICATION_INFO);
        else if (flipsLeft == 1)
            showMessage(nickname + " flipped the hourglass!!! There will be " + flipsLeft + " flip left at the end of this timer.", NOTIFICATION_INFO);
        else
            showMessage(nickname + " flipped the hourglass!!! There will be " + flipsLeft + " flips left at the end of this timer.", NOTIFICATION_INFO);

    }

    /**
     * Displays a message prompting the user to place their placeholder.
     * This method sets the client state to a specific placeholder state
     * and informs the user that their placeholder has not yet been positioned.
     * The message encourages the user to act quickly and place the placeholder before others.
     */
    @Override
    public void showFirstToEnter() {
        clientState = PLACE_PLACEHOLDER;
        showMessage("""
                Your placeholder has not been placed yet!!!
                Press any key to place it faster than the others...""", STANDARD);
    }

    /**
     * Displays the menu to inform the user about invalid ship components
     * on their board and allows them to choose components to remove.
     * <br>
     * The method transitions the client state to indicate invalid shipboard
     * components, retrieves the shipboard of the current user, and identifies
     * incorrectly positioned components. It highlights these invalid components
     * with a specified color (red) when displaying the shipboard.
     * <br>
     * After showing the shipboard with invalid components, the method
     * transitions the client state further to allow the user to select and
     * remove a component for correction.
     */
    @Override
    public void showInvalidShipBoardMenu() {
        setClientState(CHECK_SHIPBOARD_INVALID);
        String nickname = clientModel.getMyNickname();
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        Set<Coordinates> invalidCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
        Map<String, Set<Coordinates>> coloredCoordinates = new HashMap<>();
        coloredCoordinates.put(ANSI_RED, invalidCoordinates);
        showShipBoard(shipBoard, nickname, coloredCoordinates);

        setClientState(CHECK_SHIPBOARD_CHOOSE_COMPONENT_TO_REMOVE);
        showChooseComponentToRemoveMenu();
    }

    /**
     * Displays the valid shipboard menu for the client.
     * Updates the client state to indicate that the shipboard is correct.
     * Retrieves the nickname of the client and the associated shipboard.
     * Displays the client's shipboard and a standard message indicating no further action is required.
     */
    @Override
    public void showValidShipBoardMenu() {
        setClientState(CHECK_SHIPBOARD_CORRECT);
        String nickname = clientModel.getMyNickname();
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        showShipBoard(shipBoard, nickname);
        showMessage("No action needed, your ship is all set.", STANDARD);
    }

    /**
     * Checks the state of the shipboard after an attack and takes appropriate actions.
     * If there is a hit component, it triggers the client controller to start checking the shipboard
     * with the associated player's nickname and resets the hit component. Otherwise, it informs the player
     * that their shipboard is safe and prompts them to continue.
     *
     * The method transitions between two possible scenarios:
     * 1. When a hit component is detected, it initializes a check on the client's shipboard.
     * 2. When no hit component is detected, it displays a success message and sets the client state
     *    to confirm the shipboard check result.
     */
    @Override
    public void checkShipBoardAfterAttackMenu() {
        if (hitComponent != null) {
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
            hitComponent = null;
        } else {
            showMessage("GOOD JOB! You are safe!", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(CONFIRM_CHECK_SHIPBOARD_AFTER_ATTACK);
        }
    }

    /**
     * Displays a menu prompting the user to input the coordinates of a component to remove.
     *
     * The method generates a message that provides instructions for the user to enter
     * the coordinates of a component they wish to remove. If there are incorrectly positioned
     * components on the shipboard, the valid coordinates are listed and shown for reference.
     * The coordinates are converted from 0-based indices to 1-based indices for display purposes.
     * If no incorrectly positioned components are found, the message notifies the user accordingly.
     *
     * The user is instructed to enter the coordinates in the format 'row column' (e.g., 'x y').
     * The resultant message is displayed to the user using the standard messaging mechanism.
     */
    @Override
    public void showChooseComponentToRemoveMenu() {
        //showMessage("Insert the coordinates of the component you want to remove: ", STANDARD);

        // Ottiene le coordinate dei componenti incorrettamente posizionati
        Set<Coordinates> incorrectCoords = clientModel.getShipboardOf(clientController.getNickname())
                .getIncorrectlyPositionedComponentsCoordinates();

        StringBuilder message = new StringBuilder();
        message.append("Insert the coordinates of the component you want to remove.\n");

        if (!incorrectCoords.isEmpty()) {
            message.append("Valid coordinates (shown in red on the ship board):\n");

            // Converti le coordinate da 0-based a 1-based per la visualizzazione
            incorrectCoords.forEach(coord ->
                    message.append("  (").append(coord.getX() + 1).append(",").append(coord.getY() + 1).append(")\n")
            );

            message.append("Enter coordinates as 'row column' (e.g., 'x y'): ");
        } else {
            message.append("No incorrectly positioned components found.\n");
        }

        showMessage(message.toString(), STANDARD);
    }

    /**
     * Displays a menu to choose ship parts to keep from a list of options.
     * The menu assigns each set of ship parts a color for visual identification
     * and prompts the user to select one by entering its corresponding number.
     *
     * @param shipPartsList A list of sets of coordinates representing the available ship parts
     *                      that can be chosen. Each set contains the coordinates for a specific
     *                      ship part, and each ship part is visually differentiated by a color.
     */
    @Override
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipPartsList) {
        setClientState(CHECK_SHIPBOARD_CHOOSE_SHIP_PART_TO_KEEP);

        // Crea mappa per visualizzazione
        List<String> colors = Arrays.asList(ANSI_BLUE, ANSI_GREEN, ANSI_YELLOW, ANSI_RED);
        Map<String, Set<Coordinates>> displayColorMap = new HashMap<>();

        for (int i = 0; i < shipPartsList.size() && i < colors.size(); i++) {
            displayColorMap.put(colors.get(i), shipPartsList.get(i));
        }

        String nickname = clientModel.getMyNickname();
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        showShipBoard(shipBoard, nickname, displayColorMap);

        StringBuilder menu = new StringBuilder("\nChoose the ship part to KEEP by entering its number:\n");
        for (int i = 0; i < shipPartsList.size(); i++) {
            String color = i < colors.size() ? colors.get(i) : "";
            menu.append((i + 1)).append(". Ship part ").append(color).append("(colored part)\n").append(ANSI_RESET);
        }

        showMessage(menu.toString(), ASK);
    }

    /**
     * Displays the selected little deck based on the user's choice.
     *
     * @param littleDeckChoice the index of the little deck chosen by the user
     */
    @Override
    public void showLittleDeck(int littleDeckChoice) {
        StringBuilder littleDeck = new StringBuilder();
        littleDeck.append("\nHere is the little deck you chose:\n");
        clientModel.getLittleVisibleDecks().get(littleDeckChoice - 1).forEach(card -> littleDeck.append(card.toString()).append("\n"));
        showMessage(littleDeck.toString(), STANDARD);
    }

    /**
     * Displays the currently picked component along with the corresponding menu of options for the user to interact with.
     *
     * The method updates the state of the client to indicate that a component is being focused on the shipboard and displays a menu
     * offering various actions that can be performed with the component. The available options include viewing the focused component,
     * rotating the component, placing it on the ship board, or releasing it. An additional option to reserve the component is
     * displayed depending on the state of the `isTestFlight` flag. Additionally, users may use a specific command to view the ship
     * board of another player.
     *
     * The menu is displayed using the `showMessage` method with the content built dynamically based on the current state.
     */
    @Override
    public void showPickedComponentAndMenu() {
        clientState = BUILDING_SHIPBOARD_WITH_FOCUSED_COMPONENT;
        StringBuilder output = new StringBuilder();
        output.append("""
                    \nChoose an action:
                    1. Show focus component
                    2. Rotate the component
                    3. Place component on ship board
                    4. Release component
                    """);

        if (!isTestFlight)
            output.append("""
                    5. Reserve component
                    """);

        output.append("""
                ("show [nickname]" to watch other's player ship board)
                >\s""");

        showMessage(output.toString(), ASK);
    }

    /**
     * Reads and parses coordinates from user input.
     * The method repeatedly prompts the user to input two integers separated by a space,
     * representing the row and column of a coordinate. It validates the input, ensuring
     * it is in the expected format and within valid number ranges. The input is adjusted
     * for zero-based indexing before creating and returning a Coordinates object.
     *
     * @return a Coordinates object representing the parsed row and column entered by the user.
     *         The coordinates are zero-indexed, meaning (1,1) input by the user is converted
     *         to (0,0) in the Coordinates object.
     */
    public Coordinates readCoordinatesFromUserInput(String prompt) {

        while (true) {
            String input = this.askForInput("", prompt);
            if (input.equals(INPUT_INTERRUPT)) return null;
            if (input.isEmpty()
                    && (prompt.equals("Select the coordinates (row column) for the double engines you would like to activate or press enter to skip: ")
                    || prompt.equals("Select the coordinates (row column) for the battery box you would like to activate or press enter to skip: ")
                    || prompt.equals("Choose the coordinates (row column) of the storage where you would like to store the cargo cube or press enter to skip this reward: ")))
                return null;
            String[] tokens = input.trim().split("\\s+");
            if (tokens.length == 2) {
                try {
                    int row = Integer.parseInt(tokens[0]);
                    int col = Integer.parseInt(tokens[1]);
                    return new Coordinates(row - 1, col - 1);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter two numbers separated by space.");
                }
            } else {
                System.out.println("Please enter exactly two numbers separated by space.");
            }
        }

    }

    /**
     * Displays the details of the cubes stored in the storages of a ShipBoardClient.
     * If no storages or cubes are found, it notifies accordingly.
     *
     * @param shipboardOf the instance of ShipBoardClient whose storages and cubes are to be displayed
     * @param nickname    the nickname of the user for whom the storage details are being displayed
     */
    @Override
    public void showCubes(ShipBoardClient shipboardOf, String nickname) {
        StringBuilder output = new StringBuilder();
        if(shipboardOf.getStorages().isEmpty())
            output.append("No storages were found.\n");
        else {
            shipboardOf.getCoordinatesAndStorages().forEach((coordinates, storage) -> {
                if (storage.getStockedCubes().isEmpty())
                    output.append("There aren't stocked cubes in the storage at " + (coordinates.getX()+1) +" "+(coordinates.getY()+1) + "\n");
                else {
                    for (CargoCube cube : storage.getStockedCubes()) {
                        output.append("In the storage at " + (coordinates.getX()+1)+" "+ (coordinates.getY()+1) + " there are " + storage.getStockedCubes().size() + " cubes:\n");
                        output.append(cube.toString()).append("\n");
                    }
                }
            });
        }

        showMessage(output.toString(), STANDARD);
    }

    /**
     * Displays a notification message indicating that the component has been stolen
     * and renders the build ship board menu.
     *
     * This method is typically used when an action leads to the invalidation or
     * unavailability of a specific component, prompting the user to select or use
     * a different option. The notification provides user feedback, while the
     * invocation of the build ship board menu facilitates the next steps in the
     * application's workflow.
     */
    @Override
    public void showStolenVisibleComponent() {
        showMessage("The component was stolen, try another one", NOTIFICATION_INFO);
        showBuildShipBoardMenu();
    }

    /**
     * Displays the shipboard and associated information for the specified user.
     *
     * @param shipBoardClient the client interface used for displaying the shipboard
     * @param shipBoardOwnerNickname the nickname of the shipboard's owner whose information is being displayed
     */
    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname) {
        showShipBoard(shipBoardClient, shipBoardOwnerNickname, Collections.emptyMap());
        showBatteryBoxesInfo(shipBoardOwnerNickname);
        showCabinsInfo(shipBoardOwnerNickname);
        showPlayerCreditsInfo(shipBoardOwnerNickname);
        showNotActiveComponentsInfo(shipBoardOwnerNickname);
        showStoragesInfo(shipBoardOwnerNickname);
    }

    /**
     * Displays the ship board of a particular player along with legends and component details.
     *
     * @param shipBoardClient the client object holding the ship board data and configurations
     * @param shipBoardOwnerNickname the nickname of the owner of the displayed ship board
     * @param colorMap a map containing coordinates and their associated color set for rendering ship components
     */
    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {

        BiFunction<Integer, Integer, Boolean> isOut = isTestFlight ? Level1ShipBoard::isOutsideShipboard : Level2ShipBoard::isOutsideShipboard;

        Component[][] shipBoard = shipBoardClient.getShipMatrix();
        List<Component> bookedComponents = shipBoardClient.getBookedComponents();
        List<String> reservedComponent1 = new ArrayList<>();
        List<String> reservedComponent2 = new ArrayList<>();

        if (!bookedComponents.isEmpty()) {
            reservedComponent1.addAll(Arrays.stream(shipBoardClient.getBookedComponents().getFirst().toString().split("\\n")).collect(Collectors.toList()));
            reservedComponent1.removeFirst();
        }
        if (bookedComponents.size() == 2) {
            reservedComponent2.addAll(Arrays.stream(shipBoardClient.getBookedComponents().get(1).toString().split("\\n")).collect(Collectors.toList()));
            reservedComponent2.removeFirst();
        }

        String[] legendLines = {
                "LEGEND - component label and explanation on attributes:",
                "• BBX = battery box - number of remaining batteries",
                "• CAB = cabin - number and type of members",
                "• CNN = cannon - fire direction",
                "• 2CN = double cannons - fire direction",
                "• 2EN = double engines - power direction",
                "• ENG = engine - power direction",
                "• LSP = life support - type of life support",
                "• MCB = main cabin - number and type of members",
                "• SLD = shield - covered directions",
                "• SPS = special storage - left storages",
                "• STS = standard storage - left storages",
                "• STR = structural modules",
                ""
        };

        StringBuilder output = new StringBuilder();

        int legendIndex = 0;
        int componentIndex = 0;

        output.append(String.format("\nHere's the ship board of " + shipBoardOwnerNickname + ":\n"));

        // Stampa numeri delle colonne
        output.append("       ");
        for (int col = 4; col <= 10; col++) {
            output.append(String.format("   %2d     ", col));
        }

        output.append(String.format("\t\t" + legendLines[legendIndex++] + "\n"));

        for (int i = 4; i <= 8; i++) {
            // Ogni cella viene stampata su 4 righe
            for (int line = 0; line < 4; line++) {
                if (line == 2) {
                    output.append(String.format(" %2d   ", i + 1));
                } else {
                    output.append("      ");
                }
                for (int j = 3; j <= 9; j++) {
                    Component cell = shipBoard[i][j];

                    // Determina il colore per questa coordinata
                    String componentColor = getColorForCoordinate(i, j, colorMap);
                    String resetColor = componentColor.isEmpty() ? "" : ANSI_RESET;

                    switch (line) {
                        case 0:
                            output.append("+---------");
                            break;

                        case 1:
                            String northConnector = isOut.apply(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.NORTH).fromConnectorTypeToValue()));
                            output.append(String.format("|    %s%1s%s    ",
                                    componentColor, northConnector, resetColor));
                            break;

                        case 2:
                            String westConnector = isOut.apply(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.WEST).fromConnectorTypeToValue()));
                            String eastConnector = isOut.apply(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.EAST).fromConnectorTypeToValue()));
                            String label = isOut.apply(i, j) ? (ANSI_RED + "OUT" + ANSI_RESET) :
                                    (cell == null ? "" : (componentColor + cell.getLabel() + resetColor));

                            output.append(String.format("| %s%1s%s %3s %s%1s%s ",
                                    componentColor, westConnector, resetColor,
                                    label,
                                    componentColor, eastConnector, resetColor));
                            break;

                        case 3:
                            String southConnector = isOut.apply(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue()));
                            String attribute = isOut.apply(i, j) ? "" :
                                    (cell == null ? "" : (componentColor + (cell.getMainAttribute().length() == 2 ? "" : " ") + cell.getMainAttribute() + resetColor));

                            output.append(String.format("|    %s%1s%s %2s ",
                                    componentColor, southConnector, resetColor,
                                    attribute));
                            break;
                    }
                }

                if (line == 0) {
                    output.append("+");
                } else {
                    output.append("|");
                }

                if (legendIndex <= legendLines.length - 1)
                    output.append(String.format("\t\t" + legendLines[legendIndex++] + "\n"));
                else if (componentIndex <= 7) {
                    if (reservedComponent1.size() > componentIndex)
                        if (reservedComponent1.get(componentIndex) != null) output.append(String.format("\t\t\t" + reservedComponent1.get(componentIndex)));
                    if (reservedComponent2.size() > componentIndex)
                        if (reservedComponent2.get(componentIndex) != null) output.append(String.format("\t\t\t" + reservedComponent2.get(componentIndex)));
                    output.append("\n");
                    componentIndex++;
                }
            }
        }

        output.append("      ");
        output.append("+---------".repeat(7));
        output.append("+\n");
        output.append("> ");

        showMessage(output.toString(), ASK);
    }

    /**
     * Displays the menu for selecting a prefabricated ship to the user.
     * This method generates a list of available prefabricated ships and prompts the user to choose one.
     * The menu also includes an option to return to the previous build menu.
     *
     * @param prefabShips a list of PrefabShipInfo objects representing the available prefabricated ships.
     *                    Each ship includes details like its name, description, and whether it is marked
     *                    for test flight use only.
     */
    public void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips) {
        clientState = BUILDING_SHIPBOARD_SELECT_PREFAB;

        StringBuilder menu = new StringBuilder("\nAvailable prefabricated ships:\n");

        for (int i = 0; i < prefabShips.size(); i++) {
            PrefabShipInfo ship = prefabShips.get(i);
            menu.append(i + 1).append(". ")
                    .append(ship.getName())
                    .append(" - ").append(ship.getDescription());

            if (ship.isForTestFlight()) {
                menu.append(" (Test Flight only)");
            }

            menu.append("\n");
        }

        menu.append("0. Go back to the build menu\n");
        menu.append("Choose a ship: ");

        showMessage(menu.toString(), ASK);
    }

    /**
     * Displays the infected crew members that were removed from specific cabin locations and their neighbors on the shipboard.
     * If there are affected cabins, they are highlighted in blue and a message is displayed indicating the affected cabins.
     * If no infected crew members were removed, a message indicating no removals is shown.
     *
     * @param cabinWithNeighbors A set of coordinates representing the cabins and their neighbors where infected crew members were removed.
     */
    @Override
    public void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors) {
        String nickname = clientModel.getMyNickname();
        ShipBoardClient shipboard = clientModel.getShipboardOf(nickname);
        if(cabinWithNeighbors.size() > 0) {
            showShipBoard(shipboard, nickname, Map.of(ANSI_BLUE, cabinWithNeighbors));
            showMessage("The colored cabins are those where you lost an infected crew membership", STANDARD);
        }else
            showMessage("You had no infected crew membership", STANDARD);
    }

    /**
     * Displays the end-game results and detailed information about the player's final performance
     * based on the specified final ranking and other game data.
     *
     * @param finalRanking a list of {@code PlayerFinalData} containing the final ranking and details
     *                     of all players, including scores, position, and other statistics.
     * @param playersNicknamesWithPrettiestShip a list of nicknames of players who received the
     *                                          prettiest ship bonus.
     */
    @Override
    public void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        setClientState(END_GAME_PHASE);

        // Get the current player's nickname
        String myNickname = clientModel.getMyNickname();

        // Find current player's data directly from finalRanking
        PlayerFinalData myData = null;
        for (PlayerFinalData data : finalRanking) {
            if (data.getNickname().equals(myNickname)) {
                myData = data;
                break;
            }
        }

        // Create a map for easy lookup of PlayerFinalData by nickname
        Map<String, PlayerFinalData> nicknameToData = new HashMap<>();
        for (PlayerFinalData data : finalRanking) {
            nicknameToData.put(data.getNickname(), data);
        }

        // Get sorted nicknames for display order
        List<String> sortedNicknames = clientModel.getSortedRanking();

        // Find current player's position (only counting non-landed players)
        int myPosition = -1;
        if (myData != null && !myData.isEarlyLanded()) {
            int positionCounter = 1;
            for (String nickname : sortedNicknames) {
                PlayerFinalData data = nicknameToData.get(nickname);
                if (data != null && !data.isEarlyLanded()) {
                    if (nickname.equals(myNickname)) {
                        myPosition = positionCounter;
                        break;
                    }
                    positionCounter++;
                }
            }
        }

        // Build the final game screen
        StringBuilder output = new StringBuilder();

        // Header
        output.append("\n\n");
        if (myData != null && myData.isEarlyLanded()) {
            output.append("                        🛬 FINE DEL VIAGGIO 🛬\n");
        } else {
            output.append("                        🚀 END OF THE JOURNEY 🚀\n");
        }
        output.append("═══════════════════════════════════════════════════════════════════════\n\n");

        output.append("🏆 FINAL RANKING\n");
        output.append("─────────────────────────────────────────────────────────────────────\n\n");

        // Show final ranking
        String[] medals = {"🥇", "🥈", "🥉"};

        // Find the maximum credits among all players
        int maxCredits = finalRanking.stream()
                .mapToInt(PlayerFinalData::getTotalCredits)
                .max()
                .orElse(0);

        // Display players in sorted order
        int displayPosition = 1;
        for (int i = 0; i < sortedNicknames.size(); i++) {
            String nickname = sortedNicknames.get(i);
            PlayerFinalData data = nicknameToData.get(nickname);

            // Skip if we don't have data for this player (shouldn't happen)
            if (data == null) continue;

            // Build player line with dynamic formatting
            String medal;
            String positionStr;

            if (data.isEarlyLanded()) {
                // Landed players don't get medals or positions
                medal = "🛬";
                positionStr = " - ";
            } else {
                // Only non-landed players get medals and positions
                medal = displayPosition <= 3 ? medals[displayPosition - 1] : "  ";
                positionStr = String.format("%d°", displayPosition);
                displayPosition++;
            }

            String playerName = nickname.equals(myNickname) ? "TU (" + nickname + ")" : nickname;

            // Format the main player info
            output.append(String.format("%s %3s  %-20s  %3d crediti cosmici",
                    medal, positionStr, playerName, data.getTotalCredits()));

            // Add winner/early landing info
            if (data.isEarlyLanded()) {
                output.append("  [LANDED EARLY]");
            } else if (data.getTotalCredits() == maxCredits && data.getTotalCredits() > 0) {
                output.append("  🎉 OVERALL WINNER!");
            } else if (nickname.equals(myNickname) && data.getTotalCredits() > 0) {
                output.append("  ✨ You're among the winners!");
            }

            output.append("\n");
        }

        // Show detailed breakdown for current player
        if (myData != null) {
            output.append("\n═══════════════════════════════════════════════════════════════════════\n\n");

            if (myData.isEarlyLanded()) {
                output.append("📊 YOUR SUMMARY (Landed Early)\n");
            } else {
                output.append("📊 YOUR SUMMARY\n");
            }
            output.append("─────────────────────────────────────────────────────────────────────\n\n");

            // Calculate initial credits by subtracting bonuses/penalties
            int initialCredits = calculateInitialCredits(myData, myPosition, playersNicknamesWithPrettiestShip.contains(myNickname));

            output.append(String.format("   💰 Initial credits: %d\n\n", initialCredits));

            // Show bonuses and penalties
            if (!myData.isEarlyLanded()) {
                // Normal player gets full bonuses
                int positionBonus = getPositionBonus(myPosition);
                output.append(String.format("   ✅ Arrival reward (%d° posto): +%d 💰\n", myPosition, positionBonus));

                int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), false);
                output.append(String.format("   ✅ Goods sold %s: +%d 💰\n", formatCubes(myData.getAllOwnedCubes()), cubesValue));

                if (playersNicknamesWithPrettiestShip.contains(myNickname)) {
                    int prettiestBonus = getPrettiestShipBonus();
                    output.append(String.format("   ✅ Prettiest ship: +%d 💰\n", prettiestBonus));
                }
            } else {
                // Early landed player
                output.append("   ❌ Arrival rewards: -- (landed early)\n");

                int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), true);
                output.append(String.format("   ⚠️  Goods solds %s (HALVED): +%d 💰\n",
                        formatCubes(myData.getAllOwnedCubes()), cubesValue));

                output.append("   ❌ Prettiest ship: -- (landed earlier)\n");
            }

            // Lost components penalty (always applied)
            if (myData.getLostComponents() > 0) {
                output.append(String.format("   ❌ Lost components (%d): -%d 💰\n",
                        myData.getLostComponents(), myData.getLostComponents()));
            }

            output.append("\n   ─────────────────────────────────────────\n");
            output.append(String.format("   💎 FINAL TOTAL: %d 💰\n", myData.getTotalCredits()));
            output.append("\n═══════════════════════════════════════════════════════════════════════\n\n");

            // Final message
            if (myData.isEarlyLanded()) {
                if (myData.getTotalCredits() > 0) {
                    output.append("   🛬 You landed early but still earned some credits!\n");
                } else {
                    output.append("   🛬 You landed early and earned no credits.\n");
                }
            }

            if (myData.getTotalCredits() == maxCredits && myData.getTotalCredits() > 0) {
                output.append("   🏆 Congratulation! You're the overall winner! 🎊\n");
            } else if (myData.getTotalCredits() > 0) {
                output.append("   🎉 Congratulation! You're among the winners!\n");
            } else {
                output.append("   😔 Unfortunately, you're not among the winners this time...\n");
            }
        }

        output.append("\n");

        showMessage(output.toString(), STANDARD);
        showMessage("Press any key to leave the game", ASK);
    }


    // Helper method to calculate initial credits by removing bonuses/penalties
    private int calculateInitialCredits(PlayerFinalData data, int position, boolean hasPrettiestShip) {
        int totalCredits = data.getTotalCredits();

        // Remove position bonus (only for normal landing)
        if (!data.isEarlyLanded()) {
            totalCredits -= getPositionBonus(position);
        }

        // Remove cubes value
        int cubesValue = calculateCubesValue(data.getAllOwnedCubes(), data.isEarlyLanded());
        totalCredits -= cubesValue;

        // Remove prettiest ship bonus (only for normal landing)
        if (!data.isEarlyLanded() && hasPrettiestShip) {
            totalCredits -= getPrettiestShipBonus();
        }

        // Add back lost components penalty (it was subtracted, so we add it)
        totalCredits += data.getLostComponents();

        return totalCredits;
    }

    // Helper method to get position bonus based on game rules
    private int getPositionBonus(int position) {
        // Check if it's test flight or normal game
        if (isTestFlight) {
            // Test flight bonuses: 4, 3, 2, 1
            switch (position) {
                case 1: return 4;
                case 2: return 3;
                case 3: return 2;
                case 4: return 1;
                default: return 0;
            }
        } else {
            // Normal game bonuses: 8, 6, 4, 2
            switch (position) {
                case 1: return 8;
                case 2: return 6;
                case 3: return 4;
                case 4: return 2;
                default: return 0;
            }
        }
    }

    // Helper method to get prettiest ship bonus based on game mode
    private int getPrettiestShipBonus() {
        return isTestFlight ? 2 : 4;
    }

    // Helper method to calculate total value of cargo cubes
    private int calculateCubesValue(List<CargoCube> cubes, boolean isHalved) {
        int total = 0;
        for (CargoCube cube : cubes) {
            switch (cube) {
                case BLUE: total += 1; break;
                case GREEN: total += 2; break;
                case YELLOW: total += 3; break;
                case RED: total += 4; break;
            }
        }
        return isHalved ? total / 2 : total;
    }

    // Helper method to format cubes display
    private String formatCubes(List<CargoCube> cubes) {
        int red = 0, yellow = 0, green = 0, blue = 0;

        for (CargoCube cube : cubes) {
            switch (cube) {
                case RED: red++; break;
                case YELLOW: yellow++; break;
                case GREEN: green++; break;
                case BLUE: blue++; break;
            }
        }

        return String.format("%d🟥 %d🟨 %d🟩 %d🟦", red, yellow, green, blue);
    }

    /**
     * Displays a message indicating that a player has landed early in the game.
     * The message differs depending on whether the current player or another player
     * has landed early. It provides detailed information about the consequences
     * and status updates for the early landing.
     *
     * @param nickname The nickname of the player who landed early. If the nickname matches the
     *                 current player's nickname, a personalized message is shown. Otherwise,
     *                 a general announcement message about the other player's early landing is displayed.
     */
    @Override
    public void showPlayerEarlyLanded(String nickname) {
        // Check if it's the current player who landed early
        if (nickname.equals(clientModel.getMyNickname())) {
            // Current player has landed early
            StringBuilder output = new StringBuilder();
            output.append("\n");
            output.append("╔═══════════════════════════════════════════════════════════════════════╗\n");
            output.append("║                    🛬 EARLY LANDING 🛬                                ║\n");
            output.append("╠═══════════════════════════════════════════════════════════════════════╣\n");
            output.append("║                                                                       ║\n");
            output.append("║ Your route marker rocket has been removed from the flight board!      ║\n");
            output.append("║                                                                       ║\n");
            output.append("║ You have left the space race and landed safely.                       ║\n");
            output.append("║ From the next card on, you will only be a spectator.                  ║\n");
            output.append("║                                                                       ║\n");
            output.append("║ ⚠️  REMEMBER:                                                         ║\n");
            output.append("║ • No cards will affect you anymore                                   ║\n");
            output.append("║ • You won't receive rewards for arrival order                        ║\n");
            output.append("║ • You can't compete for the prettiest ship                           ║\n");
            output.append("║ • Your goods will be sold at half price                              ║\n");
            output.append("║ • You'll still pay penalties for lost components                     ║\n");
            output.append("║                                                                       ║\n");
            output.append("║ You can still win if you’ve earned enough credits!                    ║\n");
            output.append("║                                                                       ║\n");
            output.append("╚═══════════════════════════════════════════════════════════════════════╝\n");
            showMessage(output.toString(), STANDARD);

        } else {
            // Another player has landed early - dynamic formatting for name
            StringBuilder output = new StringBuilder();
            output.append("\n");
            output.append("╔═══════════════════════════════════════════════════════════════════════╗\n");
            output.append("║                        📢 FLIGHT ANNOUNCEMENT 📢                       ║\n");
            output.append("╠═══════════════════════════════════════════════════════════════════════╣\n");
            output.append("║                                                                       ║\n");

            // Format the announcement with dynamic spacing
            String announcement = nickname + " has left the race!";
            int padding = (69 - announcement.length()) / 2;
            String paddedAnnouncement = String.format("%" + padding + "s%s%" + padding + "s", "", announcement, "");
            output.append(String.format("║%-69s║\n", paddedAnnouncement));

            output.append("║                                                                       ║\n");
            output.append("║ Their rocket has landed early.                                        ║\n");
            output.append("║ From the next card onward, they will no longer join the adventures.  ║\n");
            output.append("║                                                                       ║\n");
            output.append("╚═══════════════════════════════════════════════════════════════════════╝\n");

            showMessage(output.toString(), NOTIFICATION_INFO);
        }
    }


    /**
     * Determines the color associated with the specified coordinate based on the given color map.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @param colorMap a map where each key is a color and the associated value is a set of coordinates
     *                 that are mapped to that color
     * @return the color associated with the specified coordinate if found, or an empty string if no color is specified
     */
    private String getColorForCoordinate(int x, int y, Map<String, Set<Coordinates>> colorMap) {
        Coordinates coord = new Coordinates(x, y);

        for (Map.Entry<String, Set<Coordinates>> entry : colorMap.entrySet()) {
            if (entry.getValue().contains(coord)) {
                return entry.getKey();
            }
        }

        return ""; // Nessun colore specificato, usa il default
    }

    /**
     * Displays a list of visible components and prompts the user to choose one of them.
     * If no visible components are available, a message is displayed and the Build Ship Board menu is shown.
     *
     * @param visibleComponents a map containing visible components, where the key is the component's index
     *                           and the value is the component object
     */
    @Override
    public void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {

        if (visibleComponents.isEmpty()) {
            showMessage("No visible components available.", STANDARD);
            showBuildShipBoardMenu();
            return;
        }

        StringBuilder visibleComponentsList = new StringBuilder();
        visibleComponentsList.append("\nHere's the visible components:");

        visibleComponents.keySet().forEach(index -> {
            String[] visibleComponent = visibleComponents.get(index).toString().split("\\n");
            StringBuilder output = new StringBuilder();
            output.append("\n").append(index).append(". ").append(visibleComponent[1]);
            for (int i = 2; i < visibleComponent.length; i++) {
                output.append("\n").append(visibleComponent[i]);
            }
            output.append("\n");
            visibleComponentsList.append(output);
        });

        showMessage(visibleComponentsList.toString(), STANDARD);
        showMessage("Choose one of the visible components (0 to go back): ", ASK);
    }


    /**
     * Displays the visit location menu based on the type of adventure card the player has encountered.
     * This method provides contextual information about the location, including potential rewards
     * and the associated costs or requirements to visit it. Additionally, it checks if the player
     * meets the necessary conditions (e.g., sufficient crew members) to visit the location.
     * If the player does not meet the requirements, the client state is updated, and appropriate
     * messages are shown. Otherwise, the player is prompted to decide whether to visit the location.
     *
     * Behavior:
     * - For "AbandonedShip" cards:
     *   - Describes the rewards and penalties of visiting the abandoned ship.
     *   - Checks if the player has enough crew members. If not, disallows the visit and informs the player.
     * - For "AbandonedStation" cards:
     *   - Describes the rewards and the required minimum crew members.
     *   - Checks if the player has enough crew members. If not, disallows the visit and informs the player.
     * - Prompts the user with the question of whether they want to visit the location.
     *
     * Preconditions:
     * - The client's current adventure card is properly set in the client model.
     * - The client model should provide accurate and up-to-date information about the player's
     *   ship, crew members, and other relevant details.
     *
     * Postconditions:
     * - If the player does not meet the requirements for visiting the location, the client state
     *   will be set to {@code ClientState.CANNOT_VISIT_LOCATION}, and the player will not be allowed to proceed.
     * - A menu option is displayed to allow the player to decide if they want to visit the location.
     */
    @Override
    public void showVisitLocationMenu() {
        //setClientState(ClientState.VISIT_LOCATION_MENU);

        ClientCard card = clientModel.getCurrAdventureCard();
        StringBuilder message = new StringBuilder("\n");

        if (card.getCardType().equals("AbandonedShip")) {
            ClientAbandonedShip shipCard = (ClientAbandonedShip) card;
            message.append("You've found an abandoned ship!\n\n");
            message.append("If you choose to visit this ship, you'll need to sacrifice ")
                    .append(shipCard.getCrewMalus()).append(" crew members.\n");
            message.append("In return, you'll receive ").append(shipCard.getReward())
                    .append(" credits, but you'll move back ").append(shipCard.getStepsBack())
                    .append(" spaces on the route.\n\n");

            // Check if player has enough crew
            int totalCrew = clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size();
            System.out.println("\n\n my crew members: " + totalCrew);
            if (totalCrew < shipCard.getCrewMalus()) {
                setClientState(ClientState.CANNOT_VISIT_LOCATION);
                message.append(ANSI_RED + "WARNING: You only have ").append(totalCrew)
                        .append(" crew members. You cannot accept this reward!\n\n"+ANSI_RESET);
                showMessage(message.toString(), STANDARD);
                showMessage("Press any key to continue.", ASK);
                return;
            }
        } else if (card.getCardType().equals("AbandonedStation")) {
            ClientAbandonedStation stationCard = (ClientAbandonedStation) card;
            message.append("You've found an abandoned station!\n\n");
            message.append("If you have at least ").append(stationCard.getRequiredCrewMembers())
                   .append(" crew members, you can visit to get cargo.\n");

            // Check if player has enough crew
            int totalCrew = clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size();
            if (totalCrew < stationCard.getRequiredCrewMembers()) {
                setClientState(ClientState.CANNOT_VISIT_LOCATION);
                message.append(ANSI_RED + "WARNING: You only have ").append(totalCrew)
                       .append(" crew members. You cannot accept this reward!\n\n" + ANSI_RESET);
                showMessage(message.toString(), STANDARD);
                showMessage("Press any key to continue.", ASK);
                return;
            }
        }

        message.append("Do you want to visit this location? [Y/n]");
        showMessage(message.toString(), ASK);
    }

    /**
     * Displays a disconnect message to the user and terminates the application.
     *
     * @param message the disconnect message to be displayed
     */
    @Override
    public void showDisconnectMessage(String message) {
        showMessage(message, ERROR);
        System.exit(0);
    }

    /**
     * Displays the menu or messages associated with throwing dice during specific game events based on the current adventure card and the player's turn.
     *
     * This method identifies the type of adventure card and presents relevant messages to the player or other participants.
     * It determines the client state depending on whether it is the player's turn and provides instructions or updates accordingly.
     *
     * Behavior:
     * - If the adventure card type is "Pirates" or "SlaveTraders", an appropriate message indicating enemy attack is shown.
     * - If the adventure card type is "MeteoriteStorm", a message indicating an impending meteorite storm is displayed.
     * - If it is the player's turn, the client state is set to THROW_DICES_MENU, and a prompt to throw the dice is shown.
     * - If it is not the player's turn, the client state is set to WAIT_PLAYER, and a message indicating that another player is throwing dice is displayed.
     */
    @Override
    public void showThrowDicesMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card.getCardType().equals("Pirates") ||card.getCardType().equals("SlaveTraders")) {
            showMessage("\nThe enemies are firing at you!", STANDARD);
        } else if (card.getCardType().equals("MeteoriteStorm")) {
            showMessage("\nMeteors are heading your way!", STANDARD);
        }

        if (clientModel.isMyTurn()) {
            setClientState(ClientState.THROW_DICES_MENU);
            showMessage("Press Enter to throw dice and see where they hit...", ASK);
        } else {
            setClientState(WAIT_PLAYER);
            showMessage("The first player is throwing dices, wait...", STANDARD);
        }
    }

    /**
     * Updated menu display methods that use ClientCard information
     */
    @Override
    public void showChoosePlanetMenu() {
        setClientState(ClientState.CHOOSE_PLANET_MENU);

        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientPlanets)) {
            showMessage("Error: Expected Planets card", ERROR);
            return;
        }

        ClientPlanets planets = (ClientPlanets) card;
        StringBuilder planetsInfo = new StringBuilder("\nAvailable planets:\n");

        for (int i = 0; i < planets.getAvailablePlanets().size(); i++) {
            Planet planet = planets.getAvailablePlanets().get(i);
            if (!planet.isBusy()) {
                planetsInfo.append(i + 1).append(". Planet ").append(i + 1).append(": ");
                planet.getReward().forEach(cube -> planetsInfo.append(cube.name()).append(" "));
                planetsInfo.append("\n");
            }
        }

        planetsInfo.append("0. Skip (don't land on any planet)\n");
        planetsInfo.append("Choose a planet to land on: ");

        showMessage(planetsInfo.toString(), ASK);
    }

    /**
     * Displays the menu for choosing engines during the player's turn in the game.
     * This method handles the logic for displaying the available engine options
     * and necessary information for the player to make a decision.
     *
     * Functionality includes:
     * - Resetting the selection states for engines and batteries.
     * - Showing the current state of the engine components using a visual highlight.
     * - Managing conditions where certain engine options (e.g., double engines)
     *   may be unavailable due to game constraints, such as the absence of batteries
     *   or eligible engine types.
     * - Displaying relevant messages to inform or warn the player based on game state,
     *   such as the risk of elimination in specific scenarios.
     * - Allowing the player to specify engine coordinates or confirm their selections.
     * - Updating the client state to appropriately reflect the current action context.
     */
    @Override
    public void showChooseEnginesMenu() {

        // Reset the selection state
        selectedEngines.clear();
        selectedBatteries.clear();

        showEngineWithColor();

        if(clientModel.getShipboardOf(clientController.getNickname()).getDoubleEngines().isEmpty() ) {
            showMessage("No double engines available.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_ENGINES);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate double engine.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_ENGINES);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate double engine.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_ENGINES);
            return;
        }

        showMessage("\nYou can activate double engines." +
                "Each double engine requires one battery.", STANDARD);
        setClientState(ClientState.CHOOSE_ENGINES_MENU);
        showMessage("Enter coordinates of a double engine (row column) or 'done' when finished: ", ASK);
    }

    /**
     * Displays the "Accept the Reward" menu to the user based on the current adventure card.
     *
     * Depending on the type of the adventure card and its attributes, this method extracts
     * the reward details and the associated cost (e.g., steps lost in the game) if the card
     * contains a reward. The details are presented to the user in a sequence of messages.
     *
     * The user is then prompted to decide whether to accept the reward or not.
     *
     * Behavioral Notes:
     * - If the current adventure card has no reward, the menu only shows a success message.
     * - Rewards and step costs are displayed for specific card types that extend the
     *   {@code ClientCard} class (e.g., {@code ClientPirates}, {@code ClientSlaveTraders}).
     * - The specific handling logic for other card types, such as {@code ClientAbandonedShip},
     *   may be included in the future but is currently commented out.
     *
     * Messages:
     * - A success message indicating that the user has succeeded in the adventure.
     * - A detailed message about the potential reward and its cost, if applicable.
     * - A prompt asking the user to accept or decline the reward.
     */
    @Override
    public void showAcceptTheRewardMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        String rewardStr = "";
        String stepsStr = "";

        // Extract reward and steps information based on card type
        if (card.hasReward()) {
            if (card instanceof ClientPirates) {
                ClientPirates pirates = (ClientPirates) card;
                rewardStr = String.valueOf(pirates.getReward());
                stepsStr = String.valueOf(pirates.getStepsBack());
//            } else if (card instanceof ClientAbandonedShip) {
//                ClientAbandonedShip ship = (ClientAbandonedShip) card;
//                rewardStr = String.valueOf(ship.getReward());
//                stepsStr = String.valueOf(ship.getStepsBack());
            } else if (card instanceof ClientSlaveTraders) {
                ClientSlaveTraders traders = (ClientSlaveTraders) card;
                rewardStr = String.valueOf(traders.getReward());
                stepsStr = String.valueOf(traders.getStepsBack());
            }
        }

        showMessage("\nYou've succeeded!", STANDARD);
        if (!rewardStr.isEmpty() && !stepsStr.isEmpty()) {
            showMessage("You can get " + rewardStr + " credits but will lose " + stepsStr + " flight days.", STANDARD);
        }

        showMessage("Do you want to accept the reward? [Y/n]", ASK);
    }

    /**
     * Displays the menu to choose cannons for a combat scenario in the game.
     * This method provides information about the current adversary, their required firepower,
     * and the available options for using cannons (single or double) based on the player's resources.
     *
     * The method performs the following actions:
     * 1. Resets the selected cannons and batteries to clear previous state.
     * 2. Determines the type of adventure card currently in play and displays relevant
     *    enemy firepower and conditions for defeating them.
     * 3. Checks the availability of double cannons and batteries:
     *    - If double cannons are unavailable or batteries are insufficient, the method adjusts
     *      the state to restrict usage to single cannons and prompts the player to proceed.
     * 4. If double cannons can be activated, instructions are displayed to guide the player
     *    through selecting double cannon coordinates or finalizing their selection.
     *
     * The client's state is updated dynamically based on the availability of resources, ensuring
     * that appropriate restrictions or options are presented to the player.
     */
    @Override
    public void showChooseCannonsMenu() {
        // Reset the selection state
        selectedCannons.clear();
        selectedBatteries.clear();

        ClientCard card = clientModel.getCurrAdventureCard();
        StringBuilder message = new StringBuilder("\n");

        // Extract fire power requirement based on card type
        if (card instanceof ClientPirates) {
            message.append("\nEnemy firepower: " ).append(((ClientPirates) card).getRequiredFirePower());
            message.append("\n You can choose double cannons to defeat pirates");
            message.append("\n REMEMBER! If you don't defeat, you will be attacked by shots!");
        } else if (card instanceof ClientSlaveTraders) {
            message.append("\nEnemy firepower: " ).append(((ClientSlaveTraders) card).getRequiredFirePower());
            message.append("\n You can choose double cannons to defeat slave traders");
            message.append("\n REMEMBER! If you don't defeat, you will lose crew members!");
        } else if (card instanceof ClientSmugglers) {
            message.append("\nEnemy firepower: " ).append(((ClientSmugglers) card).getRequiredFirePower());
            message.append("\n You can choose double cannons to defeat slave traders");
            message.append("\n REMEMBER! If you don't defeat, you will lose cargo cubes!");
        }

        showCannonWithColor();

        showMessage(message.toString(), STANDARD);

        if(clientModel.getShipboardOf(clientController.getNickname()).getDoubleCannons().isEmpty() ) {
            showMessage("No double cannon available.", STANDARD);
            showMessage("You can use only single cannon", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_CANNONS);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate double cannon.", STANDARD);
            showMessage("You can use only single cannon", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_CANNONS);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate double cannon.", STANDARD);
            showMessage("You can use only single cannon", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_CHOOSE_CANNONS);
            return;
        }

        showMessage("\nYou can activate double cannon." +
                "Each double cannon requires one battery.", STANDARD);
        setClientState(ClientState.CHOOSE_CANNONS_MENU);
        showMessage("Enter coordinates of a double cannon (row column) or 'done' when finished: ", ASK);
    }

    /**
     * Displays the menu for handling a small dangerous object in the game.
     * This method provides options and information to the player for defending against
     * an approaching small dangerous object or allowing it to hit their ship. It also
     * checks the availability of resources (shields and batteries) to determine if the player
     * can actively defend.
     *
     * Behavior:
     * 1. Displays information about the incoming small dangerous object.
     * 2. Visually highlights the shield options with appropriate colors.
     * 3. Determines if the player has no shields, no battery boxes, or no available batteries,
     *    and displays relevant messages.
     * 4. If no defense is possible, the game state is updated accordingly and the player
     *    is informed they cannot defend.
     * 5. If defense is possible, prompts the player to activate a shield by inputting shield
     *    coordinates, or skip the defense.
     *
     * Client-specific state and messages are managed to ensure proper communication
     * of the game's ongoing situation.
     */
    @Override
    public void showSmallDanObjMenu() {
        //setClientState(ClientState.HANDLE_SMALL_DANGEROUS_MENU);

        StringBuilder smallDangerousInfo = new StringBuilder();
        smallDangerousInfo.append("\n").append(clientModel.getCurrDangerousObj().getType()).append(" incoming!");
        smallDangerousInfo.append("\n").append(clientModel.getCurrDangerousObj().toString());
        showMessage(smallDangerousInfo.toString(),STANDARD);

        showShieldWithColor();

        if (clientModel.getShipboardOf(clientController.getNickname()).getShields().isEmpty() ) {
            showMessage("No shield available.", STANDARD);
            showMessage("ATTENTION! You can't defend!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_SMALL_DANGEROUS_OBJ);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate shield.", STANDARD);
            showMessage("ATTENTION! You can't defend!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_SMALL_DANGEROUS_OBJ);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare nessuno scudo
        if (!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate shield.", STANDARD);
            showMessage("ATTENTION! You can't defend!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_SMALL_DANGEROUS_OBJ);
            return;
        }


        showMessage("You can activate a shield or let the object hit your ship.", STANDARD);
        showMessage("Enter coordinates of a shield (row column) or 'done' to skip: ", ASK);

    }

    /**
     * Displays the menu for handling a big meteorite event in the game. This method guides the user to
     * either defend against the big meteorite using available weaponry or skip the*/
    @Override
    public void showBigMeteoriteMenu() {
        // setClientState(ClientState.HANDLE_BIG_METEORITE_MENU);
        selectedCannons.clear();
        selectedBatteries.clear();

        StringBuilder bigMeteoriteInfo = new StringBuilder();
        bigMeteoriteInfo.append("\n").append(" Big Meteorite incoming!");
        bigMeteoriteInfo.append("\n").append(clientModel.getCurrDangerousObj().toString());
        showMessage(bigMeteoriteInfo.toString(),STANDARD);

        showCannonWithColor();

        if(clientModel.getShipboardOf(clientController.getNickname()).getDoubleCannons().isEmpty() ) {
            showMessage("No double Cannon available.", STANDARD);
            showMessage("ATTENTION! You can defend only with single cannon!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_BIG_DANGEROUS_OBJ);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate Double Cannons.", STANDARD);
            showMessage("ATTENTION! You can defend only with single cannon!", NOTIFICATION_INFO);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_BIG_DANGEROUS_OBJ);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate double Cannon.", STANDARD);
            showMessage("You can use only single Cannon", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(CANNOT_DEFEND_FROM_BIG_DANGEROUS_OBJ);
            return;
        }

        showMessage("You can use a double or single cannon to destroy it or let it hit your ship.", STANDARD);
        showMessage("Enter coordinates of a double cannon (row column) or 'done' to skip: ", ASK);
    }

    /**
     * Displays the "Big Shot" menu to the user, providing details about an incoming
     * dangerous object and the ensuing impact on the ship.
     *
     * This method constructs and outputs formatted messages with information about the
     * type and details of the incoming "Big Shot" object, accompanied by a prompt
     * for the user to proceed and view its effects.
     *
     * The messages displayed include:
     * - The type of the incoming dangerous object.
     * - Details of the incoming object as provided by its string representation.
     * - A warning about the unstoppable nature of the "Big Shot."
     * - A prompt directing the user to press Enter to proceed and observe the effects.
     */
    @Override
    public void showBigShotMenu() {
        StringBuilder BigShotInfo = new StringBuilder();
        BigShotInfo.append("\n").append(clientModel.getCurrDangerousObj().getType()).append(" incoming!");
        BigShotInfo.append("\n").append(clientModel.getCurrDangerousObj().toString());
        showMessage(BigShotInfo.toString(),STANDARD);
        showMessage("\nBig Shot incoming! Nothing can stop this...", STANDARD);
        showMessage("Press Enter to see the effect on your ship", ASK);
    }

    /**
     * Handles the display and interaction for removing crew members during the game.
     * This method is invoked when the player needs to sacrifice crew members as part of the game mechanics.
     *
     * The method updates the client state to a specific menu, displays the ship board to give the player
     * a visual representation of occupied cabins, and prompts the player to remove the required number
     * of crew members from specific cabins. It ensures the game state is properly updated while validating
     * any potential edge cases, such as insufficient crew members.
     *
     * Key operations performed by the method:
     * - Retrieves the number of crew members to be removed from the current adventure card.
     * - Displays the list of currently occupied cabins, including coordinates and crew count.
     * - Checks whether the player has enough crew members to meet the removal requirement and handles
     *   edge cases accordingly (e.g., not enough crew members or no occupied cabins).
     * - Prompts the user to input cabin coordinates to remove crew from, or to indicate completion.
     *
     * Messages are displayed to guide the player through the process, including status messages for
     * illegal states or warnings when the required crew removal exceeds available crew members.
     */
    @Override
    public void showHandleRemoveCrewMembersMenu() {
        setClientState(ClientState.CHOOSE_CABIN_MENU);

        // Show the ship to visualize cabins
        this.showMyShipBoard();

        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        int crewToRemove = 0;

        crewToRemove = card.getCrewMalus();

        showMessage("\nYou need to remove " + crewToRemove + " crew member(s).", STANDARD);

        // Get cabins with crew
        Map<Coordinates, Cabin> cabinsWithCrew = clientModel.getShipboardOf(clientModel.getMyNickname())
                .getCoordinatesAndCabinsWithCrew();

        // Show cabins with crew
        StringBuilder cabinInfo = new StringBuilder("\nYour ship has the following occupied cabins:\n");

        if (cabinsWithCrew.isEmpty()) {
            cabinInfo.append("You have no occupied cabins. You cannot sacrifice crew members.\n");
            showMessage(cabinInfo.toString(), STANDARD);
            showMessage("ILLEGAL STATE", ERROR);
            return;
        } else {
            for (Map.Entry<Coordinates, Cabin> entry : cabinsWithCrew.entrySet()) {
                Coordinates coords = entry.getKey();
                Cabin cabin = entry.getValue();
                cabinInfo.append(String.format("%s(%d, %d)%s: %s - Contains %d crew member(s)\n",
                        ANSI_GREEN, coords.getX() + 1, coords.getY() + 1, ANSI_RESET,
                        cabin.getLabel(), cabin.getInhabitants().size()));
            }
        }

        if (card.getCrewMalus() >= clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size()) {
            cabinInfo.append("You have not enough crew members. You must sacrifice all of them.\n");
            showMessage(cabinInfo.toString(), STANDARD);
            showMessage("ATTENTION! you will be eliminated", ERROR);
        }

        showMessage(cabinInfo.toString(), STANDARD);
        showMessage("Enter coordinates of a cabin to remove crew from (row column) or 'done' when finished: ", ASK);

    }

    /**
     * Displays the Handle Cubes Reward Menu, allowing the player to handle cube rewards during the game.
     *
     * This method is responsible for managing the process of distributing cargo cubes to the player's ship,
     * ensuring that the player has available storage space. If no storage space is available, appropriate
     * messages are displayed, and the method finalizes without proceeding to cube redistribution.
     *
     * Functionality includes:
     * - Updating the client state to HANDLE_CUBES_REWARD_MENU.
     * - Extracting the cube rewards directly from the current card.
     * - Initializing a storage manager to manage the cube rewards and available storage space on the player's ship.
     * - Verifying storage availability. Displays critical notifications if no storage is available.
     * - Initiating the cube redistribution process if storage constraints are met.
     * - Displaying the Cube Redistribution Menu for further distribution actions.
     *
     * Note: If storage is unavailable, the client state is updated to CANNOT_ACCEPT_CUBES_REWARDS,
     * and the game offers an option to proceed to the next player.
     */
    @Override
    public void showHandleCubesRewardMenu() {
        setClientState(ClientState.HANDLE_CUBES_REWARD_MENU);

        // Get cube rewards directly from the current card
        List<CargoCube> rewardCubes;
        rewardCubes = clientModel.extractCubeRewardsFromCurrentCard();

        // Initialize the storage manager with the cube rewards
        storageManager = new StorageSelectionManager(rewardCubes, 0, getMyShipBoard());

        // Check if the player has any storage available
        if (!storageManager.hasAnyStorage()) {
            showMessage("\nYou have no available storage on your ship. You can’t accept any reward cubes", NOTIFICATION_CRITICAL);
            showMessage("The game will continue with the next player.", STANDARD);
            showMessage("Press any key to continue.", ASK);
            setClientState(ClientState.CANNOT_ACCEPT_CUBES_REWARDS);
            return;
        }

        // Inizializza modalità ridistribuzione con i cubi bonus
        storageManager.startRedistribution(rewardCubes);

        showCubeRedistributionMenu();
    }

    private void showStoragesInfo() {
        showStoragesInfo(clientModel.getMyNickname());
    }

    /**
     * Shows storage information for the specified player
     * @param playerNickname the nickname of the player whose storages to display
     */
    public void showStoragesInfo(String playerNickname) {
        StringBuilder storageInfo = new StringBuilder("\n=== AVAILABLE STORAGE - " + playerNickname + " ===\n");

        ShipBoardClient shipBoard = clientModel.getShipboardOf(playerNickname);
        if (shipBoard == null) {
            storageInfo.append("Player not found or no ship data available.\n");
            showMessage(storageInfo.toString(), STANDARD);
            return;
        }

        Map<Coordinates, Storage> coordinatesAndStorages = shipBoard.getCoordinatesAndStorages();

        if (coordinatesAndStorages.isEmpty()) {
            storageInfo.append("No storage available on this ship.\n");
        } else {
            int totalStorages = coordinatesAndStorages.size();
            int totalCubes = 0;
            int totalCapacity = 0;
            int totalValue = 0;

            for (Map.Entry<Coordinates, Storage> entry : coordinatesAndStorages.entrySet()) {
                Coordinates coords = entry.getKey();
                Storage storage = entry.getValue();
                List<CargoCube> storedCubes = storage.getStockedCubes();
                boolean isSpecial = storage instanceof SpecialStorage;

                // Coordinate con colore
                storageInfo.append(String.format("%s(%d,%d)%s: ",
                    ANSI_GREEN, coords.getX() + 1, coords.getY() + 1, ANSI_RESET));

                // Tipo storage con indicatore compatibilità
                if (isSpecial) {
                    storageInfo.append(ANSI_YELLOW + "SpecialStorage" + ANSI_RESET);
                } else {
                    storageInfo.append("StandardStorage ");
                }

                // Capacità con indicatore di stato
                storageInfo.append(" - ").append(storage.getStockedCubes().size()).append("/").append(storage.getMaxCapacity());

                // Extract current/max capacity for totals
                totalCubes += storage.getStockedCubes().size();
                totalCapacity += storage.getMaxCapacity();

                if (storage.isFull()) {
                    storageInfo.append(" [FULL] ");
                }

                // Contenuto con colori
                storageInfo.append(" - Contains: ");
                if (storedCubes.isEmpty()) {
                    storageInfo.append(ANSI_CYAN + "empty" + ANSI_RESET);
                } else {
                    List<String> formattedCubes = new ArrayList<>();
                    for (CargoCube cube : storedCubes) {
                        String cubeColor = switch (cube) {
                            case RED -> ANSI_RED;
                            case YELLOW -> ANSI_YELLOW;
                            case GREEN -> ANSI_GREEN;
                            case BLUE -> ANSI_BLUE;
                        };
                        formattedCubes.add(cubeColor + cube.name() + ANSI_RESET);
                        totalValue += cube.getValue();
                    }
                    storageInfo.append(String.join(" ", formattedCubes));
                }

                storageInfo.append("\n");
            }

            // Add summary information
            storageInfo.append(String.format("\nTotal Storages: %d | Total Cubes Stored: %d/%d\n",
                totalStorages, totalCubes, totalCapacity));

            if (totalValue > 0) {
                storageInfo.append(String.format("Total Cube Value: %d points\n", totalValue));
            }
        }
        showMessage(storageInfo.toString(), STANDARD);
    }

    /**
     * Shows battery boxes information for the specified player
     * @param playerNickname the nickname of the player whose battery boxes to display
     */
    public void showBatteryBoxesInfo(String playerNickname) {
        StringBuilder batteryInfo = new StringBuilder("\n=== BATTERY BOXES INFO - " + playerNickname + " ===\n");

        ShipBoardClient shipBoard = clientModel.getShipboardOf(playerNickname);
        if (shipBoard == null) {
            batteryInfo.append("Player not found or no ship data available.\n");
            showMessage(batteryInfo.toString(), STANDARD);
            return;
        }

        Map<Coordinates, BatteryBox> coordinatesAndBatteries = shipBoard.getCoordinatesAndBatteries();

        if (coordinatesAndBatteries.isEmpty()) {
            batteryInfo.append("No battery boxes available on this ship.\n");
        } else {
            int totalBatteries = 0;
            int totalCapacity = 0;

            // Process battery boxes using the coordinates map
            for (Map.Entry<Coordinates, BatteryBox> entry : coordinatesAndBatteries.entrySet()) {
                Coordinates coords = entry.getKey();
                BatteryBox batteryBox = entry.getValue();

                int remaining = batteryBox.getRemainingBatteries();
                int max = batteryBox.getMaxBatteryCapacity();
                totalBatteries += remaining;
                totalCapacity += max;

                batteryInfo.append(String.format("%s(%d,%d)%s: BatteryBox - %d/%d batteries remaining",
                    ANSI_GREEN, coords.getX() + 1, coords.getY() + 1, ANSI_RESET, remaining, max));

                if (remaining == 0) {
                    batteryInfo.append(" " + ANSI_RED + "[EMPTY]" + ANSI_RESET);
                } else if (remaining == max) {
                    batteryInfo.append(" " + ANSI_GREEN + "[FULL]" + ANSI_RESET);
                }

                batteryInfo.append("\n");
            }

            batteryInfo.append(String.format("\nTotal Battery Boxes: %d | Total Batteries Available: %d/%d\n",
                coordinatesAndBatteries.size(), totalBatteries, totalCapacity));
        }

        showMessage(batteryInfo.toString(), STANDARD);
    }

    /**
     * Shows cabins information for the specified player
     * @param playerNickname the nickname of the player whose cabins to display
     */
    public void showCabinsInfo(String playerNickname) {
        StringBuilder cabinInfo = new StringBuilder("\n=== CABINS INFO - " + playerNickname + " ===\n");

        ShipBoardClient shipBoard = clientModel.getShipboardOf(playerNickname);
        if (shipBoard == null) {
            cabinInfo.append("Player not found or no ship data available.\n");
            showMessage(cabinInfo.toString(), STANDARD);
            return;
        }

        Map<Coordinates, Cabin> coordinatesAndCabins = shipBoard.getCoordinatesAndCabins();
        MainCabin mainCabin = shipBoard.getMainCabin();

        if (coordinatesAndCabins.isEmpty() && mainCabin == null) {
            cabinInfo.append("No cabins available on this ship.\n");
        } else {
            int totalCrewMembers = 0;
            int humanCount = 0;
            int alienCount = 0;
            int totalCabins = 0;

            // Process regular cabins using the coordinates map
            for (Map.Entry<Coordinates, Cabin> entry : coordinatesAndCabins.entrySet()) {
                Coordinates coords = entry.getKey();
                Cabin cabin = entry.getValue();
                totalCabins++;

                if(cabin==mainCabin)
                    continue;

                List<CrewMember> inhabitants = cabin.getInhabitants();

                cabinInfo.append(String.format("%s(%d,%d)%s: Cabin - %d inhabitants → ",
                    ANSI_GREEN, coords.getX() + 1, coords.getY() + 1, ANSI_RESET,
                    inhabitants.size()));

                if (inhabitants.isEmpty()) {
                    cabinInfo.append(ANSI_CYAN + "[EMPTY]" + ANSI_RESET);
                } else {
                    List<String> crewList = new ArrayList<>();
                    for (CrewMember crew : inhabitants) {
                        crewList.add(crew.name());
                        totalCrewMembers++;
                        if (crew.name().equals("HUMAN")) {
                            humanCount++;
                        } else {
                            alienCount++;
                        }
                    }
                    cabinInfo.append(String.join(" ", crewList));
                }

                cabinInfo.append("\n");
            }

            // Process main cabin - always at position (7,7) if not destroyed
            if (mainCabin != null) {
                totalCabins++;
                List<CrewMember> inhabitants = mainCabin.getInhabitants();

                cabinInfo.append(String.format("%s(7,7)%s: MainCabin - %d inhabitants → ",
                    ANSI_YELLOW, ANSI_RESET, inhabitants.size()));

                if (inhabitants.isEmpty()) {
                    cabinInfo.append(ANSI_CYAN + "[EMPTY]" + ANSI_RESET);
                } else {
                    List<String> crewList = new ArrayList<>();
                    for (CrewMember crew : inhabitants) {
                        crewList.add(crew.name());
                        totalCrewMembers++;
                        if (crew.name().equals("HUMAN")) {
                            humanCount++;
                        } else {
                            alienCount++;
                        }
                    }
                    cabinInfo.append(String.join(" ", crewList));
                }

                cabinInfo.append("\n");
            } else {
                cabinInfo.append(String.format("%s(7,7)%s: MainCabin - %s[DESTROYED]%s\n",
                    ANSI_YELLOW, ANSI_RESET, ANSI_RED, ANSI_RESET));
            }

            cabinInfo.append(String.format("\nTotal Cabins: %d | Total Crew Members: %d (%d Humans, %d Aliens)\n",
                totalCabins, totalCrewMembers, humanCount, alienCount));
        }

        showMessage(cabinInfo.toString(), STANDARD);
    }

    /**
     * Shows the current player's credits information
     */
    public void showPlayerCreditsInfo(String playerName) {
        PlayerClientData myData = clientModel.getPlayerClientData().get(playerName);

        if (!playerName.equals(clientModel.getMyNickname())) {
            showMessage("Cannot see other player's credits.\n", STANDARD);
            return;
        }

        int credits = myData.getCredits();
        showMessage("Current Credits: " + credits + "\n", STANDARD);
    }

    /**
     * Shows not active components information for the specified player
     * @param playerNickname the nickname of the player whose not active components to display
     */
    public void showNotActiveComponentsInfo(String playerNickname) {
        StringBuilder componentInfo = new StringBuilder("\n=== NOT ACTIVE COMPONENTS - " + playerNickname + " ===\n");

        ShipBoardClient shipBoard = clientModel.getShipboardOf(playerNickname);
        if (shipBoard == null) {
            componentInfo.append("Player not found or no ship data available.\n");
            showMessage(componentInfo.toString(), STANDARD);
            return;
        }

        List<Component> bookedComponents = shipBoard.getBookedComponents();
        int totalComponents = bookedComponents.size();

        componentInfo.append("Total Components: ").append(totalComponents).append("\n\n");

        if (totalComponents > 0) {
            componentInfo.append("These components may be:\n");
            componentInfo.append("- Reserved components not yet used during build phase\n");
            componentInfo.append("- Components lost during the journey due to damage or events\n");
        } else {
            componentInfo.append("No components are currently not active.\n");
        }

        showMessage(componentInfo.toString(), STANDARD);
    }


    private void processImpossibleCubes() {
        boolean anyAutoSkipped = false;

        // Process cubes that can't be accepted until we find one that can
        // Process cubes that can't be accepted until we find one that can
        while (!storageManager.isSelectionComplete()) {
            String impossibilityReason = storageManager.getCurrentCubeImpossibilityReason();
            if (impossibilityReason == null) {
                break; // This cube can be accepted, stop auto-processing
            }

            // This cube can't be accepted, show reason and skip it
            CargoCube currentCube = storageManager.getCurrentCube();
            showMessage(impossibilityReason + ". This cube will be automatically skipped.", NOTIFICATION_INFO);
            storageManager.skipCurrentCube();
            anyAutoSkipped = true;
        }

        // If all cubes have been processed automatically because none could be accepted, submit and return
        if (storageManager.isSelectionComplete() && anyAutoSkipped) {
            showMessage("All cubes have been processed automatically. Sending data to the server...", STANDARD);
            List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
            clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
        }
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Displays the epidemic menu to notify the user of an ongoing epidemic event.
     * The method presents messages explaining the impact of the epidemic on
     * connected occupied cabins in the fleet and prompts the user to proceed.
     *
     * The displayed messages include:
     * - Notification of the epidemic spreading through the fleet.
     * - Explanation of how the epidemic affects occupied cabins.
     * - An instruction to press any key to continue and observe the epidemic's spread.
     *
     * Utilizes the showMessage method to output information to the user.
     */
    @Override
    public void showEpidemicMenu() {
        showMessage("\nAn epidemic is spreading throughout the fleet!", STANDARD);
        showMessage("Each occupied cabin connected to another occupied cabin will lose one crew member.", STANDARD);
        showMessage("Press any key see how the epidemic is going to spread...", STANDARD);
    }

    /**
     * Displays the Stardust menu in the game client.
     *
     * This method updates the client state to indicate the Stardust menu is active
     * and provides contextual messages to the user about the effects of stardust
     * on their ship's progress. It calculates the number of flight days lost
     * based on exposed connectors on the player's ship and informs the user about
     * the result. The menu prompts the user to proceed to view the card effect.
     */
    @Override
    public void showStardustMenu() {
        setClientState(ClientState.STARDUST_MENU);

        showMessage("\nStardust has been detected in your flight path!", STANDARD);
        showMessage("Each ship will move back one space for every exposed connector.", STANDARD);

        int exposedConnector = clientModel.getShipboardOf(clientModel.getMyNickname()).countExposed();
        if(exposedConnector > 0) {
            showMessage("You will lose " + exposedConnector + " flight days.", STANDARD);
        }
        else
            showMessage("You will lose no flight days, GOOD JOB ;)", STANDARD);

        showMessage("Press any key to see the effect of the card...", STANDARD);
    }

    /**
     * Notifies the user that there are no more hidden components and
     * prompts them to focus on the visible components. This method
     * displays an informational message and invokes a method to
     * present the options related to the "Build Ship Board" menu.
     *
     * Overrides the implementation of a parent class or interface.
     */
    @Override
    public void showNoMoreHiddenComponents() {
        showMessage("Hidden components are no longer available, look among the visible ones...", NOTIFICATION_INFO);
        showBuildShipBoardMenu();
    }

    /**
     * Notifies the client that a player has disconnected and handles
     * the transition to the appropriate client state.
     *
     * @param disconnectedPlayerNickname the nickname of the player who has disconnected
     */
    @Override
    public void notifyPlayerDisconnected(String disconnectedPlayerNickname) {
        showMessage(disconnectedPlayerNickname + " disconnected.", ERROR);
        showMessage("GAME ENDED", STANDARD);
        showMessage("Press any key to quit...", STANDARD);
        setClientState(WAITING_TO_EXIT);
    }

    /**
     * Handles the process of showing and managing the Cube Malus Menu interaction for the player.
     * This method is triggered when the player needs to handle a "cube malus" event by removing a specified
     * number of cargo cubes from their storage or, in special cases, using batteries instead.
     *
     * The method checks the player's turn and determines if they possess sufficient resources to handle the cube malus.
     * It provides appropriate feedback and interaction prompts based on the current game state.
     *
     * Functional details:
     * - If it's not the player's turn, the client state is set to wait for the other player.
     * - Retrieves the cube malus from the current adventure card and initializes the storage selection manager
     *   to manage the malus removal process.
     * - Displays the ship's storage with color-coded visuals for user interaction.
     * - If no removable cubes are available, the method checks if batteries can be used as a substitute for the cube malus:
     *   - If batteries are available: prompts the user to return battery cubes.
     *   - If neither cubes nor batteries are available: informs the user that they are safe, and no action is required for now.
     * - If removable cubes are available, sets the client state to allow selection of storage for cube removal.
     * - Prompts the user to input coordinates for removing cubes or batteries, depending on the scenario.
     *
     * State transitions:
     * - WAIT_PLAYER: If it's not the player's turn.
     * - CHOOSE_BATTERY_CUBES: If no cubes are available and batteries need to be removed.
     * - CANNOT_HANDLE_CUBE_MALUS: If neither cubes nor batteries can be removed.
     * - CHOOSE_STORAGE_FOR_CUBEMALUS: If cubes are available for removal.
     */
    @Override
    public void showHandleCubesMalusMenu() {

        if(!clientModel.isMyTurn()){
            setClientState(WAIT_PLAYER);
            return;
        }

        CubeMalusCard card = (CubeMalusCard) clientModel.getCurrAdventureCard();
        cubeMalus = card.getCubeMalus();

        // Initialize StorageSelectionManager for malus mode
        storageManager = new StorageSelectionManager(cubeMalus, clientModel.getShipboardOf(clientModel.getMyNickname()));

        // Mostra la nave per visualizzare i depositi
        this.showStorageWithColor();
        showMessage("\nYou must remove " + storageManager.getRemainingCubesToRemove() +" cargo cubes!", STANDARD);

        // Check if there are cubes available for removal
        if (!storageManager.hasAvailableCubes()) {
            // No cubes available, check if batteries can be used instead
            if(!clientModel.getShipboardOf(clientModel.getMyNickname()).getBatteryBoxes().isEmpty() &&
               clientModel.getShipboardOf(clientModel.getMyNickname()).getBatteries() > 0) {
                showMessage("You don't have any cube", STANDARD);
                showMessage("You have to give back the batteries instead of the cubes", STANDARD);
                setClientState(CHOOSE_BATTERY_CUBES);
                showBatteryBoxesWithColor();
                showMessage("Enter coordinates of a battery to remove: ", ASK);
            } else {
                showMessage("You don't have any cube or battery box", STANDARD);
                showMessage("You are safe...for now", STANDARD);
                showMessage("Press any key to continue...", ASK);
                setClientState(CANNOT_HANDLE_CUBE_MALUS);
            }
            return;
        }

        // Cubes are available for removal
        setClientState(ClientState.CHOOSE_STORAGE_FOR_CUBEMALUS);
        showAvailableCubeTypes();
        showMessage("Enter coordinates of a storage to remove a cargo cube from: ", ASK);

    }

    /**
     * Retrieves the shipboard associated with the current client's nickname.
     *
     * @return the ShipBoardClient instance corresponding to the current client's nickname
     */
    private ShipBoardClient getMyShipBoard() {
        return clientModel.getShipboardOf(clientModel.getMyNickname());
    }

    /**
     * Displays the player's ship board along with their nickname.
     * This method retrieves the player's ship board and invokes another method
     * to handle the display of the ship board associated with the player's nickname.
     */
    private void showMyShipBoard() {
        this.showShipBoard(getMyShipBoard(), clientModel.getMyNickname());
    }

    /**
     * Displays the current ranking of players in a formatted manner.
     *
     * This method retrieves the ranking details of all active players from the client model,
     * sorts them based on their position on the flying board, and formats the ranking
     * with additional details like scores and differences from the leader. The output
     * also includes a legend explaining the format of the ranking information.
     *
     * Behavior:
     * - If there are no active players, an error message is displayed.
     * - Players who have landed early are marked with "[EARLY LANDED]".
     * - For other players, their position, score, and steps behind the leader (if any) are displayed.
     *
     * Output is generated and displayed using the `showMessage` method, with ASK or ERROR indicators.
     *
     * Exceptions:
     * - Catches and handles NoSuchElementException if there are no players in the ranking list.
     *
     * Dependencies:
     * - Requires the clientModel object to retrieve player data and ranking information.
     * - Relies on the showMessage method to display messages to the user.
     */
    public void showCurrentRanking() {
        StringBuilder output = new StringBuilder();
        output.append("==========  Ranking  ==========\n");

        List<String> sortedRanking = clientModel.getSortedRanking();

        int topScore;
        try {
            topScore = clientModel.getPlayerClientData().get(sortedRanking.getFirst()).getFlyingBoardPosition();
        } catch (NoSuchElementException e) {
            showMessage("No active players found!", ERROR);
            return;
        }

        for (int i = 0; i < sortedRanking.size(); i++) {
            String playerNickname = sortedRanking.get(i);
            PlayerClientData playerData = clientModel.getPlayerClientData().get(playerNickname);

            if(playerData.isLanded())
                output.append(String.format("-  %-20s | [EARLY LANDED]\n", playerNickname));
            else{
                int playerScore = playerData.getFlyingBoardPosition();
                int diff = topScore - playerScore;
                output.append(String.format("%d. %-20s | %-2d %s\n", i + 1, playerNickname, playerScore, (diff == 0 ? "" : "(" + (-diff) + ")")));
            }
        }

        output.append("===============================\n");
        output.append("Legend: the score shows the position of each player and how many steps behind the leader they are.\n> ");
        showMessage(output.toString(), ASK);
    }

    /**
     * Displays the interface for the crew placement phase within the game. This method allows
     * the player to assign crew members (Purple or Brown Aliens) to specific cabins on their spaceship,
     * ensuring proper placement while visualizing available life support connections.
     *
     * Key functionalities of this method:
     *
     * - Sets the client state to CREW_PLACEMENT_MENU.
     * - Clears existing cabin placement mappings without resetting previous choices.
     * - Retrieves information about cabins with available life support connections from the client model.
     * - Highlights and categorizes cabins as either available or already occupied based on player choices.
     * - Presents a visual representation of the ship's board with life support configurations and cabin statuses.
     * - Provides the player with a menu interface for selecting and managing crew placement:
     *   - Displays the cabins currently selected by the player with respective crew members.
     *   - Enumerates cabins with available life support, detailing specific connection types.
     *   - Differentiates between cabins with Purple and Brown Alien life support compatibilities, and indicates if they are still available for placement.
     *   - Allows removal of selected crew members from cabins or placing new members in cabins.
     * - Displays a summary of cabins with crew members and the cabins that will default to hosting humans.
     * - Provides options to confirm choices, reset all assignments, or proceed if no life-support-connected cabins are present.
     *
     * When no cabins with life support are available, the player receives a notification, and the state transitions
     * to NO_CREW_TO_PLACE, skipping further crew placement.
     */
    @Override
    public void showCrewPlacementMenu() {
        setClientState(ClientState.CREW_PLACEMENT_MENU);

        // Non resettare mai crewChoices qui, solo crewPlacementCoordinatesMap
        crewPlacementCoordinatesMap.clear();

        // Ottieni cabine con supporto vitale
        ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
        Map<Coordinates, Set<ColorLifeSupport>> cabinsWithLifeSupport = shipBoard.getCabinsWithLifeSupport();

        // Prepara la mappa dei colori per la visualizzazione
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();
        Set<Coordinates> availableCabins = new HashSet<>(cabinsWithLifeSupport.keySet());
        availableCabins.removeAll(crewChoices.keySet()); // Rimuovi cabine già scelte

        colorMap.put(ANSI_GREEN, availableCabins); // Cabine disponibili
        colorMap.put(ANSI_BLUE, crewChoices.keySet()); // Cabine già scelte

        // Mostra la nave con le cabine evidenziate
        this.showShipBoard(shipBoard, clientModel.getMyNickname(), colorMap);

        if (cabinsWithLifeSupport.isEmpty()) {
            showMessage("\nYou don't have any cabins connected to life support. All cabins will receive humans.", STANDARD);
            showMessage("Press any key to continue...", ASK);
            setClientState(NO_CREW_TO_PLACE);
            //clientController.submitCrewChoices(new HashMap<>());
            return;
        }

        // Mostra opzioni
        StringBuilder menu = new StringBuilder("\n=== CREW PLACEMENT PHASE ===\n");
        menu.append("Choose where to place your aliens. " + ANSI_YELLOW + "All cabins will receive 2 humans by default." + ANSI_RESET + "\n");

        // Mostra un riepilogo delle scelte attuali
        if (!crewChoices.isEmpty()) {
            menu.append("\nYOUR CURRENT CHOICES:\n");
            for (Map.Entry<Coordinates, CrewMember> entry : crewChoices.entrySet()) {
                Coordinates coords = entry.getKey();
                CrewMember crew = entry.getValue();

                menu.append("▶ Cabin at (").append(coords.getX() + 1).append(",").append(coords.getY() + 1).append("): ");
                menu.append(crew == CrewMember.PURPLE_ALIEN ? ANSI_BLUE + "Purple Alien" + ANSI_RESET
                        : ANSI_YELLOW + "Brown Alien" + ANSI_RESET).append("\n");
            }
            menu.append("\n");
        }

        // Conta alieni già selezionati
        boolean purpleSelected = crewChoices.values().stream().anyMatch(c -> c == CrewMember.PURPLE_ALIEN);
        boolean brownSelected = crewChoices.values().stream().anyMatch(c -> c == CrewMember.BROWN_ALIEN);

        // Informa l'utente sullo stato attuale degli alieni
        menu.append("Alien status: ");
        menu.append("Purple: ").append(purpleSelected ? ANSI_RED + "Already selected" + ANSI_RESET : ANSI_GREEN + "Available" + ANSI_RESET);
        menu.append(" | Brown: ").append(brownSelected ? ANSI_RED + "Already selected" + ANSI_RESET : ANSI_GREEN + "Available" + ANSI_RESET);
        menu.append("\n\nCabins with life support:\n");

        int index = 1;

        // Itera attraverso tutte le cabine con supporto vitale
        for (Map.Entry<Coordinates, Set<ColorLifeSupport>> entry : cabinsWithLifeSupport.entrySet()) {
            Coordinates coords = entry.getKey();
            Set<ColorLifeSupport> supportedColors = entry.getValue();

            // Aggiungi mapping indice->coordinate
            crewPlacementCoordinatesMap.put(index, coords);

            // Se questa cabina ha già una scelta, mostrala diversamente
            if (crewChoices.containsKey(coords)) {
                CrewMember chosen = crewChoices.get(coords);
                String alienColor = chosen == CrewMember.PURPLE_ALIEN ? ANSI_BLUE + "Purple Alien" + ANSI_RESET : ANSI_YELLOW + "Brown Alien" + ANSI_RESET;

                menu.append(ANSI_BLUE)
                        .append(index).append(". Cabin at (").append(coords.getX() + 1)
                        .append(",").append(coords.getY() + 1).append("): ")
                        .append(alienColor)
                        .append(" [Press ").append(index).append(" to remove]")
                        .append(ANSI_RESET).append("\n");
            } else {
                // Cabina senza scelte ancora - non mostra opzione umani
                menu.append(index).append(". Cabin at (").append(coords.getX() + 1)
                        .append(",").append(coords.getY() + 1).append("): ");

                if (supportedColors.size() == 1) {
                    ColorLifeSupport color = supportedColors.iterator().next();
                    menu.append("Connected to ").append(color).append(" life support\n");

                    // Mostra solo opzioni per alieni disponibili
                    if (color == ColorLifeSupport.PURPLE) {
                        if (!purpleSelected) {
                            menu.append("   - Press ").append(index).append("P for 1 purple alien\n");
                        } else {
                            menu.append("   - " + ANSI_RED + "Purple alien already selected" + ANSI_RESET + "\n");
                        }
                    } else {
                        if (!brownSelected) {
                            menu.append("   - Press ").append(index).append("B for 1 brown alien\n");
                        } else {
                            menu.append("   - " + ANSI_RED + "Brown alien already selected" + ANSI_RESET + "\n");
                        }
                    }
                } else if (supportedColors.size() == 2) {
                    menu.append("Connected to both life support types\n");

                    if (!purpleSelected) {
                        menu.append("   - Press ").append(index).append("P for 1 purple alien\n");
                    } else {
                        menu.append("   - " + ANSI_RED + "Purple alien already selected" + ANSI_RESET + "\n");
                    }

                    if (!brownSelected) {
                        menu.append("   - Press ").append(index).append("B for 1 brown alien\n");
                    } else {
                        menu.append("   - " + ANSI_RED + "Brown alien already selected" + ANSI_RESET + "\n");
                    }
                }
            }

            index++;
        }

        // Conto cabine totali vs. cabine con scelte
        int totalCabins = shipBoard.getCabin().size();
        int cabinsWithChoices = crewChoices.size();
        int cabinsWithoutChoices = totalCabins - cabinsWithChoices;

        // il cabinsWithoutChoices + 1 serve per considerare anche la MainCabin
        menu.append("\n" + ANSI_YELLOW + "Summary: " + cabinsWithChoices + " cabin(s) with aliens, "
                + (cabinsWithoutChoices + 1) + " cabin(s) will receive humans." + ANSI_RESET + "\n");

        menu.append("\nC. Confirm choices " + ANSI_GREEN + "(all remaining cabins will receive 2 humans)" + ANSI_RESET + "\n");
        menu.append("R. Reset all choices\n");
        menu.append("\nEnter your choice: ");

        showMessage(menu.toString(), ASK);
    }


    /**
     * Notifies the user that the timer has ended and provides information about the remaining flips.
     *
     * @param flipsLeft the number of flips left when the timer ends
     */
    @Override
    public void notifyTimerEnded(int flipsLeft) {
        if (flipsLeft == 0)
            showMessage("Timer ended! You cannot build your ship anymore.", NOTIFICATION_CRITICAL);
        else if (flipsLeft == 1)
            showMessage("Timer ended! There is now " + flipsLeft + " flip left.", NOTIFICATION_INFO);
        else
            showMessage("Timer ended! There are now " + flipsLeft + " flips left.", NOTIFICATION_INFO);
    }

    /**
     * Updates the time left and displays a message when specific conditions are met.
     *
     * @param timeLeft the remaining amount of time to be updated
     * @param flipsLeft the remaining number of flips; currently unused in this method
     */
    @Override
    public void updateTimeLeft(int timeLeft, int flipsLeft) {
        if (timeLeft % 20 == 0 && timeLeft != 0 && timeLeft != 60) {
            showMessage("Time left: " + timeLeft, NOTIFICATION_INFO);
        }
    }

    private Optional<Integer> convertInput(String input) throws NumberFormatException {
        return input.equals(INPUT_INTERRUPT) ? Optional.empty() : Optional.of(Integer.parseInt(input));
    }

    private static final BiFunction<CallableOnGameController, String, Component> INTERRUPTED = (s, n) -> null;

    /**
     * Displays a question asking the user to input the number of players.
     * The message specifies the query and is shown in the context of asking for user input.
     * Typically used to prompt the user during the initialization phase of a multiplayer game.
     */
    public void showNumPlayersQuestion() {
        showMessage("How many players do you want to play with? ", ASK);
    }

    /**
     * Displays a message prompting the user with a question asking if they
     * want to participate in the test flight. The method uses a message
     * display system with a specified format to convey the prompt.
     */
    public void showTestFlightQuestion() {
        showMessage("Do you want to play the test flight? [y/n] ", ASK);
    }

    /**
     * Displays a message prompting the user to select a reserved component.
     * The message includes guidance for users to press '0' if they wish to go back.
     * This method utilizes the {@code showMessage} function to present the prompt.
     */
    public void showPickReservedComponentQuestion() {
        showMessage("Please pick a reserved component (0 to go back): ", ASK);
    }

    private void handleEngineSelection(String input) {
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getDoubleEngines().contains(component)) {
                showMessage("No double engine at these coordinates.", ERROR);
                showMessage("Please try again or 'done' to confirm.", ASK);
                return;
            }

            if(selectedEngines.contains(coords)) {
                showMessage("Engine already selected", ERROR);
                showMessage("Select another one or 'done' to confirm", ASK);
                return;
            }

            selectedEngines.add(coords);
            // Passa alla selezione della batteria
            showBatteryBoxesWithColor();
            showMessage("Now select a battery box for this engine (row column) or 'cancel' to cancel the choise", ASK);
            setClientState(ClientState.CHOOSE_ENGINES_SELECT_BATTERY);
        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleBatterySelection(String input){
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getBatteryBoxes().contains(component)) {
                showMessage("No battery box at these coordinates.", ERROR);
                showMessage("Please try again or 'done' to confirm.", ASK);
                return;
            }


            BatteryBox batteryBox = (BatteryBox) component;

            //se quel box non contiene batterie non posso selezionarlo
            if(batteryBox.getRemainingBatteries()==0){
                showMessage("This batteryBox is empty!", ERROR);
                showMessage("Please select another one or 'cancel' to cancel the last choice", ASK);
                return;
            }

            if(batteryBox.getRemainingBatteries()==0){
                showMessage("This battery box is empty", ERROR);
                showMessage("Please select another one or 'cancel' to cancel the last choice", ASK);
                return;
            }

            // Aggiungi la batteria e torna alla selezione del motore
            selectedBatteries.add(coords);
            batteryBox.useBattery();


            if(clientState==CHOOSE_ENGINES_SELECT_BATTERY){
                showEngineWithColor();
                showMessage("Engine and battery selected", STANDARD);
                if(selectedEngines.size()==clientModel.getShipboardOf(clientModel.getMyNickname()).getDoubleEngines().size()){
                    setClientState(WAIT_PLAYER);
                    showMessage("You have selected all the engines", STANDARD);
                    clientController.playerChoseDoubleEngines(clientModel.getMyNickname(),selectedEngines,selectedBatteries);
                    return;
                }
                showMessage("Choose another engine or 'done' to finish: ", ASK);
                setClientState(CHOOSE_ENGINES_MENU);
                return;
            }

            if(clientState==CHOOSE_CANNONS_SELECT_BATTERY){
                showCannonWithColor();
                showMessage("Cannon and battery selected", STANDARD);
                if(selectedCannons.size()==clientModel.getShipboardOf(clientModel.getMyNickname()).getDoubleCannons().size()){
                    setClientState(WAIT_PLAYER);
                    showMessage("You have selected all the cannons", STANDARD);
                    clientController.playerChoseDoubleCannons(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
                    return;
                }
                showMessage("Choose another cannons or 'done' to finish: ", ASK);
                setClientState(CHOOSE_CANNONS_MENU);
                return;
            }

            if (clientState == HANDLE_SMALL_DANGEROUS_SELECT_BATTERY) {
                showMessage("Shield and battery selected", STANDARD);
                setClientState(WAIT_PLAYER);
                clientController.playerHandleSmallDanObj(clientController.getNickname(), selectedShields, selectedBatteries);
                selectedShields.clear();
                selectedBatteries.clear();
                return;
            }

            if (clientState == CHOOSE_CANNONS_SELECT_BATTERY_BIGMETEORITE) {
                showMessage("Double Cannon and battery selected", STANDARD);
                setClientState(WAIT_PLAYER);
                clientController.playerHandleBigMeteorite(clientController.getNickname(), selectedCannons, selectedBatteries);
                selectedCannons.clear();
                selectedBatteries.clear();
            }

            if (clientState == CHOOSE_BATTERY_CUBES) {

                // Calculate total items selected (cubes + batteries)
                int selectedCubes = (storageManager != null && storageManager.isInMalusMode())
                    ? storageManager.getSelectedStoragesForRemoval().size()
                    : selectedStorage.size();

                if (selectedBatteries.size() + selectedCubes == cubeMalus
                        || clientModel.getShipboardOf(clientModel.getMyNickname()).getTotalAvailableBattery() == selectedBatteries.size()) {
                    showMessage("All cargo cubes and batteries has been selected\nOr you don't have any more batteries to select", STANDARD);
                    setClientState(WAIT_PLAYER);

                    // Get final list of selected storages
                    List<Coordinates> finalSelectedStorages = (storageManager != null && storageManager.isInMalusMode())
                        ? storageManager.getSelectedStoragesForRemoval()
                        : selectedStorage;

                    clientController.playerChoseStorageAndBattery(clientController.getNickname(), finalSelectedStorages, selectedBatteries);

                    // Cleanup
                    selectedBatteries.clear();
                    selectedStorage.clear();
                    if (storageManager != null) {
                        storageManager.resetMalusState();
                    }
                } else {
                    showBatteryBoxesWithColor();
                    showMessage("You still need to remove " + (cubeMalus - selectedCubes - selectedBatteries.size()) + " battery(s). Enter another battery box coordinate: ", STANDARD);
                    showMessage("Battery box selected. Enter another battery box. ", ASK);
                    setClientState(CHOOSE_BATTERY_CUBES);
                }

            }

        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleCannonSelection(String input)  {
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getDoubleCannons().contains(component)) {
                showMessage("No double cannon at these coordinates.", ERROR);
                showMessage("Please try again or 'done' to confirm.", ASK);
                return;
            }

            if(selectedCannons.contains(coords)) {
                showMessage("Cannon already selected", ERROR);
                showMessage("Select another one or 'done' to confirm", ASK);
                return;
            }

            // Passa alla selezione della batteria
            selectedCannons.add(coords);
            showBatteryBoxesWithColor();
            showMessage("Now select a battery box for this cannon (row column): ", ASK);

            if (clientState == ClientState.HANDLE_BIG_METEORITE_MENU) {
                setClientState(CHOOSE_CANNONS_SELECT_BATTERY_BIGMETEORITE);
            } else if (clientState == CHOOSE_CANNONS_MENU) {
                setClientState(CHOOSE_CANNONS_SELECT_BATTERY);
            }

        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleShieldSelection(String input) {
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getShields().contains(component)) {
                showMessage("No shield at these coordinates.", ERROR);
                showMessage("Please try again or 'done' to confirm.", ASK);
                return;
            }

            // Passa alla selezione della batteria
            selectedShields.add( coords);
            showBatteryBoxesWithColor();
            showMessage("Now select a battery box for this shield (row column): ", ASK);
            setClientState(ClientState.HANDLE_SMALL_DANGEROUS_SELECT_BATTERY);
        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleStorageSelectionForReward(String input) {
        if (input.equalsIgnoreCase("done")) {
            // Se l'utente ha finito ma non ha selezionato tutti gli storage possibili
            if (!storageManager.isSelectionComplete() && storageManager.canAcceptCurrentCube()) {
                showMessage("You have not selected storage for all cubes you can accept. " +
                                "\nRemaining cubes will be discarded. Continue with 'confirm' or select other storage.",
                        NOTIFICATION_INFO);
                return;
            }

            // Invia la selezione al server (completa o parziale)
            List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
            clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            return;
        }

        if (input.equalsIgnoreCase("confirm")) {
            // Conferma anche se non tutti i cubi sono stati selezionati
            List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();

            // Aggiungi coordinate invalide per i cubi rimanenti che non possono essere accettati
            while (selectedCoordinates.size() < storageManager.getTotalCubesCount()) {
                selectedCoordinates.add(new Coordinates(-1, -1)); // Coordinate invalide
            }

            clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            return;
        }

        if (input.equalsIgnoreCase("skip")) {
            // The player gives up all reward cubes
            showMessage("You are giving up all reward cubes...", STANDARD);
            List<Coordinates> emptyList = new ArrayList<>();
            clientController.playerChoseStorage(clientController.getNickname(), emptyList);
            return;
        }

        if (input.equalsIgnoreCase("next")) {
            // Skip only the current cube
            CargoCube currentCube = storageManager.getCurrentCube();
            showMessage("Skipping cube " + currentCube + "...", STANDARD);
            storageManager.skipCurrentCube();

            // Check if there are more cubes to process
            if (storageManager.isSelectionComplete()) {
                showMessage("All cubes have been processed. Sending data to the server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Show menu for the next cube
                CargoCube nextCube = storageManager.getCurrentCube();
                StringBuilder message = new StringBuilder("\nNext cube to place: ")
                        .append(nextCube)
                        .append(" (value: ").append(nextCube.getValue()).append(")")
                        .append(nextCube == CargoCube.RED ? " - This cube requires special storage!" : "")
                        .append("\n");
                message.append("Enter the coordinates of a storage (row column), ")
                       .append("\n'next' to skip this cube, 'skip' to give up all, ")
                       .append("\n'done' to confirm: ");
                showMessage(message.toString(), ASK);
            }
            return;
        }

        // Check if the current cube can be accepted
        if (!storageManager.canAcceptCurrentCube()) {
            showMessage("You can't accept this cube. It has been automatically marked as discarded.", NOTIFICATION_INFO);

            // Add invalid coordinates to indicate the cube is being skipped
            storageManager.skipCurrentCube();

            // Check if selection is complete or there's another cube to handle
            if (storageManager.isSelectionComplete()) {
                showMessage("Selection complete. Sending data to the server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Show menu for the next cube
                CargoCube nextCube = storageManager.getCurrentCube();
                if (nextCube != null) {
                    showMessage("\nNext cube to place: " + nextCube +
                                " (value: " + nextCube.getValue() + ")" +
                                (nextCube == CargoCube.RED ? " - This cube requires special storage!" : ""),
                            STANDARD);
                    showMessage("Enter the coordinates of a storage (row column), " +
                                "\n'next' to skip this cube, 'skip' to give up all, " +
                                "\n'done' to confirm: ", ASK);
                }
            }
            return;
        }

        try {
            // Parse coordinates
            Coordinates coords = parseCoordinates(input);
            if (coords == null) {
                showMessage("Invalid coordinate format. Use 'row column' (e.g., '5 7').", ERROR);
                showMessage("Or use commands: 'done', 'skip', 'confirm'", STANDARD);
                return;
            }

            // Check if the coordinates refer to a valid storage
            String storageStatus = storageManager.checkStorageStatus(coords);
            if (storageStatus == null) {
                showMessage("No storage found at the specified coordinates.", ERROR);
                return;
            }

            // Get info on the current cube
            CargoCube currentCube = storageManager.getCurrentCube();

            // Try adding the storage to the selection
            boolean added = storageManager.addStorageSelection(coords);
            if (!added) {
                if (currentCube == CargoCube.RED) {
                    showMessage("WARNING: RED cubes can only be placed in special storage!", ERROR);
                } else {
                    showMessage("Error adding storage to selection.", ERROR);
                }
                return;
            }

            // Successfully added storage
            showMessage("Storage selected: " + storageStatus, STANDARD);

            // If we've selected all possible storages
            if (storageManager.isSelectionComplete()) {
                showMessage("\nYou’ve selected storage for all cubes you can accept. Sending data to the server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Otherwise, show menu for next cube
                CargoCube nextCube = storageManager.getCurrentCube();
                if (nextCube != null) {
                    StringBuilder message = new StringBuilder("\nCube ").append(currentCube)
                            .append(" successfully placed.\n\n");
                    message.append("Next cube to place: ").append(nextCube)
                            .append(" (value: ").append(nextCube.getValue()).append(")")
                            .append(nextCube == CargoCube.RED ? " - This cube requires special storage!" : "")
                            .append("\n");
                    message.append("Enter the coordinates of a storage (row column), ")
                            .append("\n'done' to confirm, 'skip' to give up all, ")
                            .append("\n'confirm' to confirm even partial selections: ");
                    showMessage(message.toString(), ASK);
                }
            }
        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleStorageSelectionForMalus(String input){
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getStorages().contains(component)) {
                showMessage("No storage at these coordinates.", ERROR);
                showStorageWithColor();
                showMessage("Please try again or 'done' to confirm.", ASK);
                return;
            }

            selectedStorage.add(coords);

        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private Coordinates parseCoordinates(String input) {
        try {
            String[] parts = input.trim().split("\\s+");
            if (parts.length != 2) {
                showMessage("Invalid format. Please enter 'row column'.", ERROR);
                return null;
            }

            int row = Integer.parseInt(parts[0]) - 1; // Converte da 1-based a 0-based
            int col = Integer.parseInt(parts[1]) - 1;
            return new Coordinates(row, col);
        } catch (NumberFormatException e) {
            showMessage("Invalid numbers. Please enter valid coordinates.", ERROR);
            return null;
        }
    }

    // ======== METODI PER LA RIDISTRIBUZIONE CUBI ========

    /**
     * Displays the cube redistribution menu with a list of available cubes and options.
     */
    public void showCubeRedistributionMenu() {
        // Mostra sempre gli storage disponibili
        showStoragesInfo();

        List<CargoCube> available = storageManager.getAvailableCubes();

        if (available.isEmpty()) {
            showMessage("\n" + ANSI_GREEN + "=== REDISTRIBUTION COMPLETED ===" + ANSI_RESET, STANDARD);
            showMessage("All cubes have been placed!", STANDARD);
            showMessage("Press 'c' to confirm and send to server: ", ASK);
            return;
        }

        StringBuilder menu = new StringBuilder("\n" + "=== CUBES TO PLACE ===" + "\n");
        
        for (int i = 0; i < available.size(); i++) {
            CargoCube cube = available.get(i);
            String marker = (i == storageManager.getSelectedCubeIndex()) ? ANSI_YELLOW + " -> " + ANSI_RESET : "    ";

            // Colore del cubo
            String cubeColor = switch (cube) {
                case RED -> ANSI_RED;
                case YELLOW -> ANSI_YELLOW;
                case GREEN -> ANSI_GREEN;
                case BLUE -> ANSI_BLUE;
            };

            menu.append(marker).append(i).append(". ").append(cubeColor).append(cube).append(ANSI_RESET);
            if (cube == CargoCube.RED) {
                menu.append(" " + ANSI_RED + "(SPECIAL STORAGE ONLY)" + ANSI_RESET);
            }
            menu.append(" [valore: ").append(cube.getValue()).append("]\n");
        }

        menu.append("\n" + "=== COMMANDS ===" + "\n");
        menu.append("0-").append(available.size()-1).append(": Select cube | ");

        CargoCube selectedCube = storageManager.getSelectedCube();

        if(selectedCube != null) {
            menu.append("a [row] [column]: Add | ");
        }
        menu.append("r [row] [column]: Remove");
        menu.append("\n");
        menu.append("c: Confirm\n");

        if (selectedCube != null) {
            String cubeColor = switch (selectedCube) {
                case RED -> ANSI_RED;
                case YELLOW -> ANSI_YELLOW;
                case GREEN -> ANSI_GREEN;
                case BLUE -> ANSI_BLUE;
            };
            menu.append("\n").append(ANSI_YELLOW).append("▶ Selected cube: ").append(cubeColor).append(selectedCube).append(ANSI_RESET);
            if (selectedCube == CargoCube.RED) {
                menu.append(" " + ANSI_RED + "(requires SpecialStorage)" + ANSI_RESET);
            }
            menu.append("\n");
        } else {
            menu.append("\n").append(ANSI_CYAN).append("No cube selected. Select a cube by index.").append(ANSI_RESET).append("\n");
        }

        showMessage(menu.toString(), ASK);
    }

    private void handleRedistributionInput(String input) {
        try {
            // Comando di selezione cubo per indice
            if (input.matches("\\d+")) {
                int index = Integer.parseInt(input);
                if (storageManager.selectCubeByIndex(index)) {
                    CargoCube selectedCube = storageManager.getSelectedCube();
                    showMessage("Cube " + selectedCube + " selected", NOTIFICATION_INFO);
                } else {
                    showMessage("Invalid index. Use a number from 0 to " + (storageManager.getAvailableCubes().size()-1), ERROR);
                }
                showCubeRedistributionMenu();
                return;
            }

            // Comando aggiungi cubo
            if (input.toLowerCase().startsWith("a ")) {
                handleAddCubeCommand(input.substring(2));
                return;
            }

            // Comando rimuovi cubo
            if (input.toLowerCase().startsWith("r ")) {
                handleRemoveCubeCommand(input.substring(2));
                return;
            }

            // Altri comandi
            switch (input.toLowerCase()) {
                case "c":
                    confirmRedistribution();
                    return;
                default:
                    showMessage("Command not recognized. Use 'h' to see available commands.", ERROR);
                    showCubeRedistributionMenu();
            }
        } catch (Exception e) {
            showMessage("Error processing command: " + e.getMessage(), ERROR);
            showCubeRedistributionMenu();
        }
    }

    private void handleAddCubeCommand(String coordinates) {
        if (storageManager.getSelectedCube() == null) {
            showMessage(ANSI_RED + "❌ No cube selected! Select a cube by index first." + ANSI_RESET, ERROR);
            showCubeRedistributionMenu();
            return;
        }

        CargoCube selectedCube = storageManager.getSelectedCube();
        Coordinates coords = parseCoordinates(coordinates);
        if (coords == null) {
            showCubeRedistributionMenu();
            return;
        }

        // Pre-validazione per feedback migliore
        ShipBoardClient shipBoard = getMyShipBoard();
        Storage storage = shipBoard.getCoordinatesAndStorages().get(coords);

        if (storage == null) {
            showMessage(ANSI_RED + "❌ No storage found at coordinates " + formatCoordinates(coords) + ANSI_RESET, ERROR);
            showCubeRedistributionMenu();
            return;
        }

        if (selectedCube == CargoCube.RED && !(storage instanceof SpecialStorage)) {
            showMessage(ANSI_RED + "❌ RED cubes can only be placed in SpecialStorage!" + ANSI_RESET, ERROR);
            showCubeRedistributionMenu();
            return;
        }

        // Feedback pre-azione se storage è pieno
        String preActionMessage = "";
        if (storage.isFull() && !storage.getStockedCubes().isEmpty()) {
            CargoCube worstCube = storage.getStockedCubes().stream()
                .min(java.util.Comparator.comparing(CargoCube::getValue))
                .orElse(null);
            if (worstCube != null) {
                preActionMessage = ANSI_YELLOW + "⚠️ Full storage: " + worstCube + " will be replaced" + ANSI_RESET;
            }
        }

        if (storageManager.addSelectedCubeToStorage(coords)) {
            String cubeColor = switch (selectedCube) {
                case RED -> ANSI_RED;
                case YELLOW -> ANSI_YELLOW;
                case GREEN -> ANSI_GREEN;
                case BLUE -> ANSI_BLUE;
            };

            if (!preActionMessage.isEmpty()) {
                showMessage(preActionMessage, NOTIFICATION_INFO);
            }
            showMessage(ANSI_GREEN + "✅ Cube " + cubeColor + selectedCube + ANSI_GREEN +
                       " added to storage at " + formatCoordinates(coords) + ANSI_RESET, NOTIFICATION_INFO);
            
            clientModel.refreshShipBoardOf(clientModel.getMyNickname());
        } else {
            showMessage(ANSI_RED + "❌ Cannot add cube to storage" + ANSI_RESET, ERROR);
        }

        showCubeRedistributionMenu();
    }


    private void handleRemoveCubeCommand(String coordinates) {
        Coordinates coords = parseCoordinates(coordinates);
        if (coords == null) {
            showCubeRedistributionMenu();
            return;
        }

        // Verifica che ci sia uno storage alle coordinate
        ShipBoardClient shipBoard = getMyShipBoard();
        Storage storage = shipBoard.getCoordinatesAndStorages().get(coords);

        if (storage == null) {
            showMessage("❌ No storage found at coordinates " + formatCoordinates(coords), ERROR);
            showCubeRedistributionMenu();
            return;
        }

        if (storage.getStockedCubes().isEmpty()) {
            showMessage("⚠️ Storage at coordinates " + formatCoordinates(coords) + " is already empty", ERROR);
            showCubeRedistributionMenu();
            return;
        }

        // Feedback pre-azione: mostra quale cubo verrà rimosso
        CargoCube cubeToRemove = storage.getStockedCubes().get(storage.getStockedCubes().size() - 1);
        String cubeColor = switch (cubeToRemove) {
            case RED -> ANSI_RED;
            case YELLOW -> ANSI_YELLOW;
            case GREEN -> ANSI_GREEN;
            case BLUE -> ANSI_BLUE;
        };

        storageManager.removeCubeFromStorage(coords);
        showMessage(ANSI_GREEN + "✅ Cube " + cubeColor + cubeToRemove + ANSI_GREEN +
                   " removed from storage at " + formatCoordinates(coords) + " and added to available cubes" + ANSI_RESET, STANDARD);
        
        // Aggiorna la visualizzazione
        clientModel.refreshShipBoardOf(clientModel.getMyNickname());
        showCubeRedistributionMenu();
    }


    private void confirmRedistribution() {

        List<CargoCube> remaining = storageManager.getAvailableCubes();
        showMessage("Warning: " + remaining.size() + " cubes will be discarded: " + remaining, NOTIFICATION_CRITICAL);

        sendRedistributionToServer();
    }


    private void sendRedistributionToServer() {
        Map<Coordinates, List<CargoCube>> storageUpdates = storageManager.getFinalUpdates();
        showMessage("Invio configurazione storage al server...", STANDARD);
        clientController.sendStorageUpdates(storageUpdates);
    }


    private String formatCoordinates(Coordinates coords) {
        return "(" + (coords.getX() + 1) + "," + (coords.getY() + 1) + ")";
    }

    private void handleCabinSelection(@NotNull String input) {
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        try {
            // Check if the user wants to select multiple cabins
            if (input.equalsIgnoreCase("done")) {
                // Process all selected cabins
                if (selectedCabins.isEmpty()) {
                    showMessage("You must select at least one cabin.", ERROR);
                } else {
                    boolean success = clientController.checkCabinSelection(clientController.getNickname(), selectedCabins);
                    if(success) {
                        clientController.playerChoseCabins(clientModel.getMyNickname(), selectedCabins);
                        selectedCabins.clear();
                        return;
                    }else{
                        selectedCabins.clear();
                        showMessage("Invalid choices. Start again with your selection.", ERROR);
                    }
                }
            } else {
                if(card.getCrewMalus() - selectedCabins.size() == 0 || selectedCabins.size()==clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size()) {
                    showMessage("You cannot select more cabin. Please press done: ", ASK);
                    return;
                }

                // Parse coordinates
                String[] parts = input.trim().split("\\s+");
                if (parts.length != 2) {
                    showMessage("Invalid format. Please enter 'row column'.", ERROR);
                    return;
                }

                int row = Integer.parseInt(parts[0]) - 1; // Convert to 0-based
                int col = Integer.parseInt(parts[1]) - 1;
                Coordinates coords = new Coordinates(row, col);

                // Verify it's a cabin with crew
                ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
                Component component = shipBoard.getComponentAt(coords);

                if (component instanceof Cabin && ((Cabin)component).hasInhabitants()) {
                    selectedCabins.add(coords);
                    //showMessage("Cabin selected. Enter another cabin or 'done' to confirm.", STANDARD);
                } else {
                    showMessage("No occupied cabin at these coordinates.", ERROR);
                }
            }
            if ((card.getCrewMalus() - selectedCabins.size()) == 0 || selectedCabins.size() == clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size())
               showMessage("You have completed your choices, please press done: ", ASK);
            else
                showMessage("You still need to remove " + (card.getCrewMalus() - selectedCabins.size()) +" crew member(s). Enter another cabin coordinate: ", ASK);
        } catch (NumberFormatException e) {
            showMessage("Invalid coordinates. Please enter numbers.", ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
            showMessage("Error: " + e.getMessage(), ERROR);
        }
    }

    private boolean handleCrewPlacementInput(String input) {
        if (input.equalsIgnoreCase("C")) {
            // Conferma le scelte
            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            int totalCabins = shipBoard.getCabin().size();
            int cabinsWithChoices = crewChoices.size();

            // Messaggio di conferma con riepilogo delle scelte
            StringBuilder confirmMessage = new StringBuilder(ANSI_GREEN + "Crew placement confirmed!" + ANSI_RESET + "\n");
            confirmMessage.append("• ").append(cabinsWithChoices).append(" cabin(s) will receive aliens\n");
            confirmMessage.append("• ").append(totalCabins - cabinsWithChoices).append(" cabin(s) will receive humans\n");

            showMessage(confirmMessage.toString(), STANDARD);

            clientController.submitCrewChoices(new HashMap<>(crewChoices));
            return true;
        }

        if (input.equalsIgnoreCase("R")) {
            // Reset delle scelte
            crewChoices.clear();
            showMessage("All choices have been reset.", STANDARD);
            showCrewPlacementMenu();
            return true;
        }

        try {
            // Verifica se è solo un numero (rimuove l'alieno da cabine già scelte)
            if (input.matches("\\d+")) {
                int index = Integer.parseInt(input);

                if (!crewPlacementCoordinatesMap.containsKey(index)) {
                    showMessage("Invalid cabin index. Please select a number between 1 and " +
                            crewPlacementCoordinatesMap.size(), ERROR);
                    return false;
                }

                Coordinates coords = crewPlacementCoordinatesMap.get(index);

                // Verifica se questa cabina ha già una scelta
                if (crewChoices.containsKey(coords)) {
                    // Rimuovi semplicemente l'alieno
                    CrewMember removed = crewChoices.remove(coords);
                    showMessage("Removed " + (removed == CrewMember.PURPLE_ALIEN ? "purple" : "brown") +
                            " alien from cabin. It will receive humans instead.", STANDARD);
                    showCrewPlacementMenu();
                    return true;
                } else {
                    showMessage("This cabin doesn't have an alien assigned yet. Use " + index + "P or " + index + "B to assign an alien.", STANDARD);
                    return false;
                }
            }

            // Formato per assegnare direttamente un alieno: [index][P|B]
            if (input.length() >= 2) {
                int index = Integer.parseInt(input.substring(0, input.length() - 1));
                char choice = Character.toUpperCase(input.charAt(input.length() - 1));

                if (!crewPlacementCoordinatesMap.containsKey(index)) {
                    showMessage("Invalid cabin index. Please select a number between 1 and " +
                            crewPlacementCoordinatesMap.size(), ERROR);
                    return false;
                }

                Coordinates coords = crewPlacementCoordinatesMap.get(index);
                ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
                Map<Coordinates, Set<ColorLifeSupport>> cabinsWithLifeSupport = shipBoard.getCabinsWithLifeSupport();
                Set<ColorLifeSupport> supportedColors = cabinsWithLifeSupport.get(coords);

                // Conta alieni già selezionati (escludendo quelli nella cabina corrente)
                boolean purpleAlreadySelected = crewChoices.entrySet().stream()
                        .anyMatch(e -> e.getValue() == CrewMember.PURPLE_ALIEN && !e.getKey().equals(coords));

                boolean brownAlreadySelected = crewChoices.entrySet().stream()
                        .anyMatch(e -> e.getValue() == CrewMember.BROWN_ALIEN && !e.getKey().equals(coords));

                String feedbackMessage = null;

                switch (choice) {
                    case 'P':
                        // Verifica se supporta Purple e se non è già stato selezionato
                        if (!supportedColors.contains(ColorLifeSupport.PURPLE)) {
                            showMessage("This cabin is not connected to a purple life support module", ERROR);
                            return false;
                        }

                        if (purpleAlreadySelected) {
                            showMessage("You can only have 1 purple alien on your ship", ERROR);
                            return false;
                        }

                        crewChoices.put(coords, CrewMember.PURPLE_ALIEN);
                        feedbackMessage = "Cabin will receive 1 purple alien";
                        break;

                    case 'B':
                        // Verifica se supporta Brown e se non è già stato selezionato
                        if (!supportedColors.contains(ColorLifeSupport.BROWN)) {
                            showMessage("This cabin is not connected to a brown life support module", ERROR);
                            return false;
                        }

                        if (brownAlreadySelected) {
                            showMessage("You can only have 1 brown alien on your ship", ERROR);
                            return false;
                        }

                        crewChoices.put(coords, CrewMember.BROWN_ALIEN);
                        feedbackMessage = "Cabin will receive 1 brown alien";
                        break;

                    default:
                        showMessage("Invalid choice. Use P for purple alien or B for brown alien", ERROR);
                        return false;
                }

                // Mostra messaggio di feedback e aggiorna il menu
                if (feedbackMessage != null) {
                    showMessage(feedbackMessage, STANDARD);
                }

                // Aggiorna il menu per mostrare lo stato corrente
                showCrewPlacementMenu();

                return true;
            } else {
                showMessage("Invalid input format. Use [number] to remove an alien, [number]P or [number]B to assign aliens, C to confirm, or R to reset", ERROR);
                return false;
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid input format. Use [number] to remove an alien, [number]P or [number]B to assign aliens, C to confirm, or R to reset", ERROR);
            return false;
        }
    }

    private void showCurrentCrewChoices() {
        StringBuilder summary = new StringBuilder("\nCurrent choices:\n");

        if (crewChoices.isEmpty()) {
            summary.append("All cabins will receive humans\n");
        } else {
            for (Map.Entry<Coordinates, CrewMember> entry : crewChoices.entrySet()) {
                Coordinates coords = entry.getKey();
                CrewMember crew = entry.getValue();

                summary.append("Cabin at (").append(coords.getX() + 1).append(",").append(coords.getY() + 1).append("): ");
                summary.append(crew == CrewMember.PURPLE_ALIEN ? "Purple Alien" : "Brown Alien").append("\n");
            }
        }

        showMessage(summary.toString(), STANDARD);
        showCrewPlacementMenu();
    }

    /**
     * Handles various user input commands and controls the flow of the application based on the current client state.
     * Recognizes and processes commands such as registering, creating/joining a game, building a shipboard, and other
     * game-related or menu commands. Includes validation for specific input contexts.
     *
     * @param input the user command input in String format. The input must not be null and represents the action
     *              to be performed or the data required for the current client state.
     */
    public void handleInput(@NotNull String input) {
        String[] coordinates;
        int row;
        int column;

        if (input.equals("exit")) {
            clientController.leaveGame();
            System.exit(0);
        } else if (input.trim().split("\\s+")[0].equals("show")) {
            if (input.trim().split("\\s+").length != 2) {
                showMessage("Invalid show command", ERROR);
                return;
            }
            clientController.showShipBoard(input.trim().split("\\s+")[1]);
            return;
        } else if (input.trim().split("\\s+")[0].equals("cube")) {
            if (input.trim().split("\\s+").length != 2) {
                showMessage("Invalid cubes command", ERROR);
                return;
            }
            showStoragesInfo(input.trim().split("\\s+")[1]);
            return;
        } else if (input.trim().split("\\s+")[0].equals("battery")) {
            if (input.trim().split("\\s+").length != 2) {
                showMessage("Invalid cubes command", ERROR);
                return;
            }
            showBatteryBoxesInfo(input.trim().split("\\s+")[1]);
            return;
        } else if (input.trim().split("\\s+")[0].equals("crew")) {
                if (input.trim().split("\\s+").length != 2) {
                    showMessage("Invalid cubes command", ERROR);
                    return;
                }
                showCabinsInfo(input.trim().split("\\s+")[1]);
                return;
        }else if (input.trim().split("\\s+")[0].equals("notActiveComponent")) {
            if (input.trim().split("\\s+").length != 2) {
                showMessage("Invalid cubes command", ERROR);
                return;
            }
            showNotActiveComponentsInfo(input.trim().split("\\s+")[1]);
            return;
        }else if (input.equals("credit")) {
            showPlayerCreditsInfo(clientModel.getMyNickname());
            return;
        } else if (input.equals("rank")) {
            showCurrentRanking();
            return;
        } else if (input.equals("land")){
            GameState state = clientModel.getGameState();
            if(!(state==GameState.PLAY_CARD || state == GameState.CHECK_PLAYERS || state==GameState.DRAW_CARD)){
                showMessage("You cannot land right now!", ERROR);
                return;
            }
            clientController.land();
            return;
        } else if (input.equals("skipToLastCard")) {
            GameState currentState = clientModel.getGameState();
            if (currentState == GameState.SETUP ||
                    currentState == GameState.BUILD_SHIPBOARD ||
                    currentState == GameState.CHECK_SHIPBOARD ||
                    currentState == GameState.PLACE_CREW ||
                    currentState == GameState.END_GAME) {
                showMessage("You cannot skip cards right now!", ERROR);
                return;
            }
            clientController.skipToLastCard();
            return;
        }

        try {
            switch (clientState) {

                case REGISTER:
                    clientController.register(input);
                    break;

                case MAIN_MENU:
                    switch (Integer.parseInt(input)) {
                        case 1:
                            clientState = CREATE_GAME_CHOOSE_NUM_PLAYERS;
                            showNumPlayersQuestion();
                            break;

                        case 2:
                            clientState = JOIN_GAME_CHOOSE_GAME_ID;
                            showAvailableGames(clientController.getObservableGames());
                            break;

                        default:
                            showMessage("Invalid input. Please enter 1 or 2.", ERROR);
                            break;
                    }
                    break;

                case CREATE_GAME_CHOOSE_NUM_PLAYERS:
                    clientState = CREATE_GAME_CHOOSE_IS_TEST_FLIGHT;
                    stringQueue.add(input);
                    showTestFlightQuestion();
                    break;

                case CREATE_GAME_CHOOSE_IS_TEST_FLIGHT:
                    clientState = CREATE_GAME_CHOOSE_COLOR;
                    stringQueue.add(input);
                    showColorQuestion();
                    break;

                case CREATE_GAME_CHOOSE_COLOR:
                    stringQueue.add(input);
                    try {
                        clientState = WAIT_FOR_PLAYERS;
                        int numPlayers = Integer.parseInt(Objects.requireNonNull(stringQueue.poll()));
                        boolean isTestFlight = Objects.requireNonNull(stringQueue.poll()).equalsIgnoreCase("y");
                        PlayerColor playerColor = PlayerColor.getPlayerColor(Integer.parseInt(Objects.requireNonNull(stringQueue.poll())));
                        clientController.handleCreateGameMenu(numPlayers, isTestFlight, playerColor);
                    } catch (NumberFormatException | NullPointerException e) {
                        showMessage("\nOne or more values were incorrect. Please try again.\n", ERROR);
                        clientState = CREATE_GAME_CHOOSE_NUM_PLAYERS;
                        stringQueue.clear();
                        showNumPlayersQuestion();
                    }
                    break;

                case JOIN_GAME_CHOOSE_GAME_ID:
                    stringQueue.add(input);
                    clientState = JOIN_GAME_CHOOSE_COLOR;
                    showAvailableColorsQuestion(input);
                    break;

                case JOIN_GAME_CHOOSE_COLOR:
                    stringQueue.add(input);
                    try {
                        clientState = WAIT_FOR_PLAYERS;
                        String gameId = stringQueue.poll();
                        PlayerColor playerColor = PlayerColor.getPlayerColor(Integer.parseInt(Objects.requireNonNull(stringQueue.poll())));
                        clientController.joinGame(gameId, playerColor);
                    } catch (NumberFormatException | NullPointerException e) {
                        showMessage("\nOne or more values were incorrect. Please try again.\n", ERROR);
                        clientState = JOIN_GAME_CHOOSE_GAME_ID;
                        stringQueue.clear();
                        showAvailableGames(clientController.getGames());
                    }
                    break;

                case WAIT_FOR_PLAYERS:
                    showMessage("""
                            Enter "exit" to leave the game.
                            Waiting for the game to start...""", STANDARD);
                    break;

                case BUILDING_SHIPBOARD_MENU:
                    switch (Integer.parseInt(input)) {
                        case 1:
                            clientState = BUILDING_SHIPBOARD_WITH_FOCUSED_COMPONENT;
                            clientController.pickRandomComponent();
                            break;

                        case 2:
                            clientState = BUILDING_SHIPBOARD_PICK_VISIBLE_COMPONENT;
                            showVisibleComponentAndMenu(clientController.getClientModel().getVisibleComponents());
                            break;

                        case 6:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-3.\n> ", ASK);
                                showBuildShipBoardMenu();
                                break;
                            }
                            clientState = BUILDING_SHIPBOARD_PICK_RESERVED_COMPONENT;
                            showMyShipBoard();
                            showPickReservedComponentQuestion();
                            break;

                        case 4:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-3.\n> ", ASK);
                                showBuildShipBoardMenu();
                                break;
                            }
                            clientController.restartHourglass();
                            break;

                        case 5:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-3.\n> ", ASK);
                                showBuildShipBoardMenu();
                                break;
                            }

                            long componentInShipBoard = clientModel.getMyShipboard().getNumberOfComponents();
                            if (componentInShipBoard == 1) {
                                showMessage("""
                                        You cannot watch a little deck before having placed any component""", STANDARD);
                                showBuildShipBoardMenu();
                                break;
                            }

                            clientState = WATCH_LITTLE_DECK;
                            showMessage("""
                                    Which little deck would you like to watch?
                                    >\s""", ASK);
                            break;

                        case 3:
                            clientState = BUILDING_SHIPBOARD_WAITING;
                            clientController.endBuildShipBoardPhase();
                            break;

                        case 0:
                            clientState = BUILDING_SHIPBOARD_SELECT_PREFAB;
                            clientController.requestPrefabShipsList();
                            break;

                        default:
                            showMessage("Invalid choice. Please select 1-6.\n> ", ASK);
                    }
                    break;

                case BUILDING_SHIPBOARD_SELECT_PREFAB:
                    try {
                        int choice = Integer.parseInt(input);

                        if (choice == 0) {
                            clientState = BUILDING_SHIPBOARD_MENU;
                            showBuildShipBoardMenu();
                            break;
                        }

                        List<PrefabShipInfo> prefabShips = clientModel.getAvailablePrefabShips();

                        if (prefabShips.isEmpty()) {
                            showMessage("No prefab ships available yet. Please wait or try again.", ERROR);
                            break;
                        }

                        if (choice >= 1 && choice <= prefabShips.size()) {
                            PrefabShipInfo selectedShip = prefabShips.get(choice - 1);
                            clientController.selectPrefabShip(selectedShip.getId());
                            //clientState = BUILDING_SHIPBOARD_WAITING;
                            clientState = BUILDING_SHIPBOARD_MENU;
                            showBuildShipBoardMenu();
                        } else {
                            showMessage("Invalid choice. Please select a number between 0 and " + prefabShips.size(), ERROR);
                        }
                    } catch (NumberFormatException e) {
                        showMessage("Please enter a valid number.", ERROR);
                    }
                    break;

                case BUILDING_SHIPBOARD_WAITING:
                    showMessage("Invalid command", STANDARD);
                    showMessage("""
                            Your ship is ready, now wait for other player to finish theirs, they are so sloooooow.
                            Anyway use show command as before to see any shipboard or "rank" to see the current ranking.
                            You could also exit but it's not recommended.
                            >\s""", ASK);
                    break;

                case PLACE_PLACEHOLDER:
                    clientController.placePawn();
                    break;

                case BUILDING_SHIPBOARD_PICK_VISIBLE_COMPONENT:
                    if (Integer.parseInt(input) == 0) {
                        clientState = BUILDING_SHIPBOARD_MENU;
                        showBuildShipBoardMenu();
                        break;
                    }
                    clientController.pickVisibleComponent(Integer.parseInt(input));
                    break;

                case BUILDING_SHIPBOARD_WITH_FOCUSED_COMPONENT:
                    Component focusedComponent = clientModel.getPlayerClientData().get(clientController.getNickname()).getShipBoard().getFocusedComponent();
                    String[] focusedComponentString;
                    StringBuilder focusedComponentStringBuilder = new StringBuilder();
                    if (focusedComponent == null) {
                        showMessage("Still picking a component. Please wait...\n> ", ASK);
                        break;
                    }
                    switch (Integer.parseInt(input)) {
                        case 1:
                            focusedComponentString = focusedComponent.toString().split("\\n");
                            for (int i = 1; i < focusedComponentString.length; i++) {
                                focusedComponentStringBuilder.append(focusedComponentString[i]).append("\n");
                            }
                            showMessage(String.format("""
                                    \nComponent details:
                                    %s
                                    """, focusedComponentStringBuilder), STANDARD);
                            showPickedComponentAndMenu();
                            break;

                        case 2:
                            focusedComponent.rotate();
                            focusedComponentString = focusedComponent.toString().split("\\n");
                            for (int i = 1; i < focusedComponentString.length; i++) {
                                focusedComponentStringBuilder.append(focusedComponentString[i]).append("\n");
                            }
                            showMessage(String.format("""
                                    \nComponent details:
                                    %s
                                    """, focusedComponentStringBuilder), STANDARD);
                            showPickedComponentAndMenu();
                            break;

                        case 3:
                            clientState = PLACE_FOCUSED_COMPONENT;
                            showMessage("Select coordinates where to place the focused component (row column): ", ASK);
                            break;

                        case 5:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-4.\n> ", STANDARD);
                                showPickedComponentAndMenu();
                                break;
                            }
                            clientController.reserveFocusedComponent();
                            break;

                        case 4:
                            clientController.releaseFocusedComponent();
                            break;

                        default:
                            showMessage("Invalid choice. Please select 1-5.\n> ", STANDARD);
                    }
                    break;

                case BUILDING_SHIPBOARD_PICK_RESERVED_COMPONENT:
                    clientController.pickReservedComponent(Integer.parseInt(input));
                    break;

                case WATCH_LITTLE_DECK:
                    int littleDeckIndex = Integer.parseInt(input);
                    if (littleDeckIndex <= 0 || littleDeckIndex > 3) {
                        showMessage("Invalid little deck index. Please select 1-3.\n> ", STANDARD);
                        break;
                    }
                    showLittleDeck(littleDeckIndex);
                    showBuildShipBoardMenu();
                    break;

                case PLACE_FOCUSED_COMPONENT:
                    coordinates = input.trim().split("\\s+");

                    // Verifica che ci siano esattamente due numeri
                    if (coordinates.length != 2) {
                        showMessage("Invalid input. Please enter both row and column separated by space (e.g. '8 6').", ERROR);
                        break;
                    }

                    // Prova a convertire le stringhe in numeri
                    row = Integer.parseInt(coordinates[0]);
                    column = Integer.parseInt(coordinates[1]);
                    row--;
                    column--;
                    clientController.placeFocusedComponent(row, column);
                    break;

                case CHECK_SHIPBOARD_CHOOSE_COMPONENT_TO_REMOVE:
                    coordinates = input.trim().split("\\s+");

                    // Verifica che ci siano esattamente due numeri
                    if (coordinates.length != 2) {
                        showMessage("Invalid input. Please enter both row and column separated by space (e.g. '8 6').", ERROR);
                        break;
                    }

                    try {
                        // Prova a convertire le stringhe in numeri
                        row = Integer.parseInt(coordinates[0]);
                        column = Integer.parseInt(coordinates[1]);

                        // Converte le coordinate da 1-based (input utente) a 0-based (sistema interno)
                        Coordinates targetCoords = new Coordinates(row - 1, column - 1);

                        // Ottiene le coordinate dei componenti incorrettamente posizionati
                        Set<Coordinates> incorrectCoords = clientModel.getShipboardOf(clientController.getNickname())
                                .getIncorrectlyPositionedComponentsCoordinates();

                        // Controlla se le coordinate inserite sono tra quelle incorrette
                        if (incorrectCoords.contains(targetCoords)) {
                            clientController.removeComponent(targetCoords.getX(), targetCoords.getY());
                        } else {
                            showMessage("The coordinates (" + row + ", " + column + ") do not correspond to an incorrectly positioned component.", ERROR);
                            showMessage("Please choose coordinates from the red-highlighted components.", ERROR);
                        }
                    } catch (NumberFormatException e) {
                        showMessage("Invalid input. Please enter valid numbers for row and column.", ERROR);
                    }
                    break;

                case CHECK_SHIPBOARD_CHOOSE_SHIP_PART_TO_KEEP:
                    try {
                        int choice = Integer.parseInt(input.trim());
                        clientController.handleShipPartSelection(choice);
                    } catch (NumberFormatException e) {
                        showMessage("Please enter a valid number.", ERROR);
                    }
                    break;


                case CREW_PLACEMENT_MENU:
                    setClientState(WAITING_FOR_SERVER);
                    boolean isInputValid = handleCrewPlacementInput(input);
                    if(!isInputValid){
                        setClientState(CREW_PLACEMENT_MENU);
                    }
                    break;

                case NO_CREW_TO_PLACE:
                    setClientState(WAITING_FOR_SERVER);
                    clientController.submitCrewChoices(new HashMap<>());
                    break;

                case WAITING_FOR_SERVER:
                    break;

                case VISIT_LOCATION_MENU:
                    if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                        clientController.playerWantsToVisitLocation(clientController.getNickname(), true);
                    } else if (input.equalsIgnoreCase("N")) {
                        clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
                    } else {
                        showMessage("Invalid input. Please enter Y or N.", ERROR);
                    }
                    break;

                case THROW_DICES_MENU:
                    // Qualsiasi input fa tirare i dadi
                    clientController.playerWantsToThrowDices(clientController.getNickname());
                    break;

                case CHOOSE_PLANET_MENU:
                    try {
                        int planetChoice = Integer.parseInt(input);

                        ClientPlanets card = (ClientPlanets) clientModel.getCurrAdventureCard();
                        if(planetChoice != 0 && (card.getAvailablePlanets().get(planetChoice-1)==null || card.getAvailablePlanets().get(planetChoice-1).isBusy()))
                            throw new NumberFormatException();

                        clientController.playerWantsToVisitPlanet(clientController.getNickname(), planetChoice);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        showMessage("Please enter a valid planet number.", ERROR);
                    }
                    break;

                case CHOOSE_CABIN_MENU:
                    handleCabinSelection(input);
                    break;


                case CHOOSE_ENGINES_MENU:

                    if (input.equalsIgnoreCase("done")) {

                        if (selectedEngines.isEmpty()) {
                            showMessage("You didn't select any engine.", STANDARD);
                            showMessage("Only your single engines will count toward your engine power.", STANDARD);
                        }
                        setClientState(WAIT_PLAYER);
                        clientController.playerChoseDoubleEngines(
                                clientController.getNickname(), selectedEngines, selectedBatteries);
                        selectedEngines.clear();
                        selectedBatteries.clear();

                    } else {
                        showEngineWithColor();
                        handleEngineSelection(input);
                    }

                    break;

                case ACCEPT_REWARD_MENU:
                    if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                        clientController.playerWantsToAcceptTheReward(clientController.getNickname(), true);
                    } else if (input.equalsIgnoreCase("N")) {
                        clientController.playerWantsToAcceptTheReward(clientController.getNickname(), false);
                    } else {
                        showMessage("Invalid input. Please enter Y or N.", ERROR);
                    }
                    break;

                case HANDLE_SMALL_DANGEROUS_MENU:

                    if (input.equalsIgnoreCase("done")){

                        if(selectedShields.isEmpty()){
                            showMessage("You didn't select any shield. The meteorite hits your ship", STANDARD);
                        }
                        setClientState(WAIT_PLAYER);
                        clientController.playerHandleSmallDanObj(
                                clientController.getNickname(), selectedShields, selectedBatteries);

                        selectedBatteries.clear();
                        selectedShields.clear();
                    }
                    else {
                        showShieldWithColor();
                        handleShieldSelection(input);
                    }
                    break;

                case HANDLE_BIG_METEORITE_MENU:

                    if (input.equalsIgnoreCase("done")){

                        if(selectedCannons.isEmpty()){
                            showMessage("You didn't select any cannon. The meteorite won't be stopped", STANDARD);
                        }
                        setClientState(WAIT_PLAYER);
                        clientController.playerHandleBigMeteorite(clientController.getNickname(), selectedCannons, selectedBatteries);
                        selectedCannons.clear();
                        selectedBatteries.clear();
                    } else {
                        showCannonWithColor();
                        handleCannonSelection(input);
                    }
                    break;

                case HANDLE_BIG_SHOT_MENU:
                    // Qualsiasi input va bene per confermare
                    clientController.playerHandleBigShot(clientController.getNickname());
                    break;

                case EPIDEMIC_MENU:
                    // Qualsiasi input conferma
                    clientController.spreadEpidemic(clientController.getNickname());
                    break;

                case STARDUST_MENU:
                    // Qualsiasi input conferma
                    clientController.stardustEvent(clientController.getNickname());
                    break;

                case HANDLE_CUBES_REWARD_MENU:
                    if (storageManager != null && storageManager.isInRedistributionMode()) {
                        handleRedistributionInput(input);
                    } else {
                        handleStorageSelectionForReward(input); // Fallback al metodo vecchio
                    }
                    break;

                case CANNOT_ACCEPT_CUBES_REWARDS:
                    // Send an empty list to the server to proceed
                    List<Coordinates> emptyList = new ArrayList<>();
                    clientController.playerChoseStorage(clientController.getNickname(), emptyList);
                    break;

                case HANDLE_CUBES_MALUS_MENU:
                    handleStorageSelectionForMalus(input); // true = malus
                    break;

                case CANNOT_VISIT_LOCATION:
                    clientController.playerWantsToAcceptTheReward(clientController.getNickname(), false);
                    break;

                case CONFIRM_REDISTRIBUTION:
                    if (input.equalsIgnoreCase("y")) {
                        sendRedistributionToServer();
                    } else {
                        showCubeRedistributionMenu();
                    }
                    break;

                case END_GAME_PHASE:
                    clientController.leaveGame();
                    break;

                case CHOOSE_CANNONS_MENU:
                    if (input.equalsIgnoreCase("done")) {

                        if (selectedEngines.isEmpty()) {
                            showMessage("You didn't select any engine.", STANDARD);
                            showMessage("Only your single cannons will count toward your engine power.", STANDARD);
                        }
                        setClientState(WAIT_PLAYER);
                        clientController.playerChoseDoubleCannons(
                                clientController.getNickname(), selectedCannons, selectedBatteries);
                        selectedCannons.clear();
                        selectedBatteries.clear();

                    } else {
                        showCannonWithColor();
                        handleCannonSelection(input);
                    }
                    break;

                case CHOOSE_CANNONS_SELECT_BATTERY:
                    if (input.equalsIgnoreCase("cancel")) {
                        selectedCannons.removeLast();
                        showMessage("You canceled the last chosen cannon.", STANDARD);

                        setClientState(CHOOSE_CANNONS_MENU);
                        showMessage("Please choose an cannon or 'done' to confirm.", ASK);
                    }
                    else {
                        handleBatterySelection(input);
                    }
                    break;

                case CHOOSE_ENGINES_SELECT_BATTERY:
                    if (input.equalsIgnoreCase("cancel")) {
                        selectedEngines.removeLast();
                        showMessage("You canceled the last chosen engine.", STANDARD);

                        setClientState(CHOOSE_ENGINES_MENU);
                        showMessage("Please choose an engine or 'done' to confirm.", ASK);
                    }
                    else {
                        handleBatterySelection(input);
                    }
                    break;

                case HANDLE_SMALL_DANGEROUS_SELECT_BATTERY:
                    if (input.equalsIgnoreCase("cancel")) {
                        selectedShields.clear();
                        showMessage("You canceled the last chosen shield.", STANDARD);
                        setClientState(HANDLE_SMALL_DANGEROUS_MENU);
                        showMessage("Please choose a shield or 'done' to confirm.", ASK);
                    }
                    else {
                        handleBatterySelection(input);
                    }
                    break;

                case CHOOSE_CANNONS_SELECT_BATTERY_BIGMETEORITE:
                    if (input.equalsIgnoreCase("cancel")) {
                        selectedCannons.clear();
                        showMessage("You canceled the last chosen Cannons.", STANDARD);
                        setClientState(HANDLE_BIG_METEORITE_MENU);
                        showMessage("Please choose a Cannon or 'done' to confirm.", ASK);
                    }
                    else {
                        handleBatterySelection(input);
                    }
                    break;

                case CHOOSE_BATTERY_CUBES:

                    if (input.equalsIgnoreCase("cancel")) {
                        showBatteryBoxesWithColor();
                        showMessage("Please choose another box: ", ASK);
                    } else {
                        handleBatterySelection(input);
                    }
                    break;

                case EVALUATE_CREW_MEMBERS_MENU:
                    showMessage("Waiting for others to start the phase, hoping it won't take to long", STANDARD);
                    clientController.evaluatedCrewMembers();
                    break;
                case WAIT_PLAYER:
                    showMessage("This isn't your turn. Wait for other player, they are so sloooooow. Please wait...", STANDARD);
                    break;

                case CHECK_SHIPBOARD_AFTER_ATTACK:
                    break;

                case CHOOSE_STORAGE_FOR_CUBEMALUS:
                    try {
                        Coordinates coords = parseCoordinates(input);

                        // Validate selection using StorageSelectionManager
                        if (storageManager.isValidStorageSelection(coords)) {
                            // Select the storage and get cube type that will be removed
                            CargoCube removedCube = storageManager.getMostPreciousCubeAvailable();
                            storageManager.selectStorageForRemoval(coords);

                            showMessage("Storage selected. Cube " + removedCube.name() + " will be removed.", STANDARD);

                            // Check if more cubes need to be removed
                            if (storageManager.hasRemainingCubesToRemove()) {
                                // Check if there are more available cubes in the ship
                                if (storageManager.hasAvailableCubes()) {
                                    // Continue with cube selection
                                    showStorageWithColor();
                                    showAvailableCubeTypes();
                                    showMessage("You still need to remove " + storageManager.getRemainingCubesToRemove() + " cube(s). Enter another storage coordinate: ", ASK);
                                } else {
                                    // There are no more available cubes, start removing batteries
                                    int requiredBatteries = storageManager.getRequiredBatteriesCount();
                                    setClientState(CHOOSE_BATTERY_CUBES);
                                    showBatteryBoxesWithColor();
                                    showMessage("No more cubes available. You still need to remove " + requiredBatteries + " battery(s). Enter a battery box coordinate: ", ASK);
                                }
                            } else {
                                // Malus completato, invia al server
                                List<Coordinates> selectedStoragesForRemoval = storageManager.getSelectedStoragesForRemoval();
                                setClientState(WAIT_PLAYER);
                                clientController.playerChoseStorageAndBattery(clientController.getNickname(), selectedStoragesForRemoval, selectedBatteries);

                                // Cleanup
                                selectedBatteries.clear();
                                storageManager.resetMalusState();
                            }
                        } else {
                            // Invalid selection, show error and valid options
                            String errorMessage = storageManager.getValidationErrorMessage(coords);
                            showMessage(errorMessage, ERROR);

                            showMessage("You still need to remove " + storageManager.getRemainingCubesToRemove() + " cube(s). \nEnter another storage coordinate: ", ASK);
                        }

                    } catch (NumberFormatException e) {
                        showMessage("Invalid input. Please enter valid numbers for row and column.", ERROR);
                    }
                    break;

                case CANNOT_CHOOSE_CANNONS:
                    setClientState(WAIT_PLAYER);
                    clientController.playerChoseDoubleCannons(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
                    break;
                case CANNOT_CHOOSE_ENGINES:
                    setClientState(WAIT_PLAYER);
                    clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedEngines, selectedBatteries);
                    break;
                case CANNOT_HANDLE_CUBE_MALUS:
                    setClientState(WAIT_PLAYER);
                    clientController.playerChoseStorage(clientController.getNickname(), selectedStorage);
                    storageManager.resetMalusState();
                    break;
                case CANNOT_DEFEND_FROM_SMALL_DANGEROUS_OBJ:
                    setClientState(WAIT_PLAYER);
                    clientController.playerHandleSmallDanObj(clientModel.getMyNickname(),selectedShields,selectedBatteries);
                    break;
                case CANNOT_DEFEND_FROM_BIG_DANGEROUS_OBJ:
                    setClientState(WAIT_PLAYER);
                    clientController.playerHandleBigMeteorite(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
                    break;
                case CONFIRM_CHECK_SHIPBOARD_AFTER_ATTACK:
                    setClientState(WAIT_PLAYER);
                    clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
                    break;

                case WAITING_TO_EXIT:
                    System.exit(0);
                    break;

                default:
                    showMessage("", ERROR);
                    break;

            }
        } catch (NumberFormatException e) {
            showMessage("\nPlease enter a valid number.\n", ERROR);
        }

    }

    private boolean isThereAvailableBattery() {
        List<BatteryBox> batteryBoxes = clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes();
        for (BatteryBox batteryBox : batteryBoxes) {
            if (batteryBox.getRemainingBatteries() >= 1) {
                return true;
            }
        }
        return false ;
    }

    private void showBatteryBoxesWithColor(){
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<BatteryBox> availableBatteryBoxes = clientModel.getShipboardOf(clientModel.getMyNickname()).getBatteryBoxes();
        availableBatteryBoxes.removeAll(selectedBatteries);
        Set<Coordinates> availableBatteryBoxCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableBatteryBoxes);
        colorMap.put(ANSI_GREEN, availableBatteryBoxCoords);

        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()),clientModel.getMyNickname(), colorMap);
        showBatteryBoxesInfo(clientModel.getMyNickname());
    }

    /**
     * Displays the shipboard with engines highlighted in specific colors according to their type.
     * Double engines are highlighted in green and single engines are highlighted in blue.
     *
     * The method retrieves the available double engines and their coordinates, as well as all single
     * engines with their respective coordinates. A map is created to associate colors with these
     * coordinates, which is then passed to the rendering function.
     *
     * This method uses the client model to fetch the user's shipboard and identify the locations of
     * the double and single engines. Color-coded visualization is applied using ANSI color codes.
     */
    public void showEngineWithColor(){
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<DoubleEngine> availableDoubleEngines = clientModel.getShipboardOf(clientModel.getMyNickname()).getDoubleEngines();
        availableDoubleEngines.removeAll(selectedEngines);
        Set<Coordinates> availableDoubleEngineCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableDoubleEngines);
        colorMap.put(ANSI_GREEN, availableDoubleEngineCoords);

        List<Engine> engine = clientModel.getShipboardOf(clientModel.getMyNickname()).getSingleEngines();
        Set<Coordinates> engineCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(engine);
        colorMap.put(ANSI_BLUE, engineCoords);

        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()),clientModel.getMyNickname(), colorMap);
    }

    /**
     * Displays the shipboard with a specific color highlighting the available shields.
     * The method retrieves the available shield components of the shipboard
     * for the current player's ship and highlights their positions in green.
     *
     * The process includes:
     * - Accessing the shipboard of the current player.
     * - Determining which shield components are available for selection by removing already selected shields.
     * - Mapping the coordinates of the available shields to the green color code.
     * - Showing the shipboard with the updated color mapping applied.
     */
    public void showShieldWithColor() {
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<Shield> availableShields = clientModel.getShipboardOf(clientModel.getMyNickname()).getShields();
        availableShields.removeAll(selectedShields);
        Set<Coordinates> availableShieldsCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableShields);
        colorMap.put(ANSI_GREEN, availableShieldsCoords);
        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()), clientModel.getMyNickname(), colorMap);
    }

    /**
     * Displays the current shipboard view with colored indicators representing the positions of different types of cannons.
     * The method associates predefined colors with available double cannons and single cannons, and overlays these
     * representations on the shipboard.
     *
     * Green (ANSI_GREEN) is used to highlight the positions of available double cannons that have not
     * been selected yet. Blue (ANSI_BLUE) is used to represent the positions of single cannons on the shipboard.
     *
     * The method retrieves the shipboard of the current user, identifies the relevant cannon positions,
     * and then applies the color mappings. Finally, it calls {@code showShipBoard} to update and render the shipboard
     * with the visualizations.
     */
    public void showCannonWithColor(){
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<DoubleCannon> availableDoubleCannons = clientModel.getShipboardOf(clientModel.getMyNickname()).getDoubleCannons();
        availableDoubleCannons.removeAll(selectedCannons);
        Set<Coordinates> availableDoubleCannonsCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableDoubleCannons);
        colorMap.put(ANSI_GREEN, availableDoubleCannonsCoords);

        List<Cannon> cannons = clientModel.getShipboardOf(clientModel.getMyNickname()).getSingleCannons();
        Set<Coordinates> cannonsCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(cannons);
        colorMap.put(ANSI_BLUE, cannonsCoords);

        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()),clientModel.getMyNickname(), colorMap);
    }

    /**
     * Displays the storage locations on the shipboard with associated colors
     * based on their type (Special or Standard storage).
     *
     * - Special storages are highlighted in green.
     * - Standard storages are highlighted in blue.
     *
     * This method retrieves the storage locations belonging to the current user
     * by filtering out the already selected storages and then assigns a
     * corresponding color identifier to each storage type. The color-coded
     * storage locations are visualized on the shipboard. Additionally, it
     * provides information about the storages.
     *
     * The method utilizes the following classes and objects:
     * - Coordinates: Represents the coordinates of storage locations.
     * - SpecialStorage and StandardStorage: Different types of storages.
     * - Shipboard: The shipboard associated with the current user, from which
     *   storages and their locations are retrieved.
     * - SelectedStorage: List of currently selected storage components.
     *
     * The method makes use of predefined ANSI constants for color representation:
     * - ANSI_GREEN: For special storages.
     * - ANSI_BLUE: For standard storages.
     *
     * Steps performed:
     * 1. Retrieves and filters the special and standard storages for the current user.
     * 2. Maps the locations of available special and standard storages to their
     *    respective colors.
     * 3. Calls functions to display the color-coded shipboard and additional
     *    storage information.
     */
    public void showStorageWithColor(){
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<SpecialStorage> availableSpecialStorages = clientModel.getShipboardOf(clientModel.getMyNickname()).getSpecialStorages();
        availableSpecialStorages.removeAll(selectedStorage);
        Set<Coordinates> availableSpecialStoragesCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableSpecialStorages);
        colorMap.put(ANSI_GREEN, availableSpecialStoragesCoords);

        List<StandardStorage> availableStandardStorages = clientModel.getShipboardOf(clientModel.getMyNickname()).getStandardStorages();
        availableStandardStorages.removeAll(selectedStorage);
        Set<Coordinates> availableStandardStoragesCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableStandardStorages);
        colorMap.put(ANSI_BLUE, availableStandardStoragesCoords);

        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()),clientModel.getMyNickname(), colorMap);
        showStoragesInfo();
    }

    /**
     * Shows information about available cube types for malus removal.
     * Called by showHandleCubesMalusMenu to inform user about next cube to remove.
     */
    private void showAvailableCubeTypes() {
        if (storageManager != null && storageManager.isInMalusMode()) {
            CargoCube mostPrecious = storageManager.getMostPreciousCubeAvailable();
            if (mostPrecious != null) {
                List<Coordinates> validOptions = storageManager.getValidStorageOptionsForMostPreciousCube();
                showMessage("Next cube to remove: " + mostPrecious.name() + " (from " +
                        validOptions.size() + " storage(s))", STANDARD);
            }
        }
    }

    /**
     * Displays valid storage options for the given type of cargo cube. If the storage manager is
     * in malus mode, it retrieves and prints valid coordinates for storing the most valuable cubes.
     * Each valid coordinate is displayed in the message window.
     *
     * @param cubeType The type of cargo cube for which valid storage options are being shown.
     */
    private void showValidStorageOptions(CargoCube cubeType) {
        if (storageManager != null && storageManager.isInMalusMode()) {
            List<Coordinates> validCoords = storageManager.getValidStorageOptionsForMostPreciousCube();
            showMessage("Valid storage coordinates for " + cubeType.name() + " cubes:", STANDARD);
            for (Coordinates coord : validCoords) {
                showMessage("  - (" + coord.getX() + ", " + coord.getY() + ")", STANDARD);
            }
        }
    }

}

