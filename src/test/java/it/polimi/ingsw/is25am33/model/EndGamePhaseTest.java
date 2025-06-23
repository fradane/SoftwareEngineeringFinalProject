package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipFactory;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level1ShipBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import it.polimi.ingsw.is25am33.network.DNS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.rmi.RemoteException;

public class EndGamePhaseTest {

    private GameModel gameModel;
    private DNS dns;
    private GameController gameController;

    @BeforeEach
    void setUp() throws RemoteException {
        // Crea un DNS reale
        dns = new DNS();
    }

    /**
     * Classe stub per simulare un client controller senza mock
     */
    private static class StubClientController implements CallableOnClientController {
        private final List<String> notifications = new ArrayList<>();

        @Override
        public void notifyShipCorrect(String nicknameToNotify) throws RemoteException {
            notifications.add("shipCorrect:" + nicknameToNotify);
        }

        @Override
        public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws RemoteException {
            notifications.add("gameInfos:" + nicknameToNotify);
        }

        @Override
        public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws RemoteException {
            notifications.add("playerJoined:" + newPlayerNickname);
        }

        @Override
        public void notifyGameStarted(String nicknameToNotify, GameInfo gameInfo) throws RemoteException {
            notifications.add("gameStarted:" + nicknameToNotify);
        }

        @Override
        public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws RemoteException {
            notifications.add("hourglassRestarted:" + nickname);
        }

        @Override
        public void notifyShipPartSelection(String nicknameToNotify, List<Set<List<Integer>>> shipParts) throws RemoteException {
            notifications.add("shipPartSelection:" + nicknameToNotify);
        }

        @Override
        public void notifyRemovalResult(String nicknameToNotify, boolean success) throws RemoteException {
            notifications.add("removalResult:" + success);
        }

        @Override
        public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("invalidShipBoard:" + shipOwnerNickname);
        }

        @Override
        public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("validShipBoard:" + shipOwnerNickname);
        }

        @Override
        public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("shipPartsGenerated:" + shipOwnerNickname);
        }

        @Override
        public void notifyCardStarted(String nicknameToNotify) throws RemoteException {
            notifications.add("cardStarted:" + nicknameToNotify);
        }

        @Override
        public void notifyGameState(String nickname, GameState gameState) throws RemoteException {
            notifications.add("gameState:" + gameState);
        }

        @Override
        public void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) throws RemoteException {
            notifications.add("dangerousObjAttack:" + dangerousObj.getType());
        }

        @Override
        public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws RemoteException {
            notifications.add("currPlayerChanged:" + nickname);
        }

        @Override
        public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws RemoteException {
            notifications.add("currAdventureCard:" + adventureCard.getCardName());
        }

        @Override
        public void notifyAddVisibleComponents(String nickname, int index, Component component) throws RemoteException {
            notifications.add("addVisibleComponent:" + index);
        }

        @Override
        public void notifyRemoveVisibleComponents(String nickname, int index) throws RemoteException {
            notifications.add("removeVisibleComponent:" + index);
        }

        @Override
        public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws RemoteException {
            notifications.add("componentPlaced:" + coordinates.getX() + "," + coordinates.getY());
        }

        @Override
        public void notifyIncorrectlyPositionedComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException {
            notifications.add("notifyIncorrectlyPositionedComponentPlaced:" + coordinates.getX() + "," + coordinates.getY());
        }

        @Override
        public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws RemoteException {
            notifications.add("focusedComponent:" + nickname);
        }

        @Override
        public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws RemoteException {
            notifications.add("releaseComponent:" + nickname);
        }

        @Override
        public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws RemoteException {
            notifications.add("bookedComponent:" + nickname);
        }

        @Override
        public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws RemoteException {
            notifications.add("playerCredits:" + nickname + "=" + credits);
        }

        @Override
        public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws RemoteException {
            notifications.add("rankingUpdate:" + nickname + "=" + newPosition);
        }

        @Override
        public void notifyStopHourglass(String nicknameToNotify) throws RemoteException {
            notifications.add("stopHourglass:" + nicknameToNotify);
        }

        @Override
        public void notifyFirstToEnter(String nicknameToNotify) throws RemoteException {
            notifications.add("firstToEnter:" + nicknameToNotify);
        }

        @Override
        public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws RemoteException {
            notifications.add("adventureCardUpdate:" + adventureCard.getCardName());
        }

        @Override
        public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws RemoteException {
            notifications.add("playerVisitedPlanet:" + nickname);
        }

        @Override
        public void notifyCrewPlacementPhase(String nicknameToNotify) throws RemoteException {
            notifications.add("crewPlacementPhase:" + nicknameToNotify);
        }

        @Override
        public void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("crewPlacementComplete:" + playerNickname);
        }

        @Override
        public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws RemoteException {
            notifications.add("eliminatedPlayer:" + nickname);
        }

        @Override
        public void notifyCardState(String nickname, CardState cardState) throws RemoteException {
            notifications.add("cardState:" + cardState);
        }

        @Override
        public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("shipBoardUpdate:" + nickname);
        }

        @Override
        public void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws RemoteException {
            notifications.add("componentHit:" + coordinates.getX() + "," + coordinates.getY());
        }

        @Override
        public void notifyVisibleDeck(String nickname, List<List<String>> littleVisibleDecks) throws RemoteException {
            notifications.add("visibleDeck:" + nickname);
        }

        @Override
        public void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {
            notifications.add("componentPerType:" + playerNickname);
        }

        @Override
        public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayerNickname) throws RemoteException {
            notifications.add("playerDisconnected:" + disconnectedPlayerNickname);
        }

        @Override
        public void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws RemoteException {
            notifications.add("prefabShipsAvailable:" + prefabShips.size());
        }

        @Override
        public void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipInfo) throws RemoteException {
            notifications.add("playerSelectedPrefabShip:" + playerNickname);
        }

        @Override
        public void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws RemoteException {
            notifications.add("prefabShipSelectionResult:" + success);
        }

        @Override
        public void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws RemoteException {
            notifications.add("infectedCrewMembersRemoved:" + cabinCoordinatesWithNeighbors.size());
        }

        @Override
        public void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) throws RemoteException {
            notifications.add("playersFinalData:ranking=" + finalRanking.size() + ",prettiest=" + playersNicknamesWithPrettiestShip.size());
        }

        @Override
        public void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws RemoteException {
            notifications.add("playerEarlyLanded:" + nickname);
        }

        @Override
        public void pongToClientFromServer(String nickname) throws RemoteException {
            notifications.add("pong:" + nickname);
        }

        @Override
        public void pingToClientFromServer(String nickname) throws RemoteException {
            notifications.add("ping:" + nickname);
        }

        @Override
        public void forcedDisconnection(String nicknameToNotify, String gameId) throws RemoteException {
            notifications.add("forcedDisconnection:" + gameId);
        }

        @Override
        public void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations) throws RemoteException {
            notifications.add("notifyLeastResourcedPlayer:" + nicknameAndMotivations);
        }

        @Override
        public void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {
            notifications.add("notifyErrorWhileBookingComponent:" + nickname);
        }

        @Override
        public void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException {
            notifications.add("notifyNotActiveComponents:" + nickname);
        }

        public List<String> getNotifications() {
            return notifications;
        }
    }

    private Player createPlayerWithShip(String nickname, PlayerColor color, boolean isTestFlight, String prefabShipId, GameModel gameModel) {
        // Crea uno stub client controller
        StubClientController stubController = new StubClientController();

        // Crea GameClientNotifier con clientControllers reali
        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put(nickname, stubController);

        // Crea la ShipBoard con GameClientNotifier
        ShipBoard shipBoard = isTestFlight ?
                new Level1ShipBoard(color, gameModel.getGameClientNotifier(), false) :
                new Level2ShipBoard(color, gameModel.getGameClientNotifier(), false);

        Player player = new Player(nickname, shipBoard, color);
        player.setGameClientNotifier(gameModel.getGameClientNotifier());
        shipBoard.setPlayer(player);

        // Applica nave prefabbricata
        PrefabShipFactory.applyPrefabShip(shipBoard, prefabShipId);

        // Riempi cabine con umani di default
        for (Cabin cabin : shipBoard.getCabin()) {
            if (!cabin.hasInhabitants()) {
                cabin.fillCabin(CrewMember.HUMAN);
            }
        }
        shipBoard.getMainCabin().fillCabin(CrewMember.HUMAN);

        return player;
    }

    @Test
    @DisplayName("Test 1: Partita normale completa - Tutti i giocatori finiscono regolarmente")
    void testNormalGameCompletion() throws RemoteException {
        // Setup game normale (non test flight)
        gameController = new GameController("game1", 4, false, dns);
        gameModel = gameController.getGameModel();

        // Crea clientControllers per GameClientNotifier
        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());
        clientControllers.put("Charlie", new StubClientController());
        clientControllers.put("Diana", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        // Crea 4 giocatori con navi diverse
        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "test_cargo_full", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "test_no_exposed", gameModel);
        Player player3 = createPlayerWithShip("Charlie", PlayerColor.GREEN, false, "test_many_exposed", gameModel);
        Player player4 = createPlayerWithShip("Diana", PlayerColor.YELLOW, false, "basic_ship", gameModel);

        // Aggiungi giocatori al model
        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);
        gameModel.getPlayers().put("Charlie", player3);
        gameModel.getPlayers().put("Diana", player4);

        // Imposta ranking (posizioni sulla flying board)
        gameModel.setCurrRanking(Arrays.asList(player1, player2, player3, player4));
        gameModel.getFlyingBoard().getRanking().put(player1, 30);
        gameModel.getFlyingBoard().getRanking().put(player2, 25);
        gameModel.getFlyingBoard().getRanking().put(player3, 20);
        gameModel.getFlyingBoard().getRanking().put(player4, 15);

        // Aggiungi crediti iniziali
        player1.setOwnedCredits(5);
        player2.setOwnedCredits(3);
        player3.setOwnedCredits(2);
        player4.setOwnedCredits(1);

        // Esegui calcolo crediti finali
        gameModel.calculatePlayersCredits();

        // Verifica crediti finali
        // Player1: 5 (iniziali) + 8 (1° posto) + cubi (1 RED=4, 3 YELLOW=9, 1 GREEN=2, 2 BLUE=2) = 30
        assertEquals(30, player1.getOwnedCredits());

        // Player2: 3 + 6 (2° posto) + 4 (nave più bella - 0 connettori esposti) = 13
        assertEquals(13, player2.getOwnedCredits());

        // Player3: 2 + 4 (3° posto) + 0 (molti connettori esposti) = 6
        assertEquals(6, player3.getOwnedCredits());

        // Player4: 1 + 2 (4° posto) = 3
        assertEquals(3, player4.getOwnedCredits());

        // Verifica nave più bella
        List<Player> prettiestShips = gameModel.getPlayerWithPrettiestShip();
        assertTrue(prettiestShips.contains(player2));

        // Verifica PlayerFinalData
        List<PlayerFinalData> finalData = gameModel.getRankingWithPlayerFinalData();
        assertEquals(4, finalData.size());
        PlayerFinalData AlicesFinalData = finalData.stream().filter(f -> f.getNickname().equals("Alice")).findFirst().get();
        assertFalse(AlicesFinalData.isEarlyLanded());
        assertEquals(30, AlicesFinalData.getTotalCredits());
    }

    @Test
    @DisplayName("Test 2: Atterraggio volontario - Alcuni giocatori atterrano anticipatamente")
    void testVoluntaryLanding() throws RemoteException {
        gameController = new GameController("game2", 3, false, dns);
        gameModel = gameController.getGameModel();

        // Setup client controllers
        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());
        clientControllers.put("Charlie", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "test_cargo_full", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "test_cargo_full", gameModel);
        Player player3 = createPlayerWithShip("Charlie", PlayerColor.GREEN, false, "test_cargo_full", gameModel);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);
        gameModel.getPlayers().put("Charlie", player3);

        // Bob atterra volontariamente
        player2.setEarlyLanded(true);
        gameModel.getFlyingBoard().addOutPlayer(player2, true);

        // Ranking solo con player1 e player3
        gameModel.setCurrRanking(Arrays.asList(player1, player3, player2));
        gameModel.getFlyingBoard().getRanking().put(player1, 25);
        gameModel.getFlyingBoard().getRanking().put(player3, 20);

        // Calcola crediti
        gameModel.calculatePlayersCredits();

        // Player2 (atterrato): NO bonus posizione, cubi a metà prezzo
        assertTrue(player2.isEarlyLanded());

        // Verifica crediti finali
        // Player2 0 (iniziali) + 0 (atterrato in anticipo) + cubi (1 RED=4, 3 YELLOW=9, 1 GREEN=2, 2 BLUE=2, tatale da dimezzare: 17/2 = 9) = 9
        assertEquals(9, player2.getOwnedCredits());

        // Verifica che player2 non sia considerato per nave più bella
        List<Player> prettiestShips = gameModel.getPlayerWithPrettiestShip();
        assertFalse(prettiestShips.contains(player2));

        // Verifica PlayerFinalData
        List<PlayerFinalData> finalData = gameModel.getRankingWithPlayerFinalData();
        PlayerFinalData BobsFinalData = finalData.stream().filter(f -> f.getNickname().equals("Bob")).findFirst().get();
        assertTrue(BobsFinalData.isEarlyLanded()); // Bob è ultimo nel ranking
    }

    @Test
    @DisplayName("Test 3: Eliminazione per perdita tutti umani")
    void testEliminationNoHumans() throws RemoteException {
        gameController = new GameController("game3", 2, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "basic_ship", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "test_no_humans", gameModel);

        // Rimuovi tutti gli umani da player2
        for (Cabin cabin : player2.getPersonalBoard().getCabin()) {
            cabin.getInhabitants().clear();
        }
        player2.getPersonalBoard().getMainCabin().getInhabitants().clear();

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);

        gameModel.setCurrRanking(Arrays.asList(player1, player2));
        gameModel.getFlyingBoard().getRanking().put(player1, 25);
        gameModel.getFlyingBoard().getRanking().put(player2, 20);

        // Simula eliminazione
        GameState.CHECK_PLAYERS.run(gameModel);

        gameModel.calculatePlayersCredits();

        // Verifica che player2 sia eliminato
        assertTrue(player2.isEarlyLanded());
        assertTrue(gameModel.getFlyingBoard().getOutPlayers().contains(player2));
    }

    @Test
    @DisplayName("Test 5: Calcolo nave più bella con pareggio")
    void testPrettiestShipTie() throws RemoteException {
        gameController = new GameController("game5", 3, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());
        clientControllers.put("Charlie", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        // Due giocatori con lo stesso numero di connettori esposti
        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "test_no_exposed", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "test_no_exposed", gameModel);
        Player player3 = createPlayerWithShip("Charlie", PlayerColor.GREEN, false, "test_many_exposed", gameModel);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);
        gameModel.getPlayers().put("Charlie", player3);

        gameModel.setCurrRanking(Arrays.asList(player1, player2, player3));

        // Calcola nave più bella
        List<Player> prettiestShips = gameModel.getPlayerWithPrettiestShip();

        // Entrambi player1 e player2 dovrebbero vincere
        assertEquals(2, prettiestShips.size());
        assertTrue(prettiestShips.contains(player1));
        assertTrue(prettiestShips.contains(player2));
        assertFalse(prettiestShips.contains(player3));
    }

    @Test
    @DisplayName("Test 6: Test flight mode - crediti ridotti")
    void testTestFlightMode() throws RemoteException {
        gameController = new GameController("game6", 2, true, dns); // Test flight mode
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, true, "basic_ship", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, true, "basic_ship", gameModel);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);

        gameModel.setCurrRanking(Arrays.asList(player1, player2));
        gameModel.getFlyingBoard().getRanking().put(player1, 10);
        gameModel.getFlyingBoard().getRanking().put(player2, 5);

        gameModel.calculatePlayersCredits();

        // In test flight: 1° posto = 4 crediti, nave più bella = 2 crediti
        int player1Credits = player1.getOwnedCredits();
        assertTrue(player1Credits >= 4); // Almeno i crediti della posizione

        // In test flight: 1° posto = 4 crediti, nave più bella = 2 crediti, totale = 6
        assertEquals(6, player1.getOwnedCredits());
        // In test flight: 2° posto = 3 crediti, nave più bella = 2 crediti, totale = 5
        assertEquals(5, player2.getOwnedCredits());
    }

    @Test
    @DisplayName("Test 7: Penalità componenti persi")
    void testLostComponentsPenalty() throws RemoteException {
        gameController = new GameController("game7", 2, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "test_many_lost", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "basic_ship", gameModel);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);

        gameModel.setCurrRanking(Arrays.asList(player1, player2));
        gameModel.getFlyingBoard().getRanking().put(player1, 20);
        gameModel.getFlyingBoard().getRanking().put(player2, 15);

        // Player1 ha 5 componenti persi
        assertEquals(5, player1.getPersonalBoard().getNotActiveComponents().size());

        player1.setOwnedCredits(10); // Crediti iniziali
        gameModel.calculatePlayersCredits();

        // 10 + 8 (1° posto) + 4 (nave più bella) - 5 (componenti persi) = 13
        assertEquals(17, player1.getOwnedCredits());

        // Verifica PlayerFinalData
        List<PlayerFinalData> finalData = gameModel.getRankingWithPlayerFinalData();
        PlayerFinalData AlicesFinalData = finalData.stream().filter(f -> f.getNickname().equals("Alice")).findFirst().get();
        assertEquals(5, AlicesFinalData.getLostComponents());
    }

    @Test
    @DisplayName("Test 8: Tutti i giocatori atterrati anticipatamente")
    void testAllPlayersEarlyLanded() throws RemoteException {
        gameController = new GameController("game8", 2, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "basic_ship", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "basic_ship", gameModel);

        // Entrambi atterrati
        player1.setEarlyLanded(true);
        player2.setEarlyLanded(true);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);

        gameModel.setCurrRanking(Arrays.asList(player1, player2));
        gameModel.getFlyingBoard().getRanking().put(player1, 20);
        gameModel.getFlyingBoard().getRanking().put(player2, 15);

        // Nessuno dovrebbe ricevere bonus posizione o nave più bella
        gameModel.calculatePlayersCredits();

        // Verifica che nessuno abbia vinto il premio nave più bella
        List<Player> prettiestShips = gameModel.getPlayerWithPrettiestShip();
        assertTrue(prettiestShips.isEmpty());

        // Verifica PlayerFinalData
        List<PlayerFinalData> finalData = gameModel.getRankingWithPlayerFinalData();
        assertTrue(finalData.get(0).isEarlyLanded());
        assertTrue(finalData.get(1).isEarlyLanded());
    }

    @Test
    @DisplayName("Test 9: Un solo giocatore rimasto")
    void testSinglePlayerRemaining() throws RemoteException {
        gameController = new GameController("game9", 3, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());
        clientControllers.put("Charlie", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "test_cargo_full", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "basic_ship", gameModel);
        Player player3 = createPlayerWithShip("Charlie", PlayerColor.GREEN, false, "basic_ship", gameModel);

        // Player2 e player3 eliminati
        player2.setEarlyLanded(true);
        player3.setEarlyLanded(true);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);
        gameModel.getPlayers().put("Charlie", player3);

        gameModel.setCurrRanking(Arrays.asList(player1, player2, player3));
        gameModel.getFlyingBoard().getRanking().put(player1, 30);

        gameModel.calculatePlayersCredits();

        // Player1 dovrebbe ricevere tutti i bonus del primo posto
        assertTrue(player1.getOwnedCredits() > 8); // Almeno crediti posizione + cubi

        // Solo player1 considerato per nave più bella
        List<Player> prettiestShips = gameModel.getPlayerWithPrettiestShip();
        assertEquals(1, prettiestShips.size());
        assertTrue(prettiestShips.contains(player1));
    }

    @Test
    @DisplayName("Test 10: Doppiaggio - giocatore troppo indietro")
    void testPlayerDoubled() throws RemoteException {
        gameController = new GameController("game10", 2, false, dns);
        gameModel = gameController.getGameModel();

        ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
        clientControllers.put("Alice", new StubClientController());
        clientControllers.put("Bob", new StubClientController());

        gameModel.createGameClientNotifier(clientControllers);

        Player player1 = createPlayerWithShip("Alice", PlayerColor.RED, false, "basic_ship", gameModel);
        Player player2 = createPlayerWithShip("Bob", PlayerColor.BLUE, false, "basic_ship", gameModel);

        gameModel.getPlayers().put("Alice", player1);
        gameModel.getPlayers().put("Bob", player2);

        // Player1 molto avanti, player2 molto indietro (oltre runLength)
        gameModel.getFlyingBoard().getRanking().put(player1, 50);
        gameModel.getFlyingBoard().getRanking().put(player2, 20); // Differenza > 24 (runLength)

        // Simula controllo doppiaggio
        gameModel.getFlyingBoard().getDoubledPlayers();

        // Player2 dovrebbe essere eliminato
        assertTrue(gameModel.getFlyingBoard().getOutPlayers().contains(player2));
        assertFalse(gameModel.getFlyingBoard().getRanking().containsKey(player2));
    }
}