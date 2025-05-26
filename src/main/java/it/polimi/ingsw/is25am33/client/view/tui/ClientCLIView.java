package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.*;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;

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
    Map<String, Set<Coordinates>> coloredCoordinates = new HashMap<>();

    // Definizione dei colori ANSI (funziona nei terminali che supportano i colori ANSI).
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
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
                break;
            case NOTIFICATION_INFO:
                System.out.print(ANSI_BLUE + "Info: " + message + ANSI_RESET + "\n> ");
                break;
            case NOTIFICATION_CRITICAL:
                System.out.print(ANSI_YELLOW + "Important: " + message + ANSI_RESET + "\n> ");
                if (waitingForInput) {
                    waitingForInput = false;
                }
                break;
        }

    }

    @Override
    public void showError(String errorMessage) {
        showMessage(errorMessage, MessageType.ERROR);
    }

    @Override
    public void askNickname() {
        showMessage("Please enter your nickname: ", ASK);
    }

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
                Choose your color:
                1. RED
                2. BLUE
                3. GREEN
                4. YELLOW
                >\s""";
        showMessage(colorMenu, ASK);
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
        colorMenu.append(">\s");
        showMessage(colorMenu.toString(), ASK);
    }



    public String askPlayerColor(@NotNull List<PlayerColor> availableColors) {
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
                >\s""";
        showMessage(menu, ASK);
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
        showMessage(nickname + " joined the game with color "+ gameInfo.getConnectedPlayers().get(nickname) + ". Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers(), NOTIFICATION_INFO);
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {
        System.out.println(ANSI_BLUE + nickname + ANSI_RESET + " left the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    public void notifyGameCreated(String gameId) {
        showMessage("Game created! ID: " + gameId, NOTIFICATION_INFO);
    }

    @Override
    public void showWaitingForPlayers() {
        String menu = """
                Successfully joined game!
                Enter "exit" to leave the game.
                Waiting for the game to start...
                """;
        showMessage(menu, STANDARD);
    }

    @Override
    public void notifyGameStarted(GameState gameState) {
        waitingForGameStart = false;
        showMessage("""
                The game is now in progress...
                
                """, STANDARD);
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


    @Override
    public void showBuildShipBoardMenu() {
        clientState = BUILDING_SHIPBOARD_MENU;
        String menu = """
                        \nChoose an option:
                        1. Pick a random covered component from the table
                        2. Pick a visible component from the table
                        3. Place a reserved component
                        4. Restart hourglass
                        5. Watch a little deck
                        ("show [nickname]" to watch other's player ship board)
                        >\s""";
        showMessage(menu, ASK);
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
        showShipBoard(shipBoard, clientModel.getMyNickname());

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

    @Override
    public void showValidShipBoardMenu() {
        setClientState(CHECK_SHIPBOARD_CORRECT);
        String nickname = clientModel.getMyNickname();
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        showShipBoard(shipBoard, nickname);
        showMessage("TEXT TO BE CHANGED: non fare nulla che stai apposto così", STANDARD);
    }

    @Override
    public void showChooseComponentToRemoveMenu() {
        //showMessage("Insert the coordinates of the component you wanna remove: ", STANDARD);

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

            message.append("Enter coordinates as 'row column' (e.g., '5 7'): ");
        } else {
            message.append("No incorrectly positioned components found.\n");
        }

        showMessage(message.toString(), STANDARD);
    }

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

    @Override
    public void showLittleDeck(int littleDeckChoice) {
        StringBuilder littleDeck = new StringBuilder();
        littleDeck.append("\nHere is the little deck you chose:\n");
        clientModel.getLittleVisibleDecks().get(littleDeckChoice - 1).forEach(
                card -> {
                    String[] cardLines = card.split("\\n");
                    for (int i = 1; i < cardLines.length; i++) {
                        littleDeck.append(cardLines[i]).append("\n");
                    }
                }
        );
        showMessage(littleDeck.toString(), STANDARD);
    }

    @Override
    public void notifyNoMoreComponentAvailable() {
        clientState = BUILDING_SHIPBOARD_MENU;
        this.showMessage("""
                No more component available.
                Tip: if you want more components to build your shipboard look among the visible ones.
                """, STANDARD);
    }

    @Override
    public void showPickedComponentAndMenu() {
        clientState = BUILDING_SHIPBOARD_WITH_FOCUSED_COMPONENT;
        String menu = """
                    \nChoose an action:
                    1. Show focus component
                    2. Rotate the component
                    3. Place component on ship board
                    4. Reserve component
                    5. Release component
                    ("show [nickname]" to watch other's player ship board)
                    >\s""";
        showMessage(menu, ASK);
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
     * Displays the ship board of a specified player's ship in a formatted textual representation.
     * It includes components, their labels, attributes, and a legend explaining the component types.
     *
     * @param shipBoardClient the client that provides the ship matrix and booked components
     * @param shipBoardOwnerNickname the nickname of the owner of the ship board being displayed
     */
//    @Override
//    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname) {
//
//        Component[][] shipBoard = shipBoardClient.getShipMatrix();
//        List<Component> bookedComponents = shipBoardClient.getBookedComponents();
//        String[] reservedComponent1 = new String[7];
//        String[] reservedComponent2 = new String[7];
//
//        if (!bookedComponents.isEmpty())
//            reservedComponent1 = shipBoardClient.getBookedComponents().getFirst().toString().split("\\n");
//        if (bookedComponents.size() == 2)
//            reservedComponent2 = shipBoardClient.getBookedComponents().get(1).toString().split("\\n");
//
//
//        String[] legendLines = {
//                "LEGEND - component label and explanation on attributes:",
//                "",
//                "• BBX = battery box - number of remaining batteries",
//                "• CAB = cabin - number and type of members",
//                "• CAN = cannon - fire direction",
//                "• 2CN = double cannons - fire direction",
//                "• 2EN = double engines - power direction",
//                "• ENG = engine - power direction",
//                "• LSP = life support - type of life support",
//                "• MCB = main cabin - number and type of members",
//                "• SLD = shield - covered directions",
//                "• SPS = special storage - left storages",
//                "• STS = standard storage - left storages",
//                "• STR = structural modules",
//                ""
//        };
//
//        StringBuilder output = new StringBuilder();
//
//        int legendIndex = 0;
//        int componentIndex = 0;
//
//        output.append(String.format("\nHere's the ship board of " + shipBoardOwnerNickname + ":\n"));
//
//        // Stampa numeri delle colonne
//        output.append("       ");
//        for (int col = 4; col <= 10; col++) {
//            output.append(String.format("   %2d     ", col));
//        }
//
//          output.append(String.format("\t\t" + legendLines[legendIndex++] + "\n"));
//
//        // TODO generalizzare il caso per il livello 1
//        for (int i = 4; i <= 8; i++) {
//            // Ogni cella viene stampata su 4 righe
//            for (int line = 0; line < 4; line++) {
//                if (line == 2) {
//                    output.append(String.format(" %2d   ", i + 1));
//                } else {
//                    output.append("      ");
//                }
//                for (int j = 3; j <= 9; j++) {
//                    Component cell = shipBoard[i][j];
//                    switch (line) {
//                        case 0:
//                            output.append("+---------");
//                            break;
//
//                        case 1:
//                            output.append(String.format("|    %1s    ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.NORTH).fromConnectorTypeToValue())));
//                            break;
//
//                        case 2: output.append(String.format("| %1s %3s %1s ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.WEST).fromConnectorTypeToValue()),
//                                Level2ShipBoard.isOutsideShipboard(i, j) ? (ANSI_RED + "OUT" + ANSI_RESET) : (cell == null ? "" : cell.getLabel()),
//                                Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.EAST).fromConnectorTypeToValue())));
//                            break;
//
//                        case 3: output.append(String.format("|    %1s %2s ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue()),
//                                Level2ShipBoard.isOutsideShipboard(i, j) ? "" : (cell == null ? "" : (ANSI_BLUE + (cell.getMainAttribute().length() == 2 ? "" : " ") + cell.getMainAttribute() + ANSI_RESET))));
//                            break;
//                    }
//                }
//
//                if (line == 0) {
//                    output.append("+");
//                } else {
//                    output.append("|");
//                }
//
//                if (legendIndex <= legendLines.length - 1)
//                    output.append(String.format("\t\t" + legendLines[legendIndex++] + "\n"));
//                else if (componentIndex <= 6) {
//                    if (reservedComponent1[componentIndex] != null) output.append(String.format("\t\t\t" + reservedComponent1[componentIndex]));
//                    if (reservedComponent2[componentIndex] != null) output.append(String.format("\t\t\t" + reservedComponent2[componentIndex]));
//                    output.append("\n");
//                    componentIndex++;
//                }
//
//            }
//        }
//
//        output.append("      ");
//        output.append("+---------".repeat(7));
//        output.append("+\n");
//        output.append("> ");
//
//        showMessage(output.toString(), ASK);
//    }

    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname) {
        showShipBoard(shipBoardClient, shipBoardOwnerNickname, Collections.emptyMap());
    }

    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {

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

                    // Determina il colore per questa coordinata
                    String componentColor = getColorForCoordinate(i, j, colorMap);
                    String resetColor = componentColor.isEmpty() ? "" : ANSI_RESET;

                    switch (line) {
                        case 0:
                            output.append("+---------");
                            break;

                        case 1:
                            String northConnector = Level2ShipBoard.isOutsideShipboard(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.NORTH).fromConnectorTypeToValue()));
                            output.append(String.format("|    %s%1s%s    ",
                                    componentColor, northConnector, resetColor));
                            break;

                        case 2:
                            String westConnector = Level2ShipBoard.isOutsideShipboard(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.WEST).fromConnectorTypeToValue()));
                            String eastConnector = Level2ShipBoard.isOutsideShipboard(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.EAST).fromConnectorTypeToValue()));
                            String label = Level2ShipBoard.isOutsideShipboard(i, j) ? (ANSI_RED + "OUT" + ANSI_RESET) :
                                    (cell == null ? "" : (componentColor + cell.getLabel() + resetColor));

                            output.append(String.format("| %s%1s%s %3s %s%1s%s ",
                                    componentColor, westConnector, resetColor,
                                    label,
                                    componentColor, eastConnector, resetColor));
                            break;

                        case 3:
                            String southConnector = Level2ShipBoard.isOutsideShipboard(i, j) ? "X" :
                                    (cell == null ? "" : Integer.toString(cell.getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue()));
                            String attribute = Level2ShipBoard.isOutsideShipboard(i, j) ? "" :
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
     * Determina il colore ANSI da applicare alla coordinata specificata
     * basandosi sulla mappa dei colori fornita.
     *
     * @param x La coordinata x
     * @param y La coordinata y
     * @param colorMap La mappa che associa stringhe colore a set di coordinate
     * @return La stringa del colore ANSI da applicare, o stringa vuota se nessun colore è specificato
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

    @Override
    public BiConsumer<CallableOnGameController, String> showVisitLocationMenu() {

        while (true) {
            String input = askForInput("", "Do you want to visit the card location? [Y/n] ");
            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToVisitLocation(nickname, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

            } else if (input.equalsIgnoreCase("N")) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToVisitLocation(nickname, false);
                    } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private ShipBoardClient getMyShipBoard() {
        return clientModel.getShipboardOf(clientModel.getMyNickname());
    }

    private void showMyShipBoard() {
        this.showShipBoard(getMyShipBoard(), clientModel.getMyNickname());
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu() {

        while(true) {
            String input = askForInput("", "Do you want to accept the reward? [Y/n] ");
            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToAcceptTheReward(nickname, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

            } else if (input.equalsIgnoreCase("N")) {
                return (server, nickname) -> {
                    try {
                        server.playerWantsToAcceptTheReward(nickname, false);
                    } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
                    } catch (IOException e) {
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
                } catch (IOException e) {
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
                    } catch (IOException e) {
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
                } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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
                } catch (IOException e) {
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
                    } catch (IOException e) {
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
                    } catch (IOException e) {
                        e.printStackTrace();
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
        showMessage("How many players do you want to play with? ", ASK);
    }

    public void showTestFlightQuestion() {
        showMessage("Do you want to play the test flight? [y/n] ", ASK);
    }

    public void showPickReservedComponentQuestion() {
        showMessage("Please pick a reserved component (0 to go back): ", ASK);
    }

    public void handleInput(@NotNull String input) {
        String[] coordinates;
        int row;
        int column;

        if (input.equals("exit")) {
            clientController.leaveGame();
        } else if (input.trim().split("\\s+")[0].equals("show")) {
            clientController.showShipBoard(input.trim().split("\\s+")[1]);
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
                        int numPlayers = Integer.parseInt(Objects.requireNonNull(stringQueue.poll()));
                        boolean isTestFlight = Boolean.parseBoolean(stringQueue.poll());
                        PlayerColor playerColor = PlayerColor.getPlayerColor(Integer.parseInt(Objects.requireNonNull(stringQueue.poll())));
                        clientController.handleCreateGameMenu(numPlayers, isTestFlight, playerColor);
                    } catch(NumberFormatException | NullPointerException e) {
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

                        case 3:
                            clientState = BUILDING_SHIPBOARD_PICK_RESERVED_COMPONENT;
                            showMyShipBoard();
                            showPickReservedComponentQuestion();
                            break;

                        case 4:
                            clientController.restartHourglass();
                            break;

                        case 5:
                            clientState = WATCH_LITTLE_DECK;
                            showMessage("""
                                    Which little deck would you like to watch?
                                    >\s""", ASK);
                            break;

                        default:
                            showMessage("Invalid choice. Please select 1-5.", STANDARD);
                    }
                    break;

                case BUILDING_SHIPBOARD_PICK_VISIBLE_COMPONENT:
                    clientController.pickVisibleComponent(Integer.parseInt(input));
                    break;

                case BUILDING_SHIPBOARD_WITH_FOCUSED_COMPONENT:
                    Component focusedComponent = clientModel.getPlayerClientData().get(clientController.getNickname()).getShipBoard().getFocusedComponent();
                    String[] focusedComponentString;
                    StringBuilder focusedComponentStringBuilder = new StringBuilder();
                    switch (Integer.parseInt(input)) {
                        case 1:
                            if (focusedComponent == null) {
                                showMessage("Still picking the component. Please wait...\n", STANDARD);
                                break;
                            }
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
                            if (focusedComponent == null) {
                                showMessage("Still picking a component. Please wait...", STANDARD);
                                break;
                            }
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

                        case 4:
                            clientController.reserveFocusedComponent();
                            break;

                        case 5:
                            clientController.releaseFocusedComponent();
                            break;

                        default:
                            showMessage("Invalid choice. Please select 1-5.\n", STANDARD);
                    }
                    break;

                case BUILDING_SHIPBOARD_PICK_RESERVED_COMPONENT:
                    clientController.pickReservedComponent(Integer.parseInt(input));
                    break;

                case WATCH_LITTLE_DECK:
                    showLittleDeck(Integer.parseInt(input));
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

                    clientController.placeFocusedComponent(row, column);
                    break;

                case CHECK_SHIPBOARD_INVALID:
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
                            clientController.removeComponent(row, column);
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

                case CHECK_SHIPBOARD_CORRECT:
                    break;

                default:
                    showMessage("", ERROR);
                    break;

            }
        } catch (NumberFormatException e) {
            showMessage("\nPlease enter a valid number.\n", ERROR);
        }

    }

    public void showExitMenu(){
        scanner.next("exit");
    }

}