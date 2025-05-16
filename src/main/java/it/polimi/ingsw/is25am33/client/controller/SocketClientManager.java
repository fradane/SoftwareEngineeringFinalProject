package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
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
    private final boolean running = true;

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
    private SocketMessage sendAndWaitForSpecificResponse(SocketMessage message, Set<String> expectedActions) throws IOException {
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
            throw new IOException("Failed to get response: " + e.getMessage(), e);
        } finally {
            synchronized (lock) {
                waitingForResponse = false;
                expectedResponseActions = null;

                responseFuture = null;
            }
        }
    }

    @Override
    public GameController getController(String gameId) throws RemoteException {
        return null;
    }

    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException {
        return null;
    }

    @Override
    public void leaveGame(String gameId) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "leaveGame");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public boolean registerWithNickname(String nickname, CallableOnClientController controller) throws IOException {

        SocketMessage outMessage = new SocketMessage(nickname, "registerWithNickname");
        outMessage.setParamString(nickname);

        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyRegistrationSuccess", "notifyNicknameAlreadyExists"));

        if (response.getActions().equals("notifyRegistrationSuccess")) {
            this.nickname = nickname;
            return true;
        } else if (response.getActions().equals("notifyNicknameAlreadyExists")) {
            return false;
        }

        throw new IOException("Unexpected response: " + response.getActions());

    }

    public void connect() throws IOException {
        Socket socket = new Socket("127.0.0.1", 1234);
        System.out.println("Connected to server at " + socket.getRemoteSocketAddress());

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());

        startMessageHandlerThread();
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
        Thread messageHandler = new Thread(() -> {
            while (running) {
                try {
                    if (in.hasNextLine()) {
                        String line = in.nextLine();
                        handleServerMessage(line);
                    }
                } catch (Exception e) {
                    System.err.println("Error in message handler: " + e.getMessage());
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
                        clientController.notifyCurrAdventureCard(nickname, notification.getParamString());
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
                        clientController.notifyShipBoardUpdate(null, notification.getParamString(), notification.getParamShipBoardAsMatrix());
                    }
                    break;

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

                default:
                    System.err.println("Unknown notification: " + notification.getActions());
            }
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    @Override
    public void playerPicksHiddenComponent(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksHiddenComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToPlaceFocusedComponent");
        outMessage.setParamCoordinates(coordinates);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReserveFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToReleaseFocusedComponent");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerWantsToRestartHourglass(String nickname) {
        SocketMessage outMessage = new SocketMessage(nickname, "playerWantsToRestartHourglass");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerToRemoveComponent(String nickname, Component component) throws RemoteException {

    }

    @Override
    public void playerChooseShipPart(String nickname, List<Set<List<Integer>>> shipPart) throws RemoteException {

    }

    @Override
    public void playerChoseToEndBuildShipBoardPhase(String nickname) {
        // TODO
    }

    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) throws IOException {
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
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseCabin");
        outMessage.setParamCabinCoordinates(cabinCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleSmallDanObj(String nickname, Coordinates shieldCoords, Coordinates batteryBoxCoords) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleSmallMeteorite");
        outMessage.setParamActivableCoordinates(List.of(shieldCoords));
        outMessage.setParamBatteryBoxCoordinates(List.of(batteryBoxCoords));

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleBigMeteorite(String nickname, Coordinates doubleCannonCoords, Coordinates batteryBoxCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigMeteorite");
        outMessage.setParamActivableCoordinates(List.of(doubleCannonCoords));
        outMessage.setParamBatteryBoxCoordinates(List.of(batteryBoxCoords));

        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerHandleBigShot(String nickname) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerHandleBigShot");
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void playerChoseStorage(String nickname, Coordinates storageCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseStorage");
        outMessage.setParamCoordinates(storageCoords);
        out.println(ClientSerializer.serialize(outMessage));
    }

    @Override
    public void spreadEpidemic(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "spreadEpidemic");
        out.println(outMessage);
    }

    @Override
    public void stardustEvent(String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage(nickname, "stardustEvent");
        out.println(outMessage);
    }

    @Override
    public boolean playerWantsToWatchLittleDeck(String nickname, int littleDeckChoice) throws IOException {
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
}
