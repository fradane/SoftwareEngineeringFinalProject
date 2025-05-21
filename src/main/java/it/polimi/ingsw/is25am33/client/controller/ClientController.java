package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
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
import it.polimi.ingsw.is25am33.client.model.Hourglass;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;

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
    private final ObservableList<GameInfo> observableGames = FXCollections.observableArrayList();

    private List<Set<Coordinates>> currentShipPartsList = new ArrayList<>();

    public ClientController(ClientModel clientModel) throws RemoteException {
        super();
        this.clientModel = clientModel;
    }

    public void run() {
        view.askNickname();
    }

    public List<GameInfo> getGames() {
        return new ArrayList<>(observableGames);
    }

    public ObservableList<GameInfo> getObservableGames() {
        return observableGames;
    }

    public String getCurrentGameId() {
        return currentGameId;
    }

    public ClientModel getClientModel() {
        return clientModel;
    }

    public void register(String attemptedNickname) {

        try {
            boolean registered = dns.registerWithNickname(attemptedNickname, this);
            if (!registered) {
                view.showError("Nickname already exists");
            } else {
                view.showMessage("Nickname registered successfully!", STANDARD);
                this.nickname = attemptedNickname;
                clientModel.setNickname(nickname);
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

    /**
     * Handles the creation of a new game based on the specified parameters.
     * It verifies the validity of the number of players, creates the game using
     * the provided parameters, and updates the game state accordingly. Errors
     * encountered during game creation are communicated to the user.
     *
     * @param numPlayers the number of players for the new game; must be between 2 and 4
     * @param isTestFlight a flag indicating whether the game is in test flight mode
     * @param chosenColor the color chosen by the player for the game
     */
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
     * Attempts to join a selected game with the given game ID and chosen player color.
     * It handles the connection process, updates the game state, and manages error cases.
     *
     * @param chosenGameId the identifier of the game to join
     * @param chosenColor the color chosen by the player for the game
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
                List<GameInfo> games = new ArrayList<>(observableGames);
                serverController = games.stream().filter(info -> info.getGameId().equals(chosenGameId)).findFirst().orElseThrow().getGameController();
            }
            inGame = true;
            view.showWaitingForPlayers();

        } catch (NumberFormatException e) {
            view.showError("Invalid color choice: " + e.getMessage());
            view.showMainMenu();
        } catch (RemoteException e) {
            if(e.getMessage().contains("Client not registered"))
                view.showError("Error joining game: Client not registered");
            else if (e.getMessage().contains("GameModel not found"))
                view.showError("GameModel already started");
            else if (e.getMessage().contains("GameModel already started"))
                view.showError("Error joining game: GameModel already started or deleted");
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
    public static ClientView selectUserInterface(Scanner scanner, String choice) throws IOException {

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

    private CallableOnDNS setUpRMIConnection() throws IOException {
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
        try {
            if (currentGameId != null) {
                serverController.leaveGame(nickname);
                inGame = false;
                gameStarted = false;
                currentGameId = null;
                System.exit(0);
            }
        } catch (Exception e) {
            view.showError("Error leaving game: " + e.getMessage());
        }
    }

    @Override
    public void notifyShipCorrect(String nicknameToNotify) throws IOException {
        // TODO rimuovere
    }

    @Override
    public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws RemoteException {
        observableGames.clear();
        observableGames.addAll(gameInfos);
    }

    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException {
        view.showMessage(newPlayerNickname + " joined the game!", NOTIFICATION_INFO);
    }

    @Override
    public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) throws IOException {
        gameStarted = true;
        clientModel.setGameState(GameState.BUILD_SHIPBOARD);
        gameInfo.getConnectedPlayers().forEach((nickname, color) -> clientModel.addPlayer(nickname, color, gameInfo.isTestFlight(), view instanceof ClientGuiController));
        view.notifyGameStarted(GameState.BUILD_SHIPBOARD);
        view.showBuildShipBoardMenu();
        clientModel.setHourglass(new Hourglass(gameInfo.isTestFlight(), this));
        clientModel.getHourglass().start(view, "game");
    }

    @Override
    public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException {
        if (nickname.equals(this.nickname))
            nickname = "you";

        clientModel.getHourglass().start(view, nickname);
    }

    @Override
    public void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws IOException {

    }

    @Override
    public void notifyRemovalResult(String nicknameToNotify, boolean success) throws IOException {

    }

    @Override
    public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);

        // Mostra il menu solo se è la propria shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            view.showInvalidShipBoardMenu();
        }
    }

    @Override
    public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);

        // Mostra il menu solo se è la propria shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            view.showValidShipBoardMenu();
        }
    }

    @Override
    public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);


        // Gestisce la selezione solo se è la propria shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            setCurrentShipPartsList(shipParts);
            view.showChooseShipPartsMenu(currentShipPartsList);
        }
    }

    public void setCurrentShipPartsList(Set<Set<Coordinates>> shipParts) {
        this.currentShipPartsList = new ArrayList<>(shipParts);
    }


    @Override
    public void notifyGameState(String nickname, GameState gameState) throws IOException{
        clientModel.setGameState(gameState);
        view.showNewGameState();
    }

    @Override
    public void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws IOException{
        clientModel.setCurrDangerousObj(dangerousObj);
        view.showDangerousObj();
    }

    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException{
        clientModel.setCurrentPlayer(nickname);
        view.showMessage("Current player is: " + nickname, STANDARD);
    }

    @Override
    public void notifyCurrAdventureCard(String nickname, String adventureCard) throws IOException{
        clientModel.setCurrAdventureCard(adventureCard);
        view.showCurrAdventureCard(true);
    }

    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) throws IOException{
        clientModel.getVisibleComponents().put(index, component);
    }

    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) throws IOException{
        clientModel.getVisibleComponents().remove(index);
    }

    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException {
        clientModel.getShipboardOf(nickname).getShipMatrix()[coordinates.getX()][coordinates.getY()] = component;
        clientModel.getShipboardOf(nickname).getShipBoardAdapter().refreshMatrix();
    }

    // TODO marco, controllare
    public void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException {
        //clientModel.getShipboardOf(nickname).getIncorrectlyPositionedComponentsCoordinates().add(component);
    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws IOException {
        clientModel.getShipboardOf(nickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(nickname).getShipBoardAdapter().refreshMatrix();
    }

    /**
     * Notifies the client that the player called nickname has the focus on a specific component.
     *
     * @param nicknameToNotify the nickname of the player to be notified
     * @param nickname the nickname of the player who selected the component
     * @param focusedComponent the component that is being focused on
     * @throws RemoteException if a communication-related error occurs during the remote method call
     */
    @Override
    public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {
        clientModel.getShipboardOf(nickname).setFocusedComponent(focusedComponent);
        clientModel.getShipboardOf(nickname).getShipBoardAdapter().refreshMatrix();
    }

    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException {
        clientModel.getShipboardOf(nickname).setFocusedComponent(null);
    }

    @Override
    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component ) throws IOException {
        clientModel.getShipboardOf(nickname).getBookedComponents().add(component);
    }

    @Override
    public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException {
        clientModel.updatePlayerCredits(nickname, credits);
        view.showMessage(nickname + " has " + credits + " credits.", STANDARD);
    }

    @Override
    public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException{
        clientModel.updatePlayerPosition(nickname,newPosition);
    }

    @Override
    public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException{
        clientModel.eliminatePlayer(nickname);
        view.showMessage(nickname + " was eliminated.", STANDARD);
    }

    @Override
    public void notifyCardState(String nickname, CardState cardState) throws IOException {
        clientModel.setCardState(cardState);
        view.showNewCardState();
    }

    @Override
    public void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDecks) throws IOException {
        clientModel.setLittleVisibleDeck(littleVisibleDecks);
    }

    @Override
    public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayerNickname) throws IOException {
        view.showMessage(disconnectedPlayerNickname + " disconnected.", ERROR);
        view.showMessage("GAME ENDED", STANDARD);
        leaveGame();
    }

    public void cardPhase() {

        while(clientModel.getGameState() == GameState.PLAY_CARD) {
            if (clientModel.isMyTurn())
                clientModel.getCurrCardState().showRelatedMenu(view).accept(serverController, nickname);
        }

    }

    public void notifyHourglassEnded() throws IOException {
        serverController.notifyHourglassEnded(nickname);
        if (clientModel.getHourglass().getFlipsLeft() == 0) {
            //TODO
            //clientModel.setGameState(GameState.CHECK_SHIPBOARD);
            //view.showNewGameState();
        }
    }

    public void pickRandomComponent() {
        try {
            serverController.playerPicksHiddenComponent(nickname);
            view.showPickedComponentAndMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }


    public void reserveFocusedComponent() {
        try {
            // TODO aggiungere il caso in cui non si possa piu riservare
            // PS gli aggiornamenti sul modello di chi fa l'azione li gestiscono le notify o chi fa l'azione?
            // perche se io qua faccio book anche sul mio client model e poi mi arriva la notifica ne aggiungo due
            //((Level2ShipBoard) clientModel.getShipboardOf(nickname)).book();
            serverController.playerWantsToReserveFocusedComponent(nickname);
            view.showBuildShipBoardMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void releaseFocusedComponent() {
        try {
            clientModel.getShipboardOf(nickname).releaseFocusedComponent();
            serverController.playerWantsToReleaseFocusedComponent(nickname);
            view.showBuildShipBoardMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    private void handleRemoteException(IOException e) {
        System.err.println("Remote exception: " + e);
        System.exit(1);
    }

    public void showShipBoard(String nickname) {
        clientModel.getPlayerClientData()
                .keySet()
                .stream()
                .filter(player -> player.equals(nickname))
                .findFirst()
                .ifPresentOrElse(player -> view.showShipBoard(clientModel.getShipboardOf(player), nickname),
                        () -> view.showMessage("Player not found\n", STANDARD));
    }

    public void placeFocusedComponent(int row, int column) {
        try {
            row--;
            column--;
            clientModel.getShipboardOf(nickname).checkPosition(row, column);
            serverController.playerWantsToPlaceFocusedComponent(nickname, new Coordinates(row, column), clientModel.getShipboardOf(nickname).getFocusedComponent().getRotation());
            view.showBuildShipBoardMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        } catch (IllegalArgumentException e) {
            view.showMessage("Invalid coordinates: " + e.getMessage() + "\n", ERROR);
        }
    }

    public void removeComponent(int row, int column) {
        try{
            row --;
            column--;
            serverController.playerWantsToRemoveComponent(nickname, new Coordinates(row, column));
        }catch (IllegalArgumentException e) {
            view.showMessage("Invalid coordinates: " + e.getMessage() + "\n", ERROR);
        }catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void removeShipPart(Set<Coordinates> shipPart) {
        try{
            serverController.playerChoseShipPart(nickname, shipPart);
        }catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void pickVisibleComponent(int chosenIndex) {

        try {
            if (clientModel.getVisibleComponents().containsKey(chosenIndex)) {
                serverController.playerPicksVisibleComponent(nickname, chosenIndex);
                view.showPickedComponentAndMenu();
            } else {
                view.showMessage("This component is no longer available, someone stole it.", STANDARD);
                view.showBuildShipBoardMenu();
            }
        } catch (IOException e) {
            handleRemoteException(e);
        }

    }

    public void restartHourglass() {

        if (clientModel.getHourglass().isRunning()) {
            view.showMessage("The hourglass is already running, please wait for it to end.", STANDARD);
            view.showBuildShipBoardMenu();
            return;
        }

        try {
            serverController.playerWantsToRestartHourglass(nickname);
        } catch (IOException e) {
            switch (e.getMessage()) {
                case "No more flips available.",
                     "Interrupted while waiting for all clients to finish the timer.",
                     "Another player is already restarting the hourglass. Please wait.":
                    view.showMessage(e.getMessage(), STANDARD);
                    break;

                default:
                    handleRemoteException(e);
            }
        } finally {
            view.showBuildShipBoardMenu();
        }

    }

    /**
     * Allows the player to select a reserved component based on their choice input.
     * It validates the input, manages reserved components, and informs the server
     * about the selected component to focus on. Displays appropriate views and messages
     * based on the player's actions or errors encountered.
     *
     * @param choice the index of the reserved component to pick; must be a valid
     *               non-negative integer within the range of available reserved
     *               components. Value is decremented internally to match list indexing.
     */
    public void pickReservedComponent(int choice) {
        List<Component> bookedComponent = clientModel.getShipboardOf(nickname).getBookedComponents();
        choice--;   // decrement choice to match list indexing

        if (bookedComponent.isEmpty()) {
            view.showMessage("You have no reserved components.", STANDARD);
            view.showBuildShipBoardMenu();
            return;
        }

        if (choice == -1) {
            view.showBuildShipBoardMenu();
            return;
        }

        if (choice < 0 || choice >= bookedComponent.size()) {
            view.showMessage("Invalid choice. You only have " + bookedComponent.size() + " reserved components", STANDARD);
            view.showBuildShipBoardMenu();
            return;
        }

        try {
            Component component = clientModel.getShipboardOf(nickname).getBookedComponents().remove(choice);
            clientModel.getShipboardOf(nickname).setFocusedComponent(component);
            // TODO da sostituire con:
            // ((Level2ShipBoard) clientModel.getShipboardOf(nickname)).focusReservedComponent(choice);

            serverController.playerWantsToFocusReservedComponent(nickname, choice);
            view.showPickedComponentAndMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void handleShipPartSelection(int choice) {
        try {
            if (choice >= 1 && choice <= currentShipPartsList.size()) {
                Set<Coordinates> selectedShipPartToKeep = currentShipPartsList.get(choice - 1);
                serverController.playerChoseShipPart(nickname, selectedShipPartToKeep);
                currentShipPartsList.clear();
            } else {
                view.showError("Invalid choice. Please enter a number between 1 and " + currentShipPartsList.size());
            }
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }
}
