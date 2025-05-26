package it.polimi.ingsw.is25am33.network.socket;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServerManager implements Runnable, CallableOnClientController {

    final int port = 1234;
    DNS dns;

    private final Map<String, CallableOnGameController> gameControllers = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> writers = new ConcurrentHashMap<>();
    private boolean readInput = true;

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
                                if (line.equals("exit")) {
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
                        e.printStackTrace();
                    }
                });
            } catch (final IOException e) {
                break;
            }
        }
        executor.shutdown();
    }

    public Map<String, PrintWriter> getWriters() {
        return writers;
    }

    private void performAction(SocketMessage inMessage, PrintWriter out) throws IOException {

        String nickname = inMessage.getSenderNickname();
        String action = inMessage.getActions();
        SocketMessage outMessage;
        GameInfo gameInfo;

        switch (action) {

            case "leaveGame":
                gameControllers.get(nickname).leaveGame(nickname);
                writers.remove(nickname);
                break;

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
                gameControllers.get(nickname).playerPicksHiddenComponent(nickname);
                break;

            case "playerWantsToFocusReservedComponent":
                gameControllers.get(nickname).playerWantsToFocusReservedComponent(nickname, inMessage.getParamInt());
                break;

            case "playerWantsToPlaceFocusedComponent":
                Coordinates coordinates = inMessage.getParamCoordinates();
                gameControllers.get(nickname).playerWantsToPlaceFocusedComponent(nickname, coordinates, inMessage.getParamInt());
                break;

            case "playerWantsToWatchLittleDeck":
                boolean response = gameControllers.get(nickname).playerWantsToWatchLittleDeck(nickname, inMessage.getParamInt());
                outMessage = new SocketMessage("server", "notifyLittleDeckVisibility");
                outMessage.setParamBoolean(response);

                out.println(ServerSerializer.serialize(outMessage));
                break;

            case "playerEndsBuildShipBoardPhase":
                gameControllers.get(nickname).playerEndsBuildShipBoardPhase(nickname);
                break;

            case "playerPlacePlaceholder":
                gameControllers.get(nickname).playerPlacePlaceholder(nickname);
                break;

            case "playerWantsToReleaseLittleDeck":
                gameControllers.get(nickname).playerWantsToReleaseLittleDeck(nickname, inMessage.getParamInt());
                break;

            case "playerWantsToReserveFocusedComponent":
                gameControllers.get(nickname).playerWantsToReserveFocusedComponent(nickname);
                break;

            case "playerWantsToRestartHourglass":
                gameControllers.get(nickname).playerWantsToRestartHourglass(nickname);
                break;

            case "playerWantsToReleaseFocusedComponent":
                gameControllers.get(nickname).playerWantsToReleaseFocusedComponent(nickname);
                break;

            case "playerWantsToRemoveComponent":
                Coordinates coordinatesToRemove = inMessage.getParamCoordinates();
                gameControllers.get(nickname).playerWantsToRemoveComponent(nickname, coordinatesToRemove);
                break;

            case "playerChoseShipPart":
                Set<Coordinates> chosenShipPart = inMessage.getParamShipPart();
                gameControllers.get(nickname).playerChoseShipPart(nickname, chosenShipPart);
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

            case "playerChoseCabins":
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

            case "playerPicksVisibleComponent":
                gameControllers.get(nickname).playerPicksVisibleComponent(nickname, inMessage.getParamInt());
                break;

            case "playerChoseStorage":
                gameControllers.get(nickname).playerChoseStorage(nickname, inMessage.getParamActivableCoordinates());
                break;

            case "spreadEpidemic":
                gameControllers.get(nickname).spreadEpidemic(nickname);
                break;

            case "stardustEvent":
                gameControllers.get(nickname).stardustEvent(nickname);
                break;

            case "notifyHourglassEnded":
                gameControllers.get(nickname).notifyHourglassEnded(nickname);
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

    public void checkWriterStatus(PrintWriter writer,String nickname) throws IOException{
        if(writer.checkError()){
            writers.remove(nickname);
            throw new IOException("Writer is null");
        }

    }

    @Override
    public void notifyShipCorrect(String nicknameToNotify) throws IOException {
        // TODO rimuovere
    }

    @Override
    public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyGameInfos");
        outMessage.setParamGameInfo(gameInfos);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyNewPlayerJoined");
        outMessage.setParamGameId(gameId);
        outMessage.setParamString(newPlayerNickname);
        outMessage.setParamPlayerColor(color);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameStarted");
        outMessage.setParamGameInfo(List.of(gameInfo));
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyHourglassRestarted");
        outMessage.setParamInt(flipsLeft);
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyStopHourglass(String nicknameToNotify) {
        SocketMessage outMessage = new SocketMessage("server", "notifyStopHourglass");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyFirstToEnter(String nicknameToNotify) {
        SocketMessage outMessage = new SocketMessage("server", "notifyFirstToEnter");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException {
        //TODO
    }

    @Override
    public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException {
        //TODO
    }

    @Override
    public void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws IOException {

    }

    @Override
    public void notifyRemovalResult(String nicknameToNotify, boolean success) throws IOException {

    }

    @Override
    public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyInvalidShipBoard");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyValidShipBoard");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyShipPartsGeneratedDueToRemoval");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        outMessage.setParamShipParts(shipParts);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    @Override
    public void notifyCardStarted(String nicknameToNotify) throws IOException {
        //TODO
    }

    @Override
    public void notifyGameState(String nickname, GameState gameState) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameState");
        outMessage.setParamGameState(gameState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyDangerousObjAttack(String nickname, DangerousObj dangerousObj) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyDangerousObjAttack");
        outMessage.setParamDangerousObj(dangerousObj);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrPlayerChanged");
        outMessage.setParamString(nickname);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws IOException{
        //TODO da aggisutare con clientCard
//        SocketMessage outMessage = new SocketMessage("server", "notifyCurrAdventureCard");
//        outMessage.setParamString(adventureCard);
//        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
//        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyCardState(String nickname, CardState cardState) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCardState");
        outMessage.setParamCardState(cardState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyChooseComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }


    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyReleaseComponent");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyBookedComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyAddVisibleComponents");
        outMessage.setParamInt(index);
        outMessage.setParamComponent(component);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRemoveVisibleComponents");
        outMessage.setParamInt(index);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyComponentPlaced");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        outMessage.setParamCoordinates(coordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException{
        // TODO
    }

    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Object>> componentsPerType) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyShipBoardUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void  notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerCredits");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(credits);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void  notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyEliminatedPlayer");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void  notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRankingUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(newPosition);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    @Override
    public void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDeck) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyVisibleDeck");
        outMessage.setParamLittleVisibleDecks(littleVisibleDeck);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    @Override
    public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayer) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerDisconnected");
        outMessage.setParamString(disconnectedPlayer);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

}

