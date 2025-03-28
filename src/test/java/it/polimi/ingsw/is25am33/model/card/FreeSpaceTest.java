package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.Level2FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.game.Game;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FreeSpaceTest {

    private Game game;
    private List<Player> players;
    private AdventureCard card;
    private FlyingBoard flyingBoard;
    private ShipBoard shipBoard;

    @BeforeEach
    void setUp() {

         shipBoard = new Level2ShipBoard(null) {

            public int countTotalEnginePower(Stream<Engine> engineStream) {
                return 5;
            }

         };

         flyingBoard = new Level2FlyingBoard() {

            public void movePlayer(Player player, int offset) {}

         };

         players = new ArrayList<>(List.of( new Player("fra", shipBoard),
                new Player("ali", shipBoard),
                new Player("luc", shipBoard),
                new Player("mar", shipBoard)));

         game = new Game(flyingBoard, players);

         card = new FreeSpace(game);

         game.setCurrAdventureCard(card);
         game.setCurrState(GameState.START_CARD);
         game.setCurrRanking(players);

         game.startCard();

    }

    @Test
    void TestToCheckIfTheMethodIsAppliedForEveryPlayerInOrder() {

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine, doubleEngine));

        BatteryBox batteryBox = new BatteryBox(null, 20);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox, batteryBox));

        IntStream.range(0, 4).forEach(i -> {

            assertEquals(game.getCurrPlayer(), players.get(i));
            ((FreeSpace) card).currPlayerChoseEnginesToActivate(doubleEngines, batteryBoxes);

        });

        assertEquals(GameState.END_OF_CARD, game.getCurrState());

    }

    @Test
    void TestToCheckIfIllegalArgumentExceptionIsThrownWithNullParameters() {

        assertEquals(game.getCurrPlayer(), players.getFirst());

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            ((FreeSpace) card).currPlayerChoseEnginesToActivate(null, null);
        });

        assertEquals("Null lists", e.getMessage());

    }

    @Test
    void TestToCheckIfIllegalArgumentExceptionIsThrownWithDifferentSizeParameters() {

        assertEquals(game.getCurrPlayer(), players.getFirst());

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine));

        BatteryBox batteryBox = new BatteryBox(null, 3);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox, batteryBox));

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            ((FreeSpace) card).currPlayerChoseEnginesToActivate(doubleEngines, batteryBoxes);
        });

        assertEquals("The number of engines does not match the number of battery boxes", e.getMessage());

    }

    @Test
    void TestToCheckIfIllegalArgumentExceptionIsThrownWithNotEnoughBatteries() {

        assertEquals(game.getCurrPlayer(), players.getFirst());

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine, doubleEngine));

        BatteryBox batteryBox = new BatteryBox(null, 2);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox, batteryBox));

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            ((FreeSpace) card).currPlayerChoseEnginesToActivate(doubleEngines, batteryBoxes);
        });

        assertEquals("The number of required batteries is not enough", e.getMessage());

    }

    @Test
    void TestToCheckIfTheMethodPlayWorksProperly() {

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine, doubleEngine));

        BatteryBox batteryBox = new BatteryBox(null, 20);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox, batteryBox));

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        IntStream.range(0, 4).forEach(i -> {

            assertEquals(game.getCurrPlayer(), players.get(i));
            card.play(playerChoices);

        });

        assertEquals(GameState.END_OF_CARD, game.getCurrState());

    }

    @Test
    void TestToCheckIfTheUnknownStateExceptionIsThrown() {

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine, doubleEngine));

        BatteryBox batteryBox = new BatteryBox(null, 20);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox, batteryBox));

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        game.setCurrState(GameState.START_CARD);
        card.setCurrState(GameState.START_CARD);

        Exception e = assertThrows(UnknownStateException.class, () -> {
            card.play(playerChoices);
        });

        assertEquals("Unknown current state", e.getMessage());

    }

}