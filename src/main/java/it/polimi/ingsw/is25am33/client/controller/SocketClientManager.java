package it.polimi.ingsw.is25am33.client.controller;

import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
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
    public List<GameInfo> getAvailableGames() throws IOException {

        SocketMessage outMessage = new SocketMessage(nickname, "getAvailableGames");

        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyAvailableGames"));

        if (response.getActions().equals("notifyAvailableGames")) {
            return response.getParamGameInfo();
        }

        throw new IOException("Unexpected response: " + response.getActions());

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
                        this.gameState = notification.getParamGameState();
                        ((ClientController) clientController).getClientModel().setGameState(gameState);
                        clientController.notifyGameStarted(nickname, gameState, notification.getParamGameInfo().getFirst());
                    }
                    break;

                // Altri casi...
                default:
                    System.err.println("Unknown notification: " + notification.getActions());
            }
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    @Override
    public Component playerPicksHiddenComponent(String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerPicksHiddenComponent");
        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("notifyPickedComponent"));
        return response.getParamComponent();
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
    public void playerChoseToEndBuildShipBoardPhase(String nickname) {
        // TODO
    }

    @Override
    public Component[][] getShipBoardOf(String otherPlayerNickname, String askerNickname) throws IOException {
        SocketMessage outMessage = new SocketMessage(nickname, "getShipBoardOf");
        outMessage.setParamString(otherPlayerNickname);
        SocketMessage response = sendAndWaitForSpecificResponse(outMessage, Set.of("showShipBoard"));
        return response.getParamShipBoardAsMatrix();
    }

    @Override
    public Component playerPicksVisibleComponent(String nickname, Integer choice) throws RemoteException {
        return null;
    }

    @Override
    public Map<Integer, Component> showPlayerVisibleComponent(String nickname) throws RemoteException {
        return null;
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
    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException {
        SocketMessage outMessage = new SocketMessage(nickname, "playerChoseDoubleEngines");
        outMessage.setParamActivableCoordinates(doubleEnginesCoords);
        outMessage.setParamBatteryBoxCoordinates(batteryBoxesCoords);

        out.println(ClientSerializer.serialize(outMessage));
    }

    

}
