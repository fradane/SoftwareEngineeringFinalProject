package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientDeserializer;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientSerializer;
import it.polimi.ingsw.is25am33.serializationLayer.SocketMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

public class SocketClientManager implements CallableOnDNS, CallableOnGameController {

    private PrintWriter out;
    private Scanner in;
    private String nickname;
    private volatile GameState gameState;
    private final CallableOnClientController clientController;
    private boolean running = true;
    private Socket socket;
    private Thread messageHandler;

    // Queue to store notifications while waiting for specific responses
    private final Queue<SocketMessage> notificationBuffer = new ConcurrentLinkedQueue<>();

    // Flag indicating whether we are waiting for a specific response
    private volatile boolean waitingForResponse = false;
    private volatile Set<String> expectedResponseActions;
    private volatile CompletableFuture<SocketMessage> responseFuture = null;

    // Lock to synchronize operations
    private final Object lock = new Object();

    /**
     * Constructs a new SocketClientManager with the specified client controller
     *
     * @param clientController The controller handling client-side operations
     */
    public SocketClientManager(CallableOnClientController clientController) {
        this.clientController = clientController;
    }

    /**
     * Invia un messaggio e aspetta una risposta specifica
     */
    private SocketMessage sendAndWaitForSpecificResponse(SocketMessage message, Set<String> expectedActions) throws RemoteException {
        synchronized (lock) {
            // Set the waiting state
            waitingForResponse = true;
            expectedResponseActions = expectedActions;
            responseFuture = new CompletableFuture<>();

            // Send the request
            out.println(ClientSerializer.serialize(message));
        }

        try {
            // Wait for the response
            SocketMessage response = responseFuture.get();

            // Process buffered notifications
            processBufferedNotifications();

            return response;
        } catch (InterruptedException | ExecutionException e) {
            //TODO understand where to handle these exceptions
            throw new RemoteException("Failed to get response: " + e.getMessage(), e);
        } finally {
            synchronized (lock) {
                waitingForResponse = false;
                expectedResponseActions = null;

                responseFuture = null;
            }
        }
    }

    /**
     * Gets the game controller for a specific game
     *
     * @param gameId The ID of the game
     * @return The game controller
     * @throws RemoteException If remote communication fails
     */
    @Override
    public GameController getController(String gameId)  throws RemoteException{
        return null;
    }

    /**
     * Gets information about a specific game
     *
     * @param gameId The ID of the game
     * @return Information about the game
     * @throws RemoteException If remote communication fails
     */
    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException {
        return null;
    }

    @Override
    public void leaveGameAfterCreation(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "leaveGameAfterCreation");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void leaveGameBeforeCreation(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "leaveGameBeforeCreation");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Registers a player with the specified nickname
     *
     * @param nickname   The player's chosen nickname
     * @param controller The client controller
     * @return True if registration successful, false otherwise
     * @throws RemoteException If remote communication fails
     */
    @Override
    public boolean registerWithNickname(String nickname, CallableOnClientController controller) throws RemoteException {

        SocketMessage outMessage = new SocketMessage(nickname, "registerWithNickname");
        outMessage.setParamString(nickname);

        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyRegistrationSuccess", "notifyNicknameAlreadyExists"));

        if (response.getActions().equals("notifyRegistrationSuccess")) {
            this.nickname = nickname;
            return true;
        } else if (response.getActions().equals("notifyNicknameAlreadyExists")) {
            return false;
        }

        throw new RemoteException("Unexpected response: " + response.getActions());

    }

    /**
     * Establishes a socket connection to the specified server
     *
     * @param serverAddress The server's IP address or hostname
     * @param serverPort    The server's port number
     * @throws IOException If connection cannot be established
     */
    public void connect(String serverAddress, int serverPort) throws IOException {

        try {
            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + socket.getRemoteSocketAddress());

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new Scanner(socket.getInputStream());

            startMessageHandlerThread();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number parameter: " + e.getMessage());
        }

    }

    /**
     * Creates a new game with specified parameters
     *
     * @param color        The player's chosen color
     * @param numPlayers   The number of players in the game
     * @param isTestFlight Whether this is a test flight
     * @param nickname     The player's nickname
     * @return Information about the created game
     * @throws IOException If communication fails
     */
    @Override
    public GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws IOException {

        SocketMessage outMessage = new SocketMessage(nickname, "createGame");
        outMessage.setParamPlayerColor(color);
        outMessage.setParamInt(numPlayers);
        outMessage.setParamBoolean(isTestFlight);

        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyGameCreated"));
        //TODO bisogna forse controllare da qualche parte il caso in cui non sia stato restituito un gioco
        return response.getParamGameInfo().getFirst();

    }

    /**
     * Attempts to join an existing game
     *
     * @param gameId   The ID of the game to join
     * @param nickname The player's nickname
     * @param color    The player's chosen color
     * @return True if successfully joined, false otherwise
     * @throws IOException If communication fails
     */
    @Override
    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws IOException {

        SocketMessage outMessage = new SocketMessage(nickname, "joinGame");
        outMessage.setParamGameId(gameId);
        outMessage.setParamPlayerColor(color);

        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyJoinGameResult"));
        return response.getParamBoolean();

    }

    // TODO debug
    @Override
    public void showMessage(String s) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "showMessage");
        outMessage.setParamString(s);
        out.println(ClientSerializer.serialize(outMessage));
    }


    private void startMessageHandlerThread() {
         messageHandler = new Thread(() -> {
            while (running) {
                try {
                    if (in!=null && in.hasNextLine()) {
                        String line = in.nextLine();
                        handleServerMessage(line);
                    }
                } catch (Exception e) {
                    running=false;
                    break;
                }
            }
        });
        messageHandler.setDaemon(true);
        messageHandler.start();
    }

    private void handleServerMessage(String message) {
        try {
            SocketMessage inMessage = ClientDeserializer.deserialize(message, SocketMessage.class);

            synchronized (lock) {
                // Se stiamo aspettando una risposta specifica
                if (waitingForResponse) {
                    // Se è la risposta che aspettiamo
                    if (expectedResponseActions.contains(inMessage.getActions())) {
                        responseFuture.complete(inMessage);
                        return;
                    }
                    // Altrimenti, mettiamo la notifica nel buffer
                    else {
                        notificationBuffer.add(inMessage);
                        return;
                    }
                }
            }

            // Se non stiamo aspettando risposte o il messaggio non corrisponde,
            // processiamo il messaggio immediatamente
            processNotification(inMessage);
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void processBufferedNotifications() {
        SocketMessage notification;
        while ((notification = notificationBuffer.poll()) != null) {
            processNotification(notification);
        }
    }

    private void processNotification(SocketMessage notification) {
        try {
            switch (notification.getActions()) {

                case "notifyNewPlayerJoined":
                    if (clientController != null) {
                        clientController.notifyNewPlayerJoined(null, notification.getParamGameId(), notification.getParamString(), notification.getParamPlayerColor());
                    }
                    break;

                case "notifyGameStarted":
                    if (clientController != null) {
                        clientController.notifyGameStarted(nickname, notification.getParamGameInfo().getFirst());
                    }
                    break;

                case "notifyGameState":
                    if (clientController != null) {
                        this.gameState = notification.getParamGameState();
                        clientController.notifyGameState(nickname, gameState);
                    }
                    break;

                case "notifyDangerousObjAttack":
                    if (clientController != null) {
                        clientController.notifyDangerousObjAttack(nickname, notification.getParamDangerousObj());
                    }
                    break;

                case "notifyCurrPlayerChanged":
                    if (clientController != null) {
                        clientController.notifyCurrPlayerChanged(nickname, notification.getParamString());
                    }
                    break;

                case "notifyHourglassRestarted":
                    if (clientController != null) {
                        clientController.notifyHourglassRestarted(nickname, notification.getParamString(), notification.getParamInt());
                    }
                    break;

                case "notifyCurrAdventureCard":
                    if (clientController != null) {
                        clientController.notifyCurrAdventureCard(
                                nickname,
                                notification.getParamClientCard(),
                                notification.getParamBoolean()
                        );
                    }
                    break;

                case "notifyCardState":
                    if (clientController != null) {
                        clientController.notifyCardState(nickname, notification.getParamCardState());
                    }
                    break;

                case "notifyChooseComponent":
                    if (clientController != null) {
                        clientController.notifyFocusedComponent(null, notification.getParamString(), notification.getParamComponent());
                    }
                    break;

                case "notifyReleaseComponent":
                    if (clientController != null) {
                        clientController.notifyReleaseComponent(null, notification.getParamString());
                    }
                    break;

                case "notifyBookedComponent":
                    if (clientController != null) {
                        clientController.notifyBookedComponent(null, notification.getParamString(), notification.getParamComponent());
                    }
                    break;

                case "notifyAddVisibleComponents":
                    if (clientController != null) {
                        clientController.notifyAddVisibleComponents(notification.getParamString(), notification.getParamInt(), notification.getParamComponent());
                    }
                    break;

                case "notifyStopHourglass":
                    if (clientController != null) {
                        clientController.notifyStopHourglass(notification.getParamString());
                    }
                    break;

                case "notifyFirstToEnter":
                    if (clientController != null) {
                        clientController.notifyFirstToEnter(notification.getParamString());
                    }
                    break;

                case "notifyRemoveVisibleComponents":
                    if (clientController != null) {
                        clientController.notifyRemoveVisibleComponents(notification.getParamString(), notification.getParamInt());
                    }
                    break;

                case "notifyComponentPlaced":
                    if (clientController != null) {
                        clientController.notifyComponentPlaced(null, notification.getParamString(), notification.getParamComponent(), notification.getParamCoordinates());
                    }
                    break;

                case "notifyShipBoardUpdate":
                    if (clientController != null) {
                        clientController.notifyShipBoardUpdate(null, notification.getParamString(), notification.getParamShipBoardAsMatrix(),notification.getParamComponentsPerType(), notification.getParamComponentList());
                    }
                    break;
                    //TODO da aggiustare mettondo i componentsPerType

                case "notifyPlayerCredits":
                    if (clientController != null) {
                        clientController.notifyPlayerCredits(null, notification.getParamString(), notification.getParamInt());
                    }
                    break;

                case "notifyEliminatedPlayer":
                    if (clientController != null) {
                        clientController.notifyEliminatedPlayer(null, notification.getParamString());
                    }
                    break;

                case "notifyRankingUpdate":
                    if (clientController != null) {
                        clientController.notifyRankingUpdate(null, notification.getParamString(), notification.getParamInt());
                    }
                    break;

                case "notifyVisibleDeck":
                    if (clientController != null) {
                        clientController.notifyVisibleDeck(notification.getParamString(), notification.getParamLittleVisibleDecks());
                    }
                    break;

                case "notifyPlayerDisconnected":
                    if (clientController != null) {
                        clientController.notifyPlayerDisconnected(null, notification.getParamString());
                    }
                    break;

                case "forcedDisconnection" :
                    if (clientController != null) {
                        clientController.forcedDisconnection(nickname, notification.getParamString());
                        in.close();
                        out.close();
                        socket.close();
                    }
                    break;

                case "notifyGameInfos":
                    if (clientController != null) {
                        clientController.notifyGameInfos(notification.getParamString(), notification.getParamGameInfo());
                    }
                    break;

                case "notifyInvalidShipBoard":
                    if (clientController != null) {
                        clientController.notifyInvalidShipBoard(
                                nickname,
                                notification.getParamString(),
                                notification.getParamShipBoardAsMatrix(),
                                notification.getParamIncorrectlyPositionedCoordinates(),
                                notification.getParamComponentsPerType(),
                                notification.getParamComponentList()
                        );
                    }
                    break;

                case "notifyValidShipBoard":
                    if (clientController != null) {
                        clientController.notifyValidShipBoard(
                                nickname,
                                notification.getParamString(),
                                notification.getParamShipBoardAsMatrix(),
                                notification.getParamIncorrectlyPositionedCoordinates(),
                                notification.getParamComponentsPerType(),
                                notification.getParamComponentList()
                        );
                    }
                    break;

                case "notifyStolenVisibleComponent":
                    if (clientController != null) {
                        clientController.notifyStolenVisibleComponent(nickname);
                    }
                    break;

                case "notifyShipPartsGeneratedDueToRemoval":
                    if (clientController != null) {
                        clientController.notifyShipPartsGeneratedDueToRemoval(
                                nickname,
                                notification.getParamString(),
                                notification.getParamShipBoardAsMatrix(),
                                notification.getParamIncorrectlyPositionedCoordinates(),
                                notification.getParamShipParts(),
                                notification.getParamComponentsPerType()
                        );
                    }
                    break;

                case "notifyCurrAdventureCardUpdate":
                    if (clientController != null) {
                        clientController.notifyCurrAdventureCardUpdate(
                                nickname,
                                notification.getParamClientCard()
                        );
                    }
                    break;

                case "notifyPlayerVisitedPlanet":
                    if (clientController != null) {
                        clientController.notifyPlayerVisitedPlanet(
                                nickname,
                                notification.getParamString(),
                                notification.getParamClientCard()
                        );
                    }
                    break;

                case "notifyCrewPlacementPhase":
                    if (clientController != null) {
                        clientController.notifyCrewPlacementPhase(
                                nickname
                        );
                    }
                    break;

                case "notifyCrewPlacementComplete":
                    if (clientController != null) {
                        clientController.notifyCrewPlacementComplete(
                                nickname,
                                notification.getParamString(),
                                notification.getParamShipMatrix(),
                                notification.getParamComponentsPerType()
                        );
                    }
                    break;

                case "notifyPrefabShipsAvailable":
                    if (clientController != null) {
                        clientController.notifyPrefabShipsAvailable(
                                nickname,
                                notification.getParamPrefabShips()
                        );
                    }
                    break;

                case "notifyPlayerSelectedPrefabShip":
                    if (clientController != null) {
                        clientController.notifyPlayerSelectedPrefabShip(
                                nickname,
                                notification.getParamString(),
                                notification.getParamPrefabShips().get(0)
                        );
                    }
                    break;

                case "notifyPrefabShipSelectionResult":
                    if (clientController != null) {
                        clientController.notifyPrefabShipSelectionResult(
                                nickname,
                                notification.getParamBoolean(),
                                notification.getParamString()
                        );
                    }
                    break;

                case "notifyInfectedCrewMembersRemoved":
                    if (clientController != null) {
                        clientController.notifyInfectedCrewMembersRemoved(
                                nickname,
                                notification.getParamShipPart()
                        );
                    }
                    break;

                case "notifyComponentPerType":
                    if (clientController != null) {
                        clientController.notifyComponentPerType(
                                nickname,
                                notification.getParamString(),
                                notification.getParamComponentsPerType()
                        );
                    }
                    break;

                case "notifyNoMoreHiddenComponents":
                    if (clientController != null) {
                        clientController.notifyNoMoreHiddenComponents(nickname);
                    }

                case "PING":
                    if (clientController != null) {
                        clientController.pingToClientFromServer(nickname);
                    }
                    break;

                case "PONG":
                    if (clientController != null) {
                        clientController.pongToClientFromServer(nickname);
                    }
                    break;

                case "notifyCoordinateOfComponentHit":
                    if (clientController != null) {
                        clientController.notifyCoordinateOfComponentHit(nickname, notification.getParamString(), notification.getParamCoordinates());
                    }
                    break;
                case "notifyLeastResourcedPlayer":
                    if (clientController != null) {
                        clientController.notifyLeastResourcedPlayer(nickname, notification.getParamString());
                    }
                    break;

                case "notifyPlayersFinalData":
                    if (clientController != null) {
                        clientController.notifyPlayersFinalData(nickname, notification.getParamPlayerFinalDataRanking(), notification.getParamStringList());
                    }
                    break;

                case "notifyPlayerEarlyLanded":
                    if (clientController != null) {
                        clientController.notifyPlayerEarlyLanded(nickname, notification.getParamString());
                    }
                    break;

                case "notifyErrorWhileBookingComponent":
                    if (clientController != null) {
                        clientController.notifyErrorWhileBookingComponent(nickname, notification.getParamString(), notification.getParamComponent());
                    }
                    break;

                case "notifyNotActiveComponents":
                    if (clientController != null) {
                        clientController.notifyNotActiveComponents(nickname, notification.getParamString(), notification.getParamComponentList());
                    }
                    break;

                case "notifyStorageError":
                    if (clientController != null) {
                        clientController.notifyStorageError(nickname, notification.getParamString());
                    }
                    break;

                default:
                    System.err.println("Unknown notification: " + notification.getActions());
            }
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a ping message to the server to check connection
     *
     * @param nickname The player's nickname
     * @throws IOException If message cannot be sent
     */
    public void pingToServerFromClient(String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage(nickname, "PING");
        out.println(ClientSerializer.serialize(outMessage));
        //System.out.println("Ping inviato al server");
    }

    /**
     * Sends a pong response to the server
     *
     * @param nickname The player's nickname
     * @throws IOException If message cannot be sent
     */
    public void pongToServerFromClient(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "PONG");
        out.println(ClientSerializer.serialize(outMessage));
        //System.out.println("Pong inviato al server");
    }

    /**
     * Notifies the server that the player wants to pick a hidden component.
     *
     * @param nickname The nickname of the player performing the action.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerPicksHiddenComponent(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksHiddenComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to place the currently focused component
     * at the specified coordinates with a given rotation.
     *
     * @param nickname  The player's nickname.
     * @param coordinates The coordinates where the component is to be placed.
     * @param rotation   The rotation of the component.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToPlaceFocusedComponent");
        outMessage.setParamCoordinates(coordinates);
        outMessage.setParamInt(rotation);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to reserve the currently focused component.
     *
     * @param nickname The player's nickname.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReserveFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to release the currently focused component.
     *
     * @param nickname The player's nickname.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReleaseFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to manually restart the hourglass timer.
     *
     * @param nickname The nickname of the player requesting the restart.
     */
    @Override
    public void playerWantsToRestartHourglass(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToRestartHourglass");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to remove a component from the ship at the specified coordinate.
     *
     * @param nickname  The player's nickname.
     * @param coordinate The coordinate of the component to remove.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToRemoveComponent(String nickname, Coordinates coordinate) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToRemoveComponent");
        outMessage.setParamCoordinates(coordinate);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the ship part chosen by the player during the rebuild or repair phase.
     *
     * @param nickname  The player's nickname.
     * @param shipPart  The set of coordinates representing the chosen ship part.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerChoseShipPart(String nickname, Set<Coordinates> shipPart) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseShipPart");
        outMessage.setParamShipPart(shipPart);
        out.println(ClientSerializer.serialize(outMessage));
    }


    /**
     * Notifies the server that the player has completed the ship board building phase.
     *
     * @param nickname The nickname of the player.
     */
    @Override
    public void playerEndsBuildShipBoardPhase(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerEndsBuildShipBoardPhase");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player has placed their pawn.
     *
     * @param nickname The nickname of the player.
     */
    @Override
    public void playerPlacesPawn(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPlacePlaceholder");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Sends the player's choice to the server during an interaction requiring a decision.
     *
     * @param nickname The nickname of the player.
     * @param choice The choice made by the player.
     * @throws IOException If communication with the server fails.
     */
    @Override
    public void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "handleClientChoice");
        outMessage.setParamChoice(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Submits the player's crew placement choices to the server.
     *
     * @param nickname The nickname of the player.
     * @param choices A map of coordinates to crew members chosen for placement.
     * @throws IOException If communication with the server fails.
     */
    @Override
    public void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "submitCrewChoices");
        outMessage.setParamCrewChoices(choices);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Requests the list of available prefab ships from the server.
     *
     * @param nickname The nickname of the player requesting the prefab ships.
     * @throws IOException If communication with the server fails.
     */
    @Override
    public void requestPrefabShips(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "requestPrefabShips");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Requests the server to select a prefab ship based on the player's choice.
     *
     * @param nickname The nickname of the player making the selection.
     * @param prefabShipId The identifier of the selected prefab ship.
     * @throws IOException If communication with the server fails.
     */
    @Override
    public void requestSelectPrefabShip(String nickname, String prefabShipId) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "requestSelectPrefabShip");
        outMessage.setParamString(prefabShipId);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to land.
     *
     * @param nickname The nickname of the player.
     * @throws IOException If communication with the server fails.
     */
    @Override
    public void playerWantsToLand(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToLand");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player has picked a visible component.
     *
     * @param nickname The nickname of the player.
     * @param choice The index of the chosen component.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksVisibleComponent");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's decision regarding visiting a location.
     *
     * @param nickname The nickname of the player.
     * @param choice True if the player wants to visit the location; false otherwise.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToVisitLocation(String nickname, Boolean choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToVisitLocation");
        outMessage.setParamBoolean(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to throw the dice.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToThrowDices(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToThrowDices");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player has chosen a planet to visit.
     *
     * @param nickname The nickname of the player.
     * @param choice The index of the chosen planet.
     */
    @Override
    public void playerWantsToVisitPlanet(String nickname, int choice){
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToVisitPlanet");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's decision to accept or refuse a reward.
     *
     * @param nickname The nickname of the player.
     * @param choice True if the player accepts the reward; false otherwise.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToAcceptTheReward(String nickname, Boolean choice) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToAcceptTheReward");
        outMessage.setParamBoolean(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }


    /**
     * Notifies the server of the player's selection of double engines and associated battery boxes.
     *
     * @param nickname The nickname of the player.
     * @param doubleEnginesCoords The coordinates of the selected double engines.
     * @param batteryBoxesCoords The coordinates of the battery boxes used to activate them.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseDoubleEngines");
        outMessage.setParamActivableCoordinates(doubleEnginesCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxesCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's selection of double cannons and associated battery boxes.
     *
     * @param nickname The nickname of the player.
     * @param doubleCannonsCoords The coordinates of the selected double cannons.
     * @param batteryBoxesCoords The coordinates of the battery boxes used to activate them.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseDoubleCannons");
        outMessage.setParamActivableCoordinates(doubleCannonsCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxesCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's attempt to handle a small meteorite using shields and battery boxes.
     *
     * @param nickname The nickname of the player.
     * @param shieldCoords The coordinates of the shields used.
     * @param batteryBoxCoords The coordinates of the battery boxes powering the shields.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleSmallMeteorite");
        outMessage.setParamActivableCoordinates(shieldCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's attempt to handle a big meteorite using double cannons and battery boxes.
     *
     * @param nickname The nickname of the player.
     * @param doubleCannonCoords The coordinates of the double cannons used.
     * @param batteryBoxCoords The coordinates of the battery boxes powering the cannons.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigMeteorite");
        outMessage.setParamActivableCoordinates(doubleCannonCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to handle a big shot.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerHandleBigShot(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigShot");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's selection of storage locations.
     *
     * @param nickname The nickname of the player.
     * @param storageCoords The coordinates of the chosen storage components.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerChoseStorage(String nickname, List<Coordinates> storageCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseStorage");
        outMessage.setParamActivableCoordinates(storageCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server of the player's selection of cabins.
     *
     * @param nickname The nickname of the player.
     * @param cabinsCoords The coordinates of the chosen cabins.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerChoseCabins(String nickname, List<Coordinates> cabinsCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseCabins");
        outMessage.setParamCabinCoordinates(cabinsCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server to spread the epidemic event for the current player.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void spreadEpidemic(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "spreadEpidemic");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the Stardust event has been triggered.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void stardustEvent(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "stardustEvent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the crew members have been evaluated.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void evaluatedCrewMembers(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "evaluatedCrewMembers");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player's hourglass has ended.
     *
     * @param nickname The nickname of the player.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void notifyHourglassEnded(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "notifyHourglassEnded");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Notifies the server that the player wants to focus on one of their reserved components.
     *
     * @param nickname The nickname of the player.
     * @param choice The index of the reserved component to focus on.
     * @throws RemoteException If communication with the server fails.
     */
    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToFocusReservedComponent");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Initiates checking of the ship board after an attack
     *
     * @param nickname The player's nickname
     * @throws IOException If communication fails
     */
    public void startCheckShipBoardAfterAttack(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "startCheckShipBoardAfterAttack");
        out.println(ClientSerializer.serialize(outMessage));
    }

    /**
     * Debug method to skip to the last card
     *
     * @throws IOException If communication fails
     */
    @Override
    public void debugSkipToLastCard() throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "debugSkipToLastCard");
        out.println(ClientSerializer.serialize(outMessage));
    }
}
