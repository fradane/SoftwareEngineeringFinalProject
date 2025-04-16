package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.ComponentState;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.rmi.server.RMIServerNetworkManager;
import it.polimi.ingsw.is25am33.serializationLayer.ClientDeserializer;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Implementazione dell'interfaccia ClientView per una Command Line Interface
 */
public class ClientCLIView implements ClientView {

    private final Scanner scanner;
    private ComponentTable latestComponentTable;
    private List<String> playersNickname;
    private AdventureCard currAdventureCard;
    private volatile boolean waitingForGameStart = false;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean waitingForInput = false;
    private static final String INPUT_INTERRUPT = "";


    // Definizione di un colore rosso ANSI per gli errori (funziona nei terminali che supportano i colori ANSI).
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";



    public ClientCLIView() {
        this.scanner = new Scanner(System.in);
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
    private String askForInput(String prompt) {
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
        String isTest = askForInput("Test flight (y/n): ");
        result[1] = isTest.equalsIgnoreCase("y") ? 1 : 0;

        // Scegli il colore
        result[2] = Integer.parseInt(askPlayerColor());

        return result;
    }

    @Override
    public String[] askJoinGame(Iterable<GameInfo> games) {
        showAvailableGames(games);

        String[] result = new String[2]; // [gameId, colorChoice]
        result[0] = askForInput("Enter game ID to join: ");
        result[1] = askPlayerColor();

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
        System.out.println(nickname + " joined the game with color "+ gameInfo.getPlayersAndColors().get(nickname) + ". Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {
        System.out.println(nickname + " left the game. Players: " +
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

    /**
     * Returns the most recently updated component table of the game.
     *
     * @return the latest ComponentTable instance
     */
    @Override
    public ComponentTable getLatestComponentTable() {
        return latestComponentTable;
    }

    /**
     * Sets the most recent version of the component table.
     *
     * @param latestComponentTable the ComponentTable to be stored
     */
    @Override
    public void setLatestComponentTable(ComponentTable latestComponentTable) {
        this.latestComponentTable = latestComponentTable;
    }

    @Override
    public void setCurrAdventureCard(AdventureCard card) {
        this.currAdventureCard = card;
        showCurrAdventureCard(true);
    }

    @Override
    public void setPlayersNickname(List<String> playersNickname) {
        this.playersNickname = playersNickname;
    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {
        if (isFirstTime) System.out.println("The card has been drawn from the deck.");
        System.out.println("Current adventure card: " + currAdventureCard.toString());
    }

    /**
     * Displays the current component table in a formatted grid.
     * Each cell shows:
     * <ul>
     *     <li>'O' for visible components</li>
     *     <li>'X' for hidden components</li>
     *     <li>' ' (space) for empty cells</li>
     * </ul>
     * Also prints a list of all visible components with their details.
     */
    @Override
    public void showComponentTable() {

        final int rows = 19;
        final int cols = 8;

        // Riga superiore con intestazioni di riga
        System.out.print("     "); // spazio per i numeri di colonna
        for (int r = 0; r < rows; r++) {
            System.out.printf("  %2d ", r + 1);
        }
        System.out.println();

        // Riga dei separatori superiori
        System.out.print("     ");
        for (int r = 0; r < rows; r++) {
            System.out.print("+----");
        }
        System.out.println("+");

        // Riga per colonna
        for (int c = 0; c < cols; c++) {
            System.out.printf(" %2d  ", c + 1);
            for (int r = 0; r < rows; r++) {
                Component component = latestComponentTable.getComponent(new Coordinates(c, r));
                char ch = (component == null) ? ' ' : (component.getCurrState() == ComponentState.HIDDEN ? 'X' : 'O');
                System.out.printf("| %c  ", ch);
            }
            System.out.println("|");

            // Riga separatrice
            System.out.print("     ");
            for (int r = 0; r < rows; r++) {
                System.out.print("+----");
            }
            System.out.println("+");
        }

        latestComponentTable.getComponentsAsStream()
                .filter(component -> component != null && component.getCurrState() == ComponentState.VISIBLE)
                .forEach(System.out::println);

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
    public BiFunction<RMIServerNetworkManager, String, Boolean> showBuildShipBoardMenu() {

        while (true) {

            try {
                System.out.println("\nChoose an option:");
                System.out.println("1. Select coordinates from the table");
                System.out.println("2. Show the latest component table");
                System.out.println("3. End your shipBoard setup phase");
                System.out.println("4. Show one of the ship boards");
                String input = askForInput("Your choice: ");

                // TODO
                //if (input.equals(INPUT_INTERRUPT)) return ;

                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1:
                        Coordinates chosenCoordinates = readCoordinatesFromUserInput();

                        if(chosenCoordinates.isValidForComponentTable(latestComponentTable)) {

                            return (server, nickname) -> {
                                try {
                                    // TODO serializzare coordinate
                                    String chosenComponent = server.playerChoseComponentFromTable(nickname, chosenCoordinates.toString());
                                    BiConsumer<RMIServerNetworkManager, String> consumer = ClientCLIView.this
                                            .showChosenComponentAndMenu(ClientDeserializer.deserialize(chosenComponent, Component.class));
                                    consumer.accept(server, nickname);
                                } catch (Exception e) {
                                    System.out.println("Error connecting to server: " + e.getMessage());
                                    System.out.println("Please try again.");
                                }
                                return false;
                            };

                        }

                        System.out.println("\nInvalid coordinates: incorrect value or empty location.");
                        System.out.println("Here is the most recent update of the table:\n");
                        showComponentTable();
                        break;

                    case 2:
                        showComponentTable();
                        break;

                    case 3:
                        System.out.print("Are you sure you want to end your shipBoard setup phase? [Y/n] ");
                        String answer = scanner.nextLine();
                        if (answer.equalsIgnoreCase("Y") || answer.isEmpty()) {
                            System.out.println("Ending your shipBoard setup phase...");
                            return (server, nickname) -> {
                                try {
                                    server.playerChoseToEndBuildShipBoardPhase(nickname);
                                    return true;
                                } catch (RemoteException e) {
                                    throw new RuntimeException(e);
                                }
                            };
                        }
                        break;

                    case 4:
                        return showShipBoardsMenu();

                    default:
                        System.out.println("Invalid choice. Please select 1-3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }

        }
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
    public BiConsumer<RMIServerNetworkManager, String> showChosenComponentAndMenu(Component component) {

        System.out.println("\nSelected component details:");
        System.out.println(component.toString());

        while (true) {

            try {
                System.out.println("\nChoose an action:");
                System.out.println("1. Rotate the component");
                System.out.println("2. Place component on ship board");
                System.out.println("3. Reserve component");
                System.out.println("4. Release component");
                System.out.println("5. Show your ship board");
                System.out.print("Your choice: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        component.rotate();
                        System.out.println("\nUpdated component details:");
                        System.out.println(component);
                        break;
                    case 2:
                        Coordinates coords = readCoordinatesFromUserInput();
                        System.out.println("You chose to place the component at: " + coords);
                        return (server, nickname) -> {
                            // TODO serializzare
                            try {
                                server.playerWantsToPlaceFocusedComponent(nickname, coords.toString());
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    case 3:
                        System.out.println("Component reserved.");
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToReserveFocusedComponent(nickname);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    case 4:
                        System.out.println("Component released.");
                        return (server, nickname) -> {
                            try {
                                server.playerWantsToReleaseFocusedComponent(nickname);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
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
     * Prompts the user to input a pair of coordinates in the format "row column".
     * Validates the input and converts it to zero-based Coordinates for the component table.
     * Keeps prompting the user until valid input is received.
     *
     * @return Coordinates representing the selected cell (zero-based)
     */
    public Coordinates readCoordinatesFromUserInput() {

        while (true) {
            System.out.print("Select coordinates: ");
            String input = scanner.nextLine();
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
    public BiFunction<RMIServerNetworkManager, String, Boolean> showShipBoardsMenu() {
        while (true) {
            try {
                System.out.println("\nChoose one of the ship boards:");
                for (String player : playersNickname) {
                    System.out.println(playersNickname.indexOf(player) + 1 + ". " + player);
                }
                System.out.print("Your choice: ");
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice <= 0 || choice > playersNickname.size()) {
                    System.out.println("Invalid choice. Please enter a valid number.");
                } else {
                    return (server, nickname) -> {
                        try {
                            return server.playerWantsToSeeShipBoardOf(playersNickname.get(choice), nickname);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

    }


    public static void main(String[] args) {
        ComponentTable t = new ComponentTable();
        ClientView view = new ClientCLIView();

        view.setLatestComponentTable(t);
        view.getLatestComponentTable().getComponent(new Coordinates(0, 0)).setCurrState(ComponentState.VISIBLE);
        view.showComponentTable();

        view.showNewCardState("ENGINE");

        view.showNewGameState("set up");

        view.setPlayersNickname(List.of("ali", "fra"));

        view.showShipBoardsMenu();

        view.showBuildShipBoardMenu();

    }

}