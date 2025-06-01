package it.polimi.ingsw.is25am33.client.view.tui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
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

    // Class-level variables to track selection state
    Map<String, Set<Coordinates>> coloredCoordinates = new HashMap<>();
    private List<Coordinates> selectedEngines = new ArrayList<>();
    private List<Coordinates> selectedCabins = new ArrayList<>();
    private List<Coordinates> selectedCannons = new ArrayList<>();
    private List<Coordinates> selectedBatteries = new ArrayList<>();
    private Coordinates selectedShield = null;
    private Coordinates currentSelection = null;
    private boolean waitingForBatterySelection = false;
    private StorageSelectionManager storageManager = null;
    private Map<Integer, Coordinates> crewPlacementCoordinatesMap = new HashMap<>();
    private Map<Coordinates, CrewMember> crewChoices = new HashMap<>();

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
//            case "AbandonedStation":
//                displayAbandonedStationInfo((ClientAbandonedStation) card, output);
//                break;
//            case "Pirates":
//                displayPiratesInfo((ClientPirates) card, output);
//                break;
//            case "SlaveTraders":
//                displaySlaveTradersInfo((ClientSlaveTraders) card, output);
//                break;
//            case "SMUGGLERS":
//                displaySmugglersInfo((ClientSmugglers) card, output);
//                break;
//            case "MeteoriteStorm":
//                displayMeteoriteStormInfo((ClientMeteoriteStorm) card, output);
//                break;
//            case "FreeSpace":
//                displayFreeSpaceInfo((ClientFreeSpace) card, output);
//                break;
//            case "Epidemic":
//                displayEpidemicInfo((ClientEpidemic) card, output);
//                break;
//            case "Stardust":
//                displayStardustInfo((ClientStardust) card, output);
//                break;
//            case "WarField":
//                displayWarFieldInfo((ClientWarField) card, output);
//                break;
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

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayAbandonedShipInfo(ClientAbandonedShip ship, StringBuilder output) {
//        output.append("Crew Required: ").append(ship.getCrewMalus()).append("\n");
//        output.append("Reward: ").append(ship.getReward()).append(" credits\n");
//        output.append("Steps Back: ").append(ship.getStepsBack()).append("\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayAbandonedStationInfo(ClientAbandonedStation station, StringBuilder output) {
//        output.append("Crew Required: ").append(station.getRequiredCrewMembers()).append("\n");
//        output.append("Steps Back: ").append(station.getStepsBack()).append("\n");
//        output.append("Rewards: ");
//        station.getReward().forEach(cube -> output.append(cube.name()).append(" "));
//        output.append("\n");
//    }
    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayPiratesInfo(ClientPirates pirates, StringBuilder output) {
//        output.append("Required Fire Power: ").append(pirates.getRequiredFirePower()).append("\n");
//        output.append("Reward: ").append(pirates.getReward()).append(" credits\n");
//        output.append("Steps Back: ").append(pirates.getStepsBack()).append("\n");
//        output.append("Shots: ").append(pirates.getShotCount()).append("\n");
//
//        if (!pirates.getShots().isEmpty()) {
//            output.append("\nShot Details:\n");
//            for (int i = 0; i < pirates.getShots().size(); i++) {
//                ClientDangerousObject shot = pirates.getShots().get(i);
//                output.append("  Shot ").append(i + 1).append(": ").append(shot.getDisplayString()).append("\n");
//            }
//        }
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displaySlaveTradersInfo(ClientSlaveTraders slaveTraders, StringBuilder output) {
//        output.append("Fire Power Required: ").append(slaveTraders.getRequiredFirePower()).append("\n");
//        output.append("Crew Malus: ").append(slaveTraders.getCrewMalus()).append("\n");
//        output.append("Reward: ").append(slaveTraders.getReward()).append(" credits\n");
//        output.append("Steps Back: ").append(slaveTraders.getStepsBack()).append("\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displaySmugglersInfo(ClientSmugglers smugglers, StringBuilder output) {
//        output.append("Fire Power Required: ").append(smugglers.getRequiredFirePower()).append("\n");
//        output.append("Cube Malus: ").append(smugglers.getCubeMalus()).append("\n");
//        output.append("Steps Back: ").append(smugglers.getStepsBack()).append("\n");
//        output.append("Rewards: ");
//        smugglers.getReward().forEach(cube -> output.append(cube.name()).append(" "));
//        output.append("\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayMeteoriteStormInfo(ClientMeteoriteStorm storm, StringBuilder output) {
//        output.append("Meteorites: ").append(storm.getMeteoriteCount()).append("\n");
//
//        if (!storm.getMeteorites().isEmpty()) {
//            output.append("\nMeteorite Details:\n");
//            for (int i = 0; i < storm.getMeteorites().size(); i++) {
//                ClientDangerousObject meteorite = storm.getMeteorites().get(i);
//                output.append("  Meteorite ").append(i + 1).append(": ").append(meteorite.getDisplayString()).append("\n");
//            }
//        }
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayFreeSpaceInfo(ClientFreeSpace freeSpace, StringBuilder output) {
//        output.append("Free movement through space using engines\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayEpidemicInfo(ClientEpidemic epidemic, StringBuilder output) {
//        output.append("Epidemic spreading! Crew in adjacent cabins will be affected.\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayStardustInfo(ClientStardust stardust, StringBuilder output) {
//        output.append("Stardust field! Ships will move back based on exposed connectors.\n");
//    }

    //TODO uncommentare quando si inizia ad implementare questa carta
//    private void displayWarFieldInfo(ClientWarField warField, StringBuilder output) {
//        output.append("War Zone! Multiple evaluation phases:\n");
//        output.append("Cube Malus: ").append(warField.getCubeMalus()).append("\n");
//        output.append("Crew Malus: ").append(warField.getCrewMalus()).append("\n");
//        output.append("Steps Back: ").append(warField.getStepsBack()).append("\n");
//        output.append("Shots: ").append(warField.getShotCount()).append("\n");
//    }

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
        ClientState mappedState = cardStateToClientState(currentCardState);
        setClientState(mappedState);

        // Reset selection state
        selectedEngines.clear();
        selectedCannons.clear();
        selectedBatteries.clear();
        selectedShield = null;
        currentSelection = null;

        showMessage(String.format("""
                    \n===================================
                    🃏  [Card Update]
                    🆕  New Card State: %s
                    ===================================
                    """, currentCardState.toString()), STANDARD);

        // Automatically show the appropriate menu based on the mapped state
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
        }
    }

    public ClientState cardStateToClientState(CardState cardState) {
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
                // Determine specific type based on dangerous object
                DangerousObj obj = clientModel.getCurrDangerousObj();
                if (obj != null) {
                    String type = obj.getDangerousObjType();
                    if (type.contains("Small")) {
                        return ClientState.HANDLE_SMALL_DANGEROUS_MENU;
                    } else if (type.contains("BigMeteorite")) {
                        return ClientState.HANDLE_BIG_METEORITE_MENU;
                    } else if (type.contains("BigShot")) {
                        return ClientState.HANDLE_BIG_SHOT_MENU;
                    }
                }
                return ClientState.HANDLE_SMALL_DANGEROUS_MENU; // Default
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
            default:
                return ClientState.PLAY_CARD;
        }
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
                        6. End ship board construction
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
        showMessage("TEXT TO BE CHANGED: non fare nulla che stai apposto così", STANDARD);
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

    /**
     * Displays the ship board of a specified player's ship in a formatted textual representation.
     * It includes components, their labels, attributes, and a legend explaining the component types.
     *
     * @param shipBoardClient the client that provides the ship matrix and booked components
     * @param shipBoardOwnerNickname the nickname of the owner of the ship board being displayed
     */
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
            message.append("You've found an abandoned station! If you have enough crew, you can visit to get cargo.\n");
        }

        message.append("Do you want to visit this location? [Y/n]");
        showMessage(message.toString(), ASK);
    }

    @Override
    public void showThrowDicesMenu() {
        setClientState(ClientState.THROW_DICES_MENU);

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card.getCardType().equals("Pirates") ||card.getCardType().equals("SlaveTraders")) {
            showMessage("\nThe enemies are firing at you!", STANDARD);
        } else if (card.getCardType().equals("MeteoriteStorm")) {
            showMessage("\nMeteors are heading your way!", STANDARD);
        }

        showMessage("Press Enter to throw dice and see where they hit...", ASK);
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
        setClientState(ClientState.CHOOSE_ENGINES_MENU);

        // Reset the selection state
        selectedEngines.clear();
        selectedBatteries.clear();

        // Show ship to visualize engines
        this.showMyShipBoard();

        showMessage("\nYou can activate double engines to gain extra movement. " +
                "Each double engine requires one battery.", STANDARD);
        showMessage("Enter coordinates of a double engine (row column) or 'done' when finished: ", ASK);
    }

    @Override
    public void showAcceptTheRewardMenu() {
        setClientState(ClientState.ACCEPT_REWARD_MENU);

        ClientCard card = clientModel.getCurrAdventureCard();
        String rewardStr = "";
        String stepsStr = "";

        // Extract reward and steps information based on card type
//        if (card.hasReward()) {
//            if (card instanceof ClientPirates) {
//                ClientPirates pirates = (ClientPirates) card;
//                rewardStr = String.valueOf(pirates.getReward());
//                stepsStr = String.valueOf(pirates.getStepsBack());
//            } else if (card instanceof ClientAbandonedShip) {
//                ClientAbandonedShip ship = (ClientAbandonedShip) card;
//                rewardStr = String.valueOf(ship.getReward());
//                stepsStr = String.valueOf(ship.getStepsBack());
//            } else if (card instanceof ClientSlaveTraders) {
//                ClientSlaveTraders traders = (ClientSlaveTraders) card;
//                rewardStr = String.valueOf(traders.getReward());
//                stepsStr = String.valueOf(traders.getStepsBack());
//            }
//        }

        showMessage("\nYou've succeeded!", STANDARD);
        if (!rewardStr.isEmpty() && !stepsStr.isEmpty()) {
            showMessage("You can get " + rewardStr + " credits but will lose " + stepsStr + " flight days.", STANDARD);
        }

        showMessage("Do you want to accept the reward? [Y/n]", ASK);
    }

    @Override
    public void showChooseCannonsMenu() {
//        setClientState(ClientState.CHOOSE_CANNONS_MENU);
//
//        ClientCard card = clientModel.getCurrAdventureCard();
//        String strengthStr = "";
//
//        // Extract fire power requirement based on card type
//        if (card instanceof ClientPirates) {
//            strengthStr = String.valueOf(((ClientPirates) card).getRequiredFirePower());
//        } else if (card instanceof ClientSlaveTraders) {
//            strengthStr = String.valueOf(((ClientSlaveTraders) card).getRequiredFirePower());
//        } else if (card instanceof ClientSmugglers) {
//            strengthStr = String.valueOf(((ClientSmugglers) card).getRequiredFirePower());
//        }
//
//        if (!strengthStr.isEmpty()) {
//            showMessage("\nEnemy firepower: " + strengthStr, STANDARD);
//        }
//
//        this.showMyShipBoard();
//        showMessage("Enter coordinates of a double cannon (row column) or 'done' when finished: ", ASK);
    }

    @Override
    public void showSmallDanObjMenu() {
//        setClientState(ClientState.HANDLE_SMALL_DANGEROUS_MENU);
//
//        String dangerObj = clientModel.getCurrDangerousObj().getDangerousObjType();
//        showMessage("\n" + dangerObj + " incoming!", STANDARD);
//
//        // Show ship to visualize shields
//        this.showMyShipBoard();
//
//        showMessage("You can activate a shield or let the object hit your ship.", STANDARD);
//        showMessage("Enter coordinates of a shield (row column) or 'none' to skip: ", ASK);
    }

    @Override
    public void showBigMeteoriteMenu() {
        setClientState(ClientState.HANDLE_BIG_METEORITE_MENU);

        showMessage("\nBig Meteorite incoming!", STANDARD);

        // Show ship to visualize cannons
        this.showMyShipBoard();

        showMessage("You can use a double cannon to destroy it or let it hit your ship.", STANDARD);
        showMessage("Enter coordinates of a double cannon (row column) or 'none' to skip: ", ASK);
    }

    @Override
    public void showBigShotMenu() {
        setClientState(ClientState.HANDLE_BIG_SHOT_MENU);

        showMessage("\nBig Shot incoming! Nothing can stop this...", STANDARD);
        showMessage("Press Enter to see where it hits...", ASK);
    }

    @Override
    public void showHandleRemoveCrewMembersMenu() {
        setClientState(ClientState.CHOOSE_CABIN_MENU);

        // Show the ship to visualize cabins
        this.showMyShipBoard();

        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        int crewToRemove = 0;

        crewToRemove = card.getCrewMalus();

        // Get cabins with crew
        Map<Coordinates, Cabin> cabinsWithCrew = clientModel.getShipboardOf(clientModel.getMyNickname())
                .getCoordinatesAndCabinsWithCrew();

        // Show cabins with crew
        StringBuilder cabinInfo = new StringBuilder("\nYour ship has the following occupied cabins:\n");

        if (cabinsWithCrew.isEmpty()) {
            cabinInfo.append("You have no occupied cabins. You cannot sacrifice crew members.\n");
            showMessage(cabinInfo.toString(), STANDARD);
            showMessage("ILLEGAL STATE: non si dovrebbe mai entrare qui dentro", ERROR);
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

        showMessage(cabinInfo.toString(), STANDARD);
        showMessage("\nYou need to remove " + crewToRemove + " crew member(s).", STANDARD);
        showMessage("Enter coordinates of a cabin to remove crew from (row column) or 'done' when finished: ", ASK);

//        setClientState(ClientState.CHOOSE_CABIN_MENU);
//
//        // Mostra la nave per visualizzare le cabine
//        this.showMyShipBoard();
//
//        // Cerca di trovare il numero di membri dell'equipaggio da rimuovere
//        String cardName = clientModel.getCurrAdventureCard();
//        String crewStr = "";
//        for (String line : cardName.split("\n")) {
//            if (line.contains("Crew Lost:") || line.contains("crewMalus")) {
//                crewStr = line.replaceAll(".*[Cc]rew\\s?(Lost|Malus):\\s+x(\\d+).*", "$2").trim();
//                break;
//            }
//        }
//
//        int crewToRemove = 1;
//        try {
//            if (!crewStr.isEmpty()) {
//                crewToRemove = Integer.parseInt(crewStr);
//            }
//        } catch (NumberFormatException e) {
//            // Fallback a un valore predefinito
//        }
//
//        showMessage("\nYou need to remove " + crewToRemove + " crew member(s).", STANDARD);
//        showMessage("Enter coordinates of a cabin to remove crew from: ", ASK);
    }

    @Override
    public void showHandleCubesRewardMenu() {
        setClientState(ClientState.HANDLE_CUBES_REWARD_MENU);

        // Get cube rewards directly from the current card
        List<CargoCube> rewardCubes = extractCubeRewardsFromCurrentCard();

        // Initialize the storage manager with the cube rewards
        storageManager = new StorageSelectionManager(rewardCubes, getMyShipBoard());

        // Check if the player has any storage available
        if (!storageManager.hasAnyStorage()) {
            showMessage("\nNon hai storage disponibili sulla tua nave. Non puoi accettare nessun cubo reward.", NOTIFICATION_CRITICAL);
            showMessage("Il gioco proseguirà con il prossimo giocatore.", STANDARD);
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
        StringBuilder storageInfo = new StringBuilder("\nLa tua shipboard ha i seguenti storages:\n");
        Map<Coordinates, Storage> coordinatesAndStorages = clientModel.getShipboardOf(clientModel.getMyNickname()).getCoordinatesAndStorages();

        if (coordinatesAndStorages.isEmpty()) {
            storageInfo.append("Nessuno storage disponibile sulla tua nave.\n");
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
                storageInfo.append(String.format("cubi contenuti %s: ", capacityInfo));

                // Build the string for stored cubes
                if (storedCubes.isEmpty()) {
                    storageInfo.append("vuoto");
                } else {
                    // Format each cube with its color
                    List<String> formattedCubes = new ArrayList<>();
                    for (CargoCube cube : storedCubes) {
                        String cubeColor = "";
                        switch (cube) {
                            case RED:
                                cubeColor = ANSI_RED;
                                break;
                            case YELLOW:
                                cubeColor = ANSI_YELLOW;
                                break;
                            case GREEN:
                                cubeColor = ANSI_GREEN;
                                break;
                            case BLUE:
                                cubeColor = ANSI_BLUE;
                                break;
                            default:
                                cubeColor = "";
                        }
                        formattedCubes.add(cubeColor + cube.name() + ANSI_RESET);
                    }
                    storageInfo.append(String.join(", ", formattedCubes));
                }

                storageInfo.append("\n");
            }
        }
        showMessage(storageInfo.toString(), STANDARD);

        StringBuilder message = new StringBuilder("\nHai ottenuto i seguenti cubi come ricompensa:\n");
        for (int i = 0; i < rewardCubes.size(); i++) {
            CargoCube cube = rewardCubes.get(i);
            message.append("- ").append(cube).append(" (valore: ").append(cube.getValue()).append(")")
                    .append(cube == CargoCube.RED ? " - Richiede storage speciale!" : "")
                    .append("\n");
        }

        message.append("\nDevi selezionare uno storage per ogni cubo che puoi accettare. ")
                .append("Ricorda che:\n")
                .append("- I cubi ROSSI possono essere conservati solo in storage speciali.\n")
                .append("- Se uno storage è pieno, il cubo meno prezioso verrà sostituito.\n")
                .append("- Puoi digitare 'next' per saltare il cubo corrente e passare al successivo.\n")
                .append("- Digita 'done' quando hai finito di selezionare tutti gli storage.\n\n");

        CargoCube currentCube = storageManager.getCurrentCube();
        if (currentCube != null) {
            message.append("Prossimo cubo da posizionare: ").append(currentCube)
                    .append(" (valore: ").append(currentCube.getValue()).append(")")
                    .append(currentCube == CargoCube.RED ? " - Questo cubo richiede uno storage speciale!" : "")
                    .append("\n");
            message.append("Inserisci le coordinate di uno storage (riga colonna) per questo cubo: ")
                    .append("'next' per saltare questo cubo, 'skip' per rinunciare a tutti: ");
        }

        showMessage(message.toString(), ASK);
    }

    /**
     * Pre-processes any cubes that can't possibly be accepted due to lack of compatible storage
     */
    private void processImpossibleCubes() {
        boolean anyAutoSkipped = false;

        // Process cubes that can't be accepted until we find one that can
        while (!storageManager.isSelectionComplete()) {
            String impossibilityReason = storageManager.getCurrentCubeImpossibilityReason();
            if (impossibilityReason == null) {
                break; // This cube can be accepted, stop auto-processing
            }

            // This cube can't be accepted, show reason and skip it
            CargoCube currentCube = storageManager.getCurrentCube();
            showMessage(impossibilityReason + ". Questo cubo verrà saltato automaticamente.", NOTIFICATION_INFO);
            storageManager.skipCurrentCube();
            anyAutoSkipped = true;
        }

        // If all cubes have been processed because none could be accepted, submit and return
        if (storageManager.isSelectionComplete() && anyAutoSkipped) {
            showMessage("Tutti i cubi sono stati processati automaticamente. Invio dati al server...", STANDARD);
            List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
            clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
        }
    }

    /**
     * Extracts the information about cube rewards from the current card.
     * Uses the ClientCard object properties directly instead of string parsing.
     *
     * @return List of CargoCube that represent the rewards of the current card
     */
    private List<CargoCube> extractCubeRewardsFromCurrentCard() {
        List<CargoCube> cubes = new ArrayList<>();
        ClientCard card = clientModel.getCurrAdventureCard();

        if (card == null) {
            showMessage("No active card available.", ERROR);
            return cubes;
        }

        // Extract rewards based on card type
        if (card instanceof ClientPlanets) {
            ClientPlanets planets = (ClientPlanets) card;
            return planets.getPlayerReward(clientModel.getMyNickname());
        }
//        else if (card instanceof ClientAbandonedStation) {
//            // AbandonedStation has a direct list of rewards
//            return new ArrayList<>(((ClientAbandonedStation) card).getReward());
//        }
//        else if (card instanceof ClientSmugglers) {
//            // Smugglers have a direct list of rewards
//            return new ArrayList<>(((ClientSmugglers) card).getReward());
//        }

        // If no rewards were found or the card type doesn't have rewards
        if (cubes.isEmpty() && clientModel.getCurrCardState() == CardState.HANDLE_CUBES_REWARD) {
            showMessage("Could not determine cube rewards for this card type.", ERROR);
        }

        return cubes;
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
        setClientState(ClientState.EPIDEMIC_MENU);

        showMessage("\nAn epidemic is spreading throughout the fleet!", STANDARD);
        showMessage("Each occupied cabin connected to another occupied cabin will lose one crew member.", STANDARD);
        showMessage("Press Enter to continue...", ASK);
    }

    @Override
    public void showStardustMenu() {
        setClientState(ClientState.STARDUST_MENU);

        showMessage("\nStardust has been detected in your flight path!", STANDARD);
        showMessage("Each ship will move back one space for every exposed connector.", STANDARD);
        showMessage("Press Enter to continue...", ASK);
    }

    @Override
    public void showHandleCubesMalusMenu() {
        setClientState(ClientState.HANDLE_CUBES_MALUS_MENU);

        // Mostra la nave per visualizzare i depositi
        this.showMyShipBoard();

        showMessage("\nYou must discard some cargo cubes!", STANDARD);
        showMessage("Enter coordinates of a storage to remove a cargo cube from: ", ASK);
    }

    private ShipBoardClient getMyShipBoard() {
        return clientModel.getShipboardOf(clientModel.getMyNickname());
    }

    private void showMyShipBoard() {
        this.showShipBoard(getMyShipBoard(), clientModel.getMyNickname());
    }

//    @Override
//    public BiConsumer<CallableOnGameController, String> showThrowDicesMenu(){
//        askForInput("", "Press any key to throw dices ");
//
//        return (server, nickname) -> {
//            try {
//                server.playerWantsToThrowDices(nickname);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChoosePlanetMenu(){
////        int choice = Integer.parseInt(askForInput("Choose the index of the planet you want visit, between 1 and " + ((Planets) clientModel.getCurrAdventureCard()).getAvailablePlanets().size() + 1 + " (press 0 to skip). ", defaultInterrogationPrompt));
////        return(server, nickname) -> {
////            try {
////                server.playerWantsToVisitPlanet(nickname, choice);
////            } catch (RemoteException e) {
////                throw new RuntimeException(e);
////            }
////        };
//        return null;
//        // TODO
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChooseEnginesMenu() {
//
//        this.showMyShipBoard();
//        showMessage("\n", STANDARD);
//        List<Coordinates> doubleEnginesCoordinates = new ArrayList<>();
//        List<Coordinates> batteryBoxesCoordinates = new ArrayList<>();
//        ShipBoardClient shipBoard = getMyShipBoard();
//
//        while(true) {
//
//            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double engines you would like to activate or press enter to skip: ");
//            if (coords == null) break;
//            if (shipBoard.getDoubleEngines().contains(shipBoard.getComponentAt(coords)))
//                doubleEnginesCoordinates.add(coords);
//            else {
//                showMessage("The selected coordinates are not related to any double engine, try again", STANDARD);
//                continue;
//            }
//
//            while(true) {
//
//                Coordinates batteryCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
//                if(batteryCoords == null){
//                    showMessage("You have to select a battery box after choosing a double engine.", STANDARD);
//                    continue;
//                }
//                if (shipBoard.getBatteryBoxes().contains(shipBoard.getComponentAt(batteryCoords))) {
//                    batteryBoxesCoordinates.add(batteryCoords);
//                    break;
//                }
//                else
//                    showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
//            }
//        }
//
//        return (server, nickname) -> {
//            try {
//                server.playerChoseDoubleEngines(nickname, doubleEnginesCoordinates, batteryBoxesCoordinates);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu() {
//
//        while(true) {
//            String input = askForInput("", "Do you want to accept the reward? [Y/n] ");
//            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
//                return (server, nickname) -> {
//                    try {
//                        server.playerWantsToAcceptTheReward(nickname, true);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//            } else if (input.equalsIgnoreCase("N")) {
//                return (server, nickname) -> {
//                    try {
//                        server.playerWantsToAcceptTheReward(nickname, false);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//            } else {
//                showMessage("Invalid input. Please enter Y or N.", STANDARD);
//            }
//        }
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChooseCannonsMenu(){
//        this.showMyShipBoard();
//        System.out.println();
//        List<Coordinates> doubleCannonsCoordinates = new ArrayList<>();
//        List<Coordinates> batteryBoxesCoordinates = new ArrayList<>();
//        ShipBoardClient myShipBoard = getMyShipBoard();
//
//        while(true) {
//
//            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double cannon you would like to activate or press enter to skip: ");
//            if (coords == null) break;
//            if (myShipBoard.getDoubleCannons().contains(myShipBoard.getComponentAt(coords)))
//                doubleCannonsCoordinates.add(coords);
//            else {
//                showMessage("The selected coordinates are not related to any double cannons, try again", STANDARD);
//                continue;
//            }
//
//            while(true) {
//
//                Coordinates batteryCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
//                if(batteryCoords == null){
//                    showMessage("You have to select a battery box after choosing a double engine.", STANDARD);
//                    continue;
//                }
//                if (myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryCoords))) {
//                    batteryBoxesCoordinates.add(batteryCoords);
//                    break;
//                }
//                else
//                    showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
//            }
//        }
//
//        return (server, nickname) -> {
//            try {
//                server.playerChoseDoubleCannons(nickname, doubleCannonsCoordinates, batteryBoxesCoordinates);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu() {
//        this.showMyShipBoard();
//        System.out.println();
//        List<Coordinates> cabinCoordinates = new ArrayList<>();
//        ShipBoardClient myShipBoard = getMyShipBoard();
//
//        while (true) {
//            Coordinates coords = readCoordinatesFromUserInput("Select the coordinates (row column) of the cabins from which you want to remove the crew members: ");
//            if (coords == null) {
//                showMessage("You have to select cabins", STANDARD);
//                continue;
//            }
//            if (myShipBoard.getCabin().contains(myShipBoard.getComponentAt(coords))) {
//                cabinCoordinates.add(coords);
//                break;
//            } else {
//                showMessage("The selected coordinates are not related to any cabin, try again", STANDARD);
//            }
//        }
//
//        return (server, nickname) -> {
//            try {
//                server.playerChoseCabins(nickname, cabinCoordinates);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showSmallDanObjMenu() {
//
//        showMessage(clientModel.getCurrDangerousObj().getDangerousObjType() + " incoming!!!", STANDARD);
//        showMessage("Choose how to defend from it", STANDARD);
//        ShipBoardClient myShipBoard = getMyShipBoard();
//
//        while (true) {
//            Coordinates activableCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the shield you would like to activate or press enter if you don't need to activate one: ");
//            if (activableCoords == null)
//                return (server, nickname) -> {
//                    try {
//                        server.playerHandleSmallDanObj(nickname, new Coordinates(), new Coordinates());
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//            if(myShipBoard.getShields().contains(myShipBoard.getComponentAt(activableCoords))) {
//                showMessage("The selected coordinates are not related to any shield, try again", STANDARD);
//                continue;
//            }
//
//            Coordinates batteryBoxCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
//            if(myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryBoxCoords))) {
//                showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
//                continue;
//            }
//
//            return (server, nickname) -> {
//                try {
//                    server.playerHandleSmallDanObj(nickname, activableCoords, batteryBoxCoords);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            };
//        }
//
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu() {
//
//        showMessage(clientModel.getCurrDangerousObj().getDangerousObjType() + " incoming!!!", STANDARD);
//        showMessage("Choose how to defend from it", STANDARD);
//        ShipBoardClient myShipBoard = getMyShipBoard();
//
//        while (true) {
//            Coordinates doubleCannonCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the double cannon you would like to activate or press enter if you don't need to activate one: ");
//            if (doubleCannonCoords == null)
//                return (server, nickname) -> {
//                    try {
//                        server.playerHandleBigMeteorite(nickname, new Coordinates(), new Coordinates());
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//            if(myShipBoard.getDoubleCannons().contains(myShipBoard.getComponentAt(doubleCannonCoords))) {
//                showMessage("The selected coordinates are not related to any double cannon, try again", STANDARD);
//                continue;
//            }
//
//            Coordinates batteryBoxCoords = readCoordinatesFromUserInput("Select the coordinates (row column) for the battery box you would like to activate: ");
//            if(myShipBoard.getBatteryBoxes().contains(myShipBoard.getComponentAt(batteryBoxCoords))) {
//                showMessage("The selected coordinates are not related to any battery box, try again", STANDARD);
//                continue;
//            }
//
//            return (server, nickname) -> {
//                try {
//                    server.playerHandleBigMeteorite(nickname, doubleCannonCoords, batteryBoxCoords);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            };
//        }
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showBigShotMenu() {
//
//        showMessage("Big shot incoming!!!\nLet's hope it will miss your ship because there is nothing you can do :(", STANDARD);
//        return (server, nickname) -> {
//            try {
//                server.playerHandleBigShot(nickname);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showEpidemicMenu() {
//        showMessage("An epidemic is spreading!!!\nRemoving 1 crew member (human or alien) from every occupied cabin connected to another occupied cabin...", STANDARD);
//        return(server, nickname) -> {
//            try {
//                server.spreadEpidemic(nickname);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showStardustMenu() {
//        showMessage("Stardust is coming!\nMaking one step back for every exposed component on you ship...", STANDARD);
//        return(server, nickname) -> {
//            try {
//                server.stardustEvent(nickname);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu() {
//
//        while (true){
//            Coordinates coords = readCoordinatesFromUserInput("Choose the coordinates (row column) of the storage where you would like to store the cargo cube or press enter to skip this reward: ");
//            if (coords == null) return (server, nickname) -> {
//                try {
//                    server.playerChoseStorage(nickname, new Coordinates());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            };
//
//            ShipBoardClient myShipBoard = getMyShipBoard();
//
//            List<Component> storages = new ArrayList<>();
//            storages.addAll(myShipBoard.getSpecialStorages());
//            storages.addAll(myShipBoard.getStandardStorages());
//
//            if (storages.contains(myShipBoard.getComponentAt(coords)))
//                return (server, nickname) -> {
//                    try {
//                        server.playerChoseStorage(nickname, coords);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//            else
//                showMessage("The selected coordinates are not related to any storage, try again", STANDARD);
//        }
//
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu() {
//
//        while (true){
//            Coordinates coords = readCoordinatesFromUserInput("Choose the coordinates (row column) of the storage where you would like to remove one cargo cube: ");
//            ShipBoardClient myShipBoard = getMyShipBoard();
//
//            List<Component> storages = new ArrayList<>();
//            storages.addAll(myShipBoard.getStandardStorages());
//            storages.addAll(myShipBoard.getSpecialStorages());
//
//            if (storages.contains(myShipBoard.getComponentAt(coords)))
//                return (server, nickname) -> {
//                    try {
//                        server.playerChoseStorage(nickname, coords);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                };
//            else
//                showMessage("The selected coordinates are not related to any storage, try again", STANDARD);
//        }
//
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showVisitLocationMenu() {
//
//        while (true) {
//            String input = askForInput("", "Do you want to visit the card location? [Y/n] ");
//            if (input.equalsIgnoreCase("Y") || input.isEmpty()) {
//                return (server, nickname) -> {
//                    try {
//                        server.playerWantsToVisitLocation(nickname, true);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//            } else if (input.equalsIgnoreCase("N")) {
//                return (server, nickname) -> {
//                    try {
//                        server.playerWantsToVisitLocation(nickname, false);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                };
//
//            } else {
//                showMessage("Invalid input. Please enter Y or N.", STANDARD);
//            }
//        }
//
//    }



    public void showCurrentRanking() {
        StringBuilder output = new StringBuilder();
        output.append("==========  Ranking  ==========\n");

        List<String> sortedRanking = clientModel.getSortedRanking();

        int topScore = clientModel.getPlayerClientData().get(sortedRanking.getFirst()).getFlyingBoardPosition();

        if (topScore == 0) {
            showMessage("Think about building your ship board, being the leader without a ship board is roughly impossible.\n> ", ASK);
            return;
        }

        for (int i = 0; i < sortedRanking.size(); i++) {
            String player = sortedRanking.get(i);
            int playerScore = clientModel.getPlayerClientData().get(player).getFlyingBoardPosition();
            int diff = topScore - playerScore;
            output.append(String.format("%d. %-20s | %-2d %s\n", i + 1, player, playerScore, (diff == 0 ? "" : "(" + (-diff) + ")")));
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

        menu.append("\n" + ANSI_YELLOW + "Summary: " + cabinsWithChoices + " cabin(s) with aliens, "
                + cabinsWithoutChoices + " cabin(s) will receive humans." + ANSI_RESET + "\n");

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

    private void handleEngineSelection(String input) {
        try {
            Coordinates coords = parseCoordinates(input);
            if (coords == null) return;

            ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
            Component component = shipBoard.getComponentAt(coords);

            if (component == null || !shipBoard.getDoubleEngines().contains(component)) {
                showMessage("No double engine at these coordinates.", ERROR);
                return;
            }

            // Passa alla selezione della batteria
            selectedEngines.add(coords);
            showMessage("Now select a battery box for this engine (row column): ", ASK);
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
                return;
            }

            // Aggiungi la batteria e torna alla selezione del motore
            selectedBatteries.add(coords);
            showMessage("Engine and battery selected. Enter another engine or 'done' to finish: ", ASK);
            setClientState(ClientState.CHOOSE_ENGINES_MENU);
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
                return;
            }

            // Passa alla selezione della batteria
            selectedCannons.add(coords);
            showMessage("Now select a battery box for this cannon (row column): ", ASK);
            setClientState(ClientState.CHOOSE_CANNONS_SELECT_BATTERY);
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
                return;
            }

            // Passa alla selezione della batteria
            selectedShield = coords;
            showMessage("Now select a battery box for this shield (row column): ", ASK);
            setClientState(ClientState.HANDLE_SMALL_DANGEROUS_SELECT_BATTERY);
        } catch (Exception e) {
            showMessage("Error processing coordinates: " + e.getMessage(), ERROR);
        }
    }

    private void handleStorageSelection(String input) {
        //TODO OPTIONAL: aggiugnere possibilità di rifare da capo le scelte. Per esempio in caso un client sbagliasse a posizionare un cubo
        if (input.equalsIgnoreCase("done")) {
            // Se l'utente ha finito ma non ha selezionato tutti gli storage possibili
            if (!storageManager.isSelectionComplete() && storageManager.canAcceptCurrentCube()) {
                showMessage("Non hai selezionato storage per tutti i cubi che puoi accettare. " +
                                "\nI cubi rimanenti verranno scartati. Continua con 'confirm' o seleziona altri storage.",
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
            // L'utente rinuncia a tutti i cubi reward
            showMessage("Rinuncia a tutti i cubi reward...", STANDARD);
            List<Coordinates> emptyList = new ArrayList<>();
            clientController.playerChoseStorage(clientController.getNickname(), emptyList);
            return;
        }

        if (input.equalsIgnoreCase("next")) {
            // Skip only the current cube
            CargoCube currentCube = storageManager.getCurrentCube();
            showMessage("Salto il cubo " + currentCube + "...", STANDARD);
            storageManager.skipCurrentCube();

            // Check if there are more cubes to process
            if (storageManager.isSelectionComplete()) {
                showMessage("Tutti i cubi sono stati processati. Invio dati al server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Show menu for the next cube
                CargoCube nextCube = storageManager.getCurrentCube();
                StringBuilder message = new StringBuilder("\nProssimo cubo da posizionare: ")
                        .append(nextCube)
                        .append(" (valore: ").append(nextCube.getValue()).append(")")
                        .append(nextCube == CargoCube.RED ? " - Questo cubo richiede uno storage speciale!" : "")
                        .append("\n");
                message.append("Inserisci le coordinate di uno storage (riga colonna), ")
                        .append("\n'next' per saltare questo cubo, 'skip' per rinunciare a tutti, ")
                        .append("\n'done' per confermare: ");
                showMessage(message.toString(), ASK);
            }
            return;
        }

        // Verifica se il cubo corrente può essere accettato
        if (!storageManager.canAcceptCurrentCube()) {
            showMessage("Non puoi accettare questo cubo. Aggiunto automaticamente come 'scartato'.", NOTIFICATION_INFO);

            // Aggiungiamo coordinate invalide per segnalare che questo cubo viene saltato
            storageManager.skipCurrentCube();

            // Verifica se abbiamo finito o se c'è un altro cubo
            if (storageManager.isSelectionComplete()) {
                showMessage("Selezione completata. Invio dati al server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Mostra il menu per il prossimo cubo
                CargoCube nextCube = storageManager.getCurrentCube();
                if (nextCube != null) {
                    showMessage("\nProssimo cubo da posizionare: " + nextCube +
                                    " (valore: " + nextCube.getValue() + ")" +
                                    (nextCube == CargoCube.RED ? " - Questo cubo richiede uno storage speciale!" : ""),
                            STANDARD);
                    showMessage("Inserisci le coordinate di uno storage (riga colonna), " +
                            "\n'next' per saltare questo cubo, 'skip' per rinunciare a tutti, " +
                            "\n'done' per confermare: ", ASK);
                }
            }
            return;
        }

        try {
            // Parsa le coordinate
            Coordinates coords = parseCoordinates(input);
            if (coords == null) {
                showMessage("Formato coordinate non valido. Usa 'riga colonna' (es. '5 7').", ERROR);
                showMessage("Oppure usa i comandi: 'done', 'skip', 'confirm'", STANDARD);
                return;
            }

            // Verifica se le coordinate corrispondono a uno storage valido
            String storageStatus = storageManager.checkStorageStatus(coords);
            if (storageStatus == null) {
                showMessage("Nessuno storage alle coordinate specificate.", ERROR);
                return;
            }

            // Ottieni informazioni sul cubo corrente
            CargoCube currentCube = storageManager.getCurrentCube();

            // Tenta di aggiungere lo storage alla selezione
            boolean added = storageManager.addStorageSelection(coords);
            if (!added) {
                // Se non è stato possibile aggiungere lo storage, mostra un messaggio di errore
                if (currentCube == CargoCube.RED) {
                    showMessage("ATTENZIONE: I cubi ROSSI possono essere messi solo in storage speciali!", ERROR);
                } else {
                    showMessage("Errore nell'aggiungere lo storage alla selezione.", ERROR);
                }
                return;
            }

            // Storage aggiunto con successo
            showMessage("Storage selezionato: " + storageStatus, STANDARD);

            // Se abbiamo selezionato tutti gli storage possibili
            if (storageManager.isSelectionComplete()) {
                showMessage("\nHai selezionato storage per tutti i cubi che puoi accettare. Invio dati al server...", STANDARD);
                List<Coordinates> selectedCoordinates = storageManager.getSelectedStorageCoordinates();
                clientController.playerChoseStorage(clientController.getNickname(), selectedCoordinates);
            } else {
                // Altrimenti, mostra il menu per il prossimo cubo
                CargoCube nextCube = storageManager.getCurrentCube();
                if (nextCube != null) {
                    StringBuilder message = new StringBuilder("\nCubo ").append(currentCube)
                            .append(" posizionato con successo.\n\n");
                    message.append("Prossimo cubo da posizionare: ").append(nextCube)
                            .append(" (valore: ").append(nextCube.getValue()).append(")")
                            .append(nextCube == CargoCube.RED ? " - Questo cubo richiede uno storage speciale!" : "")
                            .append("\n");
                    message.append("Inserisci le coordinate di uno storage (riga colonna), ")
                            .append("\n'done' per confermare, 'skip' per rinunciare a tutti, ")
                            .append("\n'confirm' per confermare anche selection parziali, ");
                    showMessage(message.toString(), ASK);
                }
            }
        } catch (Exception e) {
            showMessage("Errore nel processare le coordinate: " + e.getMessage(), ERROR);
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
                if(card.getCrewMalus() - selectedCabins.size() == 0) {
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
            if((card.getCrewMalus() - selectedCabins.size()) == 0)
               showMessage("You have completed your choices, please press done: ", ASK);
            else
                showMessage("You still need to remove " + (card.getCrewMalus() - selectedCabins.size()) +" crew member(s). Enter another cabin coordinate: ", ASK);
        } catch (NumberFormatException e) {
            showMessage("Invalid coordinates. Please enter numbers.", ERROR);
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), ERROR);
        }
    }

    private void handleCrewPlacementInput(String input) {
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
            return;
        }

        if (input.equalsIgnoreCase("R")) {
            // Reset delle scelte
            crewChoices.clear();
            showMessage("All choices have been reset.", STANDARD);
            showCrewPlacementMenu();
            return;
        }

        try {
            // Verifica se è solo un numero (rimuove l'alieno da cabine già scelte)
            if (input.matches("\\d+")) {
                int index = Integer.parseInt(input);

                if (!crewPlacementCoordinatesMap.containsKey(index)) {
                    showMessage("Invalid cabin index. Please select a number between 1 and " +
                            crewPlacementCoordinatesMap.size(), ERROR);
                    return;
                }

                Coordinates coords = crewPlacementCoordinatesMap.get(index);

                // Verifica se questa cabina ha già una scelta
                if (crewChoices.containsKey(coords)) {
                    // Rimuovi semplicemente l'alieno
                    CrewMember removed = crewChoices.remove(coords);
                    showMessage("Removed " + (removed == CrewMember.PURPLE_ALIEN ? "purple" : "brown") +
                            " alien from cabin. It will receive humans instead.", STANDARD);
                    showCrewPlacementMenu();
                } else {
                    showMessage("This cabin doesn't have an alien assigned yet. Use " + index + "P or " + index + "B to assign an alien.", STANDARD);
                }

                return;
            }

            // Formato per assegnare direttamente un alieno: [index][P|B]
            if (input.length() >= 2) {
                int index = Integer.parseInt(input.substring(0, input.length() - 1));
                char choice = Character.toUpperCase(input.charAt(input.length() - 1));

                if (!crewPlacementCoordinatesMap.containsKey(index)) {
                    showMessage("Invalid cabin index. Please select a number between 1 and " +
                            crewPlacementCoordinatesMap.size(), ERROR);
                    return;
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
                            return;
                        }

                        if (purpleAlreadySelected) {
                            showMessage("You can only have 1 purple alien on your ship", ERROR);
                            return;
                        }

                        crewChoices.put(coords, CrewMember.PURPLE_ALIEN);
                        feedbackMessage = "Cabin will receive 1 purple alien";
                        break;

                    case 'B':
                        // Verifica se supporta Brown e se non è già stato selezionato
                        if (!supportedColors.contains(ColorLifeSupport.BROWN)) {
                            showMessage("This cabin is not connected to a brown life support module", ERROR);
                            return;
                        }

                        if (brownAlreadySelected) {
                            showMessage("You can only have 1 brown alien on your ship", ERROR);
                            return;
                        }

                        crewChoices.put(coords, CrewMember.BROWN_ALIEN);
                        feedbackMessage = "Cabin will receive 1 brown alien";
                        break;

                    default:
                        showMessage("Invalid choice. Use P for purple alien or B for brown alien", ERROR);
                        return;
                }

                // Mostra messaggio di feedback e aggiorna il menu
                if (feedbackMessage != null) {
                    showMessage(feedbackMessage, STANDARD);
                }

                // Aggiorna il menu per mostrare lo stato corrente
                showCrewPlacementMenu();
            } else {
                showMessage("Invalid input format. Use [number] to remove an alien, [number]P or [number]B to assign aliens, C to confirm, or R to reset", ERROR);
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid input format. Please enter a valid number.", ERROR);
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
            clientController.showShipBoard(input.trim().split("\\s+")[1]);
            return;
        } else if (input.equals("rank")) {
            showCurrentRanking();
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

                        case 6:
                            clientState = BUILDING_SHIPBOARD_WAITING;
                            clientController.endBuildShipBoardPhase();
                            break;

                        default:
                            showMessage("Invalid choice. Please select 1-6.\n> ", ASK);
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
                    clientController.placePlaceholder();
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

                case CREW_PLACEMENT_MENU:
                    handleCrewPlacementInput(input);
                    break;

                case NO_CREW_TO_PLACE:
                    clientController.submitCrewChoices(new HashMap<>());
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
                        clientController.playerWantsToVisitPlanet(clientController.getNickname(), planetChoice);
                    } catch (NumberFormatException e) {
                        showMessage("Please enter a valid planet number.", ERROR);
                    }
                    break;

                case CHOOSE_CABIN_MENU:
                    handleCabinSelection(input);
                    break;


                case CHOOSE_ENGINES_MENU:
                    if (input.equalsIgnoreCase("done")) {
                        if (selectedEngines.isEmpty()) {
                            showMessage("You didn't select any engines.", ERROR);
                        } else {
                            clientController.playerChoseDoubleEngines(
                                    clientController.getNickname(), selectedEngines, selectedBatteries);
                            selectedEngines.clear();
                            selectedBatteries.clear();
                        }
                    } else {
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
                    //TODO controllare che numero di batterie e cannoni sono in numero uguale, forse già fatto all'interno di handleShipSelection
                    if (input.equalsIgnoreCase("none")) {
                        clientController.playerHandleSmallDanObj(
                                clientController.getNickname(), new Coordinates(-1, -1), new Coordinates(-1, -1));
                    } else {
                        handleShieldSelection(input);
                    }
                    break;

                case HANDLE_BIG_METEORITE_MENU:
                    if (input.equalsIgnoreCase("none")) {
                        clientController.playerHandleBigMeteorite(
                                clientController.getNickname(), new Coordinates(-1, -1), new Coordinates(-1, -1));
                    } else {
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
                    handleStorageSelection(input); // false = reward
                    break;

                case CANNOT_ACCEPT_CUBES_REWARDS:
                    // Send an empty list to the server to proceed
                    List<Coordinates> emptyList = new ArrayList<>();
                    clientController.playerChoseStorage(clientController.getNickname(), emptyList);
                    break;

                case HANDLE_CUBES_MALUS_MENU:
                    handleStorageSelection(input); // true = malus
                    break;

                case CANNOT_VISIT_LOCATION:
                    clientController.playerWantsToAcceptTheReward(clientController.getNickname(), false);
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