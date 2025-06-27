package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.client.ClientPingPongManager;
import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.CrewMalusCard;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.client.model.Hourglass;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;

public class ClientController extends UnicastRemoteObject implements CallableOnClientController {

    private ClientView view;
    private CallableOnDNS dns;
    private CallableOnGameController serverController;
    private GameInfo currentGameInfo;
    private boolean inGame = false;
    private String nickname;
    boolean gameStarted = false;
    private final ClientModel clientModel;
    private final List<GameInfo> observableGames = new ArrayList<>();
    private boolean isTestFlight;
    private final ClientPingPongManager clientPingPongManager;

    private List<Set<Coordinates>> currentShipPartsList = new ArrayList<>();

    public ClientController(ClientModel clientModel, ClientPingPongManager clientPingPongManager) throws RemoteException {
        super();
        this.clientModel = clientModel;
        this.clientPingPongManager = clientPingPongManager;
    }

    public void run() {
        view.askNickname();
    }

    public List<GameInfo> getGames() {
        return new ArrayList<>(observableGames);
    }

    public List<GameInfo> getObservableGames() {
        return observableGames;
    }

    public String getCurrentGameId() {
        return currentGameInfo.getGameId();
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
                new Thread(()->{
                    clientPingPongManager.start(
                            ()-> {
                                try {
                                    pingToServerFromClient(nickname);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                }).start();

                this.nickname = attemptedNickname;
                clientModel.setNickname(nickname);
                view.showMainMenu();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
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
            currentGameInfo = gameInfo;
            this.isTestFlight = gameInfo.isTestFlight();
            view.setIsTestFlight(this.isTestFlight);
            inGame = true;

            view.notifyGameCreated(gameInfo.getGameId());
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
    public boolean joinGame(String chosenGameId, PlayerColor chosenColor) {

        try {

            boolean success = dns.joinGame(chosenGameId, nickname, chosenColor);

            if (!success) {
                view.showError("Error joining game");
                view.showMainMenu();
                return false;
            }

            // if the dns was SocketClientManager (e.g., the client is socket), the serverController is the SocketClientManager used as a dns before
            if (dns instanceof SocketClientManager) {
                serverController = (SocketClientManager) dns;
            } else {
                List<GameInfo> games = new ArrayList<>(observableGames);
                serverController = games.stream().filter(info -> info.getGameId().equals(chosenGameId)).findFirst().orElseThrow().getGameController();
            }
            inGame = true;
            view.showWaitingForPlayers();
            currentGameInfo = observableGames.stream().filter(info -> info.getGameId().equals(chosenGameId)).findFirst().orElseThrow();
            isTestFlight = currentGameInfo.isTestFlight();
            view.setIsTestFlight(isTestFlight);

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

        return true;
    }

    public void setView(ClientView view) {
        this.view = view;
    }

    /**
     * Allows the user to select the user interface
     * @param scanner Scanner to read user input
     * @return The chosen implementation of ClientView
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
     * Allows the user to select the network protocol
     * @return The chosen implementation of NetworkManager
     */
    public CallableOnDNS selectNetworkProtocol(boolean isRmi, String serverAddress, int serverPort) {

        try {
            return isRmi ?
                    this.setUpRMIConnection(serverAddress, serverPort) :
                    this.setUpSocketConnection(serverAddress, serverPort);
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

    private CallableOnDNS setUpRMIConnection(String serverAddress, int serverPort) throws IOException {

        try {
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort);
            CallableOnDNS dns = (CallableOnDNS) registry.lookup(NetworkConfiguration.DNS_NAME);
            System.out.println("[RMI] Connected to RMI Server");
            return dns;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number parameter: " + e.getMessage());
        } catch (Exception e) {
            throw new RemoteException("Could not connect to RMI Server: " + e.getMessage());
        }

        return null;
    }

    private CallableOnDNS setUpSocketConnection(String serverAddress, int serverPort) throws IOException {

        SocketClientManager socketClientManager = new SocketClientManager(this);
        socketClientManager.connect(serverAddress, serverPort);

        return socketClientManager;

    }

    public void leaveGame() {
        try {
            if (currentGameInfo != null) {
                inGame = false;
                gameStarted = false;
                currentGameInfo = null;
                serverController.leaveGameAfterCreation(nickname,true);
                System.exit(0);
            }
            else {
                dns.leaveGameBeforeCreation(nickname);
                System.exit(0);
            }
        } catch (Exception e) {
            view.showError("Error leaving game: " + e.getMessage());
        }
    }

    @Override
    public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws RemoteException {
        observableGames.clear();
        observableGames.addAll(gameInfos);
        view.refreshGameInfos(gameInfos);
    }

    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) {
        view.showMessage(newPlayerNickname + " joined the game!", NOTIFICATION_INFO);
    }

    @Override
    public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) {
        gameStarted = true;
        clientModel.setGameState(GameState.BUILD_SHIPBOARD);
        gameInfo.getConnectedPlayers().forEach((nickname, color) -> clientModel.addPlayer(nickname, color, gameInfo.isTestFlight(), view instanceof ClientGuiController));
        view.notifyGameStarted(GameState.BUILD_SHIPBOARD);
        currentGameInfo = gameInfo;
        view.showBuildShipBoardMenu();

        if (!gameInfo.isTestFlight()) {
            clientModel.setHourglass(new Hourglass(this));
            clientModel.getHourglass().start(view, "game");
        }

    }

    @Override
    public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) {
        if (nickname.equals(this.nickname))
            nickname = "you";

        clientModel.getHourglass().start(view, nickname);
    }

    @Override
    public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);
        clientModel.getShipboardOf(shipOwnerNickname).setComponentsPerType(componentsPerType);
        clientModel.refreshShipBoardOf(shipOwnerNickname);

        // Shows the menu only if it is your own shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            view.showInvalidShipBoardMenu();
        }
    }

    @Override
    public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);
        clientModel.getShipboardOf(shipOwnerNickname).setComponentsPerType(componentsPerType);
        clientModel.refreshShipBoardOf(shipOwnerNickname);

        // Shows the menu only if it is your own shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            view.showValidShipBoardMenu();
        }
    }

    @Override
    public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);
        clientModel.getShipboardOf(shipOwnerNickname).setComponentsPerType(componentsPerType);
        clientModel.refreshShipBoardOf(shipOwnerNickname);

        // Handles the selection only if it is your own shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            setCurrentShipPartsList(shipParts);
            view.showChooseShipPartsMenu(currentShipPartsList);
        }
    }

    public void setCurrentShipPartsList(Set<Set<Coordinates>> shipParts) {
        this.currentShipPartsList = new ArrayList<>(shipParts);
    }

    @Override
    public void notifyGameState(String nickname, GameState gameState) {
        clientModel.setGameState(gameState);
        view.showNewGameState();
    }

    @Override
    public void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) {
        clientModel.setCurrDangerousObj(dangerousObj);
        //view.showDangerousObj();
    }

    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) {
        clientModel.setCurrentPlayer(nickname);
        view.showMessage("Current player is: " + nickname, STANDARD);

//        if(clientModel.isMyTurn())
//            view.showNewCardState();
    }

    @Override
    public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) {
        new Thread(() -> {
            clientModel.setCurrAdventureCard(adventureCard);
            view.showCurrAdventureCard(isFirstTime);
        }).start();
    }

    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) {
        clientModel.getVisibleComponents().put(index, component);
        clientModel.refreshVisibleComponents();
    }

    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) {
        clientModel.getVisibleComponents().remove(index);
        clientModel.refreshVisibleComponents();
    }

    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getShipboardOf(nickname);
        shipboard.setFocusedComponent(component);
        shipboard.placeComponentWithFocus(coordinates.getX(), coordinates.getY());
        clientModel.refreshShipBoardOf(nickname);
    }

    /**
     * Notifies the client that the player called nickname has the focus on a specific component.
     *
     * @param nicknameToNotify the nickname of the player to be notified
     * @param nickname the nickname of the player who selected the component
     * @param focusedComponent the component that is being focused on
     */
    @Override
    public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component focusedComponent) {
        clientModel.getShipboardOf(nickname).setFocusedComponent(focusedComponent);
        clientModel.refreshShipBoardOf(nickname);
    }

    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) {
        clientModel.getShipboardOf(nickname).setFocusedComponent(null);
        clientModel.refreshShipBoardOf(nickname);
    }

    @Override
    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException {
        clientModel.getShipboardOf(nickname).getBookedComponents().add(component);
        clientModel.getShipboardOf(nickname).setFocusedComponent(null);
        clientModel.refreshShipBoardOf(nickname);
    }

    @Override
    public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) {
        int oldCredits = clientModel.getPlayerClientData().get(nickname).getCredits();
        clientModel.updatePlayerCredits(nickname, credits);
        if(clientModel.getMyNickname().equals(nickname)) {
            view.showMessage("You have just earned " + (credits - oldCredits) + " credits\n", STANDARD);
            view.showMessage("You now own " + credits + " credits\n", STANDARD);
        } else
            view.showMessage(nickname + " has " + credits + " credits.", STANDARD);
    }

    @Override
    public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) {
        clientModel.updatePlayerPosition(nickname,newPosition);
        view.showMessage("\n" + nickname + " has changed its position", NOTIFICATION_INFO);
        view.showCurrentRanking();
        clientModel.refreshRanking();
    }

    @Override
    public void notifyStopHourglass(String nicknameToNotify) {
        clientModel.getHourglass().stop();
    }

    @Override
    public void notifyFirstToEnter(String nicknameToNotify) {
        view.showFirstToEnter();
    }

    @Override
    public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException {
        clientModel.setCurrAdventureCard(adventureCard);
    }

    @Override
    public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException {
        if(!clientModel.isMyTurn())
            view.showMessage(clientModel.getCurrentPlayer() + " just visited a planet", NOTIFICATION_INFO);
    }

    @Override
    public void notifyCrewPlacementPhase(String nicknameToNotify) throws IOException {
        clientModel.setGameState(GameState.PLACE_CREW);
        view.showCrewPlacementMenu();
    }

    @Override
    public void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException {
        clientModel.getShipboardOf(playerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(playerNickname).setComponentsPerType(componentsPerType);
        clientModel.refreshShipBoardOf(playerNickname);

        if (playerNickname.equals(nickname)) {
            view.showMessage("Your crew placement is complete!", STANDARD);
        } else {
            view.showMessage(playerNickname + " has completed crew placement.", NOTIFICATION_INFO);
        }
    }

    public void submitCrewChoices(Map<Coordinates, CrewMember> choices) {
        try {
            // Local validation
            validateCrewChoices(choices);

            // Send to server
            serverController.submitCrewChoices(nickname, choices);
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            view.showCrewPlacementMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    private void validateCrewChoices(Map<Coordinates, CrewMember> choices) {
        // Check at most 1 alien per color
        long purpleCount = choices.values().stream().filter(c -> c == CrewMember.PURPLE_ALIEN).count();
        long brownCount = choices.values().stream().filter(c -> c == CrewMember.BROWN_ALIEN).count();

        if (purpleCount > 1) {
            throw new IllegalArgumentException("You can have at most 1 purple alien");
        }
        if (brownCount > 1) {
            throw new IllegalArgumentException("You can have at most 1 brown alien");
        }
    }

    @Override
    public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) {
        clientModel.eliminatePlayer(nickname);
        clientModel.refreshRanking();
        view.showMessage(nickname + " was eliminated.", STANDARD);
        view.showPlayerEarlyLanded(nickname);
    }

    @Override
    public void notifyCardState(String nickname, CardState cardState) {
        if (isStateRegardingCurrentPlayerOnly(cardState)) {
            if (!clientModel.isMyTurn()) {
                view.showMessage(clientModel.getCurrentPlayer() + " is currently playing. Soon will be your turn\n", NOTIFICATION_INFO);
                return;
            }
        }

        clientModel.setCardState(cardState);
        view.showNewCardState();

        //TODO probabilemnte da rimuovere perchè inutile
        if (!clientModel.isMyTurn() && cardState != CardState.START_CARD && cardState != CardState.END_OF_CARD && cardState != CardState.STARDUST ) {
            view.showMessage("Wait for " + clientModel.getCurrentPlayer() + " to make his choice", NOTIFICATION_INFO);
        }

    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException{
        clientModel.getShipboardOf(nickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(nickname).setComponentsPerType(componentsPerType);
        clientModel.refreshShipBoardOf(nickname);
    }

    public void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws IOException{
        view.showComponentHitInfo(coordinates);
    }

    public  void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations){
        view.showMessage(nicknameAndMotivations, STANDARD);
    }

    @Override
    public void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {
        view.showMessage("Cannot book more than 2 components", ERROR);
    }

    @Override
    public void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException {
        clientModel.getShipboardOf(nickname).setNotActiveComponents(notActiveComponents);
    }

    public void evaluatedCrewMembers(){
        try {
            serverController.evaluatedCrewMembers(nickname);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    //Set of states that should not be notified unless you are the current player
    public boolean isStateRegardingCurrentPlayerOnly(CardState cardState) {
        //TODO figure out which other states fall into this category and add them below. Probably to be removed as all states are likely RegardingCurrentPlayerOnly
        return cardState == CardState.HANDLE_CUBES_REWARD
                || cardState == CardState.CHOOSE_PLANET
                || cardState == CardState.VISIT_LOCATION
                || cardState == CardState.REMOVE_CREW_MEMBERS
                || cardState == CardState.CHOOSE_ENGINES
                || cardState == CardState.DANGEROUS_ATTACK
                || cardState == CardState.CHECK_SHIPBOARD_AFTER_ATTACK
                || cardState == CardState.ACCEPT_THE_REWARD
                || cardState == CardState.CHOOSE_CANNONS
                || cardState == CardState.EPIDEMIC
                || cardState == CardState.STARDUST
                || cardState == CardState.EVALUATE_CREW_MEMBERS;

    }

    @Override
    public void notifyVisibleDeck(String nickname, List<List<ClientCard>> littleVisibleDecks) {
        clientModel.setLittleVisibleDeck(littleVisibleDecks);
    }

    @Override
    public void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType ){
        ShipBoardClient shipBoardClient = clientModel.getShipboardOf(playerNickname);
        shipBoardClient.setComponentsPerType(componentsPerType);
    }

    @Override
    public void notifyNoMoreHiddenComponents(String nicknameToNotify) throws IOException {
        view.showNoMoreHiddenComponents();
    }

    @Override
    public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayerNickname) {
        view.showMessage(disconnectedPlayerNickname + " disconnected.", ERROR);
        view.showMessage("GAME ENDED", STANDARD);
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
            if(clientModel.getShipboardOf(nickname).getBookedComponents().size() >= 2) {
                view.showMessage("You cannot book more than 2 components", ERROR);
                view.showPickedComponentAndMenu();
            }
            else {
                serverController.playerWantsToReserveFocusedComponent(nickname);
                view.showBuildShipBoardMenu();
            }
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

    public void handleRemoteException(IOException e) {
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

    public void showCubes(String nickname) {
        clientModel.getPlayerClientData()
                .keySet()
                .stream()
                .filter(player -> player.equals(nickname))
                .findFirst()
                .ifPresentOrElse(player -> view.showCubes(clientModel.getShipboardOf(player), nickname),
                        () -> view.showMessage("Player not found\n", STANDARD));
    }



    public void placeFocusedComponent(int row, int column) {
        try {
            ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
            shipBoard.checkPosition(row, column);

            serverController.playerWantsToPlaceFocusedComponent(nickname, new Coordinates(row, column), shipBoard.getFocusedComponent().getRotation());
            view.showBuildShipBoardMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        } catch (IllegalArgumentException e) {
            view.showMessage("Invalid coordinates: " + e.getMessage() + "\n", ERROR);
        }
    }

    public void removeComponent(int row, int column) {
        try {
            serverController.playerWantsToRemoveComponent(nickname, new Coordinates(row, column));
        } catch (IllegalArgumentException e) {
            view.showMessage("Invalid coordinates: " + e.getMessage() + "\n", ERROR);
        } catch (IOException e) {
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
            //Component component = clientModel.getShipboardOf(nickname).getBookedComponents().remove(choice);
            Component component = clientModel.getShipboardOf(nickname).getBookedComponents().get(choice);
            clientModel.getMyShipboard().setFocusedComponent(component);
            // TODO to be replaced: add the case where you can no longer reserve
            //((Level2ShipBoard) clientModel.getShipboardOf(nickname)).focusReservedComponent(choice);
            serverController.playerWantsToFocusReservedComponent(nickname, choice);

            clientModel.refreshShipBoardOf(nickname);
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

    //sends the ping from the client to the server
    public void pingToServerFromClient(String nickname) throws IOException{
        dns.pingToServerFromClient(nickname);
    }

    //when I receive the pong reply from the server, I reset the timeout
    public void pongToClientFromServer(String nickname) throws IOException{
        //System.out.println("Pong dal server");
        clientPingPongManager.onPongReceived(this::handleDisconnection);
    }

    //when the server sends the ping to the client
    public void pingToClientFromServer(String nickname) throws IOException{
        //System.out.println("Ping dal server");
        dns.pongToServerFromClient(nickname);
    }

    public void handleDisconnection() {
        view.showDisconnectMessage("DISCONNECTION: No pong received from server");
    }

    public void forcedDisconnection(String nicknameToNotify, String gameId) throws IOException {
        leaveGame();
    }

    public void endBuildShipBoardPhase() {
        try {
            view.showMessage("""
                    Your ship is ready, now wait for other player to finish theirs, they are sooooooo slow.
                    Anyway use the <show> command as before to see any shipboard or <rank> to see the current ranking.
                    >\s""", ASK);
            serverController.playerEndsBuildShipBoardPhase(nickname);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void placePawn() {
        try {
            serverController.playerPlacesPawn(nickname);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public GameInfo getCurrentGameInfo() {
        return currentGameInfo;
    }


    public void playerWantsToVisitLocation(String nickname, Boolean choice){
        if (!clientModel.isMyTurn()) {
            view.showMessage("This is not your turn, please wait for others to choose...", ERROR);
            return;
        }

        // Handle both AbandonedShip and AbandonedStation
        ClientCard currentCard = clientModel.getCurrAdventureCard();
        int crewRequirement = 0;

        if (currentCard instanceof CrewMalusCard) {
            crewRequirement = ((CrewMalusCard) currentCard).getCrewMalus();
        }

        int totalCrew = clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size();

        if (choice==true && totalCrew < crewRequirement) {
            view.showMessage("You only have " + totalCrew + " crew members. you cannot visit the location", ERROR);
            view.showMessage("Your choice has been automatically set to false", STANDARD);
            choice=false;
        }

        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setWantsToVisit(choice)
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerWantsToThrowDices(String nickname){
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords){
        if(doubleEnginesCoords.size()!=batteryBoxesCoords.size())
            throw new IllegalArgumentException("the number of engines does not match the number of batteries");

        if(!clientModel.isMyTurn()) {
            view.showMessage("This is not your turn, please wait for others to chose ...", ERROR);
            return;
        }

        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleEngines(doubleEnginesCoords)
                .setChosenBatteryBoxes(batteryBoxesCoords)
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) {

        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleCannons(doubleCannonsCoords)
                .setChosenBatteryBoxes(batteryBoxesCoords)
                .build();

        try {
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public boolean checkCabinSelection(String nickname, List<Coordinates> cabinCoords) {
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();

        if (shipBoard.getCrewMembers().size() <= card.getCrewMalus() && cabinCoords.size() < shipBoard.getCrewMembers().size()) {
            view.showMessage("You must select all your crew members", ERROR);
            return false;
        }

        if (cabinCoords.size()<card.getCrewMalus() && shipBoard.getCrewMembers().size() > card.getCrewMalus() ) {
            view.showMessage("Not the right amount of crew members", ERROR);
            return false;
        }

        List<Cabin> cabins = cabinCoords
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        for (Cabin uniqueCabin : cabins.stream().distinct().toList()) {
            long count = cabins.stream()
                    .filter(c -> c == uniqueCabin)
                    .count();

            if (count > uniqueCabin.getInhabitants().size()) {
                view.showMessage("You have selected a cabin more times than its actual crewMember occupancy", ERROR);
                return false;
            }
        }

        return true;
    }

    public void playerChoseCabins(String nickname, List<Coordinates> cabinCoords) {
        try {
            PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenCabins(cabinCoords)
                    .build();
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void playerWantsToVisitPlanet(String nickname, int choice) {
        if (!clientModel.isMyTurn()) {
            view.showMessage("This is not your turn, please wait for others to chose ...", ERROR);
            return;
        }
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenPlanetIndex(choice)
                .build();

        try {
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void playerWantsToAcceptTheReward(String nickname, Boolean choice){
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(choice)
                .build();

        try {
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords){
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);

        PlayerChoicesDataStructure playerChoiceDataStructure;

        playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxCoords)
                .setChosenShield(shieldCoords)
                .build();

        try {
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        } catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords){
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);

        PlayerChoicesDataStructure playerChoiceDataStructure;

        playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxCoords)
                .setChosenDoubleCannons(doubleCannonCoords)
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerHandleBigShot(String nickname){
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerChoseStorageAndBattery(String nickname, List<Coordinates> storageCoords, List<Coordinates> batteryBoxCoords){
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);

        PlayerChoicesDataStructure playerChoiceDataStructure;

        playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxCoords)
                .setChosenStorage(storageCoords)
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void playerChoseStorage(String nickname, List<Coordinates> storageCoords){
        ShipBoardClient shipBoard = clientModel.getShipboardOf(nickname);
        List<Storage> storages = new ArrayList<>();

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(storageCoords)
                .build();

        try {
            serverController.handleClientChoice(nickname, choice);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    public void spreadEpidemic(String nickname){
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .build();

        try{
            serverController.handleClientChoice(nickname, playerChoiceDataStructure);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void stardustEvent(String nickname){
        try{
            serverController.stardustEvent(nickname);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void startCheckShipBoardAfterAttack(String nickname, Coordinates coordinates) {
        try {
            serverController.startCheckShipBoardAfterAttack(nickname);
        } catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void requestPrefabShipsList() {
        try {
            // Request the list asynchronously
            view.showMessage("Requesting prefabricated ships list...", STANDARD);
            serverController.requestPrefabShips(nickname);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    @Override
    public void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws IOException {
        // Stores the available ships in the model
        clientModel.setAvailablePrefabShips(prefabShips);

        // Shows the menu with available ships
        view.showPrefabShipsMenu(prefabShips);
    }

    @Override
    public void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipInfo) throws IOException {
        if (!playerNickname.equals(nickname)) {
            view.showMessage(playerNickname + " has selected a prefabricated ship: " + prefabShipInfo.getName(), NOTIFICATION_INFO);
        }
    }

    public void selectPrefabShip(String prefabShipId) {
        try {
            view.showMessage("Requesting prefab ship selection...", STANDARD);
            serverController.requestSelectPrefabShip(nickname, prefabShipId);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    @Override
    public void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws IOException {
        if (success) {
            view.showMessage("Prefab ship selected successfully! Waiting for other players...", STANDARD);
            // Update the state if necessary
        } else {
            view.showError("Failed to select prefab ship: " + errorMessage);
            view.showBuildShipBoardMenu();
        }
    }

    @Override
    public void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws IOException {
        view.showInfectedCrewMembersRemoved(cabinCoordinatesWithNeighbors);
    }

    @Override
    public void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) throws IOException {
        view.showEndGameInfo(finalRanking, playersNicknamesWithPrettiestShip);
    }

    @Override
    public void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws IOException {
        clientModel.eliminatePlayer(nickname);
        view.showPlayerEarlyLanded(nickname);
    }

    public void land() {
        try {
            serverController.playerWantsToLand(nickname);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    public void skipToLastCard() {
        try {
            serverController.debugSkipToLastCard();
            view.showMessage("Successfully skipped to last card!", NOTIFICATION_INFO);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            view.showMessage("Failed to skip cards: " + e.getMessage(), ERROR);
        }
    }

    @Override
    public void notifyStorageError(String nicknameToNotify, String errorMessage) throws IOException {
        view.showError("Storage selection error: " + errorMessage);
        // Returns to the storage selection phase for retry
        view.showMessage("Please try again with a valid configuration.", STANDARD);
    }

    /**
     * Sends storage updates to the server using the new data structure.
     *
     * @param storageUpdates map of storage updates
     */
    public void sendStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
        try {
            PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure.Builder()
                    .setStorageUpdates(storageUpdates)
                    .build();

            serverController.handleClientChoice(nickname, choices);
        } catch (IOException e) {
            view.showError("Error while sending to server: " + e.getMessage());
            view.showMessage("Retry with 'c' to confirm", STANDARD);
        }
    }
}
