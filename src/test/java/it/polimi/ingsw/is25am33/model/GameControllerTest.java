package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.*;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.LifeSupport;
import it.polimi.ingsw.is25am33.model.dangerousObj.BigMeteorite;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import it.polimi.ingsw.is25am33.network.DNS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Equality;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    private GameController gameController;
    private static final String PLAYER_NICKNAME = "luca";
    private DNS dns;
    private CallableOnClientController clientController;

    @BeforeEach
    void setUp() throws RemoteException {
        dns = new DNS();
        gameController = new GameController("1234", 1, false, dns);
        clientController = new CallableOnClientController() {
            @Override
            public void notifyGameInfos(String nicknameToNotify, List<GameInfo> gameInfos) throws IOException {

            }

            @Override
            public void notifyNewPlayerJoined(String nicknameToNotify, String gameId, String newPlayerNickname, PlayerColor color) throws IOException {

            }

            @Override
            public void notifyGameStarted(String nickname, GameInfo gameInfo) throws IOException {

            }

            @Override
            public void notifyGameState(String nickname, GameState gameState) throws IOException {

            }

            @Override
            public void notifyDangerousObjAttack(String nickname, ClientDangerousObject dangerousObj) throws IOException {

            }

            @Override
            public void notifyCurrPlayerChanged(String nicknameToNotify, String nickname) throws IOException {

            }

            @Override
            public void notifyCardState(String nickname, CardState cardState) throws IOException {

            }

            @Override
            public void notifyFocusedComponent(String nicknameToNotify, String nickname, Component component) throws IOException {

            }

            @Override
            public void notifyReleaseComponent(String nicknameToNotify, String nickname) throws IOException {

            }

            @Override
            public void notifyBookedComponent(String nicknameToNotify, String nickname, Component component) throws IOException {

            }

            @Override
            public void notifyCurrAdventureCard(String nickname, ClientCard adventureCard, boolean isFirstTime) throws IOException {

            }

            @Override
            public void notifyAddVisibleComponents(String nicknameToNotify, int index, Component component) throws IOException {

            }

            @Override
            public void notifyRemoveVisibleComponents(String nicknameToNotify, int index) throws IOException {

            }

            @Override
            public void notifyComponentPlaced(String nicknameToNotify, String nickname, Component component, Coordinates coordinates) throws IOException {

            }

            @Override
            public void notifyShipBoardUpdate(String nicknameToNotify, String nickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException {

            }

            @Override
            public void notifyPlayerCredits(String nicknameToNotify, String nickname, int credits) throws IOException {

            }

            @Override
            public void notifyEliminatedPlayer(String nicknameToNotify, String nickname) throws IOException {

            }

            @Override
            public void notifyRankingUpdate(String nicknameToNotify, String nickname, int newPosition) throws IOException {

            }

            @Override
            public void notifyVisibleDeck(String nickname, List<List<ClientCard>> littleVisibleDeck) throws IOException {

            }

            @Override
            public void notifyHourglassRestarted(String nicknameToNotify, String nickname, Integer flipsLeft) throws IOException {

            }

            @Override
            public void notifyPlayerDisconnected(String nicknameToNotify, String disconnectedPlayer) throws IOException {

            }

            @Override
            public void notifyInvalidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {

            }

            @Override
            public void notifyValidShipBoard(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {

            }

            @Override
            public void notifyShipPartsGeneratedDueToRemoval(String nicknameToNotify, String shipOwnerNickname, Component[][] shipMatrix, Set<Coordinates> incorrectlyPositionedComponentsCoordinates, Set<Set<Coordinates>> shipParts, Map<Class<?>, List<Component>> componentsPerType) throws RemoteException {

            }

            @Override
            public void notifyStopHourglass(String nicknameToNotify) throws IOException {

            }

            @Override
            public void notifyFirstToEnter(String nicknameToNotify) throws IOException {

            }

            @Override
            public void notifyCurrAdventureCardUpdate(String nicknameToNotify, ClientCard adventureCard) throws IOException {

            }

            @Override
            public void notifyPlayerVisitedPlanet(String nicknameToNotify, String nickname, ClientCard adventureCard) throws IOException {

            }

            @Override
            public void forcedDisconnection(String nickname, String gameId) throws IOException {

            }

            @Override
            public void pingToClientFromServer(String nickname) throws IOException {

            }

            @Override
            public void pongToClientFromServer(String nickname) throws IOException {

            }

            @Override
            public void notifyComponentPerType(String nicknameToNotify, String playerNickname, Map<Class<?>, List<Component>> componentsPerType) throws IOException {

            }

            @Override
            public void notifyCrewPlacementPhase(String nicknameToNotify) throws IOException {

            }

            @Override
            public void notifyCrewPlacementComplete(String nicknameToNotify, String playerNickname, Component[][] shipMatrix, Map<Class<?>, List<Component>> componentsPerType) throws IOException {

            }

            @Override
            public void notifyPrefabShipsAvailable(String nicknameToNotify, List<PrefabShipInfo> prefabShips) throws IOException {

            }

            @Override
            public void notifyPlayerSelectedPrefabShip(String nicknameToNotify, String playerNickname, PrefabShipInfo prefabShipName) throws IOException {

            }

            @Override
            public void notifyPrefabShipSelectionResult(String nicknameToNotify, boolean success, String errorMessage) throws IOException {

            }

            @Override
            public void notifyCoordinateOfComponentHit(String nicknameToNotify, String nickname, Coordinates coordinates) throws IOException {

            }

            @Override
            public void notifyInfectedCrewMembersRemoved(String nicknameToNotify, Set<Coordinates> cabinCoordinatesWithNeighbors) throws IOException {

            }

            @Override
            public void notifyPlayersFinalData(String nicknameToNotify, List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) throws IOException {

            }

            @Override
            public void notifyPlayerEarlyLanded(String nicknameToNotify, String nickname) throws IOException {

            }

            @Override
            public void notifyLeastResourcedPlayer(String nicknameToNotify, String nicknameAndMotivations) throws IOException {

            }

            @Override
            public void notifyErrorWhileBookingComponent(String nicknameToNotify, String nickname, Component focusedComponent) throws IOException {

            }

            @Override
            public void notifyNotActiveComponents(String nicknameToNotify, String nickname, List<Component> notActiveComponents) throws IOException {

            }

            @Override
            public void notifyNoMoreHiddenComponents(String nicknameToNotify) throws IOException {

            }

            @Override
            public void notifyStorageError(String nicknameToNotify, String errorMessage) throws IOException {

            }
        };
    }

    @Test
    void testGameControllerInitialization() {
        assertNotNull(gameController.getGameModel());
        assertEquals("1234", gameController.getGameInfo().getGameId());
        assertEquals(1, gameController.getGameInfo().getMaxPlayers());
        assertFalse(gameController.getGameInfo().isStarted());
        assertFalse(gameController.getGameInfo().isTestFlight());
    }

    @Test
    void testAddPlayer() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        GameInfo gameInfo = gameController.getGameInfo();
        assertTrue(gameInfo.getConnectedPlayers().containsKey(PLAYER_NICKNAME));
        assertEquals(color, gameInfo.getConnectedPlayers().get(PLAYER_NICKNAME));
    }

    @Test
    void testRemovePlayer() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        gameController.removePlayer(PLAYER_NICKNAME);

        GameInfo gameInfo = gameController.getGameInfo();
        assertFalse(gameInfo.getConnectedPlayers().containsKey(PLAYER_NICKNAME));
    }

    @Test
    void testStartGame() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        gameController.startGame();

        assertTrue(gameController.getGameInfo().isStarted());
        assertEquals(GameState.BUILD_SHIPBOARD, gameController.getGameModel().getCurrGameState());
    }

    @Test
    void testLeaveGameAfterCreation() throws RemoteException {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        gameController.leaveGameAfterCreation(PLAYER_NICKNAME, true);

        assertFalse(gameController.getGameModel().getGameClientNotifier().getClientControllers().containsKey(PLAYER_NICKNAME));
    }

    @Test
    void testPlayerPicksHiddenComponent() {
        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);

        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        gameController.playerPicksHiddenComponent(PLAYER_NICKNAME);

        ShipBoard playerBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();
        assertNotNull(playerBoard.getFocusedComponent(), "Il componente focalizzato non dovrebbe essere null");
    }

    @Test
    void testPlayerPicksHiddenComponentWhenEmpty() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        // gira tutti i componenti nascosti
        ComponentTable componentTable = gameController.getGameModel().getComponentTable();
        while(componentTable.pickHiddenComponent() != null) {}

        gameController.playerPicksHiddenComponent(PLAYER_NICKNAME);

        ShipBoard playerBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();
        assertNull(playerBoard.getFocusedComponent(), "Il componente focalizzato dovrebbe essere null quando non ci sono più componenti");
    }

    @Test
    void testPlayerWantsToPlaceFocusedComponent() throws RemoteException {

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        ShipBoard playerBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();

        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);

        Cabin c1 = new Cabin(connectors);
        playerBoard.setFocusedComponent(c1);

        Coordinates coords = new Coordinates(6, 7);
        int rotation = 1;

        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, coords, rotation);

        assertNotNull(playerBoard.getComponentAt(coords),
                "Dovrebbe esserci un componente nelle coordinate specificate");
    }

    @Test
    void testPlayerEndsBuildShipBoardPhase() {
        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        gameController.playerEndsBuildShipBoardPhase(PLAYER_NICKNAME);

        // Quando tutti i giocatori hanno finito, il gioco dovrebbe passare alla fase CHECK_SHIPBOARD
        assertEquals(GameState.PLACE_CREW, gameController.getGameModel().getCurrGameState());
    }

    @Test
    void testPlayerWantsToReleaseFocusedComponent(){

        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        // Prima prendi un componente
        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        gameController.playerPicksHiddenComponent(PLAYER_NICKNAME);

        ShipBoard playerBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();
        Component focusedComponent = playerBoard.getFocusedComponent();

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerWantsToReleaseFocusedComponent(PLAYER_NICKNAME);
        assertNotNull(focusedComponent);

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);

        gameController.playerWantsToReleaseFocusedComponent(PLAYER_NICKNAME);

        assertNull(playerBoard.getFocusedComponent(),
                "Non dovrebbe esserci più un componente focalizzato");
        assertTrue(gameController.getGameModel().getComponentTable().getVisibleComponents().containsValue(focusedComponent),
                "Il componente rilasciato dovrebbe essere tra i componenti visibili");
    }

    @Test
    void testPlayerWantsToReserveFocusedComponent() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        // Prima prendi un componente
        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        gameController.playerPicksHiddenComponent(PLAYER_NICKNAME);

        ShipBoard playerBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();
        Component focusedComponent = playerBoard.getFocusedComponent();

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerWantsToReserveFocusedComponent(PLAYER_NICKNAME);
        assertNotNull(focusedComponent);

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        gameController.playerWantsToReserveFocusedComponent(PLAYER_NICKNAME);
        assertTrue(((Level2ShipBoard)playerBoard).getBookedComponents().contains(focusedComponent),
                "Il componente dovrebbe essere tra i componenti prenotati");
    }


    @Test
    void testPlayerWantsToWatchLittleDeck() {
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);

        int littleDeckChoice = 1;
        boolean result = gameController.playerWantsToWatchLittleDeck(PLAYER_NICKNAME, littleDeckChoice);

        assertTrue(result);
    }

    @Test
    void testSubmitCrewChoicesValid() throws IOException {

        // Preparazione
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        ShipBoard shipBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();

        // Creazione delle componenti
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST, ConnectorType.UNIVERSAL);
        connectors.put(Direction.SOUTH, ConnectorType.UNIVERSAL);
        connectors.put(Direction.WEST, ConnectorType.UNIVERSAL);
        Cabin cabin = new Cabin(connectors);
        LifeSupport l1 = new LifeSupport(connectors,ColorLifeSupport.PURPLE);

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        // Posizionamento cabina
        shipBoard.setFocusedComponent(cabin);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 7), 1);
        shipBoard.setFocusedComponent(l1);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 8), 1);

        // Test scelta equipaggio valida
        Map<Coordinates, CrewMember> validChoices = new HashMap<>();
        validChoices.put(new Coordinates(6, 7), CrewMember.PURPLE_ALIEN);

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.submitCrewChoices(PLAYER_NICKNAME, validChoices);

        assertTrue(shipBoard.getCabin().stream()
                .anyMatch(c -> c.hasInhabitants()));
    }

    @Test
    void testPlayerWantsToRemoveComponent() throws RemoteException {
        // Preparazione
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        ShipBoard shipBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();

        // Creazione delle componenti
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.DOUBLE);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.DOUBLE);
        connectors.put(Direction.WEST, ConnectorType.DOUBLE);
        Cabin cabin = new Cabin(connectors);
        Map<Direction, ConnectorType> connectors1 = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.SINGLE);
        connectors.put(Direction.EAST, ConnectorType.SINGLE);
        connectors.put(Direction.SOUTH, ConnectorType.SINGLE);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);
        LifeSupport l1 = new LifeSupport(connectors,ColorLifeSupport.PURPLE);

        // Posizionamento cabina
        shipBoard.setFocusedComponent(cabin);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 7), 1);
        shipBoard.setFocusedComponent(l1);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 8), 1);

        gameController.playerWantsToRemoveComponent(PLAYER_NICKNAME,new Coordinates(6,8));
        assertEquals(0, shipBoard.getIncorrectlyPositionedComponentsCoordinates().size());

    }

    @Test
    void testPlayerChooseShipPart() throws RemoteException {
        // Preparazione
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        ShipBoard shipBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();

        // Creazione delle componenti
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.DOUBLE);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.DOUBLE);
        connectors.put(Direction.WEST, ConnectorType.DOUBLE);
        Cabin cabin = new Cabin(connectors);
        Map<Direction, ConnectorType> connectors1 = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.SINGLE);
        connectors.put(Direction.EAST, ConnectorType.SINGLE);
        connectors.put(Direction.SOUTH, ConnectorType.SINGLE);
        connectors.put(Direction.WEST, ConnectorType.SINGLE);
        LifeSupport l1 = new LifeSupport(connectors,ColorLifeSupport.PURPLE);

        // Posizionamento cabina
        shipBoard.setFocusedComponent(cabin);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 7), 1);
        shipBoard.setFocusedComponent(l1);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 8), 1);
        shipBoard.setFocusedComponent(l1);
        gameController.playerWantsToPlaceFocusedComponent(PLAYER_NICKNAME, new Coordinates(6, 9),1);

        gameController.playerWantsToRemoveComponent(PLAYER_NICKNAME,new Coordinates(6,8));

        gameController.playerChoseShipPart(PLAYER_NICKNAME, Set.of(new Coordinates(6,7)));

        assertTrue(shipBoard.getIncorrectlyPositionedComponentsCoordinates().isEmpty());
        assertNull(shipBoard.getComponentAt(new Coordinates(6,9)));

    }

    @Test
    void testPlayerPicksVisibleComponent(){
        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        ShipBoard shipBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();
        gameController.playerPicksHiddenComponent(PLAYER_NICKNAME);
        gameController.playerWantsToReleaseFocusedComponent(PLAYER_NICKNAME);
        assertEquals(1,gameController.getGameModel().getComponentTable().getVisibleComponents().size());
        assertNull(shipBoard.getFocusedComponent());
        gameController.playerPicksVisibleComponent(PLAYER_NICKNAME,1);
        assertNotNull(shipBoard.getFocusedComponent());
    }

    @Test
    void testPlayerPlacesPown() {

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerPlacesPawn(PLAYER_NICKNAME);
        assertFalse(gameController.getGameModel().getFlyingBoard().getRanking().containsKey(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME)));

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);
        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        gameController.playerPlacesPawn(PLAYER_NICKNAME);
        assertTrue(gameController.getGameModel().getFlyingBoard().getRanking().containsKey(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME)));

    }

    @Test
    void testPlayerHAndleBigMeteorite(){

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerHandleBigMeteorite(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerHandleBigMeteorite(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        MeteoriteStorm meteoriteStorm = new MeteoriteStorm(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };

        meteoriteStorm.setGame(gameController.getGameModel());
        meteoriteStorm.setMeteorites(List.of());
        meteoriteStorm.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(meteoriteStorm);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () -> gameController.playerHandleBigMeteorite(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7))));

    }

    @Test
    void testPlayerHAndleSmallObject(){

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerHandleSmallDanObj(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerHandleSmallDanObj(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        MeteoriteStorm meteoriteStorm = new MeteoriteStorm(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };

        meteoriteStorm.setGame(gameController.getGameModel());
        meteoriteStorm.setMeteorites(List.of());
        meteoriteStorm.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(meteoriteStorm);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () -> gameController.playerHandleSmallDanObj(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7))));
    }

    @Test
    void testPlayerWantsToVisitLocation(){

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerWantsToVisitLocation(PLAYER_NICKNAME,true);

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerWantsToVisitLocation(PLAYER_NICKNAME,true);

        AbandonedStation abandonedStation = new AbandonedStation(){

            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }

        };

        abandonedStation.setGame(gameController.getGameModel());
        abandonedStation.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(abandonedStation);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () ->  gameController.playerWantsToVisitLocation(PLAYER_NICKNAME,true));
    }

    @Test
    void testPlayerWantsToThrowDices(){
        AdventureCard planets = new Planets(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices==null)
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };
        gameController.getGameModel().setCurrAdventureCard(planets);
        assertDoesNotThrow(() -> gameController.playerWantsToThrowDices(PLAYER_NICKNAME));
    }

    @Test
    void testPlayerChooseDoubleCannons(){
        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerChoseDoubleCannons(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerChoseDoubleCannons(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        MeteoriteStorm meteoriteStorm = new MeteoriteStorm(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };

        meteoriteStorm.setGame(gameController.getGameModel());
        meteoriteStorm.setMeteorites(List.of());
        meteoriteStorm.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(meteoriteStorm);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () -> gameController.playerChoseDoubleCannons(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7))));
    }

    @Test
    void testPlayerChooseCabin(){
        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerChoseCabins(PLAYER_NICKNAME,List.of(new Coordinates(6,7)));

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerChoseCabins(PLAYER_NICKNAME,List.of(new Coordinates(6,7)));

        Epidemic epidemic = new Epidemic(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };

        epidemic.setGame(gameController.getGameModel());
        epidemic.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(epidemic);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () -> gameController.playerChoseDoubleCannons(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7))));
    }

    @Test
    void testPlayerChooseDoubleEngine(){
        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerChoseDoubleEngines(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        Planets planets = new Planets();
        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerChoseDoubleEngines(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7)));

        FreeSpace freeSpace = new FreeSpace(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }
        };

        freeSpace.setGame(gameController.getGameModel());
        freeSpace.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(freeSpace);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () -> gameController.playerChoseDoubleEngines(PLAYER_NICKNAME,List.of(new Coordinates(6,7)),List.of(new Coordinates(6,7))));
    }

    @Test
    void testPlayerWantsToVisitPlanets(){
        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerWantsToVisitPlanet(PLAYER_NICKNAME,1);

        AbandonedStation abandonedStation = new AbandonedStation();
        abandonedStation.setGame(gameController.getGameModel());
        abandonedStation.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(abandonedStation);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerWantsToVisitPlanet(PLAYER_NICKNAME,1);

        Planets planets = new Planets(){

            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }

        };

        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () ->  gameController.playerWantsToVisitLocation(PLAYER_NICKNAME,true));
    }

    @Test
    void testPlayerChooseStorage(){

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerChoseStorage(PLAYER_NICKNAME,List.of(new Coordinates(6,7)));

        AbandonedShip abandonedShip = new AbandonedShip();
        abandonedShip.setGame(gameController.getGameModel());
        abandonedShip.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(abandonedShip);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerChoseStorage(PLAYER_NICKNAME,List.of(new Coordinates(6,7)));

        Planets planets = new Planets(){

            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }

        };

        planets.setGame(gameController.getGameModel());
        planets.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(planets);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () ->  gameController.playerChoseStorage(PLAYER_NICKNAME,List.of(new Coordinates(6,7))));
    }

    @Test
    void testPlayerWantsToFocusReservedComponent(){

        gameController.getGameModel().setCurrGameState(GameState.BUILD_SHIPBOARD);

        PlayerColor color = PlayerColor.BLUE;
        gameController.addPlayer(PLAYER_NICKNAME, color, clientController);
        ShipBoard shipBoard = gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME).getPersonalBoard();

        // Creazione delle componenti
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH, ConnectorType.DOUBLE);
        connectors.put(Direction.EAST, ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH, ConnectorType.DOUBLE);
        connectors.put(Direction.WEST, ConnectorType.DOUBLE);
        Cabin cabin = new Cabin(connectors);
        Cabin cabin2 = new Cabin(connectors);
        Cabin cabin3 = new Cabin(connectors);
        shipBoard.setFocusedComponent(cabin);
        gameController.playerWantsToReserveFocusedComponent(PLAYER_NICKNAME);
        gameController.playerWantsToFocusReservedComponent(PLAYER_NICKNAME,0);
        assertEquals(shipBoard.getFocusedComponent(), cabin);
        assertTrue(shipBoard.getBookedComponents().contains(cabin));

        gameController.playerWantsToReleaseFocusedComponent(PLAYER_NICKNAME);
        shipBoard.setFocusedComponent(cabin2);
        gameController.playerWantsToReserveFocusedComponent(PLAYER_NICKNAME);
        shipBoard.setFocusedComponent(cabin3);
        gameController.playerWantsToReserveFocusedComponent(PLAYER_NICKNAME);
        assertEquals(2, shipBoard.getBookedComponents().size());
        assertTrue(shipBoard.getBookedComponents().contains(cabin));
        assertTrue(shipBoard.getBookedComponents().contains(cabin2));

    }

    @Test
    void testPlayerHandleBigShot(){
        AdventureCard planets = new Planets(){
            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices==null)
                    throw new IllegalArgumentException("Il parametro non è valido");
            }
        };
        gameController.getGameModel().setCurrAdventureCard(planets);
        assertDoesNotThrow(() -> gameController.playerHandleBigShot(PLAYER_NICKNAME));
    }

    @Test
    void testPlayerWantsToAcceptTheReward(){

        gameController.getGameModel().setCurrGameState(GameState.PLACE_CREW);
        gameController.playerWantsToAcceptTheReward(PLAYER_NICKNAME, true);

        AbandonedShip abandonedShip = new AbandonedShip();
        abandonedShip.setGame(gameController.getGameModel());
        abandonedShip.setCurrState(CardState.START_CARD);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrAdventureCard(abandonedShip);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        gameController.playerWantsToAcceptTheReward(PLAYER_NICKNAME,true);

        Smugglers smugglers = new Smugglers(){

            @Override
            public void play(PlayerChoicesDataStructure playerChoices){
                if(playerChoices.getChosenDoubleCannons().isEmpty() || playerChoices.getChosenBatteryBoxes().isEmpty())
                    throw new IllegalArgumentException("Il giocatore non ha selezionato tutti i componenti necessari");
            }

        };

        smugglers.setGame(gameController.getGameModel());
        smugglers.setCurrState(CardState.START_CARD);
        gameController.getGameModel().setCurrAdventureCard(smugglers);
        gameController.getGameModel().getFlyingBoard().getRanking().put(gameController.getGameModel().getPlayers().get(PLAYER_NICKNAME),1);
        gameController.getGameModel().setCurrGameState(GameState.PLAY_CARD);
        assertDoesNotThrow( () ->  gameController.playerWantsToAcceptTheReward(PLAYER_NICKNAME,true));

    }






}
