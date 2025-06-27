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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;

/**
 * Main controller for managing the client in a distributed game application.
 * This class coordinates the interaction between the client model, view, and network
 * communication, handling all operations related to game participation, from
 * nickname registration to game phase management.
 *
 * The class extends UnicastRemoteObject to support RMI communication and implements
 * CallableOnClientController to receive notifications from the game server.
 *
 * @author Your development team
 * @version 1.0
 * @since 1.0
 */
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

    /**
     * Constructs a new ClientController with the specified client model and ping-pong manager.
     * Initializes the controller preparing it for handling network operations and model management.
     *
     * @param clientModel the client model that maintains the local game state
     * @param clientPingPongManager the manager for network connection monitoring
     * @throws RemoteException if an error occurs during remote object initialization
     */
    public ClientController(ClientModel clientModel, ClientPingPongManager clientPingPongManager) throws RemoteException {
        super();
        this.clientModel = clientModel;
        this.clientPingPongManager = clientPingPongManager;
    }

    /**
     * Starts the execution of the client controller by requesting nickname input.
     * This method represents the main entry point for user interaction,
     * delegating to the view the request for initial credentials.
     */
    public void run() {
        view.askNickname();
    }

    /**
     * Returns a copy of the list of currently observable games.
     * Provides read-only access to available game information
     * to prevent unauthorized modifications to the internal data structure.
     *
     * @return a new list containing the information of observable games
     */
    public List<GameInfo> getGames() {
        return new ArrayList<>(observableGames);
    }

    /**
     * Returns the direct reference to the observable games list.
     * This method provides direct access to the internal data structure and
     * should be used with caution to avoid unwanted modifications.
     *
     * @return the list of observable games maintained internally
     */
    public List<GameInfo> getObservableGames() {
        return observableGames;
    }

    /**
     * Returns the identifier of the current game.
     * Provides access to the ID of the game to which the client is currently connected.
     *
     * @return the current game ID, or null if no active game is present
     */
    public String getCurrentGameId() {
        return currentGameInfo.getGameId();
    }

    /**
     * Returns the client model associated with this controller.
     * Provides access to the model that maintains the local client state.
     *
     * @return the client model instance
     */
    public ClientModel getClientModel() {
        return clientModel;
    }

    /**
     * Handles the nickname registration process with the DNS server.
     * Attempts to register the specified nickname and starts the ping-pong
     * mechanism for connection monitoring if successful. Displays appropriate
     * error messages if registration fails.
     *
     * @param attemptedNickname the nickname to register with the server
     */
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

    /**
     * Sets the DNS service reference for network communication.
     * Establishes the connection to the DNS service that manages
     * game discovery and player registration.
     *
     * @param dns the DNS service implementation to use for network operations
     */
    public void setDns(CallableOnDNS dns) {
        this.dns = dns;
    }

    /**
     * Returns the current DNS service reference.
     * Provides access to the DNS service used for network communication.
     *
     * @return the current DNS service instance
     */
    public CallableOnDNS getDns() {
        return dns;
    }

    /**
     * Returns the nickname of the current player.
     * Provides access to the registered nickname for this client session.
     *
     * @return the player's nickname, or null if not yet registered
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Handles the creation of a new game based on the specified parameters.
     * Validates the number of players, creates the game using the provided
     * parameters, and updates the game state accordingly. Displays error
     * messages to the user if game creation fails.
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
     * Handles the connection process, updates the game state, and manages error cases.
     * Returns true if the join operation was successful, false otherwise.
     *
     * @param chosenGameId the identifier of the game to join
     * @param chosenColor the color chosen by the player for the game
     * @return true if successfully joined the game, false otherwise
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

    /**
     * Sets the view component for this controller.
     * Establishes the connection between the controller and the user interface
     * component responsible for displaying game information and collecting user input.
     *
     * @param view the view implementation to use for user interaction
     */
    public void setView(ClientView view) {
        this.view = view;
    }

    /**
     * Allows the user to select the network protocol for communication.
     * Establishes the network connection using either RMI or Socket protocol
     * based on the specified parameters.
     *
     * @param isRmi true to use RMI protocol, false to use Socket TCP/IP
     * @param serverAddress the server address to connect to
     * @param serverPort the server port to connect to
     * @return the chosen implementation of CallableOnDNS, or null if connection fails
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

    /**
     * Leaves the current game and terminates the client application.
     * Handles the disconnection process by notifying the server and
     * performing cleanup operations before exiting the application.
     */
    public void leaveGame() {
        try {
            if (currentGameInfo != null) {
                inGame = false;
                gameStarted = false;
                currentGameInfo = null;
                serverController.leaveGameAfterCreation(nickname);
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
    public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {
        clientModel.getShipboardOf(shipOwnerNickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(shipOwnerNickname).setIncorrectlyPositionedComponentsCoordinates(incorrectlyPositionedComponentsCoordinates);
        clientModel.getShipboardOf(shipOwnerNickname).setComponentsPerType(componentsPerType);
        clientModel.getShipboardOf(shipOwnerNickname).setNotActiveComponents(notActiveComponentsList);
        clientModel.refreshShipBoardOf(shipOwnerNickname);

        // Shows the menu only if it is your own shipBoard
        if (shipOwnerNickname.equals(nickname)) {
            view.showInvalidShipBoardMenu();
        }
    }

    @Override
    public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {
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
    public void notifyStolenVisibleComponent(String nicknameToNotify) throws IOException{
        view.showStolenVisibleComponent();
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

    /**
     * Submits crew placement choices to the server for validation.
     * Performs local validation of crew choices before sending them to the server.
     * Displays appropriate error messages if validation fails.
     *
     * @param choices a map of coordinates to crew members representing the placement choices
     */
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
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws IOException{
        clientModel.getShipboardOf(nickname).setShipMatrix(shipMatrix);
        clientModel.getShipboardOf(nickname).setComponentsPerType(componentsPerType);
        clientModel.getShipboardOf(nickname).setNotActiveComponents(notActiveComponentsList);
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

    /**
     * Notifies the server that crew member evaluation has been completed.
     * This method is called when the player has finished evaluating their crew members
     * and is ready to proceed to the next game phase.
     */
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
        view.notifyPlayerDisconnected(disconnectedPlayerNickname);
    }

    /**
     * Notifies the server that the hourglass timer has ended.
     * This method is called when the time limit for a game phase has expired,
     * triggering the appropriate server-side handling for timeout scenarios.
     *
     * @throws IOException if a network communication error occurs
     */
    public void notifyHourglassEnded() throws IOException {
        serverController.notifyHourglassEnded(nickname);
    }

    /**
     * Requests a random component from the hidden component deck.
     * Initiates the process of picking a random component and displays
     * the appropriate menu for component handling.
     */
    public void pickRandomComponent() {
        try {
            serverController.playerPicksHiddenComponent(nickname);
            view.showPickedComponentAndMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    /**
     * Attempts to reserve the currently focused component.
     * Validates that the player hasn't exceeded the maximum number of
     * reserved components (2) before sending the reservation request to the server.
     */
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

    /**
     * Releases the currently focused component.
     * Removes the focus from the current component both locally and on the server,
     * returning to the ship board building menu.
     */
    public void releaseFocusedComponent() {
        try {
            clientModel.getShipboardOf(nickname).releaseFocusedComponent();
            serverController.playerWantsToReleaseFocusedComponent(nickname);
            view.showBuildShipBoardMenu();
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    /**
     * Handles remote exceptions by logging the error and terminating the application.
     * This method provides centralized error handling for network communication failures.
     *
     * @param e the IOException that occurred during remote communication
     */
    public void handleRemoteException(IOException e) {
        System.err.println("Remote exception: " + e);
        System.exit(1);
    }

    /**
     * Displays the ship board of the specified player.
     * Searches for the player in the client data and shows their ship board
     * if found, otherwise displays an error message.
     *
     * @param nickname the nickname of the player whose ship board to display
     */
    public void showShipBoard(String nickname) {
        clientModel.getPlayerClientData()
                .keySet()
                .stream()
                .filter(player -> player.equals(nickname))
                .findFirst()
                .ifPresentOrElse(player -> view.showShipBoard(clientModel.getShipboardOf(player), nickname),
                        () -> view.showMessage("Player not found\n", STANDARD));
    }

    /**
     * Displays the cargo cubes of the specified player.
     * Shows the current cargo cube configuration on the player's ship board.
     *
     * @param nickname the nickname of the player whose cubes to display
     */
    public void showCubes(String nickname) {
        clientModel.getPlayerClientData()
                .keySet()
                .stream()
                .filter(player -> player.equals(nickname))
                .findFirst()
                .ifPresentOrElse(player -> view.showCubes(clientModel.getShipboardOf(player), nickname),
                        () -> view.showMessage("Player not found\n", STANDARD));
    }

    /**
     * Places the currently focused component at the specified coordinates.
     * Validates the position and sends the placement request to the server.
     * Displays error messages for invalid coordinates.
     *
     * @param row the row coordinate for component placement
     * @param column the column coordinate for component placement
     */
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

    /**
     * Removes a component from the specified coordinates.
     * Sends a removal request to the server for the component at the given position.
     *
     * @param row the row coordinate of the component to remove
     * @param column the column coordinate of the component to remove
     */
    public void removeComponent(int row, int column) {
        try {
            serverController.playerWantsToRemoveComponent(nickname, new Coordinates(row, column));
        } catch (IllegalArgumentException e) {
            view.showMessage("Invalid coordinates: " + e.getMessage() + "\n", ERROR);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    /**
     * Removes a ship part consisting of multiple connected components.
     * Sends the selected ship part to the server for removal from the ship board.
     *
     * @param shipPart a set of coordinates representing the connected ship part to remove
     */
    public void removeShipPart(Set<Coordinates> shipPart) {
        try{
            serverController.playerChoseShipPart(nickname, shipPart);
        }catch (IOException e) {
            handleRemoteException(e);
        }
    }

    /**
     * Picks a visible component from the available selection.
     * Validates that the component is still available before sending the
     * selection request to the server.
     *
     * @param chosenIndex the index of the visible component to pick
     */
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

    /**
     * Restarts the hourglass timer for the current game phase.
     * Checks if the hourglass is already running and validates the request
     * before sending the restart command to the server.
     */
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
     * Validates the input, manages reserved components, and informs the server
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

    /**
     * Handles the selection of a ship part from the available options.
     * Validates the choice and sends the selected ship part to the server.
     * Displays error messages for invalid selections.
     *
     * @param choice the 1-based index of the ship part to select
     */
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

    /**
     * Sends a ping message from the client to the server.
     * This method is part of the keep-alive mechanism to monitor network connectivity.
     *
     * @param nickname the nickname of the player sending the ping
     * @throws IOException if a network communication error occurs
     */
    public void pingToServerFromClient(String nickname) throws IOException{
        dns.pingToServerFromClient(nickname);
    }

    /**
     * Handles the pong reply received from the server.
     * Resets the timeout counter when a pong response is received, maintaining
     * the connection monitoring mechanism.
     *
     * @param nickname the nickname of the player receiving the pong
     * @throws IOException if a network communication error occurs
     */
    public void pongToClientFromServer(String nickname) throws IOException{
        //System.out.println("Pong dal server");
        clientPingPongManager.onPongReceived(this::handleDisconnection);
    }

    /**
     * Handles ping messages received from the server.
     * Responds with a pong message to maintain the bidirectional keep-alive mechanism.
     *
     * @param nickname the nickname of the player receiving the ping
     * @throws IOException if a network communication error occurs
     */
    public void pingToClientFromServer(String nickname) throws IOException{
        //System.out.println("Ping dal server");
        dns.pongToServerFromClient(nickname);
    }

    /**
     * Handles client disconnection scenarios.
     * Displays appropriate disconnection messages to inform the user
     * about network connectivity issues.
     */
    public void handleDisconnection() {
        view.showDisconnectMessage("DISCONNECTION: No pong received from server");
    }

    /**
     * Handles forced disconnection initiated by the server.
     * Performs cleanup operations and exits the game when the server
     * forces a disconnection.
     *
     * @param nicknameToNotify the nickname of the player being disconnected
     * @param gameId the ID of the game from which the player is being disconnected
     * @throws IOException if a network communication error occurs during disconnection
     */
    public void forcedDisconnection(String nicknameToNotify, String gameId) throws IOException {
        leaveGame();
    }

    /**
     * Ends the ship board building phase for the current player.
     * Notifies the server that the player has completed their ship construction
     * and is ready to proceed to the next game phase.
     */
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

    /**
     * Places the player's pawn on the game board.
     * Initiates the pawn placement process by sending the request to the server.
     */
    public void placePawn() {
        try {
            serverController.playerPlacesPawn(nickname);
        } catch (IOException e) {
            handleRemoteException(e);
        }
    }

    /**
     * Returns the current game information.
     * Provides access to the complete game state and metadata.
     *
     * @return the current GameInfo instance, or null if no game is active
     */
    public GameInfo getCurrentGameInfo() {
        return currentGameInfo;
    }

    /**
     * Handles the player's decision to visit a location.
     * Validates crew requirements for location visits and sends the choice to the server.
     * Automatically sets the choice to false if the player doesn't have enough crew members.
     *
     * @param nickname the nickname of the player making the choice
     * @param choice true if the player wants to visit the location, false otherwise
     */
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

    /**
     * Handles the player's action to throw dice.
     * Sends a dice throwing request to the server for the current player.
     *
     * @param nickname the nickname of the player throwing the dice
     */
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

    /**
     * Handles the player's choice of double engines and corresponding battery boxes.
     * Validates that the number of engines matches the number of batteries before
     * sending the selection to the server.
     *
     * @param nickname the nickname of the player making the choice
     * @param doubleEnginesCoords the coordinates of the selected double engines
     * @param batteryBoxesCoords the coordinates of the corresponding battery boxes
     * @throws IllegalArgumentException if the number of engines doesn't match the number of batteries
     */
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

    /**
     * Handles the player's choice of double cannons and corresponding battery boxes.
     * Sends the cannon and battery selection to the server for processing.
     *
     * @param nickname the nickname of the player making the choice
     * @param doubleCannonsCoords the coordinates of the selected double cannons
     * @param batteryBoxesCoords the coordinates of the corresponding battery boxes
     */
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

    /**
     * Validates the selection of cabins for crew member removal.
     * Checks that the correct number of crew members are selected and that
     * no cabin is selected more times than it has inhabitants.
     *
     * @param nickname the nickname of the player making the selection
     * @param cabinCoords the coordinates of the selected cabins
     * @return true if the cabin selection is valid, false otherwise
     */
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

    /**
     * Handles the player's choice of cabins for crew member operations.
     * Sends the cabin selection to the server after validation.
     *
     * @param nickname the nickname of the player making the choice
     * @param cabinCoords the coordinates of the selected cabins
     */
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

    /**
     * Handles the player's choice to visit a specific planet.
     * Validates that it's the player's turn before sending the planet choice to the server.
     *
     * @param nickname the nickname of the player making the choice
     * @param choice the index of the chosen planet
     */
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

    /**
     * Handles the player's decision to accept or reject a reward.
     * Sends the player's choice regarding reward acceptance to the server.
     *
     * @param nickname the nickname of the player making the choice
     * @param choice true if the player accepts the reward, false otherwise
     */
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

    /**
     * Handles the player's response to small dangerous objects.
     * Manages shield and battery box selections for defending against small threats.
     *
     * @param nickname the nickname of the player making the choice
     * @param shieldCoords the coordinates of selected shields
     * @param batteryBoxCoords the coordinates of selected battery boxes
     */
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

    /**
     * Handles the player's response to big meteorite attacks.
     * Manages double cannon and battery box selections for defending against large meteorites.
     *
     * @param nickname the nickname of the player making the choice
     * @param doubleCannonCoords the coordinates of selected double cannons
     * @param batteryBoxCoords the coordinates of selected battery boxes
     */
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

    /**
     * Handles the player's response to big shot attacks.
     * Sends an empty choice structure to the server for big shot handling.
     *
     * @param nickname the nickname of the player responding to the attack
     */
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

    /**
     * Handles the player's choice of storage and battery components.
     * Manages storage and battery box selections for cargo operations.
     *
     * @param nickname the nickname of the player making the choice
     * @param storageCoords the coordinates of selected storage components
     * @param batteryBoxCoords the coordinates of selected battery boxes
     */
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

    /**
     * Handles the player's choice of storage components only.
     * Sends storage selections to the server for processing.
     *
     * @param nickname the nickname of the player making the choice
     * @param storageCoords the coordinates of selected storage components
     */
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

    /**
     * Handles the epidemic spreading mechanism.
     * Sends an epidemic event to the server for processing.
     *
     * @param nickname the nickname of the player triggering the epidemic
     */
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

    /**
     * Handles the stardust event mechanism.
     * Initiates a stardust event on the server for the specified player.
     *
     * @param nickname the nickname of the player triggering the stardust event
     */
    public void stardustEvent(String nickname){
        try{
            serverController.stardustEvent(nickname);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    /**
     * Initiates the ship board check process after an attack.
     * Starts the validation process for ship integrity following combat damage.
     *
     * @param nickname the nickname of the player whose ship board needs checking
     * @param coordinates the coordinates where the attack occurred
     */
    public void startCheckShipBoardAfterAttack(String nickname, Coordinates coordinates) {
        try {
            serverController.startCheckShipBoardAfterAttack(nickname);
        } catch (IOException e){
            handleRemoteException(e);
        }
    }

    /**
     * Requests the list of available prefabricated ships from the server.
     * Initiates an asynchronous request to retrieve all available ship templates.
     */
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

    /**
     * Selects a prefabricated ship based on the provided ship ID.
     * Sends a selection request to the server for the specified prefab ship.
     *
     * @param prefabShipId the unique identifier of the prefabricated ship to select
     */
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

    /**
     * Initiates the landing process for the current player.
     * Sends a landing request to the server, allowing the player to end their participation.
     */
    public void land() {
        try {
            serverController.playerWantsToLand(nickname);
        }catch (IOException e){
            handleRemoteException(e);
        }
    }

    /**
     * Debug method to skip to the last card in the game.
     * This method is intended for testing and debugging purposes only.
     * Allows rapid progression through the game for development testing.
     */
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
     * Transmits changes to storage configurations including cargo cube placements.
     *
     * @param storageUpdates map containing storage coordinates and their cargo cube contents
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