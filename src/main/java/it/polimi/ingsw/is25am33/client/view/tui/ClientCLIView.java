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
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
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
    private boolean isTestFlight;

    // Class-level variables to track selection state
    Map<String, Set<Coordinates>> coloredCoordinates = new HashMap<>();
    private final List<Coordinates> selectedEngines = new ArrayList<>();
    private final List<Coordinates> selectedCabins = new ArrayList<>();
    private final List<Coordinates> selectedCannons = new ArrayList<>();
    private final List<Coordinates> selectedBatteries = new ArrayList<>();
    private final List<Coordinates> selectedShields = new ArrayList<>();
    private Coordinates currentSelection = null;
    private Coordinates hitComponent = null;
    private boolean waitingForBatterySelection = false;
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
    public void setIsTestFlight(boolean isTestFlight) {
        this.isTestFlight = isTestFlight;
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
        colorMenu.append("> ");
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
        if (isFirstTime) showMessage("The card has been drawn from the deck.", STANDARD);

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card == null) {
            showMessage("No current card available.", STANDARD);
            return;
        }

        StringBuilder output = new StringBuilder();
        output.append("Current adventure card:\n");
        output.append("═══════════════════════════════════════\n");
        output.append("Name: ").append(card.getCardName()).append("\n");
        output.append("═══════════════════════════════════════\n");

        showMessage(output.toString(), STANDARD);

        output = new StringBuilder();

        // Display card-specific information
        displayCardSpecificInfo(card, output);

        showMessage(output.toString(), STANDARD);

//        if (isFirstTime) System.out.println("The card has been drawn from the deck.");
//
//        StringBuilder output = new StringBuilder();
//        output.append("Current adventure card:\n");
//        String[] cardLines = clientModel.getCurrAdventureCard().split("\\n");
//        for (int i = 1; i < cardLines.length; i++) {
//            output.append(cardLines[i]).append("\n");
//        }
//        showMessage(output.toString(), STANDARD);
    }

    /**
     * Helper method to display card-specific information
     */
    private void displayCardSpecificInfo(ClientCard card, StringBuilder output) {
        switch (card.getCardType()) {
            case "Planets":
                displayPlanetsInfo((ClientPlanets) card, output);
                break;
            //TODO uncommentare quando si inizia ad implementare questa carta
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

    public void showComponentHitInfo(Coordinates coordinates) {
            showMessage("The component at coordinates " + coordinates.getX() + "-"+ coordinates.getY() + " has been hit", STANDARD);
            hitComponent = coordinates;
    }

    public void showCrewMembersInfo(){
        showMessage("You have " + clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size() + " crew members", STANDARD);
        for(String player: clientModel.getSortedRanking()) {
            if(!player.equals(clientModel.getMyNickname()))
                showMessage(player + "has" + clientModel.getShipboardOf(player).getCrewMembers().size() + " crew members", STANDARD);
        }
        if(clientModel.isMyTurn()){
            showMessage("Press enter to start this phase", ASK);
        }
        else
            setClientState(WAIT_PLAYER);
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
                // TODO clientCLI
        showCardStateMenu(mappedState);
    }

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

    @Override
    public void showFirstToEnter() {
        clientState = PLACE_PLACEHOLDER;
        showMessage("""
                Your placeholder has not been placed yet!!!
                Press any key to place it faster than the others...""", STANDARD);
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
        showMessage("No action needed, your ship is all set.", STANDARD);
    }

    @Override
    public void checkShipBoardAfterAttackMenu() {
        if (hitComponent != null) {
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
            hitComponent = null;
        } else {
            showMessage("GOOD JOB! You are safe!", STANDARD);
            setClientState(WAIT_PLAYER);
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
        }
    }

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

    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname) {
        showShipBoard(shipBoardClient, shipBoardOwnerNickname, Collections.emptyMap());
    }

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
            output.append("                        🛬 END OF THE JOURNEY 🛬\n");
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

    @Override
    public void showChooseEnginesMenu() {

        // Reset the selection state
        selectedEngines.clear();
        selectedBatteries.clear();

        showEngineWithColor();

        if(clientModel.getShipboardOf(clientController.getNickname()).getDoubleEngines().isEmpty() ) {
            setClientState(WAIT_PLAYER);
            showMessage("No double engines available.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedEngines, selectedBatteries);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            setClientState(WAIT_PLAYER);
            showMessage("No battery boxes available so you can't activate double engine.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedEngines, selectedBatteries);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            setClientState(WAIT_PLAYER);
            showMessage("You're out of batteries, so you can't activate double engine.", STANDARD);
            showMessage("You can use only single engine", STANDARD);
            if (clientModel.getCurrAdventureCard().getCardName().equals("FreeSpace"))
                showMessage("ATTENTION! If your ship doesn't have engine power, you will be eliminated!", NOTIFICATION_INFO);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedEngines, selectedBatteries);
            return;
        }

        showMessage("\nYou can activate double engines." +
                "Each double engine requires one battery.", STANDARD);
        setClientState(ClientState.CHOOSE_ENGINES_MENU);
        showMessage("Enter coordinates of a double engine (row column) or 'done' when finished: ", ASK);
    }

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
            setClientState(WAIT_PLAYER);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate double cannon.", STANDARD);
            showMessage("You can use only single cannon", STANDARD);
            setClientState(WAIT_PLAYER);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate double cannon.", STANDARD);
            showMessage("You can use only single cannon", STANDARD);
            setClientState(WAIT_PLAYER);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        showMessage("\nYou can activate double cannon." +
                "Each double cannon requires one battery.", STANDARD);
        setClientState(ClientState.CHOOSE_CANNONS_MENU);
        showMessage("Enter coordinates of a double cannon (row column) or 'done' when finished: ", ASK);
    }

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
            setClientState(WAIT_PLAYER);
            clientController.playerHandleSmallDanObj(clientModel.getMyNickname(),selectedShields,selectedBatteries);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate shield.", STANDARD);
            showMessage("ATTENTION! You can't defend!", NOTIFICATION_INFO);
            setClientState(WAIT_PLAYER);
            clientController.playerHandleSmallDanObj(clientModel.getMyNickname(),selectedShields,selectedBatteries);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare nessuno scudo
        if (!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate shield.", STANDARD);
            showMessage("ATTENTION! You can't defend!", NOTIFICATION_INFO);
            setClientState(WAIT_PLAYER);
            clientController.playerHandleSmallDanObj(clientModel.getMyNickname(),selectedShields,selectedBatteries);
            return;
        }


        showMessage("You can activate a shield or let the object hit your ship.", STANDARD);
        showMessage("Enter coordinates of a shield (row column) or 'done' to skip: ", ASK);

    }

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
            setClientState(WAIT_PLAYER);
            clientController.playerHandleBigMeteorite(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()){
            showMessage("No battery boxes available so you can't activate Double Cannons.", STANDARD);
            showMessage("ATTENTION! You can defend only with single cannon!", NOTIFICATION_INFO);
            setClientState(WAIT_PLAYER);
            clientController.playerHandleBigMeteorite(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showMessage("You're out of batteries, so you can't activate double Cannon.", STANDARD);
            showMessage("You can use only single Cannon", STANDARD);
            clientController.playerHandleBigMeteorite(clientModel.getMyNickname(),selectedCannons,selectedBatteries);
            return;
        }

        showMessage("You can use a double or single cannon to destroy it or let it hit your ship.", STANDARD);
        showMessage("Enter coordinates of a double cannon (row column) or 'done' to skip: ", ASK);
    }

    @Override
    public void showBigShotMenu() {
        StringBuilder BigShotInfo = new StringBuilder();
        BigShotInfo.append("\n").append(clientModel.getCurrDangerousObj().getType()).append(" incoming!");
        BigShotInfo.append("\n").append(clientModel.getCurrDangerousObj().toString());
        showMessage(BigShotInfo.toString(),STANDARD);
        showMessage("\nBig Shot incoming! Nothing can stop this...", STANDARD);
        showMessage("Press Enter to see the effect on your ship", ASK);
    }

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
            //TODO trovare un modo per mostrare al server questo errore, anche se non dovrebbe mai accadere perchè controlli già fatti
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

    @Override
    public void showHandleCubesRewardMenu() {
        setClientState(ClientState.HANDLE_CUBES_REWARD_MENU);

        // Get cube rewards directly from the current card
        List<CargoCube> rewardCubes = null;
        try {
            rewardCubes = clientModel.extractCubeRewardsFromCurrentCard();
        } catch (IllegalStateException e) {
            showMessage("ERROR: " + e.getMessage(), ERROR);
            // TODO che si fa?
        }

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

        // Pre-process any impossible cubes
        processImpossibleCubes();

        // If all cubes were impossible, we're already done
        if (storageManager.isSelectionComplete()) {
            return;
        }

//        // Check if the player can accept all cubes
//        if (!storageManager.canAcceptAllCubes()) {
//            showMessage("\n" + storageManager.getStorageCompatibilityInfo(), NOTIFICATION_INFO);
//
//            // If the player can't accept even the first cube, send an empty list immediately
//            if (!storageManager.canAcceptCurrentCube()) {
//                showMessage("Non puoi accettare nessuno dei cubi reward. Il gioco proseguirà con il prossimo giocatore.", STANDARD);
//                List<Coordinates> emptyList = new ArrayList<>();
//                clientController.playerChoseStorage(clientController.getNickname(), emptyList);
//                return;
//            }
//
//            showMessage("Potrai comunque scegliere gli storage per i cubi che puoi accettare.", STANDARD);
//        }

        // Show the ship board for the player to see available storage
        this.showMyShipBoard();


        // Display information about available storages
        StringBuilder storageInfo = new StringBuilder("\nYour shipboard has the following storages:\n");
        Map<Coordinates, Storage> coordinatesAndStorages = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesAndStorages();

        if (coordinatesAndStorages.isEmpty()) {
            storageInfo.append("No storage available on your ship.\n");
        } else {
            for (Map.Entry<Coordinates, Storage> entry : coordinatesAndStorages.entrySet()) {
                Coordinates coords = entry.getKey();
                Storage storage = entry.getValue();
                List<CargoCube> storedCubes = storage.getStockedCubes();

                // Build the string for coordinates and storage type
                String storageType = storage instanceof SpecialStorage ? "SpecialStorage" : "StandardStorage";

                // Add colored formatting for coordinates for better visibility
                storageInfo.append(String.format("%s(%d, %d)%s: %s - ",
                        ANSI_GREEN, coords.getX() + 1, coords.getY() + 1, ANSI_RESET, storageType));

                // Determine storage capacity from the component's label
                // Format is usually like "2/3" showing space available
                String capacityInfo = storage.getMainAttribute();

                // Build the string for current/max capacity
                storageInfo.append(String.format("contained cubes %s: ", capacityInfo));

                // Build the string for stored cubes
                if (storedCubes.isEmpty()) {
                    storageInfo.append("emty");
                } else {
                    // Format each cube with its color
                    List<String> formattedCubes = new ArrayList<>();
                    for (CargoCube cube : storedCubes) {
                        String cubeColor = switch (cube) {
                            case RED -> ANSI_RED;
                            case YELLOW -> ANSI_YELLOW;
                            case GREEN -> ANSI_GREEN;
                            case BLUE -> ANSI_BLUE;
                        };
                        formattedCubes.add(cubeColor + cube.name() + ANSI_RESET);
                    }
                    storageInfo.append(String.join(", ", formattedCubes));
                }

                storageInfo.append("\n");
            }
        }
        showMessage(storageInfo.toString(), STANDARD);

        StringBuilder message = new StringBuilder("\nYou’ve received the following cubes as a reward:\n");
        for (int i = 0; i < rewardCubes.size(); i++) {
            CargoCube cube = rewardCubes.get(i);
            message.append("- ").append(cube).append(" (value: ").append(cube.getValue()).append(")")
                   .append(cube == CargoCube.RED ? " - Requires special storage!" : "")
                   .append("\n");
        }

        message.append("\nYou must select one storage for each cube you can accept. ")
               .append("Remember:\n")
               .append("- RED cubes can only be stored in special storage units.\n")
               .append("- If a storage is full, the least valuable cube will be replaced.\n")
               .append("- You can type 'next' to skip the current cube and go to the next one.\n")
               .append("- Type 'done' when you’ve finished selecting all storages.\n\n");

        CargoCube currentCube = storageManager.getCurrentCube();
        if (currentCube != null) {
            showStorageWithColor();
            message.append("Next cube to place: ").append(currentCube)
                    .append(" (value: ").append(currentCube.getValue()).append(")")
                    .append(currentCube == CargoCube.RED ? " - This cube requires special storage!" : "")
                    .append("\n");
            message.append("Enter the coordinates of a storage (row column) for this cube, ")
                    .append("'next' to skip this cube, 'skip' to give up all cubes: ");
        }

        showMessage(message.toString(), ASK);
    }

    /**
     * Pre-processes any cubes that can't possibly be accepted due to lack of compatible storage
     */
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

    /**
     * Conta le occorrenze di una sottostringa in una stringa.
     *
     * @param text La stringa in cui cercare
     * @param substring La sottostringa da contare
     * @return Il numero di occorrenze della sottostringa
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    @Override
    public void showEpidemicMenu() {
        showMessage("\nAn epidemic is spreading throughout the fleet!", STANDARD);
        showMessage("Each occupied cabin connected to another occupied cabin will lose one crew member.", STANDARD);
        showMessage("Press any key see how the epidemic is going to spread...", STANDARD);
    }

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

        if(clientModel.isMyTurn())
            inputQueue.add("\n");
//            showMessage("Press Enter to continue...", ASK);
    }

    @Override
    public void showHandleCubesMalusMenu() {

        if(!clientModel.isMyTurn()){
            setClientState(WAIT_PLAYER);
            return;
        }

        if(clientModel.getCurrAdventureCard().getCardName().equals("WarField")) {
            ClientWarField card = (ClientWarField) clientModel.getCurrAdventureCard();
            cubeMalus=card.getCubeMalus();

            storageManager = new StorageSelectionManager(new ArrayList<>(), cubeMalus, clientModel.getShipboardOf(clientModel.getMyNickname()));
            // Mostra la nave per visualizzare i depositi
            this.showStorageWithColor();
            showMessage("\nYou must remove " +cubeMalus +" cargo cubes!", STANDARD);
        } else if(clientModel.getCurrAdventureCard().getCardName().equals("Smugglers")) {
            ClientSmugglers card = (ClientSmugglers) clientModel.getCurrAdventureCard();
            cubeMalus=card.getCubeMalus();
            storageManager = new StorageSelectionManager(new ArrayList<>(), cubeMalus, clientModel.getShipboardOf(clientModel.getMyNickname()));
            // Mostra la nave per visualizzare i depositi
            this.showStorageWithColor();
            showMessage("\nYou must remove " +cubeMalus +" cargo cubes!", STANDARD);
        }

        mostPreciousCube= storageManager.mostPreciousCube();

        if(clientModel.getShipboardOf(clientModel.getMyNickname()).getStorages().isEmpty()|| clientModel.getShipboardOf(clientModel.getMyNickname()).getCargoCubes().isEmpty()) {
            if(!clientModel.getShipboardOf(clientModel.getMyNickname()).getBatteryBoxes().isEmpty()) {
                showMessage("You don't have any storage",STANDARD);
                showMessage("You have to give back the batteries instead of the cubes ", STANDARD);
                setClientState(CHOOSE_BATTERY_CUBES);
                showBatteryBoxesWithColor();
                showMessage("Enter coordinates of a battery to remove: ", ASK);
                return;
            } else {
                showMessage("You don't have any storage and battery boxes",STANDARD);
                showMessage("You are safe...for now", STANDARD);
                setClientState(WAIT_PLAYER);
                clientController.playerChoseStorage(clientController.getNickname(), selectedStorage);
                mostPreciousCube.clear();
                return;
            }
        }
        setClientState(ClientState.CHOOSE_STORAGE_FOR_CUBEMALUS);
        showMessage("You have to give back the most precious cubes", STANDARD);
        showMessage("Enter coordinates of a storage to remove a cargo cube from: ", ASK);

    }

    private ShipBoardClient getMyShipBoard() {
        return clientModel.getShipboardOf(clientModel.getMyNickname());
    }

    private void showMyShipBoard() {
        this.showShipBoard(getMyShipBoard(), clientModel.getMyNickname());
    }

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

        //TODO commentato perche avere 0 come punteggio puo verificarsi anche durante il gioco non solo prima della costruzione
//        if (topScore == 0) {
//            showMessage("Think about building your ship board, being the leader without a ship board is roughly impossible.\n> ", ASK);
//            return;
//        }

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
    public void updateTimeLeft(int timeLeft, int flipsLeft) {
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
            //TODO bisognerebbe diversificare nel caso in cui si stiano scegliendo batterie al posto di cubi ma non ho voglia
            // quindi ho fatto che se premi cancel non succede un cazzo
            if(batteryBox.getRemainingBatteries()==0){
                showMessage("This batteryBox is empty!", ERROR);
                showMessage("Please select another one or 'cancel' to cancel the last choice", ASK);
                return;
            }

            //se hai selezionato gia tutte le batterie presenti in un batteryBox non puoi piu selezionarlo
            int frequency=0;
            for(Coordinates selectedBatteryBoxCoords : selectedBatteries){
                if(selectedBatteryBoxCoords.equals(coords)){
                    frequency++;
                }
            }

            //TODO stessa cosa qua
            if(batteryBox.getRemainingBatteries()==frequency){
                showMessage("This battery box is empty", ERROR);
                showMessage("Please select another one or 'cancel' to cancel the last choice", ASK);
                return;
            }

            // Aggiungi la batteria e torna alla selezione del motore
            selectedBatteries.add(coords);


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

                if (selectedBatteries.size() + selectedStorage.size() == cubeMalus
                        || clientModel.getShipboardOf(clientModel.getMyNickname()).getTotalAvailableBattery()==selectedBatteries.size()) {
                    showMessage(" All cargo cubes and batteries has been selected", STANDARD);
                    setClientState(WAIT_PLAYER);
                    clientController.playerChoseStorageAndBattery(clientController.getNickname(), selectedStorage, selectedBatteries);
                    selectedBatteries.clear();
                    selectedStorage.clear();
                    mostPreciousCube.clear();
                }else{
                    showBatteryBoxesWithColor();
                    showMessage("You still need to remove " + (cubeMalus- selectedStorage.size()- selectedBatteries.size()) +" battery (s). Enter another battery box coordinate: ", STANDARD);
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
        //TODO OPTIONAL: aggiugnere possibilità di rifare da capo le scelte. Per esempio in caso un client sbagliasse a posizionare un cubo
        if (input.equalsIgnoreCase("done")) {
            // Se l'utente ha finito ma non ha selezionato tutti gli storage possibili
            if (!storageManager.isSelectionComplete() && storageManager.canAcceptCurrentCube()) {
                showMessage("You haven’t selected storage for all cubes you can accept. " +
                                "\nThe remaining cubes will be discarded. Continue with 'confirm' or select more storage.",
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

    private void handleCabinSelection(@NotNull String input) {
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        try {
            // Check if the user wants to select multiple cabins
            if (input.equalsIgnoreCase("done")) {
                // Process all selected cabins
                if (selectedCabins.isEmpty()) {
                    showMessage("You must select at least one cabin.", ERROR);
                } else {
                    boolean success = clientController.playerChoseCabins(clientController.getNickname(), selectedCabins);
                    if(success) {
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
        }
        else if (input.trim().split("\\s+")[0].equals("cube")) {
            if (input.trim().split("\\s+").length != 2) {
                showMessage("Invalid cubes command", ERROR);
                return;
            }
            clientController.showCubes(input.trim().split("\\s+")[1]);
            return;
        } else if (input.equals("rank")) {
            showCurrentRanking();
            return;
        } else if (input.equals("land")){
            clientController.land();
            return;
        } else if (input.equals("skipToLastCard")) {
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
                            }
                            clientState = BUILDING_SHIPBOARD_PICK_RESERVED_COMPONENT;
                            showMyShipBoard();
                            showPickReservedComponentQuestion();
                            break;

                        case 4:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-3.\n> ", ASK);
                                showBuildShipBoardMenu();
                            }
                            clientController.restartHourglass();
                            break;

                        case 5:
                            if (isTestFlight) {
                                showMessage("Invalid choice. Please select 1-3.\n> ", ASK);
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
                    switch (Integer.parseInt(input)) {
                        case 1:
                            if (focusedComponent == null) {
                                showMessage("Still picking the component. Please wait...\n> ", ASK);
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
                                showMessage("Still picking a component. Please wait...\n> ", ASK);
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

                case CHECK_SHIPBOARD_INVALID:
                    // TODO RIMUOVERE
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

                case CHECK_SHIPBOARD_CORRECT:
                    // TODO RIMUOVERE
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
                    handleStorageSelectionForReward(input); // false = reward
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

                case END_GAME_PHASE:
                    //TODO controllare che il metodo corretto da chiamare sia leaveGame
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
                    clientController.evaluatedCrewMembers();
                    break;
                case WAIT_PLAYER:
                    showMessage("This isn't your turn. Wait for other player, they are so sloooooow. Please wait...", STANDARD);
                    break;

                case CHECK_SHIPBOARD_AFTER_ATTACK:
                    break;

                case CHOOSE_STORAGE_FOR_CUBEMALUS:
                        try{
                            Coordinates coords = parseCoordinates(input);

                            if(mostPreciousCube.contains(coords)){
                                mostPreciousCube.remove(coords);
                                selectedStorage.add(coords);
                                showMessage("Storage selected.", STANDARD);
                                if(!mostPreciousCube.isEmpty()){
                                    showStorageWithColor();
                                    showMessage("You still need to remove " + mostPreciousCube.size() +" cube (s). Enter another storage coordinate: ", ASK);
                                }
                                else{
                                    if(selectedStorage.size() == cubeMalus){
                                        setClientState(WAIT_PLAYER);
                                        clientController.playerChoseStorageAndBattery(clientController.getNickname(), selectedStorage, selectedBatteries);
                                        selectedStorage.clear();
                                        selectedBatteries.clear();
                                        mostPreciousCube.clear();
                                    }
                                    else{
                                        setClientState(CHOOSE_BATTERY_CUBES);
                                        showBatteryBoxesWithColor();
                                        showMessage("You still need to remove " + (cubeMalus- selectedStorage.size()) +" battery (s). Enter a battery box coordinate: ", ASK);
                                        handleBatterySelection(input);
                                    }
                                }
                            } else{
                                showStorageWithColor();
                                showMessage("ATTENTION! There are a more precious cube on the ship!", STANDARD);
                                showMessage("You must remove them. Please enter storage coordinate: ", ASK);
                            }

                        } catch (NumberFormatException e) {
                            showMessage("Invalid input. Please enter valid numbers for row and column.", ERROR);
                        }
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
    }

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

    public void showShieldWithColor() {
        Map<String, Set<Coordinates>> colorMap = new HashMap<>();

        List<Shield> availableShields = clientModel.getShipboardOf(clientModel.getMyNickname()).getShields();
        availableShields.removeAll(selectedShields);
        Set<Coordinates> availableShieldsCoords = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesOfComponents(availableShields);
        colorMap.put(ANSI_GREEN, availableShieldsCoords);
        showShipBoard(clientModel.getShipboardOf(clientModel.getMyNickname()), clientModel.getMyNickname(), colorMap);
    }

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
    }

}

