package it.polimi.ingsw.is25am33.network.socket;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.network.DNS;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerSerializer;
import it.polimi.ingsw.is25am33.serializationLayer.SocketMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServerManager implements Runnable, CallableOnClientController {

    final int port = 1234;
    DNS dns;

    private final Map<String, CallableOnGameController> gameControllers = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> writers = new ConcurrentHashMap<>();

    public SocketServerManager(DNS dns) {
        this.dns = dns;
    }

    @Override
    public void run() {

        final ExecutorService executor = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("[Socket] Server Socket pronto");
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        final Scanner in = new Scanner(socket.getInputStream());
                        final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        while (true) {
                            final String line = in.nextLine();
                            if (line.equals("quit")) {
                                break;
                            } else {

                                SocketMessage inMessage = ServerDeserializer.deserializeObj(line, SocketMessage.class);
                                performAction(inMessage, out);

                            }
                        }
                        in.close();
                        out.close();
                        socket.close();
                    } catch (final IOException e) {
                        System.err.println(e.getMessage());
                    }
                });
            } catch (final IOException e) {
                break;
            }
        }
        executor.shutdown();
    }

    private void performAction(SocketMessage inMessage, PrintWriter out) throws IOException {

        String nickname = inMessage.getSenderNickname();
        String action = inMessage.getActions();
        SocketMessage outMessage;
        GameInfo gameInfo;

        switch (action) {
            case "registerWithNickname":
                boolean registrationSuccess = dns.registerWithNickname(nickname, this);
                if (registrationSuccess) {
                    writers.put(nickname, out);
                    out.println(ServerSerializer.serialize(new SocketMessage("server", "notifyRegistrationSuccess")));
                } else {
                    out.println(ServerSerializer.serialize(new SocketMessage("server", "notifyNicknameAlreadyExists")));
                }
                break;

            case "getAvailableGames":
                List<GameInfo> availableGames = dns.getAvailableGames();
                outMessage = new SocketMessage("server", "notifyAvailableGames");
                outMessage.setParamGameInfo(availableGames);
                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "createGame":
                int numPlayers = inMessage.getParamInt();
                boolean isTestFlight = inMessage.getParamBoolean();
                PlayerColor color = inMessage.getParamPlayerColor();

                gameInfo = dns.createGame(color, numPlayers, isTestFlight, nickname);
                gameControllers.put(nickname, gameInfo.getGameController());
                outMessage = new SocketMessage("server", "notifyGameCreated");
                outMessage.setParamGameInfo(List.of(gameInfo));
                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "joinGame":
                String gameId = inMessage.getParamGameId();
                PlayerColor playerColor = inMessage.getParamPlayerColor();

                boolean result = dns.joinGame(gameId, nickname, playerColor);
                if (result) {
                    gameControllers.put(nickname, dns.getController(gameId));
                }
                outMessage = new SocketMessage("server", "notifyJoinGameResult");
                outMessage.setParamBoolean(result);
                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "playerPicksHiddenComponent":
                Component component = gameControllers.get(nickname).playerPicksHiddenComponent(nickname);
                outMessage = new SocketMessage("server", "notifyPickedComponent");
                outMessage.setParamComponent(component);

                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "playerWantsToPlaceFocusedComponent":
                Coordinates coordinates = inMessage.getParamCoordinates();
                gameControllers.get(nickname).playerWantsToPlaceFocusedComponent(nickname, coordinates);
                break;

            case "getShipBoardOf":
                Component[][] shipBoardAsMatrix = gameControllers.get(nickname).getShipBoardOf(inMessage.getParamString(), nickname);
                outMessage = new SocketMessage("server", "showShipBoard");
                outMessage.setParamShipBoardAsMatrix(shipBoardAsMatrix);

                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "playerWantsToReserveFocusedComponent":
                gameControllers.get(nickname).playerWantsToReserveFocusedComponent(nickname);
                break;

            case "playerWantsToReleaseFocusedComponent":
                gameControllers.get(nickname).playerWantsToReleaseFocusedComponent(nickname);
                break;

            case "playerWantsToVisitLocation":
                gameControllers.get(nickname).playerWantsToVisitLocation(nickname, inMessage.getParamBoolean());
                break;

            case "playerWantsToThrowDices":
                gameControllers.get(nickname).playerWantsToThrowDices(nickname);
                break;

            case "playerWantsToVisitPlanet":
                gameControllers.get(nickname).playerWantsToVisitPlanet(nickname, inMessage.getParamInt());
                break;

            case "playerWantsToAcceptTheReward":
                gameControllers.get(nickname).playerWantsToAcceptTheReward(nickname, inMessage.getParamBoolean());
                break;

            case "playerChoseDoubleEngines":
                gameControllers.get(nickname).playerChoseDoubleEngines(nickname, inMessage.getParamActivableCoordinates(), inMessage.getParamBatteryBoxCoordinates());
                break;

            case "playerChoseDoubleCannons":
                gameControllers.get(nickname).playerChoseDoubleCannons(nickname, inMessage.getParamActivableCoordinates(), inMessage.getParamBatteryBoxCoordinates());
                break;

            case "playerChoseCabin":
                gameControllers.get(nickname).playerChoseCabin(nickname, inMessage.getParamCabinCoordinates());
                break;

            case "playerHandleSmallMeteorite":
                gameControllers.get(nickname).playerHandleSmallDanObj(nickname, inMessage.getParamActivableCoordinates().getFirst(), inMessage.getParamBatteryBoxCoordinates().getFirst());
                break;

            case "playerHandleBigMeteorite":
                gameControllers.get(nickname).playerHandleBigMeteorite(nickname, inMessage.getParamActivableCoordinates().getFirst(), inMessage.getParamBatteryBoxCoordinates().getFirst());
                break;

            case "playerHandleBigShot":
                gameControllers.get(nickname).playerHandleBigShot(nickname);
                break;

            case "playerChoseStorage":
                gameControllers.get(nickname).playerChoseStorage(nickname, inMessage.getParamCoordinates());
                break;

            case "spreadEpidemic":
                gameControllers.get(nickname).spreadEpidemic(nickname);
                break;

            case "stardustEvent":
                gameControllers.get(nickname).stardustEvent(nickname);
                break;

            // TODO debug
            case "showMessage":
                String message = inMessage.getParamString();
                gameControllers.get(nickname).showMessage(message);
                break;

            default:
                System.err.println("Invalid action: " + action);
                throw new RemoteException("Not properly formatted json");
        }

    }

    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyNewPlayerJoined");
        outMessage.setParamGameId(gameId);
        outMessage.setParamString(newPlayerNickname);
        outMessage.setParamPlayerColor(color);

        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyGameStarted(String nicknameToNotify, GameState gameState, GameInfo gameInfo) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameStarted");
        outMessage.setParamGameState(gameState);
        outMessage.setParamGameInfo(List.of(gameInfo));

        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyGameState(String nickname, GameState gameState) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameState");
        outMessage.setParamGameState(gameState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyDangerousObjAttack");
        outMessage.setParamDangerousObj(dangerousObj);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname){
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrPlayerChanged");
        outMessage.setParamString(nickname);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyCurrAdventureCard(String nickname, AdventureCard adventureCard) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrAdventureCard");
        outMessage.setParamAdventureCard(adventureCard);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyCardState(String nickname, CardState cardState) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCardState");
        outMessage.setParamCardState(cardState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyChooseComponent(String nicknameToNotify, String nickname, Component component) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyChooseComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyReleaseComponent");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyBookedComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyAddVisibleComponents");
        outMessage.setParamInt(index);
        outMessage.setParamComponent(component);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRemoveVisibleComponents");
        outMessage.setParamInt(index);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyComponentPlaced");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        outMessage.setParamCoordinates(coordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyShipBoardUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void  notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerCredits");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(credits);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void  notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyEliminatedPlayer");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void  notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRankingUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(newPosition);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void  notifyVisibleDeck(String nickname, List<List<AdventureCard>> littleVisibleDeck) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyFlyingBoardUpdate");
        outMessage.setParamLittleVisibleDecks(littleVisibleDeck);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void  notifyHourglassStarted(String nicknameToNotify) throws RemoteException{
        SocketMessage outMessage = new SocketMessage("server", "notifyHourglassStarted");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }




}

