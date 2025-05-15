package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.view.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
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

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.polimi.ingsw.is25am33.client.view.MessageType.NOTIFICATION_CRITICAL;
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
    private List<GameInfo> games = new ArrayList<>();

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public ClientController(ClientModel clientModel) throws RemoteException {
        super();
        this.clientModel = clientModel;
    }

    public void run() {
        view.askNickname();
    }

    public List<GameInfo> getGames() {
        return games;
    }

    public void register(String attemptedNickname) {

        try {
            boolean registered = dns.registerWithNickname(attemptedNickname, this);
            if (!registered) {
                view.showError("Nickname already exists");
            } else {
                view.showMessage("Nickname registered successfully!", STANDARD);
                this.nickname = attemptedNickname;
                view.showMainMenu();
            }
        } catch (IOException e) {
            view.showError("Error registering nickname: " + attemptedNickname);
        }

    }

    public void setDns(CallableOnDNS dns) {
        this.dns = dns;
    }

    public CallableOnDNS getDns() {
        return dns;
    }

    public String getNickname() {
        return nickname;
    }

    public void handleCreateGameMenu(int numPlayers, boolean isTestFlight, PlayerColor chosenColor) {
        try {

            if(numPlayers < 2 || numPlayers > 4) {
                view.showError("Number of players must be between 2 and 4");
                view.showMainMenu();
                return;
            }

            GameInfo gameInfo = dns.createGame(chosenColor, numPlayers, isTestFlight, nickname);

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
        } catch (IOException e) {
            view.showError("Error creating game: " + e.getMessage());
            view.showMainMenu();
        }
    }

    /**
     * Joins an existing game.
     */
    public void joinGame(String chosenGameId, PlayerColor chosenColor) {

        try {

            boolean success = dns.joinGame(chosenGameId, nickname, chosenColor);

            if (!success) {
                view.showError("Color already in use");
                return;
            }

            currentGameId = chosenGameId;

            // if the dns was SocketClientManager (e.g., the client is socket), the serverController is the SocketClientManager used as a dns before
            if (dns instanceof SocketClientManager) {
                serverController = (SocketClientManager) dns;
            } else {
                serverController = games.stream().filter(info -> info.getGameId().equals(chosenGameId)).findFirst().orElseThrow().getGameController();
            }
            inGame = true;
            view.showMessage("Successfully joined game!", STANDARD);
            view.showMessage("Waiting for the game to start...", STANDARD);

        } catch (NumberFormatException e) {
            view.showError("Invalid color choice: " + e.getMessage());
            view.showMainMenu();
        } catch (RemoteException e) {
            if(e.getMessage().contains("Client not registered"))
                view.showError("Error joining game: Client not registered");
            else if (e.getMessage().contains("GameModel not found"))
                view.showError("GameModel already started");
            else if (e.getMessage().contains("GameModel already  or deleted"))
                view.showError("Error joining game: GameModel already started");
            else if (e.getMessage().contains("GameModel is full"))
                view.showError("Error joining game: GameModel is full");
            else if (e.getMessage().contains("You are already in this gameModel"))
                view.showError("Error joining game: You are already in this gameModel");
            view.showMainMenu();
        } catch (Exception e) {
            view.showError("Error joining game: " + e.getMessage());
            view.showMainMenu();
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
    public static ClientView selectUserInterface(Scanner scanner, String choice) throws RemoteException {

        return switch (choice) {
            case "cli" -> new ClientCLIView();
            case "gui" -> new ClientGuiController();
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
    public CallableOnDNS selectNetworkProtocol(String choice) {

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
        String serverAddress = "localhost";

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

    public void leaveGame() {
        System.out.println("VOGLIO USCIRE DAL GIOCO, MA NON POSSO :(");
        try {
            if (currentGameId != null) {
                serverController.leaveGame(nickname);
                inGame = false;
                gameStarted = false;
                currentGameId = null;
                view.showMessage("Left the game.", NOTIFICATION_CRITICAL);
            }
        } catch (Exception e) {
            view.showError("Error leaving game: " + e.getMessage());
        }
    }

    @Override
    public void notifyGameInfos(String nickname, List<GameInfo> gameInfos) {
        this.games = gameInfos;
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
    public void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws RemoteException {

    }

    @Override
    public void notifyRemovalResult(String nicknameToNotify, boolean success) throws RemoteException {

    }

    @Override
    public void notifyInvalidShip(String nicknameToNotify) throws RemoteException {
        view.showInvalidShipBoardMenu();
    }

    @Override
    public void notifyShipCorrect(String nicknameToNotify) throws RemoteException {

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
        clientModel.getShipboardOf(nickname).getShipMatrix()[coordinates.getX()][coordinates.getY()] = component;
    }


    public void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException {
        //clientModel.getShipboardOf(nickname).getIncorrectlyPositionedComponentsCoordinates().add(component);
    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws RemoteException {
        clientModel.getShipboardOf(nickname).setShipMatrix(shipMatrix);
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
        clientModel.getShipboardOf(nickname).getBookedComponents().add(component);
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
