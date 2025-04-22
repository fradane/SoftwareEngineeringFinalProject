package it.polimi.ingsw.is25am33.client.view;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

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
    private static final String INPUT_INTERRUPT = "";
    private final ClientModel clientModel;

    // Definizione di un colore rosso ANSI per gli errori (funziona nei terminali che supportano i colori ANSI).
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";



    public ClientCLIView(ClientModel clientModel) {
        this.scanner = new Scanner(System.in);
        this.clientModel = clientModel;
    }

    @Override
    public ClientModel getClientModel() {
        return clientModel;
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
        inputThread.setDaemon(true); // Termina quando il thread principale termina
        inputThread.start();

        System.out.println("=== Galaxy Trucker Client ===");
    }

    /**
     * Richiede input all'utente in modo non bloccante
     * @param prompt Il messaggio da mostrare
     * @return L'input dell'utente o stringa vuota in caso di interruzione
     */
    public String askForInput(String prompt) {
        System.out.print(prompt);
        waitingForInput = true;

        try {
            String input = null;
            // Controlla periodicamente se è arrivato input
            while (input == null && waitingForInput) {
                input = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                // Qui puoi inserire codice per gestire notifiche dal server
            }

            return input != null ? input : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            waitingForInput = false;
        }
    }

    /**
     * Metodo per interrompere l'attesa dell'input in caso di eventi importanti
     * Questo metodo va chiamato quando arriva una notifica importante dal server
     */
    public void cancelInputWaiting() {
        waitingForInput = false;
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void showError(String errorMessage) {
        System.out.println(ANSI_RED + "Error: " + errorMessage + ANSI_RESET);
    }

    @Override
    public String askNickname() {
        return askForInput("Enter your nickname: ");
    }

    @Override
    public String askServerAddress() {
        System.out.print("Enter server address: ");
        String address = askForInput("Enter server address (default: localhost): ");
        return address.isEmpty() ? "localhost" : address;
    }

    @Override
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
        }
    }

    @Override
    public int[] askCreateGame() {
        int[] result = new int[3]; // [numPlayers, isTestFlight, colorChoice]

        // Chiedi numero di giocatori
        while (true) {
            String input = askForInput("Number of players (2-4): ");
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
        String isTest = askForInput("Test flight [y/N]: ");
        result[1] = (isTest.equalsIgnoreCase("n") || isTest.isEmpty()) ? 0 : 1;

        // Scegli il colore
        result[2] = Integer.parseInt(askPlayerColor());

        return result;
    }

    @Override
    public String[] askJoinGame(List<GameInfo> games) {
        showAvailableGames(games);

        String[] result = new String[2]; // [gameId, colorChoice]

        result[0] = askForInput("Enter game ID to join: ");

        List<String> gameIds = games.stream().map(GameInfo::getGameId).toList();
        while(!gameIds.contains(result[0])){
            showError("Invalid game ID");
            result[0] = askForInput("Enter game ID to join: ");
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

    public String askPlayerColor() {
        System.out.println("Choose your color: ");
        System.out.println("1. RED");
        System.out.println("2. BLUE");
        System.out.println("3. GREEN");
        System.out.println("4. YELLOW");

        while (true) {
            String input = askForInput("Your choice: ");
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

    public String askPlayerColor(List<PlayerColor> availableColors) {

        System.out.println("Choose your color: ");

        for (PlayerColor color : availableColors) {
            System.out.println(color.getNumber() + ". " + color.toString());
        }

        while (true) {
            String input = askForInput("Your choice: ");
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
    public int showMainMenu() {
        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println("1. List available games");
            System.out.println("2. Create a new game");
            System.out.println("3. Join a game");
            System.out.println("4. Exit");

            String input = askForInput("Your choice: ");
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 4) {
                    return choice;
                } else {
                    System.out.println("Invalid choice. Please select 1-4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
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
        System.out.println("Game started! Initial state: " + gameState);
        System.out.println("The game is now in progress...");
    }

    @Override
    public void notifyGameEnded(String reason) {
        System.out.println("Game ended. Reason: " + reason);
    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {
        if (isFirstTime) System.out.println("The card has been drawn from the deck.");
        System.out.println("Current adventure card: " + clientModel.getCurrAdventureCard().toString());
    }

    /**
     * Displays an updated game state message on the command line.
     *
     * @param gameState the new game state description
     */
    @Override
    public void showNewGameState(String gameState) {
        System.out.println("===================================");
        System.out.println("📢  [Game Update]");
        System.out.println("🎮  New Game State: " + gameState);
        System.out.println("===================================");
    }

    @Override
    public void showNewCardState(String cardState) {
        System.out.println("===================================");
        System.out.println("🃏  [Card Update]");
        System.out.println("🆕  New Card State: " + cardState);
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
                System.out.println("\nChoose an option:");
                System.out.println("1. Pick a random covered component from the table");
                System.out.println("2. Pick a visible component from the table");
                System.out.println("3. End your shipBoard setup phase");
                System.out.println("4. Show one of the ship boards");
                System.out.println("5. Restart hourglass");
                String input = askForInput("Your choice: ");

                // TODO
                //if (input.equals(INPUT_INTERRUPT)) return ;

                int choice = Integer.parseInt(input);

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
                                    hasFocusComponent = consumer.apply(server, nickname);
                                }
                            } catch (Exception e) {
                                System.out.println("Error connecting to server: " + e.getMessage());
                                System.out.println("Please try again.");
                            }
                            return false;
                        };

                    case 2:
                        return (server, nickname) ->{
                            Map<Integer, Component> visibleComponents = null;
                            try {
                                visibleComponents = server.showPlayerVisibleComponent(nickname);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }

                            if (visibleComponents == null || visibleComponents.isEmpty()) {
                                ClientCLIView.this.showMessage("No visible components yet!");
                                return false;
                            }

                            BiFunction<CallableOnGameController, String, Component> function = ClientCLIView.this.showVisibleComponentAndMenu(visibleComponents);
                            if (function == null) return false; // means that the player has changed its idea
                            Component pickedComponent = function.apply(server, nickname);
                            if (pickedComponent == null) {
                                ClientCLIView.this.showError("This component is no longer available, someone has stolen it from you!");
                                return false;
                            }

                            boolean hasFocusComponent = true;
                            while (hasFocusComponent) {
                                BiFunction<CallableOnGameController, String, Boolean> consumer = ClientCLIView.this
                                        .showPickedComponentAndMenu(pickedComponent);
                                hasFocusComponent = consumer.apply(server, nickname);
                            }
                            return false;
                        };

                    case 3:
                        String answer = askForInput("Are you sure you want to end your shipBoard setup phase? [Y/n] ");
                        if (answer.equalsIgnoreCase("Y") || answer.isEmpty()) {
                            System.out.println("Ending your shipBoard setup phase...");
                            return (server, nickname) -> {
                                try {
                                    server.playerChoseToEndBuildShipBoardPhase(nickname);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                return true;
                            };
                        }
                        break;

                    case 4:
                        return showShipBoardsMenu();

                    case 5:
                        //TODO restart hourglass
                        System.out.println("Restart hourglass but not implemented yet!");
                        break;

                    default:
                        System.out.println("Invalid choice. Please select 1-5.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }

        }
    }

    @Override
    public void notifyNoMoreComponentAvailable() {
        this.showMessage("No more component available.");
        this.showMessage("Tip: if you want more components to build your shipboard look among the visible ones.");
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

        System.out.println("\nPicked component details:");
        System.out.println(component.toString());

        while (true) {

            try {
                System.out.println("\nChoose an action:");
                System.out.println("1. Rotate the component");
                System.out.println("2. Place component on ship board");
                System.out.println("3. Reserve component");
                System.out.println("4. Release component");
                System.out.println("5. Show your ship board");
                int choice = Integer.parseInt(askForInput("Your choice: "));

                switch (choice) {
                    case 1:
                        component.rotate();
                        System.out.println("\nUpdated component details:");
                        System.out.println(component);
                        break;

                    case 2:
                        Coordinates coords = readCoordinatesFromUserInput();
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToPlaceFocusedComponent(nickname, coords);
                                coords.setCoordinates(List.of(coords.getX() + 1, coords.getY() + 1));
                                ClientCLIView.this.showMessage("You placed the component at: " + coords.toString() + ".");
                                return false;
                            } catch (IOException e) {
                                ClientCLIView.this.showError(e.getMessage());
                                ClientCLIView.this.showError("Try again.");
                                return true;
                            }
                        };

                    case 3:
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToReserveFocusedComponent(nickname);
                                ClientCLIView.this.showMessage("Component reserved.");
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
                                ClientCLIView.this.showMessage("Component released.");
                                return false;
                            } catch (IOException e) {
                                ClientCLIView.this.showError(e.getMessage());
                                ClientCLIView.this.showError("Try again.");
                                return true;
                            }
                        };

                    case 5:
                        // TODO Show current state of ship board (not implemented here)
                        System.out.println("Showing your current ship board...");
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
    public Coordinates readCoordinatesFromUserInput() {

        while (true) {
            String input = this.askForInput("Select coordinates (row column): ");
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
     * Displays a menu listing all players' nicknames and prompts the user to choose
     * a ship board to view. Returns a BiFunction that triggers the request on the server
     * to view the selected player's ship board.
     *
     * @return a BiFunction to be executed with the RMIServerNetworkManager and player nickname
     */
    @Override
    public BiFunction<CallableOnGameController, String, Boolean> showShipBoardsMenu() {
        while (true) {
            try {
                System.out.println("\nChoose one of the ship boards:");
                List<String> playersNickname = clientModel.getPlayersNickname().stream().toList();
                for (String player : playersNickname) {
                    showMessage(playersNickname.indexOf(player) + 1 + ". " + player);
                }
                int choice = Integer.parseInt(this.askForInput("Your choice: "));
                if (choice <= 0 || choice > playersNickname.size()) {
                    showMessage("Invalid choice. Please enter a valid number.");
                } else {
                    return (server, nickname) -> {
                        try {
                            Component[][] selectedPlayerShipBoard = server.getShipBoardOf(playersNickname.get(choice - 1), nickname);
                            this.showShipBoard(selectedPlayerShipBoard, playersNickname.get(choice - 1));
                            return false;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
            } catch (NumberFormatException e) {
                showMessage("Invalid choice. Please enter a valid number.");
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
        System.out.println("Here's the ship board of " + shipBoardOwnerNickname + ":");

        // Stampa numeri delle colonne
        System.out.print("       ");
        for (int col = 4; col <= 10; col++) {
            System.out.printf("   %2d     ", col);
        }
        System.out.println();

        // TODO generalizzare il caso per il livello 1
        for (int i = 4; i <= 8; i++) {
            // Ogni cella viene stampata su 4 righe
            for (int line = 0; line < 4; line++) {
                if (line == 2) {
                    System.out.printf(" %2d   ", i + 1);
                } else {
                    System.out.print("      ");
                }
                for (int j = 3; j <= 9; j++) {
                    Component cell = shipBoard[i][j];
                    switch (line) {
                        case 0:
                            System.out.print("+---------");
                            break;

                        case 1:
                            System.out.printf("|    %1s    ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.NORTH).fromConnectorTypeToValue()));
                            break;

                        case 2: System.out.printf("| %1s %3s %1s ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.WEST).fromConnectorTypeToValue()),
                                Level2ShipBoard.isOutsideShipboard(i, j) ? (ANSI_RED + "out" + ANSI_RESET) : (cell == null ? "" : "val"),
                                Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.EAST).fromConnectorTypeToValue()));
                            break;

                        case 3: System.out.printf("|    %1s    ", Level2ShipBoard.isOutsideShipboard(i, j) ? "X" : (cell == null ? "" : cell.getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue()));
                            break;
                    }
                }

                if (line == 0) {
                    System.out.println("+");
                } else {
                    System.out.println("|");
                }
            }
        }

        System.out.print("      ");
        for (int i = 0; i <= 6; i++) {
            System.out.print("+---------");
        }
        System.out.println("+");
    }

    @Override
    public BiFunction<CallableOnGameController, String, Component> showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {
        showMessage("\nHere's the visible components:");

        visibleComponents.keySet().forEach(index -> {
            System.out.printf("%2d ", index);
            System.out.println(visibleComponents.get(index));
        });

        while (true) {
            try {
                int choice = Integer.parseInt(askForInput("Choose one of the visible components (0 to go back): "));

                if (choice == 0) return null;

                if (!visibleComponents.containsKey(choice)) {
                    showMessage("Invalid choice. Please enter a valid number.");
                    continue;
                }

                return (server, nickname) -> {
                    try {
                        return server.playerPicksVisibleComponent(nickname, choice);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                };

            } catch (NumberFormatException e) {
                showMessage("Invalid choice. Please enter a valid number.");
            }
        }

    }

}