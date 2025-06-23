package it.polimi.ingsw.is25am33.model.card;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.*;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.model.UnknownStateException;

public class PlanetsTest {

    private GameModel gameModel;
    private GameClientNotifier gameClientNotifier;
    private Planets planets;
    private Player player1;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel("1234", 1, false) {
            @Override
            public void setCurrGameState(GameState state) {

            }
        };
        gameClientNotifier = new GameClientNotifier(gameModel, new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);

        player1 = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        gameModel.getPlayers().put(player1.getNickname(), player1);

        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        gameModel.getFlyingBoard().insertPlayer(player1);
        planets = new Planets();
        planets.setGame(gameModel);
        gameModel.setCurrAdventureCard(planets);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_PLANET, planets.getFirstState());
    }

    @Test
    void testUnknownStateException() {
        planets.setCurrState(CardState.END_OF_CARD);
        assertThrows(UnknownStateException.class,
                () -> planets.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe sollevare UnknownStateException per stati sconosciuti");
    }

    @Test
    void testToClientCard() {
        ClientCard card = planets.toClientCard();
        assertTrue(card instanceof ClientPlanets);
    }

    @Test
    void testHandleCubesRewardFlow() {
        Planet planet1 = new Planet(List.of(CargoCube.GREEN,CargoCube.BLUE));
        planets.setCurrPlanet(planet1);
        planets.setCurrState(CardState.HANDLE_CUBES_REWARD);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Aggiungiamo due Storage liberi sulla plancia
        Storage storage1 = new StandardStorage(connectors,2);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        Storage storage2 = new StandardStorage(connectors, 3);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(Arrays.asList(new Coordinates(6,7), new Coordinates(7 ,6)))
                .build();

        planets.play(choices);

        // Verifica che gli Storage contengano i cubi assegnati
        assertTrue( storage1.getStockedCubes().contains(CargoCube.GREEN), "Il primo Storage dovrebbe contenere GREEN");
        assertTrue(storage2.getStockedCubes().contains(CargoCube.BLUE), "Il secondo Storage dovrebbe contenere BLUE");

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, planets.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testHandleCubesRewardHeChooseStorageLessTheCube() {
        Planet planet1 = new Planet(List.of(CargoCube.GREEN,CargoCube.BLUE));
        planets.setCurrPlanet(planet1);
        planets.setCurrState(CardState.HANDLE_CUBES_REWARD);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Aggiungiamo due Storage liberi sulla plancia
        Storage storage1 = new StandardStorage(connectors,2);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        Storage storage2 = new StandardStorage(connectors, 3);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(Arrays.asList(new Coordinates(6,7)))
                .build();

        planets.play(choices);

        // Verifica che gli Storage contengano i cubi assegnati
        assertTrue( storage1.getStockedCubes().contains(CargoCube.GREEN));
        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, planets.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testHandleCubesRewardFlowWithoutChosenStorage() {
        Planet planet1 = new Planet(List.of(CargoCube.GREEN,CargoCube.BLUE));
        planets.setCurrPlanet(planet1);
        planets.setCurrState(CardState.HANDLE_CUBES_REWARD);

        ShipBoard shipBoard = player1.getPersonalBoard();
        Map<Direction, ConnectorType> connectors = new ConcurrentHashMap<>();
        connectors.put(Direction.NORTH,ConnectorType.UNIVERSAL);
        connectors.put(Direction.EAST,ConnectorType.DOUBLE);
        connectors.put(Direction.SOUTH,ConnectorType.DOUBLE);
        connectors.put(Direction.WEST,ConnectorType.SINGLE);
        // Aggiungiamo due Storage liberi sulla plancia
        Storage storage1 = new StandardStorage(connectors,2);
        storage1.addCube(CargoCube.GREEN);
        storage1.addCube(CargoCube.BLUE);
        Storage storage2 = new StandardStorage(connectors, 3);
        shipBoard.getShipMatrix()[6][7] = storage1;
        shipBoard.getShipMatrix()[7][6] = storage2;

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(List.of())
                .build();

        planets.play(choices);

        // Verifica stato e reset
        assertEquals(CardState.END_OF_CARD, planets.getCurrState(), "Stato dovrebbe essere END_OF_CARD");
    }

    @Test
    void testPlayerChooseValidPlanet() {
        Planet planet1 = new Planet(List.of(CargoCube.GREEN,CargoCube.BLUE));
        Planet planet2 = new Planet(List.of(CargoCube.RED,CargoCube.YELLOW));
        planets.setAvailablePlanets(new ArrayList<>(List.of(planet1, planet2)));
        planets.setCurrState(CardState.CHOOSE_PLANET);

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenPlanetIndex(1)
                .build();

        planets.play(choices);

        // Verifica che il pianea viene tolto
        assertFalse(planets.getAvailablePlanets().contains(planet1));
        assertEquals(CardState.HANDLE_CUBES_REWARD, planets.getCurrState());

    }

    @Test
    void testPlayerNoChoosePlanet() {
        Planet planet1 = new Planet(List.of(CargoCube.GREEN,CargoCube.BLUE));
        Planet planet2 = new Planet(List.of(CargoCube.RED,CargoCube.YELLOW));
        planets.setAvailablePlanets(List.of(planet1, planet2));
        planets.setCurrState(CardState.CHOOSE_PLANET);

        PlayerChoicesDataStructure choices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenPlanetIndex(0)
                .build();

        planets.play(choices);

        // Verifica che il pianea viene tolto
        assertTrue(planets.getAvailablePlanets().contains(planet1));
        assertTrue(planets.getAvailablePlanets().contains(planet2));
        assertEquals(CardState.END_OF_CARD, planets.getCurrState(), "Stato dovrebbe essere END_OF_CARD");

    }


}


