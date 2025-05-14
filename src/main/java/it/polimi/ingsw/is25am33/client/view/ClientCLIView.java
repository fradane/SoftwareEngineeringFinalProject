package it.polimi.ingsw.is25am33.client.view;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static it.polimi.ingsw.is25am33.client.view.ClientState.*;
import static it.polimi.ingsw.is25am33.client.view.MessageType.*;

/**
 * The ClientCLIView class provides a command-line interface for interactions with the client-side
 * of the game. It extends the general client view, specifically tailored for use in a terminal
 * environment, and includes non-blocking user input handling alongside various game state
 * display functionalities.
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

    // Definizione dei colori ANSI (funziona nei terminali che supportano i colori ANSI).
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private final String defaultInterrogationPrompt = "Your choice: ";
    private String currentInterrogationPrompt = "";

    public ClientCLIView() throws RemoteException {
        this.scanner = new Scanner(System.in);
    }

    public ClientState getClientState() {
        return clientState;
    }

    public void setClientModel(ClientModel clientModel) {
        this.clientModel = clientModel;
    }

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
    }

    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    // TODO
    @Override
    public PlayerColor intToPlayerColor(int colorChoice) {
        return null;
    }

    // TODO
    @Override
    public void notifyHourglassRestarted(int flipsLeft) {

    }

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

    @Override
    public ClientController getClientController() {
        return clientController;
    }

    /**
     * Metodo per interrompere l'attesa dell'input in caso di eventi importanti
     * Questo metodo va chiamato quando arriva una notifica importante dal server
     */
    public void cancelInputWaiting() {
        synchronized (consoleLock) {
            waitingForInput = false;
        }
    }

    @Override
    public void showMessage(String message, MessageType type) {
        synchronized (consoleLock) {

            if(type == INPUT) {
                System.out.print(message);
                return;
            }

            // Se stiamo aspettando un input, va a capo per non interferire
            if (waitingForInput) {
                System.out.println();
            }

            switch (type){
                case STANDARD :
                    System.out.print(message);
                    break;
                case ERROR :
                    System.out.println(ANSI_RED + "Error: " + message + ANSI_RESET);
                    break;
                case NOTIFICATION_INFO:
                    System.out.println(ANSI_BLUE + "Info: " + message + ANSI_RESET);
                    break;
                case NOTIFICATION_CRITICAL:
                    System.out.println(ANSI_YELLOW + "Important: " + message + ANSI_RESET);
                    if (waitingForInput) {
                        waitingForInput = false;
                    }
                    break;
            }

            // Richiede l'input nuovamente se era in attesa e il tipo è INFO
            if (waitingForInput) {
                System.out.print(currentInterrogationPrompt);
            }
        }

    }

    @Override
    public void showError(String errorMessage) {
        showMessage(errorMessage, MessageType.ERROR);
    }

    @Override
    public void askNickname() {
        showMessage("Please enter your nickname: ", STANDARD);
    }

    public void showAvailableGames(Iterable<GameInfo> games) {
        boolean hasGames = false;

        System.out.println("Available games:");
        for (GameInfo game : games) {
            hasGames = true;
            System.out.println("ID: " + game.getGameId() +
                    " | Players: " + game.getConnectedPlayersNicknames().size() + "/" + game.getMaxPlayers() +
                    " | Test Flight: " + (game.isTestFlight() ? "Yes" : "No"));
        }

        if (!hasGames) {
            System.out.println("No games available.");
            clientState = ClientState.MAIN_MENU;
            showMainMenu();
        }
    }

    @Override
    public int[] askCreateGame() {
        int[] result = new int[3]; // [numPlayers, isTestFlight, colorChoice]

        // Chiedi numero di giocatori
        while (true) {
            String input = askForInput("", "Number of players (2-4): ");
            try {
                int numPlayers = Integer.parseInt(input);
                if (numPlayers >= 2 && numPlayers <= 4) {
                    result[0] = numPlayers;
                    break;
                } else {
                    System.out.println("Invalid number. Must be between 2 and 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        // Chiedi se è un volo di prova
        while (true) {
            String isTest = askForInput("", "Test flight [y/N]: ");
            if (isTest.equalsIgnoreCase("n") || isTest.isEmpty()) {
                result[1] = 0;
                break;
            } else if (isTest.equalsIgnoreCase("y")) {
                result[1] = 1;
                break;
            } else {
                System.out.println("Invalid input. Please enter y or n.");
            }
        }

        // Scegli il color

        return result;
    }

    @Override
    public String[] askJoinGame(List<GameInfo> games) {
        showAvailableGames(games);

        String[] result = new String[2]; // [gameId, colorChoice]

        result[0] = askForInput("", "Enter game ID to join: ");

        List<String> gameIds = games.stream().map(GameInfo::getGameId).toList();
        while(!gameIds.contains(result[0])){
            showError("Invalid game ID");
            result[0] = askForInput("", "Enter game ID to join: ");
        }

        List<PlayerColor> occupiedColors = games.stream()
                .filter(gameInfo -> gameInfo.getGameId().equals(result[0]))
                .flatMap(game -> game.getConnectedPlayers().values().stream())
                .toList();
        List<PlayerColor> availableColors = Arrays.stream(PlayerColor.values())
                .filter(currColor -> !occupiedColors.contains(currColor))
                .toList();

        result[1] = askPlayerColor(availableColors);

        return result;
    }

    public void showColorQuestion() {
        String colorMenu = """
                \nChoose your color:
                1. RED
                2. BLUE
                3. GREEN
                4. YELLOW
                >\s
                """;
        showMessage(colorMenu, STANDARD);
    }

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

        showMessage(colorMenu.toString(), STANDARD);
    }



    public String askPlayerColor(List<PlayerColor> availableColors) {
        String questionDescription = "\nChoose your color: \n";

        for (PlayerColor color : availableColors) {
            questionDescription = questionDescription.concat(color.getNumber() + ". " + color.toString() + "\n");
        }

        while (true) {
            String input = askForInput(questionDescription, defaultInterrogationPrompt);
            try {
                int colorChoice = Integer.parseInt(input);
                if (colorChoice >= 1 && colorChoice <= 4) {
                    return Integer.toString(colorChoice);
                } else {
                    System.out.println("Invalid choice. Please select 1-4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

    }

    @Override
    public void showMainMenu() {
        clientState = ClientState.MAIN_MENU;
        String menu = """
                \nChoose an option:
                1. Create a new game
                2. Join a game
                >\s
                """;
        showMessage(menu, STANDARD);
    }

    @Override
    public int showGameMenu() {
        System.out.println("\nEnter 1 if you want to leave the game otherwise wait for the game to start...\n");
        waitingForGameStart = true;

        try {
            while (waitingForGameStart) {
                // Controlla l'input per 500ms alla volta
                String input = inputQueue.poll(500, TimeUnit.MILLISECONDS);
                if (input != null && input.equals("1")) {
                    waitingForGameStart = false;
                    return 1; // Lascia il gioco
                }

                // Se il gioco è iniziato nel frattempo (tramite notifica dal server)
                if (!waitingForGameStart) {
                    return 0; // Il gioco è iniziato
                }
            }

            return 0; // Il gioco è iniziato
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 1; // In caso di interruzione, esci
        }
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {
        System.out.println(ANSI_BLUE + nickname + ANSI_RESET + " joined the game with color "+ gameInfo.getConnectedPlayers().get(nickname) + ". Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {
        System.out.println(ANSI_BLUE + nickname + ANSI_RESET + " left the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    public void notifyGameCreated(String gameId) {
        System.out.println("Game created! ID: " + gameId);
    }

    @Override
    public void notifyGameStarted(GameState gameState) {
        waitingForGameStart = false;
        System.out.println("The game is now in progress...");
        System.out.println("Game started! Initial state: " + gameState);
    }

    @Override
    public void notifyGameEnded(String reason) {
        System.out.println("Game ended. Reason: " + reason);
    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {
        if (isFirstTime) System.out.println("The card has been drawn from the deck.");
        System.out.println("Current adventure card: " + clientModel.getCurrAdventureCard());
    }

    @Override
    public void showNewGameState() {
        showMessage(String.format("""
                        \n===================================
                        📢  [Game Update]
                        🎮  New Game State: %s
                        ===================================
                        """, clientModel.getGameState().toString()), STANDARD);
    }

    public void showDangerousObj(){
        showMessage(String.format("""
                        \n===================================
                        📢  [Dangerous Object Attack]
                        ☄️  New Dangerous Object: %s
                        ===================================
                        """, clientModel.getCurrDangerousObj().toString()), STANDARD);
    }

    @Override
    public void showNewCardState() {
        System.out.println("===================================");
        System.out.println("🃏  [Card Update]");
        System.out.println("🆕  New Card State: " + clientModel.getCurrCardState().toString());
        System.out.println("===================================");
    }

    /**
     * Displays a menu during the ship board setup phase, allowing the player to:
     * <ul>
     *     <li>Select coordinates from the component table to place a component</li>
     *     <li>Review the latest version of the component table</li>
     *     <li>End the ship board setup phase</li>
     * </ul>
     * Returns a BiFunction representing the selected action to be performed with
     * the provided RMIServerNetworkManager and player nickname.
     *
     * @return a BiFunction with the server and nickname representing the chosen action
     */
    @Override
    public BiFunction<CallableOnGameController, String, Boolean> showBuildShipBoardMenu() {

        while (true) {

            try {
                String questionDescription = """
                        \nChoose an option:
                        1. Pick a random covered component from the table
                        2. Pick a visible component from the table
                        3. Show one of the ship boards
                        4. Restart hourglass
                        5. Watch a little deck
                        """;
                Optional<Integer> optionalInput = convertInput(askForInput(questionDescription, defaultInterrogationPrompt));
                int choice;

                if (optionalInput.isEmpty())
                    return (_, _) -> true;
                else
                    choice = optionalInput.get();

                switch (choice) {
                    case 1:
                        return (server, nickname) -> {
                            try {
                                Component pickedComponent = server.playerPicksHiddenComponent(nickname);
                                if (pickedComponent == null) {
                                    ClientCLIView.this.notifyNoMoreComponentAvailable();
                                    return false;
                                }
                                boolean hasFocusComponent = true;
                                while (hasFocusComponent) {
                                    BiFunction<CallableOnGameController, String, Boolean> consumer = ClientCLIView.this
                                            .showPickedComponentAndMenu(pickedComponent);
                                    if (consumer == null) return true;
                                    hasFocusComponent = consumer.apply(server, nickname);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
//                                System.out.println("Error connecting to server: " + e.getMessage());
//                                System.out.println("Please try again.");
                            }
                            return false;
                        };

                    case 2:
                        return (server, nickname) -> {

                            Map<Integer, Component> visibleComponents = clientModel.getVisibleComponents();

                            if (visibleComponents == null || visibleComponents.isEmpty()) {
                                ClientCLIView.this.showMessage("No visible components yet!", STANDARD);
                                return false;
                            }

                            BiFunction<CallableOnGameController, String, Component> function = ClientCLIView.this.showVisibleComponentAndMenu(visibleComponents);
                            if (function == null) return false; // means that the player has changed its idea
                            if (function == INTERRUPTED) return true; // means that the timer interrupted the player
                            Component pickedComponent = function.apply(server, nickname);
                            if (pickedComponent == null) {
                                ClientCLIView.this.showError("This component is no longer available, someone has stolen it from you!");
                                return false;
                            }

                            boolean hasFocusComponent = true;
                            while (hasFocusComponent) {
                                BiFunction<CallableOnGameController, String, Boolean> consumer = ClientCLIView.this
                                        .showPickedComponentAndMenu(pickedComponent);
                                if (consumer == null) return true;
                                hasFocusComponent = consumer.apply(server, nickname);
                            }
                            return false;
                        };

//                    case 3:
//                        String answer = askForInput("", "Are you sure you want to end your shipBoard setup phase? [Y/n] ");
//                        if (answer.equalsIgnoreCase("Y") || answer.isEmpty()) {
//                            System.out.println("Ending your shipBoard setup phase...");
//                            return (server, nickname) -> {
//                                try {
//                                    server.playerChoseToEndBuildShipBoardPhase(nickname);
//                                } catch (IOException e) {
//                                    throw new RuntimeException(e);
//                                }
//                                return true;
//                            };
//                        }
//                        break;

                    case 3:
                        showShipBoardsMenu();
                        break;

                    case 4:
                        if (clientModel.getHourglass().isRunning()) {
                            showMessage("The hourglass is already running, please wait for it to end.", STANDARD);
                            break;
                        }

                        return (server, nickname) -> {
                            try {
                                showMessage("Restarting the hourglass...", NOTIFICATION_INFO);
                                server.playerWantsToRestartHourglass(nickname);
                                showMessage("The hourglass has been restarted.", NOTIFICATION_INFO);
                            } catch (RemoteException e) {
                                switch (e.getMessage()) {

                                    case "No more flips available.",
                                         "Interrupted while waiting for all clients to finish the timer.",
                                         "Another player is already restarting the hourglass. Please wait.":
                                        showError(e.getMessage());
                                        break;

                                    default:
                                        showError("An error occurred: " + e.getMessage());
                                        break;

                                }
                            }
                            return false;
                        };

                    case 5:
                        int littleDeckChoice;
                        while (true) {
                            optionalInput = convertInput(askForInput("", "Which little deck would you like to watch? (1-3): "));

                            if (optionalInput.isEmpty())
                                return null;
                            else
                                littleDeckChoice = optionalInput.get();

                            if (littleDeckChoice >= 1 && littleDeckChoice <= 3) break;
                            showMessage("Invalid choice. Please select 1-3.", STANDARD);
                        }

                        int finalLittleDeckChoice = littleDeckChoice;
                        return (server, nickname) -> {
                            boolean response;
                            Boolean wasInterrupted = null;
                            try {
                                response = server.playerWantsToWatchLittleDeck(nickname, finalLittleDeckChoice);

                                if (!response) {
                                    showMessage("The little deck is not available right now!\nTry again later.", STANDARD);
                                    return false;
                                }

                                wasInterrupted = ClientCLIView.this.showLittleDeck(finalLittleDeckChoice);
                                server.playerWantsToReleaseLittleDeck(nickname, finalLittleDeckChoice);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return wasInterrupted;
                        };

                    default:
                        System.out.println("Invalid choice. Please select 1-6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }

        }
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

    @Override
    public Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents) {
        showShipBoard(shipBoard.getShipMatrix(), clientModel.getMyNickname());

        String questionDescription = "\nChoose a component to remove: \n";

        for (Component component : incorrectlyPositionedComponents) {
            questionDescription = questionDescription.concat(component.toString() + "\n");
        }

        while (true) {
            String input = askForInput(questionDescription, defaultInterrogationPrompt);
            try {
                int indexChoosen = Integer.parseInt(input);
                if (indexChoosen >= 1 && indexChoosen <= incorrectlyPositionedComponents.size()) {
                    return incorrectlyPositionedComponents.get(indexChoosen);
                } else {
                    System.out.println("Invalid choice. Please select 1-" + incorrectlyPositionedComponents.size() + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    @Override
    public Boolean showLittleDeck(int littleDeckChoice) {
        StringBuilder littleDeck = new StringBuilder();
        littleDeck.append("\nHere is the little deck you chose:\n");
        clientModel.getLittleVisibleDecks().get(littleDeckChoice - 1).forEach(
                card -> littleDeck.append(card).append("\n")
        );
        showMessage(littleDeck.toString(), STANDARD);
        return askForInput("", "Press enter to continue...").equals(INPUT_INTERRUPT);
    }

    @Override
    public void notifyNoMoreComponentAvailable() {
        this.showMessage("""
                No more component available.
                Tip: if you want more components to build your shipboard look among the visible ones.
                """, STANDARD);
    }

    /**
     * Displays a menu with actions the player can perform on the selected component.
     * The component is initially displayed in its current state.
     * The user can:
     * <ul>
     *     <li>Rotate the component</li>
     *     <li>Place it on the ship board by providing coordinates</li>
     *     <li>Reserve the component</li>
     *     <li>Release the component</li>
     *     <li>View the current ship board</li>
     * </ul>
     *
     * The method loops until a valid action that returns control is selected.
     *
     * @param component the component to interact with
     */
    @Override
    public BiFunction<CallableOnGameController, String, Boolean> showPickedComponentAndMenu(Component component) {

        showMessage("\nYou have selected the component:\n" + component.toString() + "\n", STANDARD);

        while (true) {

            try {
                String questionDescription = """
                        \nChoose an action:
                        1. Rotate the component
                        2. Place component on ship board
                        3. Reserve component
                        4. Release component
                        5. Show your ship board
                        """;
                int choice;
                Optional<Integer> optionalInput = convertInput(askForInput(questionDescription, defaultInterrogationPrompt));

                if (optionalInput.isEmpty())
                    return null;
                else
                    choice = optionalInput.get();

                switch (choice) {
                    case 1:
                        component.rotate();
                        System.out.println("\nComponent details:");
                        System.out.println(component);
                        break;

                    case 2:
                        Coordinates coords = readCoordinatesFromUserInput("Select coordinates (row column): ");
                        if (coords == null) return null;

                        return (server, nickname) -> {
                            try {
                                server.playerWantsToPlaceFocusedComponent(nickname, coords);
                                coords.setCoordinates(List.of(coords.getX() + 1, coords.getY() + 1));
                                ClientCLIView.this.showMessage("You placed the component at: " + coords + ".", STANDARD);
                                return false;
                            } catch (IOException e) {
                                e.printStackTrace();
//                                ClientCLIView.this.showError(e.getMessage());
//                                ClientCLIView.this.showError("Try again.");
                                return true;
                            }
                        };

                    case 3:
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToReserveFocusedComponent(nickname);
                                ClientCLIView.this.showMessage("Component reserved.", STANDARD);
                                return false;
                            } catch (IOException e) {
                                ClientCLIView.this.showError(e.getMessage());
                                ClientCLIView.this.showError("Try again.");
                                return true;
                            }
                        };

                    case 4:
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToReleaseFocusedComponent(nickname);
                                ClientCLIView.this.showMessage("Component released.", STANDARD);
                                return false;
                            } catch (IOException e) {
                                ClientCLIView.this.showError(e.getMessage());
                                ClientCLIView.this.showError("Try again.");
                                return true;
                            }
                        };

                    case 5:
                        showMyShipBoard();
                        break;

                    default:
                        System.out.println("Invalid choice. Please select 1-6.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }

        }
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
     * Displays a menu where players can view the ship boards of other participants.
     * The method retrieves a list of player nicknames and displays it as a selectable menu.
     * The user is prompted to input the number corresponding to the player whose ship board they wish to view.
     * If the input is valid, the method displays the selected player's ship board.
     * In case of invalid input, the user is prompted again until a valid choice is made.
     * <p>
     * Key functionality:
     * - Presents a numbered menu of player nicknames.
     * - Validates user input to ensure it corresponds to an available index.
     * - Displays the selected player's ship board using {@link #showShipBoard(Component[][], String)}.
     * <p>
     * The method continues to loop until a valid choice is entered, or the desired ship board is successfully displayed.
     */
    @Override
    public void showShipBoardsMenu() {
        while (true) {
            try {
                String questionDescription = "\nChoose one of the ship boards:";
                List<String> playersNickname = clientModel.getPlayersNickname().stream().toList();
                for (String player : playersNickname) {
                    questionDescription = questionDescription.concat("\n" + (playersNickname.indexOf(player) + 1) + ". " + player);
                }
                questionDescription = questionDescription.concat("\n");
                int choice = Integer.parseInt(this.askForInput(questionDescription, defaultInterrogationPrompt));
                if (choice <= 0 || choice > playersNickname.size()) {
                    showMessage("Invalid choice. Please enter a valid number.", STANDARD);
                } else {
                    String chosenNickname = playersNickname.get(choice - 1);
                    this.showShipBoard(clientModel.getShipboardOf(chosenNickname).getShipMatrix(), chosenNickname);
                }

                return;
            } catch (NumberFormatException e) {
                showMessage("Invalid choice. Please enter a valid number.", STANDARD);
            }
        }

    }

    /**
     * Displays the current state of a player's ship board on the command line.
     * This includes the ship board grid, component connectors, and positions.
     *
     * @param shipBoard the two-dimensional array representing the ship board,
     *                  where each {@code Component} represents a part of the board.
     *                  A {@code null} value indicates an empty cell.
     * @param shipBoardOwnerNickname the nickname of the player who owns the ship board.
     */
    @Override
    public void showShipBoard(Component[][] shipBoard, String shipBoardOwnerNickname) {

        String[] legendLines = {
                "LEGEND - component label and explanation on attributes:",
                "",
                "• BBX = battery box - number of remaining batteries",
                "• CAB = cabin - number and type of members",
                "• CAN = cannon - fire direction",
                "• 2CN = double cannons - fire direction",
                "• 2EN = double engines - power direction",
                "• ENG = engine - power direction",
                "• LSP = life support - type of life support",
                "• MCB = main cabin - number and type of members",
                "• SLD = shield - covered directions",
                "• SPS = special storage - left storages",
                "• STS = standard storage - left storages",
                "• STR = structural modules"
        };

        StringBuilder output = new StringBuilder();

        int legendIndex = 0;

        output.append(String.format("\nHere's the ship board of " + shipBoardOwnerNickname + ":\n"));

        // Stampa numeri delle colonne
        output.append("       ");
        for (int col = 4; col <= 10; col++) {
            output.append(String.format("   %2d     ", col));
        }

        output.append("\n");

        // TODO generalizzare il caso per il livello 1
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
                    switch (line) {
                        case 0:
                            output.append("+---------");
                            break;

                        case 1:
                            output.append(String.format("|    %1s    ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.NORTH).fromConnectorTypeToValue())));
                            break;

                        case 2: output.append(String.format("| %1s %3s %1s ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.WEST).fromConnectorTypeToValue()),
                                Level2ShipBoard.isOutsideShipboard(i, j) ? (ANSI_RED + "OUT" + ANSI_RESET) : (cell == null ? "" : cell.getLabel()),
                                Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.EAST).fromConnectorTypeToValue())));
                            break;

                        case 3: output.append(String.format("|    %1s %2s ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue()),
                                Level2ShipBoard.isOutsideShipboard(i, j) ? "" : (cell == null ? "" : (ANSI_BLUE + (cell.getMainAttribute().length() == 2 ? "" : " ") + cell.getMainAttribute() + ANSI_RESET))));
                            break;
                    }
                }

                if (line == 0) {
                    output.append("+");
                } else {
                    output.append("|");
                }

                output.append(legendIndex <= legendLines.length - 1 ? ("\t\t" + legendLines[legendIndex++] + "\n") : "\n");

            }
        }

        output.append("      ");
        output.append("+---------".repeat(7));
        output.append("+\n");

        showMessage(output.toString(), STANDARD);
    }

    @Override
    public BiFunction<CallableOnGameController, String, Component> showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {

        StringBuilder visibleComponentsList = new StringBuilder();
        visibleComponentsList.append("\nHere's the visible components:");

        visibleComponents.keySet().forEach(index -> {
            visibleComponentsList.append("\n").append(index).append(". ").append(visibleComponents.get(index));
        });
        showMessage(visibleComponentsList.toString(), STANDARD);

        while (true) {
            try {
                Optional<Integer> optionalInput = convertInput(askForInput("", "Choose one of the visible components (0 to go back): "));
                int choice;

                if (optionalInput.isEmpty())
                    return INTERRUPTED;
                else
                    choice = optionalInput.get();

                if (choice == 0) return null;

                if (!visibleComponents.containsKey(choice)) {
                    showMessage("Invalid choice. Please enter a valid number.", STANDARD);
                    continue;
                }

                return (server, nickname) -> {
                    try {
                        return server.playerPicksVisibleComponent(nickname, choice);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

            } catch (NumberFormatException e) {
                showMessage("Invalid choice. Please enter a valid number.", STANDARD);
            }
        }

    }

    @Override
    public BiConsumer<CallableOnGameController, String> showVisitLocationMenu() {

        while (true) {
            String input = askForInput("", "Do you want to visit the card location? [Y/n] ");
            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToVisitLocation(nickname, true);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            } else if (input.equalsIgnoreCase("N")) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToVisitLocation(nickname, false);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            } else {
                showMessage("Invalid input. Please enter Y or N.", STANDARD);
            }
        }

    }

    @Override
    public BiConsumer<CallableOnGameController, String> showThrowDicesMenu(){
        askForInput("", "Press any key to throw dices ");

        return (server, nickname) -> {
            try {
                server.playerWantsToThrowDices(nickname);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChoosePlanetMenu(){
//        int choice = Integer.parseInt(askForInput("Choose the index of the planet you want visit, between 1 and " + ((Planets) clientModel.getCurrAdventureCard()).getAvailablePlanets().size() + 1 + " (press 0 to skip). ", defaultInterrogationPrompt));
//        return(server, nickname) -> {
//            try {
//                server.playerWantsToVisitPlanet(nickname, choice);
//            } catch (RemoteException e) {
//                throw new RuntimeException(e);
//            }
//        };
        return null;
        // TODO
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChooseEnginesMenu() {

        this.showMyShipBoard();
        showMessage("\n", STANDARD);
        List<Coordinates> doubleEnginesCoordinates = new ArrayList<>();
        List<Coordinates> batteryBoxesCoordinates = new ArrayList<>();
        ShipBoardClient shipBoard = getMyShipBoard();

        while(true) {

            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double engines you would like to activate or press enter to skip: ");
            if (coords == null) break;
            if (shipBoard.getDoubleEngines().contains(shipBoard.getComponentAt(coords)))
                doubleEnginesCoordinates.add(coords);
            else {
                showMessage("The selected coordinates are not related to any double engine, try again", STANDARD);
                continue;
            }

            while(true) {

                Coordinates batteryCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
                if(batteryCoords == null){
                    showMessage("You have to select a battery box after choosing a double engine.", STANDARD);
                    continue;
                }
                if (shipBoard.getBatteryBoxes().contains(shipBoard.getComponentAt(batteryCoords))) {
                    batteryBoxesCoordinates.add(batteryCoords);
                    break;
                }
                else
                    showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
            }
        }

        return (server, nickname) -> {
            try {
                server.playerChoseDoubleEngines(nickname, doubleEnginesCoordinates, batteryBoxesCoordinates);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private ShipBoardClient getMyShipBoard() {
        return clientModel.getShipboardOf(clientModel.getMyNickname());
    }

    private void showMyShipBoard() {
        this.showShipBoard(getMyShipBoard().getShipMatrix(), clientModel.getMyNickname());
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu() {

        while(true) {
            String input = askForInput("", "Do you want to accept the reward? [Y/n] ");
            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToAcceptTheReward(nickname, true);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            } else if (input.equalsIgnoreCase("N")) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToAcceptTheReward(nickname, false);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else {
                showMessage("Invalid input. Please enter Y or N.", STANDARD);
            }
        }
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChooseCannonsMenu(){
        this.showMyShipBoard();
        System.out.println();
        List<Coordinates> doubleCannonsCoordinates = new ArrayList<>();
        List<Coordinates> batteryBoxesCoordinates = new ArrayList<>();
        ShipBoardClient myShipBoard = getMyShipBoard();

        while(true) {

            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double cannon you would like to activate or press enter to skip: ");
            if (coords == null) break;
            if (myShipBoard.getDoubleCannons().contains(myShipBoard.getComponentAt(coords)))
                doubleCannonsCoordinates.add(coords);
            else {
                showMessage("The selected coordinates are not related to any double cannons, try again", STANDARD);
                continue;
            }

            while(true) {

                Coordinates batteryCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
                if(batteryCoords == null){
                    showMessage("You have to select a battery box after choosing a double engine.", STANDARD);
                    continue;
                }
                if (myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryCoords))) {
                    batteryBoxesCoordinates.add(batteryCoords);
                    break;
                }
                else
                    showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
            }
        }

        return (server, nickname) -> {
            try {
                server.playerChoseDoubleCannons(nickname, doubleCannonsCoordinates, batteryBoxesCoordinates);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu() {
        this.showMyShipBoard();
        System.out.println();
        List<Coordinates> cabinCoordinates = new ArrayList<>();
        ShipBoardClient myShipBoard = getMyShipBoard();

        while (true) {
            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) of the cabins from which you want to remove the crew members: ");
            if (coords == null) {
                showMessage("You have to select cabins", STANDARD);
                continue;
            }
            if (myShipBoard.getCabin().contains(myShipBoard.getComponentAt(coords))) {
                cabinCoordinates.add(coords);
                break;
            } else {
                showMessage("The selected coordinates are not related to any cabin, try again", STANDARD);
            }
        }

        return (server, nickname) -> {
            try {
                server.playerChoseCabin(nickname, cabinCoordinates);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };

    }

    @Override
    public BiConsumer<CallableOnGameController, String> showSmallDanObjMenu() {

        showMessage(clientModel.getCurrDangerousObj().getDangerousObjType() + " incoming!!!", STANDARD);
        showMessage("Choose how to defend from it", STANDARD);
        ShipBoardClient myShipBoard = getMyShipBoard();

        while (true) {
            Coordinates activableCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the shield you would like to activate or press enter if you don't need to activate one: ");
            if (activableCoords == null)
                return (server, nickname) -> {
                    try {
                        server.playerHandleSmallDanObj(nickname, new Coordinates(), new Coordinates());
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            if(myShipBoard.getShields().contains(myShipBoard.getComponentAt(activableCoords))) {
                showMessage("The selected coordinates are not related to any shield, try again", STANDARD);
                continue;
            }

            Coordinates batteryBoxCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
            if(myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryBoxCoords))) {
                showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
                continue;
            }

            return (server, nickname) -> {
                try {
                    server.playerHandleSmallDanObj(nickname, activableCoords, batteryBoxCoords);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            };
        }

    }

    @Override
    public BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu() {

        showMessage(clientModel.getCurrDangerousObj().getDangerousObjType() + " incoming!!!", STANDARD);
        showMessage("Choose how to defend from it", STANDARD);
        ShipBoardClient myShipBoard = getMyShipBoard();

        while (true) {
            Coordinates doubleCannonCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double cannon you would like to activate or press enter if you don't need to activate one: ");
            if (doubleCannonCoords == null)
                return (server, nickname) -> {
                    try {
                        server.playerHandleBigMeteorite(nickname, new Coordinates(), new Coordinates());
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            if(myShipBoard.getDoubleCannons().contains(myShipBoard.getComponentAt(doubleCannonCoords))) {
                showMessage("The selected coordinates are not related to any double cannon, try again", STANDARD);
                continue;
            }

            Coordinates batteryBoxCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
            if(myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryBoxCoords))) {
                showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
                continue;
            }

            return (server, nickname) -> {
                try {
                    server.playerHandleBigMeteorite(nickname, doubleCannonCoords, batteryBoxCoords);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showBigShotMenu() {

        showMessage("Big shot incoming!!!\nLet's hope it will miss your ship because there is nothing you can do :(", STANDARD);
        return (server, nickname) -> {
            try {
                server.playerHandleBigShot(nickname);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showEpidemicMenu() {
        showMessage("An epidemic is spreading!!!\nRemoving 1 crew member (human or alien) from every occupied cabin connected to another occupied cabin...", STANDARD);
        return(server, nickname) -> {
            try {
                server.spreadEpidemic(nickname);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showStardustMenu() {
        showMessage("Stardust is coming!\nMaking one step back for every exposed component on you ship...", STANDARD);
        return(server, nickname) -> {
            try {
                server.stardustEvent(nickname);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu() {

        while (true){
            Coordinates coords = readCoordinatesFromUserInput("Choose the coordinates (row column) of the storage where you would like to store the cargo cube or press enter to skip this reward: ");
            if (coords == null) return (server, nickname) -> {
                try {
                    server.playerChoseStorage(nickname, new Coordinates());
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            };

            ShipBoardClient myShipBoard = getMyShipBoard();

            List<Component> storages = new ArrayList<>();
            storages.addAll(myShipBoard.getSpecialStorages());
            storages.addAll(myShipBoard.getStandardStorages());

            if (storages.contains(myShipBoard.getComponentAt(coords)))
                return (server, nickname) -> {
                    try {
                        server.playerChoseStorage(nickname, coords);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };
            else
                showMessage("The selected coordinates are not related to any storage, try again", STANDARD);
        }

    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu() {

        while (true){
            Coordinates coords = readCoordinatesFromUserInput("Choose the coordinates (row column) of the storage where you would like to remove one cargo cube: ");
            ShipBoardClient myShipBoard = getMyShipBoard();

            List<Component> storages = new ArrayList<>();
            storages.addAll(myShipBoard.getStandardStorages());
            storages.addAll(myShipBoard.getSpecialStorages());

            if (storages.contains(myShipBoard.getComponentAt(coords)))
                return (server, nickname) -> {
                    try {
                        server.playerChoseStorage(nickname, coords);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };
            else
                showMessage("The selected coordinates are not related to any storage, try again", STANDARD);
        }

    }

    public void showCurrentRanking() {
        System.out.println("==========  Ranking  ==========");

        List<String> sortedRanking = clientModel.getSortedRanking();

        int topScore = clientModel.getPlayerClientData().get(sortedRanking.getFirst()).getFlyingBoardPosition();

        for (String player : sortedRanking) {
            int playerScore = clientModel.getPlayerClientData().get(player).getFlyingBoardPosition();
            int diff = topScore - playerScore;
            System.out.printf("%-20s | %-10d%n", player, -diff);
        }

        System.out.println("===============================");
        System.out.println("Legend: the score shows how many steps behind the leader each player is.\n");
    }

    @Override
    public void notifyTimerEnded(int flipsLeft) {
        if (flipsLeft == 0)
            showMessage("Timer ended! You cannot build your ship anymore.", NOTIFICATION_CRITICAL);
        else if (flipsLeft == 1)
            showMessage("Timer ended! There is now " + flipsLeft + " flip left.", NOTIFICATION_INFO);
        else
            showMessage("Timer ended! There are now " + flipsLeft + " flips left.", NOTIFICATION_INFO);
    }

    @Override
    public void updateTimeLeft(int timeLeft) {
        if (timeLeft % 20 == 0 && timeLeft != 0 && timeLeft != 60) {
            showMessage("Time left: " + timeLeft, NOTIFICATION_INFO);
        }
    }

    private Optional<Integer> convertInput(String input) throws NumberFormatException {
        return input.equals(INPUT_INTERRUPT) ? Optional.empty() : Optional.of(Integer.parseInt(input));
    }

    private static final BiFunction<CallableOnGameController, String, Component> INTERRUPTED = (s, n) -> null;

    public void showNumPlayersQuestion() {
        showMessage("How many players do you want to play with?", STANDARD);
    }

    public void showTestFlightQuestion() {
        showMessage("Do you want to play the test flight? [y/n]", STANDARD);
    }

    public void handleInput(String input) {

        if (input.equals("exit")) {
            clientController.leaveGame();
            System.exit(0);
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
                            showAvailableGames(clientController.getGames());
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
                        int numPlayers = Integer.parseInt(stringQueue.poll());
                        boolean isTestFlight = Boolean.parseBoolean(stringQueue.poll());
                        PlayerColor playerColor = PlayerColor.getPlayerColor(Integer.parseInt(stringQueue.poll()));
                        clientController.handleCreateGameMenu(numPlayers, isTestFlight, playerColor);
                    } catch(NumberFormatException e) {
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
                        String gameId = stringQueue.poll();
                        PlayerColor playerColor = PlayerColor.getPlayerColor(Integer.parseInt(stringQueue.poll()));
                        clientController.joinGame(gameId, playerColor);
                        clientState = WAIT_FOR_PLAYERS;
                    } catch (NumberFormatException e) {
                        showMessage("\nOne or more values were incorrect. Please try again.\n", ERROR);
                        clientState = JOIN_GAME_CHOOSE_GAME_ID;
                        stringQueue.clear();
                        showAvailableGames(clientController.getGames());
                    }
                    break;

                case WAIT_FOR_PLAYERS:
                    // TODO
                    break;

                default:
                    showMessage("", ERROR);
                    break;

            }
        } catch (NumberFormatException e) {
            showMessage("\nPlease enter a valid number.\n", ERROR);
        }

    }

}