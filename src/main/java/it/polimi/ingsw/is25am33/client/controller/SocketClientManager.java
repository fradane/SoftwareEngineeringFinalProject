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

    // Coda per memorizzare le notifiche mentre aspettiamo risposte specifiche
    private final Queue<SocketMessage> notificationBuffer = new ConcurrentLinkedQueue<>();

    // Flag che indica quando siamo in attesa di una risposta specifica
    private volatile boolean waitingForResponse = false;
    private volatile Set<String> expectedResponseActions;
    private volatile CompletableFuture<SocketMessage> responseFuture = null;

    // Lock per sincronizzare le operazioni
    private final Object lock = new Object();

    public SocketClientManager(CallableOnClientController clientController) {
        this.clientController = clientController;
    }

    /**
     * Invia un messaggio e aspetta una risposta specifica
     */
    private SocketMessage sendAndWaitForSpecificResponse(SocketMessage message, Set<String> expectedActions) throws RemoteException {
        synchronized (lock) {
            // Impostiamo lo stato di attesa
            waitingForResponse = true;
            expectedResponseActions = expectedActions;
            responseFuture = new CompletableFuture<>();

            // Inviamo la richiesta
            out.println(ClientSerializer.serialize(message));
        }

        try {
            // Attendiamo la risposta
            SocketMessage response = responseFuture.get();

            // Elaboriamo le notifiche in attesa
            processBufferedNotifications();

            return response;
        } catch (InterruptedException | ExecutionException e) {
            //TODO capire dove gestirle queste eccezioni
            throw new RemoteException("Failed to get response: " + e.getMessage(), e);
        } finally {
            synchronized (lock) {
                waitingForResponse = false;
                expectedResponseActions = null;

                responseFuture = null;
            }
        }
    }

    @Override
    public GameController getController(String gameId)  throws RemoteException{
        return null;
    }

    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException {
        return null;
    }

    @Override
    public void leaveGameAfterCreation(String nickname, Boolean isFirst) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "leaveGameAfterCreation");
        outMessage.setParamBoolean(isFirst);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void leaveGameBeforeCreation(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "leaveGameBeforeCreation");
        out.println(ClientSerializer.serialize(outMessage));
    }

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
                        clientController.notifyShipBoardUpdate(null, notification.getParamString(), notification.getParamShipBoardAsMatrix(),notification.getParamComponentsPerType() );
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
                                notification.getParamComponentsPerType()
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
                                notification.getParamComponentsPerType()
                        );
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

                default:
                    System.err.println("Unknown notification: " + notification.getActions());
            }
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void pingToServerFromClient(String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage(nickname, "PING");
        out.println(ClientSerializer.serialize(outMessage));
        //System.out.println("Ping inviato al server");
    }

    public void pongToServerFromClient(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "PONG");
        out.println(ClientSerializer.serialize(outMessage));
        //System.out.println("Pong inviato al server");
    }

    @Override
    public void playerPicksHiddenComponent(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksHiddenComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToPlaceFocusedComponent");
        outMessage.setParamCoordinates(coordinates);
        outMessage.setParamInt(rotation);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReserveFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReleaseFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToRestartHourglass(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToRestartHourglass");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToRemoveComponent(String nickname, Coordinates coordinate) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToRemoveComponent");
        outMessage.setParamCoordinates(coordinate);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerChoseShipPart(String nickname, Set<Coordinates> shipPart) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseShipPart");
        outMessage.setParamShipPart(shipPart);
        out.println(ClientSerializer.serialize(outMessage));
    }


    @Override
    public void playerEndsBuildShipBoardPhase(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerEndsBuildShipBoardPhase");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerPlacesPawn(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPlacePlaceholder");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "handleClientChoice");
        outMessage.setParamChoice(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "submitCrewChoices");
        outMessage.setParamCrewChoices(choices);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void requestPrefabShips(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "requestPrefabShips");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void requestSelectPrefabShip(String nickname, String prefabShipId) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "requestSelectPrefabShip");
        outMessage.setParamString(prefabShipId);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToLand(String nickname) throws IOException {
        //TODO
    }

    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksVisibleComponent");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToVisitLocation(String nickname, Boolean choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToVisitLocation");
        outMessage.setParamBoolean(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToThrowDices(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToThrowDices");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToVisitPlanet(String nickname, int choice){
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToVisitPlanet");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToAcceptTheReward(String nickname, Boolean choice) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToAcceptTheReward");
        outMessage.setParamBoolean(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }


    @Override
    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseDoubleEngines");
        outMessage.setParamActivableCoordinates(doubleEnginesCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxesCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseDoubleCannons");
        outMessage.setParamActivableCoordinates(doubleCannonsCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxesCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerChoseCabin(String nickname, List<Coordinates> cabinCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseCabins");
        outMessage.setParamCabinCoordinates(cabinCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleSmallMeteorite");
        outMessage.setParamActivableCoordinates(shieldCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigMeteorite");
        outMessage.setParamActivableCoordinates(doubleCannonCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleBigShot(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigShot");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerChoseStorage(String nickname, List<Coordinates> storageCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseStorage");
        outMessage.setParamActivableCoordinates(storageCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void spreadEpidemic(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "spreadEpidemic");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void stardustEvent(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "stardustEvent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void evaluatedCrewMembers(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "evaluatedCrewMembers");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public boolean playerWantsToWatchLittleDeck(String nickname, int littleDeckChoice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToWatchLittleDeck");
        outMessage.setParamInt(littleDeckChoice);

        SocketMessage inMessage = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyLittleDeckVisibility"));
        return inMessage.getParamBoolean();
    }

    @Override
    public void playerWantsToReleaseLittleDeck(String nickname, int littleDeckChoice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReleaseLittleDeck");
        outMessage.setParamInt(littleDeckChoice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void notifyHourglassEnded(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "notifyHourglassEnded");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToFocusReservedComponent");
        outMessage.setParamInt(choice);
        out.println(ClientSerializer.serialize(outMessage));
    }

    public void startCheckShipBoardAfterAttack(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "startCheckShipBoardAfterAttack");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void debugSkipToLastCard() throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "debugSkipToLastCard");
        out.println(ClientSerializer.serialize(outMessage));
    }
}
