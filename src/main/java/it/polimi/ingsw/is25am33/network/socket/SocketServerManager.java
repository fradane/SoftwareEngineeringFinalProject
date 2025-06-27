package it.polimi.ingsw.is25am33.network.socket;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import it.polimi.ingsw.is25am33.network.DNS;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
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

    private final DNS dns;

    private final Map<String, CallableOnGameController> gameControllers = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> writers = new ConcurrentHashMap<>();

    /**
     * Constructs a new SocketServerManager with the specified DNS.
     *
     * @param dns The DNS service to be used by this socket server
     */
    public SocketServerManager(DNS dns) {
        this.dns = dns;
    }

    /**
     * Starts the socket server and listens for incoming client connections.
     * Each client connection is handled in a separate thread from a thread pool.
     * The server continuously accepts new connections until an IOException occurs.
     */
    @Override
    public void run() {

        final ExecutorService executor = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(NetworkConfiguration.DEFAULT_SOCKET_SERVER_PORT);
        } catch (final IOException e) {
            System.err.println("ERROR in socket.run(): " + e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("[Socket] Server Socket ready on localhost:" + NetworkConfiguration.DEFAULT_SOCKET_SERVER_PORT);
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

    /**
     * Returns the map of client nickname to PrintWriter objects.
     *
     * @return A map containing PrintWriter objects for connected clients
     */
    public Map<String, PrintWriter> getWriters() {
        return writers;
    }

    private void performAction(SocketMessage inMessage, PrintWriter out) throws IOException {

        String nickname = inMessage.getSenderNickname();
        String action = inMessage.getActions();
        SocketMessage outMessage;
        GameInfo gameInfo;

        switch (action) {

            case "leaveGameAfterCreation":
                gameControllers.get(nickname).leaveGameAfterCreation(nickname);
                writers.remove(nickname);
                break;

            case "leaveGameBeforeCreation":
                dns.leaveGameBeforeCreation(nickname);
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

            case "playerEndsBuildShipBoardPhase":
                gameControllers.get(nickname).playerEndsBuildShipBoardPhase(nickname);
                break;

            case "playerPlacePlaceholder":
                gameControllers.get(nickname).playerPlacesPawn(nickname);
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
                gameControllers.get(nickname).playerChoseCabins(nickname, inMessage.getParamCabinCoordinates());
                break;

            case "playerHandleSmallMeteorite":
                gameControllers.get(nickname).playerHandleSmallDanObj(nickname, inMessage.getParamActivableCoordinates(), inMessage.getParamBatteryBoxCoordinates());
                break;

            case "playerHandleBigMeteorite":
                gameControllers.get(nickname).playerHandleBigMeteorite(nickname, inMessage.getParamActivableCoordinates(), inMessage.getParamBatteryBoxCoordinates());
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

            case "evaluatedCrewMembers":
                gameControllers.get(nickname).evaluatedCrewMembers(nickname);
                break;

            case "notifyHourglassEnded":
                gameControllers.get(nickname).notifyHourglassEnded(nickname);
                break;

            case "handleClientChoice":
                gameControllers.get(nickname).handleClientChoice(nickname, inMessage.getParamChoice());
                break;

            case "submitCrewChoices":
                gameControllers.get(nickname).submitCrewChoices(nickname, inMessage.getParamCrewChoices());
                break;

            case "requestPrefabShips":
                gameControllers.get(nickname).requestPrefabShips(nickname);
                break;

            case "requestSelectPrefabShip":
                gameControllers.get(nickname).requestSelectPrefabShip(nickname, inMessage.getParamString());
                break;

            case "PING":
                dns.pingToServerFromClient(nickname);
                break;

            case "PONG":
                dns.pongToServerFromClient(nickname);
                break;

            case "startCheckShipBoardAfterAttack":
                    gameControllers.get(nickname).startCheckShipBoardAfterAttack(nickname);
                break;

            case "debugSkipToLastCard":
                gameControllers.get(nickname).debugSkipToLastCard();
                break;

            case "playerWantsToLand":
                gameControllers.get(nickname).playerWantsToLand(nickname);
                break;

            default:
                System.err.println("Invalid action: " + action);
                throw new RemoteException("Not properly formatted json");
        }

    }

    /**
     * Checks if the given PrintWriter is still valid and removes it from the writers map if not.
     *
     * @param writer The PrintWriter to check
     * @param nickname The nickname associated with the writer
     * @throws IOException If the writer is null or has encountered an error
     */
    public void checkWriterStatus(PrintWriter writer,String nickname) throws IOException{
        if(writer.checkError()){
            writers.remove(nickname);
            throw new IOException("Writer is null");
        }

    }

    /**
     * Notifies a client about available game information.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param gameInfos The list of game information to send
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyGameInfos");
        outMessage.setParamGameInfo(gameInfos);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that a new player has joined a game.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param gameId The ID of the game that was joined
     * @param newPlayerNickname The nickname of the player who joined
     * @param color The color chosen by the new player
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyNewPlayerJoined");
        outMessage.setParamGameId(gameId);
        outMessage.setParamString(newPlayerNickname);
        outMessage.setParamPlayerColor(color);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that a game has started.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param gameInfo The information about the started game
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameStarted");
        outMessage.setParamGameInfo(List.of(gameInfo));
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that the hourglass has been restarted.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who restarted the hourglass
     * @param flipsLeft The number of flips remaining for the hourglass
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyHourglassRestarted");
        outMessage.setParamInt(flipsLeft);
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client to stop the hourglass.
     *
     * @param nicknameToNotify The nickname of the client to notify
     */
    @Override
    public void notifyStopHourglass(String nicknameToNotify) {
        SocketMessage outMessage = new SocketMessage("server", "notifyStopHourglass");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }
    /**
     * Notifies a client that they are the first to enter a phase.
     *
     * @param nicknameToNotify The nickname of the client to notify
     */
    @Override
    public void notifyFirstToEnter(String nicknameToNotify) {
        SocketMessage outMessage = new SocketMessage("server", "notifyFirstToEnter");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a visible component has been stolen.
     *
     * @param nicknameToNotify The nickname of the client to notify
     */
    @Override
    public void notifyStolenVisibleComponent(String nicknameToNotify) {
        SocketMessage outMessage = new SocketMessage("server", "notifyStolenVisibleComponent");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about an update to the current adventure card.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param adventureCard The updated adventure card
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrAdventureCardUpdate");
        outMessage.setParamClientCard(adventureCard);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a player has visited a planet.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who visited the planet
     * @param adventureCard The adventure card associated with the planet visit
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerVisitedPlanet");
        outMessage.setParamString(nickname);
        outMessage.setParamClientCard(adventureCard);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that the crew placement phase has begun.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCrewPlacementPhase(String nicknameToNotify) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyCrewPlacementPhase");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that crew placement has been completed for a player.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param playerNickname The nickname of the player who completed crew placement
     * @param shipMatrix The ship matrix after crew placement
     * @param componentsPerType Map of components organized by their type
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyCrewPlacementComplete");
        outMessage.setParamString(playerNickname);
        outMessage.setParamShipMatrix(shipMatrix);
        outMessage.setParamComponentsPerType(componentsPerType);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about available prefabricated ships.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param prefabShips List of available prefabricated ship information
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPrefabShipsAvailable");
        outMessage.setParamPrefabShips(prefabShips);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a player has selected a prefabricated ship.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param playerNickname The nickname of the player who selected the ship
     * @param prefabShipInfo Information about the selected prefabricated ship
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipInfo) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerSelectedPrefabShip");
        outMessage.setParamString(playerNickname);
        outMessage.setParamPrefabShips(List.of(prefabShipInfo));
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about the result of a prefabricated ship selection.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param success Whether the selection was successful
     * @param errorMessage Error message in case of failure
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPrefabShipSelectionResult");
        outMessage.setParamBoolean(success);
        outMessage.setParamString(errorMessage);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that infected crew members have been removed.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param cabinCoordinatesWithNeighbors Set of coordinates of cabins with infected crew members and their neighbors
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyInfectedCrewMembersRemoved");
        outMessage.setParamShipPart(cabinCoordinatesWithNeighbors);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about final player data and rankings.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param finalRanking List of final player data in ranking order
     * @param playersNicknamesWithPrettiestShip List of players with the prettiest ships
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayersFinalData");
        outMessage.setParamPlayerFinalDataRanking(finalRanking);
        outMessage.setParamStringList(playersNicknamesWithPrettiestShip);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a player has landed early.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who landed early
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerEarlyLanded");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that there are no more hidden components available.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyNoMoreHiddenComponents(String nicknameToNotify) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyNoMoreHiddenComponents");
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a ship board is invalid.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param shipOwnerNickname The nickname of the ship owner
     * @param shipMatrix The ship matrix to evaluate
     * @param incorrectlyPositionedComponentsCoordinates Set of coordinates where components are incorrectly positioned
     * @param componentsPerType Map of components organized by their type
     * @param notActiveComponentsList List of components that are not active
     * @throws RemoteException If a remote communication error occurs
     */
    @Override
    public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyInvalidShipBoard");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        outMessage.setParamComponentsPerType(componentsPerType);
        outMessage.setParamComponentList(notActiveComponentsList);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that a ship board is valid.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param shipOwnerNickname The nickname of the ship owner
     * @param shipMatrix The ship matrix that was validated
     * @param incorrectlyPositionedComponentsCoordinates Set of coordinates where components are incorrectly positioned
     * @param componentsPerType Map of components organized by their type
     * @param notActiveComponentsList List of components that are not active
     * @throws RemoteException If a remote communication error occurs
     */
    @Override
    public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyValidShipBoard");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        outMessage.setParamComponentsPerType(componentsPerType);
        outMessage.setParamComponentList(notActiveComponentsList);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client that ship parts have been generated due to component removal.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param shipOwnerNickname The nickname of the ship owner
     * @param shipMatrix The updated ship matrix
     * @param incorrectlyPositionedComponentsCoordinates Set of coordinates where components are incorrectly positioned
     * @param shipParts Set of ship parts generated
     * @param componentsPerType Map of components organized by their type
     * @throws RemoteException If a remote communication error occurs
     */
    @Override
    public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
        SocketMessage outMessage = new SocketMessage("server", "notifyShipPartsGeneratedDueToRemoval");
        outMessage.setParamString(shipOwnerNickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamIncorrectlyPositionedCoordinates(incorrectlyPositionedComponentsCoordinates);
        outMessage.setParamShipParts(shipParts);
        outMessage.setParamComponentsPerType(componentsPerType);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about a change in game state.
     *
     * @param nickname The nickname of the client to notify
     * @param gameState The new game state
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyGameState(String nickname, GameState gameState) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyGameState");
        outMessage.setParamGameState(gameState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client about a dangerous object attack.
     *
     * @param nickname The nickname of the client to notify
     * @param dangerousObj The dangerous object that is attacking
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyDangerousObjAttack");
        outMessage.setParamDangerousObj(dangerousObj);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client about a change in the current player.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the new current player
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrPlayerChanged");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client about the current adventure card.
     *
     * @param nickname The nickname of the client to notify
     * @param adventureCard The current adventure card
     * @param isFirstTime Whether this is the first time the card is being shown
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCurrAdventureCard");
        outMessage.setParamClientCard(adventureCard);
        outMessage.setParamBoolean(isFirstTime);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client about a change in card state.
     *
     * @param nickname The nickname of the client to notify
     * @param cardState The new card state
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyCardState(String nickname, CardState cardState) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCardState");
        outMessage.setParamCardState(cardState);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client that a player has focused on a component.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who focused the component
     * @param component The component that was focused
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyChooseComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }


    /**
     * Notifies a client that a player has released a component.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who released the component
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyReleaseComponent");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that a player has booked a component.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who booked the component
     * @param component The component that was booked
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyBookedComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that a visible component has been added.
     *
     * @param nickname The nickname of the client to notify
     * @param index The index where the component was added
     * @param component The component that was added
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyAddVisibleComponents(String nickname, int index, Component component) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyAddVisibleComponents");
        outMessage.setParamInt(index);
        outMessage.setParamComponent(component);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client that a visible component has been removed.
     *
     * @param nickname The nickname of the client to notify
     * @param index The index from which the component was removed
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyRemoveVisibleComponents(String nickname, int index) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRemoveVisibleComponents");
        outMessage.setParamInt(index);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client that a component has been placed.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who placed the component
     * @param component The component that was placed
     * @param coordinates The coordinates where the component was placed
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyComponentPlaced");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(component);
        outMessage.setParamCoordinates(coordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client about an update to a ship board.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the ship owner
     * @param shipMatrix The updated ship matrix
     * @param componentsPerType Map of components organized by their type
     * @param notActiveComponentsList List of components that are not active
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType, List<Component> notActiveComponentsList) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyShipBoardUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamShipBoardAsMatrix(shipMatrix);
        outMessage.setParamComponentsPerType(componentsPerType);
        outMessage.setParamComponentList(notActiveComponentsList);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client about a player's credits.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player whose credits are being reported
     * @param credits The number of credits
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void  notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerCredits");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(credits);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client that a player has been eliminated.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who was eliminated
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void  notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyEliminatedPlayer");
        outMessage.setParamString(nickname);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client about an update to the player ranking.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player whose ranking changed
     * @param newPosition The new position in the ranking
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void  notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyRankingUpdate");
        outMessage.setParamString(nickname);
        outMessage.setParamInt(newPosition);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nicknameToNotify),nicknameToNotify);
    }

    /**
     * Notifies a client about the visible deck of cards.
     *
     * @param nickname The nickname of the client to notify
     * @param littleVisibleDeck The visible deck information
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyVisibleDeck(String nickname, List<List<ClientCard>> littleVisibleDeck) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyVisibleDeck");
        outMessage.setParamLittleVisibleDecks(littleVisibleDeck);
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        checkWriterStatus(writers.get(nickname),nickname);
    }

    /**
     * Notifies a client that a player has disconnected.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param disconnectedPlayer The nickname of the player who disconnected
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayer) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyPlayerDisconnected");
        outMessage.setParamString(disconnectedPlayer);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Forces a client to disconnect from a game.
     *
     * @param nicknameToNotify The nickname of the client to disconnect
     * @param gameId The ID of the game from which to disconnect
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void forcedDisconnection(String nicknameToNotify,String gameId) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "forcedDisconnection");
        outMessage.setParamString(gameId);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Sends a ping message to a client.
     *
     * @param nickname The nickname of the client to ping
     * @throws IOException If an I/O error occurs during notification
     */
    public void pingToClientFromServer(String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "PING");
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        //System.out.println("Ping inviato a " + nickname);
    }

    /**
     * Sends a pong message to a client.
     *
     * @param nickname The nickname of the client to send the pong to
     * @throws IOException If an I/O error occurs during notification
     */
    public void pongToClientFromServer(String nickname) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "PONG");
        writers.get(nickname).println(ServerSerializer.serialize(outMessage));
        //System.out.println("Pong inviato a " + nickname);
    }

    /**
     * Notifies a client about the components per type for a player.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param playerNickname The nickname of the player whose components are being reported
     * @param componentsPerType Map of components organized by their type
     */
    public void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType ){
        SocketMessage outMessage = new SocketMessage("server", "notifyComponentPerType");
        outMessage.setParamString(playerNickname);
        outMessage.setParamComponentsPerType(componentsPerType);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about the coordinates of a component that was hit.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player whose component was hit
     * @param coordinates The coordinates of the hit component
     * @throws IOException If an I/O error occurs during notification
     */
    public void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyCoordinateOfComponentHit");
        outMessage.setParamString(nickname);
        outMessage.setParamCoordinates(coordinates);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about the least resourced player.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nicknameAndMotivations The nickname of the least resourced player and the motivations
     * @throws IOException If an I/O error occurs during notification
     */
    public  void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations) throws IOException{
        SocketMessage outMessage = new SocketMessage("server", "notifyLeastResourcedPlayer");
        outMessage.setParamString(nicknameAndMotivations);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about an error that occurred while booking a component.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who attempted to book the component
     * @param focusedComponent The component that was being booked
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyErrorWhileBookingComponent");
        outMessage.setParamString(nickname);
        outMessage.setParamComponent(focusedComponent);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about components that are not active.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param nickname The nickname of the player who owns the components
     * @param notActiveComponents List of components that are not active
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException {
        SocketMessage outMessage = new SocketMessage("server", "notifyNotActiveComponents");
        outMessage.setParamString(nickname);
        outMessage.setParamComponentList(notActiveComponents);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

    /**
     * Notifies a client about an error related to storage.
     *
     * @param nicknameToNotify The nickname of the client to notify
     * @param errorMessage The error message describing the storage issue
     * @throws IOException If an I/O error occurs during notification
     */
    @Override
    public void notifyStorageError(String nicknameToNotify, String errorMessage) throws IOException {
        SocketMessage outMessage = new SocketMessage(nicknameToNotify, "notifyStorageError");
        outMessage.setParamString(errorMessage);
        writers.get(nicknameToNotify).println(ServerSerializer.serialize(outMessage));
    }

}

