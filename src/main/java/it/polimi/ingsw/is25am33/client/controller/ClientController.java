package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.view.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.client.Hourglass;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.polimi.ingsw.is25am33.client.view.MessageType.STANDARD;

public class ClientController extends UnicastRemoteObject implements CallableOnClientController {

    private ClientView view;
    private boolean connected = false;
    private CallableOnDNS dns;
    private CallableOnGameController serverController;
    private String currentGameId;
    private boolean inGame = false;
    private String nickname;
    boolean gameStarted = false;
    private final ClientModel clientModel;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public ClientController(ClientModel clientModel) throws RemoteException {
        super();
        this.clientModel = clientModel;
    }

    public static void main(String @NotNull [] args) throws RemoteException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Galaxy Trucker Client ===");

        ClientModel clientModel = new ClientModel();

        // Selezione dell'interfaccia utente
        ClientView view = selectUserInterface(scanner, clientModel, args[0]);
        if (view == null) {
            System.err.println("Invalid parameters. Exiting...");
            return;
        }

        // Creiamo il controller
        ClientController clientController = new ClientController(clientModel);
        clientController.setView(view);
        view.initialize();

        // Selezione del protocollo di rete
        CallableOnDNS dns = clientController.selectNetworkProtocol(args[1]);
        if (dns == null) {
            System.err.println("Invalid parameters. Exiting...");
            return;
        }

        clientController.setDns(dns);

        clientController.register();

        clientModel.setMyNickname(clientController.getNickname());

        clientController.start();
    }

    private void register() {

        boolean registered = false;

        while (!registered) {

            String attemptedNickname = view.askNickname();

            if (attemptedNickname.length() < 3) {
                view.showError("Invalid nickname - choose a nickname at least 3 characters long");
                continue;
            }

            try {
                registered = dns.registerWithNickname(attemptedNickname, this);
                if (!registered) {
                    view.showError("Nickname already exists");
                } else {
                    view.showMessage("Nickname registered successfully!", STANDARD);
                    this.nickname = attemptedNickname;
                }
            } catch (IOException e) {
                view.showError("Error registering nickname: " + attemptedNickname);
            }

        }

    }

    public void setDns(CallableOnDNS dns) {
        this.dns = dns;
    }

    public String getNickname() {
        return nickname;
    }

    private void start() {
        boolean running = true;

        while (running) {
            if (inGame) {
                running = handleGameState();
            } else {
                running = handleMainMenu();
            }
        }

        // Disconnessione
        //disconnect();

    }

    private boolean handleGameState() {

        if (gameStarted) {

            buildShipBoardPhase();

            // TODO

            cardPhase();

            view.askForInput("", "FINE PER ADESSO");

            return true;
        }

        int choice = view.showGameMenu();

        try {
            switch (choice) {
                case 0:
                    gameStarted = true;
                    return true;
                case 1: // Lascia il gioco
                    leaveGame();
                    return true;

                default:
                    view.showError("Invalid choice");
                    return true;
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
            return true;
        }
    }


    private boolean handleMainMenu() {
        int choice = view.showMainMenu();

        try {
            return switch (choice) {
                case 1 -> {
                    listAvailableGames();
                    yield true;
                }
                case 2 -> {
                    createGame();
                    yield true;
                }
                case 3 -> {
                    joinGame();
                    yield true;
                }
                case 4 -> {
                    // TODO disconnessione
                    yield false;
                }
                default -> {
                    view.showError("Invalid choice");
                    yield true;
                }
            };
        } catch (Exception e) {
            view.showError(e.getMessage());
            return true;
        }
    }

    /**
     * Displays the list of available games.
     */
    private void listAvailableGames() throws IOException {
        List<GameInfo> games = dns.getAvailableGames();
        view.showAvailableGames(games);
    }

    /**
     * Creates a new game.
     */
    private void createGame() throws IOException {
        int[] gameSettings = view.askCreateGame();
        int numPlayers = gameSettings[0];
        boolean isTestFlight = gameSettings[1] == 1;
        PlayerColor color = view.intToPlayerColor(gameSettings[2]);

        GameInfo gameInfo = dns.createGame(color, numPlayers, isTestFlight, nickname);

        // if the dns was SocketClientManager (e.g., the client is socket), the serverController is the SocketClientManager used as a dns before
        if (dns instanceof SocketClientManager) {
            serverController = (SocketClientManager) dns;
        } else {
            serverController = gameInfo.getGameController();
        }
        currentGameId = gameInfo.getGameId();
        inGame = true;

        view.notifyGameCreated(currentGameId);
        view.notifyPlayerJoined(this.nickname, gameInfo);
        view.showMessage("Waiting for other players to join...", STANDARD);
    }

    /**
     * Joins an existing game.
     */
    private void joinGame() throws RemoteException {

        try {
            String[] joinSettings = null;
            String gameId = null;
            GameInfo gameInfo;

            while (true) {
                List<GameInfo> games = dns.getAvailableGames();

                if (games.isEmpty()) {
                    view.showMessage("No games available.", STANDARD);
                    return;
                }

                joinSettings = view.askJoinGame(games);
                gameId = joinSettings[0];

                String finalGameId = gameId;
                Optional<GameInfo> gameInfoOptional = games.stream().filter(info -> info.getGameId().equals(finalGameId)).findFirst();
                if(gameInfoOptional.isPresent()){
                    gameInfo = gameInfoOptional.get();
                    break;
                } else {
                    view.showError("Game not found");
                }
            }

            // Break down the conversion to clarify it for the compiler
            int colorChoice = Integer.parseInt(joinSettings[1]);
            PlayerColor color = view.intToPlayerColor(colorChoice);

            boolean success = dns.joinGame(gameId, nickname, color);

            while(!success) {
                view.showError("Color already in use");
                List<PlayerColor> availableColors = Arrays.stream(PlayerColor.values())
                        .filter(currColor -> !gameInfo.getConnectedPlayers().containsValue(currColor))
                        .toList();
                joinSettings[1] = view.askPlayerColor(availableColors);
                colorChoice = Integer.parseInt(joinSettings[1]);
                color = view.intToPlayerColor(colorChoice);
                success = dns.joinGame(gameId, nickname, color);
            }

            currentGameId = gameId;

            // if the dns was SocketClientManager (e.g., the client is socket), the serverController is the SocketClientManager used as a dns before
            if (dns instanceof SocketClientManager) {
                serverController = (SocketClientManager) dns;
            } else {
                serverController = gameInfo.getGameController();
            }
            inGame = true;
            view.showMessage("Successfully joined game!", STANDARD);
            view.showMessage("Waiting for the game to start...", STANDARD);

        } catch (NumberFormatException e) {
            view.showError("Invalid color choice: " + e.getMessage());
        } catch (RemoteException e) {
            if(e.getMessage().contains("Client not registered"))
                view.showError("Error joining game: Client not registered");
            else if (e.getMessage().contains("GameModel not found"))
                view.showError("GameModel already started");
            else if (e.getMessage().contains("GameModel already started"))
                view.showError("Error joining game: GameModel already started");
            else if (e.getMessage().contains("GameModel is full"))
                view.showError("Error joining game: GameModel is full");
            else if (e.getMessage().contains("You are already in this gameModel"))
                view.showError("Error joining game: You are already in this gameModel");
        } catch (Exception e) {
            view.showError("Error joining game: " + e.getMessage());
        }
    }

    public void setView(ClientView view) {
        this.view = view;
    }

    /**
     * Permette all'utente di selezionare l'interfaccia utente
     * @param scanner Scanner per leggere l'input dell'utente
     * @return L'implementazione di ClientView scelta
     */
    private static ClientView selectUserInterface(Scanner scanner, ClientModel clientModel, String choice) {

        return switch (choice) {
            case "cli" -> new ClientCLIView(clientModel);
            case "gui" ->
                // TODO
                //return new ClientGUIView();
                    null;
            default -> null;
        };


//        System.out.println("Select user interface:");
//        System.out.println("1. Command Line Interface (CLI)");
//        System.out.println("2. Graphical User Interface (GUI)");
//
//        while (true) {
//            System.out.print("Your choice: ");
//            try {
//                int choice = Integer.parseInt(scanner.nextLine());
//                switch (args) {
//                    case 1:
//                        return new ClientCLIView(clientModel);
//                    case 2:
//                        //return new ClientGUIView();
//                    default:
//                        System.out.println("Invalid choice. Please enter 1 or 2.");
//                }
//            } catch (NumberFormatException e) {
//                System.out.println("Please enter a valid number.");
//            }
//        }
    }

    /**
     * Permette all'utente di selezionare il protocollo di rete
     * @return L'implementazione di NetworkManager scelta
     */
    private CallableOnDNS selectNetworkProtocol(String choice) {

        try {
            return switch (choice) {
                case "rmi" -> this.setUpRMIConnection();
                case "socket" -> this.setUpSocketConnection();
                default -> null;
            };
        } catch (IOException e) {
            view.showError("Connection refused: " + e.getMessage());
            return null;
        }

//        System.out.println("Select network protocol:");
//        System.out.println("1. RMI (Remote Method Invocation)");
//        System.out.println("2. Socket TCP/IP");
//
//        while (true) {
//            try {
//                int choice = Integer.parseInt(view.askForInput("Your choice: "));
//                switch (choice) {
//                    case 1:
//                        return this.setUpRMIConnection();
//                    case 2:
//                        return this.setUpSocketConnection();
//                    default:
//                        System.out.println("Invalid choice. Please enter 1 or 2.");
//                }
//            } catch (IOException e) {
//                view.showError("Connection refused: " + e.getMessage());
//            } catch (NumberFormatException e) {
//                view.showMessage("Invalid choice. Please enter 1 or 2.");
//            }
//        }
    }

    private CallableOnDNS setUpRMIConnection() throws RemoteException {
        String serverAddress = view.askServerAddress();

        try {
            Registry registry = LocateRegistry.getRegistry(serverAddress, NetworkConfiguration.RMI_PORT);
            CallableOnDNS dns = (CallableOnDNS) registry.lookup(NetworkConfiguration.DNS_NAME);
            connected = true;
            System.out.println("[RMI] Connected to RMI Server");
            return dns;
        } catch (Exception e) {
            throw new RemoteException("Could not connect to RMI Server: " + e.getMessage());
        }

    }

    private CallableOnDNS setUpSocketConnection() throws IOException {

        SocketClientManager socketClientManager = new SocketClientManager(this);
        socketClientManager.connect();

        return socketClientManager;

    }

    private void leaveGame() {
        System.out.println("VOGLIO USCIRE DAL GIOCO, MA NON POSSO :(");
        /*try {
            if (currentGameId != null) {
                networkManager.leaveGame(currentGameId);
                inGame = false;
                gameStarted = false;
                currentGameId = null;
                view.showMessage("Left the game.");
            }
        } catch (Exception e) {
            view.showError("Error leaving game: " + e.getMessage());
        }*/
    }

    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException {
        view.showMessage(ANSI_BLUE + newPlayerNickname + ANSI_RESET + " joined the game!", STANDARD);
    }

    @Override
    public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) throws RemoteException {
        gameStarted = true;
        clientModel.setGameState(GameState.BUILD_SHIPBOARD);
        gameInfo.getConnectedPlayers().forEach(clientModel::addPlayer);
        view.notifyGameStarted(GameState.BUILD_SHIPBOARD);
        clientModel.setHourglass(new Hourglass(gameInfo.isTestFlight(), this));
        clientModel.getHourglass().start(view, "game");

        // Se la view è di tipo ClientCLIView, possiamo interrompere l'attesa
        if (view instanceof ClientCLIView)
            view.cancelInputWaiting();
    }

    @Override
    public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws RemoteException {
        if (nickname.equals(this.nickname))
            nickname = "you";

        clientModel.getHourglass().start(view, nickname);
    }

    @Override
    public void notifyGameState(String nickname, GameState gameState) throws RemoteException{
        clientModel.setGameState(gameState);
        view.showNewGameState();
    }

    @Override
    public void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws RemoteException{
        clientModel.setCurrDangerousObj(dangerousObj);
        view.showDangerousObj();
    }

    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws RemoteException{
        clientModel.setCurrentPlayer(nickname);
        view.showMessage("Current player is: " + nickname, STANDARD);
    }

    @Override
    public void notifyCurrAdventureCard(String nickname, String adventureCard) throws RemoteException{
        clientModel.setCurrAdventureCard(adventureCard);
        view.showCurrAdventureCard(true);
    }

    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) throws RemoteException{
        clientModel.getVisibleComponents().put(index, component);
    }

    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) throws RemoteException{
        clientModel.getVisibleComponents().remove(index);
    }

    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException {
        clientModel.getShipboardOf(nickname).getShipBoardMatrix()[coordinates.getX()][coordinates.getY()] = component;
    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws RemoteException {
        clientModel.getShipboardOf(nickname).setShipBoardMatrix(shipMatrix);
    }

    @Override
    public void notifyChooseComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws RemoteException {
        clientModel.getShipboardOf(nickname).setFocusedComponent(focusedComponent);
    }

    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws RemoteException {
        clientModel.getShipboardOf(nickname).setFocusedComponent(null);
    }

    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component ) throws RemoteException {
        clientModel.getShipboardOf(nickname).getBookedComponent().add(component);
    }

    public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws RemoteException {
        clientModel.updatePlayerCredits(nickname, credits);
        view.showMessage(nickname + " has " + credits + " credits.", STANDARD);
    }

    @Override
    public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws RemoteException{
        clientModel.updatePlayerPosition(nickname,newPosition);
    }

    @Override
    public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws RemoteException{
        clientModel.eliminatePlayer(nickname);
        view.showMessage(nickname + " was eliminated.", STANDARD);
    }

    @Override
    public void notifyCardState(String nickname, CardState cardState) throws RemoteException {
        clientModel.setCardState(cardState);
        view.showNewCardState();
    }

    @Override
    public void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDecks) throws RemoteException {
        clientModel.setLittleVisibleDeck(littleVisibleDecks);
    }

    /**
     * Initiates the ship board building phase by showing the build menu
     * and executing the selected action on the server using the current nickname.
     */
    public void buildShipBoardPhase() {

        boolean hasPlayerEndedThisPhase = false;

        while(clientModel.getGameState() == GameState.BUILD_SHIPBOARD && !hasPlayerEndedThisPhase) {
            hasPlayerEndedThisPhase = view.showBuildShipBoardMenu()
                    .apply(serverController, nickname);
        }

        while(clientModel.getGameState() == GameState.BUILD_SHIPBOARD) {
            view.showShipBoardsMenu();
        }

    }

    public void cardPhase() {

        while(clientModel.getGameState() == GameState.PLAY_CARD) {
            if (clientModel.isMyTurn())
                clientModel.getCurrCardState().showRelatedMenu(view).accept(serverController, nickname);
        }

    }

    public void notifyHourglassEnded() throws RemoteException {
        serverController.notifyHourglassEnded(nickname);
        if (clientModel.getHourglass().getFlipsLeft() == 0) {
            //TODO
            clientModel.setGameState(GameState.CHECK_SHIPBOARD);
            view.showNewGameState();
        }
    }

}
