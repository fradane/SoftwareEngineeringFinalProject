package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class FreeSpaceTest {

    private GameModel gameModel;
    private List<Player> players;
    private FreeSpace card;
    private FlyingBoard flyingBoard;
    private ShipBoard shipBoard;
    private List<Player> outPlayers;
    private int movePlayerCallCount;
    private int lastMovedSteps;
    private Player lastMovedPlayer;

    @BeforeEach
    void setUp() {
        // Reset tracking variables
        outPlayers = new ArrayList<>();
        movePlayerCallCount = 0;
        lastMovedSteps = 0;
        lastMovedPlayer = null;

        // Create a mock flying board that tracks player movement and disqualification
        flyingBoard = new Level2FlyingBoard() {
            @Override
            public void movePlayer(Player player, int offset) {
                movePlayerCallCount++;
                lastMovedSteps = offset;
                lastMovedPlayer = player;
            }

            @Override
            public void addOutPlayer(Player player) {
                outPlayers.add(player);
            }
        };

        // Default shipboard with standard engine power calculation
        shipBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false);

        players = new ArrayList<>(List.of(
                new Player("fra", shipBoard, PlayerColor.BLUE),
                new Player("ali", shipBoard, PlayerColor.BLUE),
                new Player("luc", shipBoard, PlayerColor.BLUE),
                new Player("mar", shipBoard, PlayerColor.BLUE)
        ));

        gameModel = new GameModel("testGame", 4, false) {
            @Override
            public Boolean hasNextPlayer() {
                return false;
            }
        };
        card = new FreeSpace() {
            @Override
            public void setCurrState(CardState currState) {
                this.currState = currState;
            }
        };
        gameModel.setGameContext(new GameClientNotifier(gameModel, new ConcurrentHashMap<>()));
        gameModel.getFlyingBoard().setGameContext(new GameClientNotifier(gameModel, new ConcurrentHashMap<>()));
        card.setGame(gameModel);
        gameModel.setCurrAdventureCard(card);
        card.setCurrState(CardState.CHOOSE_ENGINES);
        gameModel.setCurrRanking(players);
    }

    @Test
    void testPlayWithZeroEnginePower_PlayerShouldBeDisqualified() {

        // Create shipboard that returns 0 engine power
        ShipBoard zeroEnginePowerBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 0;
            }
        };

        Player playerWithZeroEngines = new Player("zeroEngines", zeroEnginePowerBoard, PlayerColor.BLUE);
        gameModel.setCurrPlayer(playerWithZeroEngines);

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine));
        BatteryBox batteryBox = new BatteryBox(null, 5);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox));

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        card.play(playerChoices);

        // Verify player was disqualified
        assertEquals(1, outPlayers.size());
        assertEquals(playerWithZeroEngines, outPlayers.get(0));
        assertEquals(0, movePlayerCallCount); // No movement should occur
        assertEquals(5, batteryBox.getRemainingBatteries()); // Batteries should not be consumed
    }

    @Test
    void testPlayWithPositiveEnginePower_PlayerShouldMove() {
        final int EXPECTED_ENGINE_POWER = 7;
        
        // Create shipboard that returns specific engine power
        ShipBoard customEnginePowerBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return EXPECTED_ENGINE_POWER;
            }
        };

        Player playerWithEngines = new Player("hasEngines", customEnginePowerBoard, PlayerColor.BLUE);
        gameModel.setCurrPlayer(playerWithEngines);

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine, doubleEngine));
        BatteryBox batteryBox = new BatteryBox(null, 5);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox, batteryBox));

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        card.play(playerChoices);

        // Verify player moved correctly
        assertEquals(0, outPlayers.size()); // No disqualification
        assertEquals(1, movePlayerCallCount);
        assertEquals(EXPECTED_ENGINE_POWER, lastMovedSteps);
        assertEquals(playerWithEngines, lastMovedPlayer);
        assertEquals(3, batteryBox.getRemainingBatteries()); // 2 batteries consumed
    }

    @Test
    void testPlayWithDifferentEnginePowerScenarios() {
        int[] enginePowers = {1, 3, 5, 10, 15};
        
        for (int expectedPower : enginePowers) {
            setUp(); // Reset state for each test
            
            ShipBoard customBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
                @Override
                public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                    return expectedPower;
                }
            };

            Player testPlayer = new Player("testPlayer", customBoard, PlayerColor.BLUE);
            gameModel.setCurrPlayer(testPlayer);

            DoubleEngine doubleEngine = new DoubleEngine(null);
            List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine));
            BatteryBox batteryBox = new BatteryBox(null, 10);
            List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox));

            PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenBatteryBoxes(batteryBoxes)
                    .setChosenDoubleEngines(doubleEngines)
                    .build();

            card.play(playerChoices);

            assertEquals(expectedPower, lastMovedSteps);
            assertEquals(0, outPlayers.size());
            assertEquals(9, batteryBox.getRemainingBatteries());
        }
    }

    @Test
    void testPlayWithMultipleBatteryBoxConfigurations() {
        ShipBoard standardBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 5; // Standard power
            }
        };

        Player testPlayer = new Player("testPlayer", standardBoard, PlayerColor.BLUE);
        gameModel.setCurrPlayer(testPlayer);

        // Test scenario 1: Multiple same battery boxes
        BatteryBox batteryBox1 = new BatteryBox(null, 10);
        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = List.of(doubleEngine, doubleEngine, doubleEngine);
        List<BatteryBox> batteryBoxes = List.of(batteryBox1, batteryBox1, batteryBox1);

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        card.play(playerChoices);

        assertEquals(7, batteryBox1.getRemainingBatteries()); // 3 batteries used from same box
        assertEquals(5, lastMovedSteps);
        
        // Reset for test scenario 2: Different battery boxes
        setUp();
        testPlayer = new Player("testPlayer", standardBoard, PlayerColor.BLUE);
        gameModel.setCurrPlayer(testPlayer);
        
        BatteryBox batteryBox2 = new BatteryBox(null, 5);
        BatteryBox batteryBox3 = new BatteryBox(null, 5);
        batteryBoxes = List.of(batteryBox2, batteryBox3);
        doubleEngines = List.of(doubleEngine, doubleEngine);

        playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        card.play(playerChoices);

        assertEquals(4, batteryBox2.getRemainingBatteries()); // 1 battery used
        assertEquals(4, batteryBox3.getRemainingBatteries()); // 1 battery used
    }

    @Test
    void testPlayWithAllPlayersInSequence() {
        ShipBoard standardBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 3;
            }
        };

        // Replace all players with ones using the standard board
        players = new ArrayList<>(List.of(
                new Player("player1", standardBoard, PlayerColor.BLUE),
                new Player("player2", standardBoard, PlayerColor.GREEN),
                new Player("player3", standardBoard, PlayerColor.RED),
                new Player("player4", standardBoard, PlayerColor.YELLOW)
        ));
        gameModel = new GameModel("testGame", 4, false) {
            @Override
            public void setCurrAdventureCard(AdventureCard currAdventureCard) {
                // Do nothing to avoid null gameClientNotifier
            }
        };
        card = new FreeSpace() {
            @Override
            public void setCurrState(CardState currState) {
                this.currState = currState;
            }
        };
        card.setGame(gameModel);
        gameModel.setCurrAdventureCard(card);
        card.setCurrState(CardState.CHOOSE_ENGINES);
        gameModel.setCurrRanking(players);

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = new ArrayList<>(List.of(doubleEngine));
        BatteryBox batteryBox = new BatteryBox(null, 20);
        List<BatteryBox> batteryBoxes = new ArrayList<>(List.of(batteryBox));

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        // Test each player in sequence
        for (int i = 0; i < 4; i++) {
            assertEquals(players.get(i), gameModel.getCurrPlayer());
            card.play(playerChoices);
        }

        assertEquals(CardState.END_OF_CARD, card.getCurrState());
        assertEquals(4, movePlayerCallCount); // All 4 players moved
        assertEquals(16, batteryBox.getRemainingBatteries()); // 4 batteries used total
    }

    @Test
    void testPlayWithMixedScenarios_SomePlayersDisqualified() {
        List<Player> mixedPlayers = new ArrayList<>();
        
        // Player 1: Zero engine power (will be disqualified)
        ShipBoard zeroEngineBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 0;
            }
        };
        mixedPlayers.add(new Player("zeroEngine", zeroEngineBoard, PlayerColor.BLUE));

        // Player 2: Normal engine power
        ShipBoard normalBoard = new Level2ShipBoard(PlayerColor.GREEN, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 4;
            }
        };
        mixedPlayers.add(new Player("normalEngine", normalBoard, PlayerColor.GREEN));

        // Player 3: High engine power
        ShipBoard highPowerBoard = new Level2ShipBoard(PlayerColor.RED, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 8;
            }
        };
        mixedPlayers.add(new Player("highEngine", highPowerBoard, PlayerColor.RED));

        gameModel = new GameModel("testGame", mixedPlayers.size(), false) {
            @Override
            public void setCurrAdventureCard(AdventureCard currAdventureCard) {
                // Do nothing to avoid null gameClientNotifier
            }
        };
        card = new FreeSpace() {
            @Override
            public void setCurrState(CardState currState) {
                this.currState = currState;
            }
        };
        card.setGame(gameModel);
        gameModel.setCurrAdventureCard(card);
        card.setCurrState(CardState.CHOOSE_ENGINES);
        gameModel.setCurrRanking(mixedPlayers);

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = List.of(doubleEngine);
        BatteryBox batteryBox = new BatteryBox(null, 10);
        List<BatteryBox> batteryBoxes = List.of(batteryBox);

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        // Play for all three players
        for (int i = 0; i < 3; i++) {
            card.play(playerChoices);
        }

        assertEquals(CardState.END_OF_CARD, card.getCurrState());
        assertEquals(1, outPlayers.size()); // Only first player disqualified
        assertEquals(mixedPlayers.get(0), outPlayers.get(0));
        assertEquals(2, movePlayerCallCount); // Two players moved
        assertEquals(8, batteryBox.getRemainingBatteries()); // 2 batteries used
    }

    @Test
    void testPlayWithNullParametersThrowsException() {
        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(null)
                .setChosenDoubleEngines(null)
                .build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            card.play(playerChoices);
        });

        assertEquals("Null lists", e.getMessage());
    }

    @Test
    void testPlayWithMismatchedListSizesThrowsException() {
        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = List.of(doubleEngine, doubleEngine);
        BatteryBox batteryBox = new BatteryBox(null, 5);
        List<BatteryBox> batteryBoxes = List.of(batteryBox); // Different size

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            card.play(playerChoices);
        });

        assertEquals("The number of engines does not match the number of battery boxes", e.getMessage());
    }

    @Test
    void testPlayWithInsufficientBatteriesThrowsException() {
        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = List.of(doubleEngine, doubleEngine, doubleEngine);
        BatteryBox batteryBox = new BatteryBox(null, 2); // Only 2 batteries, need 3
        List<BatteryBox> batteryBoxes = List.of(batteryBox, batteryBox, batteryBox);

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            card.play(playerChoices);
        });

        assertEquals("The number of required batteries is not enough", e.getMessage());
    }

    @Test
    void testPlayWithWrongStateThrowsException() {
        card.setCurrState(CardState.START_CARD); // Wrong state

        DoubleEngine doubleEngine = new DoubleEngine(null);
        List<Engine> doubleEngines = List.of(doubleEngine);
        BatteryBox batteryBox = new BatteryBox(null, 5);
        List<BatteryBox> batteryBoxes = List.of(batteryBox);

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxes)
                .setChosenDoubleEngines(doubleEngines)
                .build();

        Exception e = assertThrows(UnknownStateException.class, () -> {
            card.play(playerChoices);
        });

        assertEquals("Unknown current state", e.getMessage());
    }

    @Test
    void testPlayWithEmptyListsButValidConfiguration() {
        ShipBoard zeroEngineBoard = new Level2ShipBoard(PlayerColor.BLUE, null, false) {
            @Override
            public int countTotalEnginePower(List<Engine> enginesToCountEnginePower) {
                return 0; // Empty lists should result in 0 power
            }
        };

        Player testPlayer = new Player("emptyLists", zeroEngineBoard, PlayerColor.BLUE);
        gameModel.setCurrPlayer(testPlayer);

        List<Engine> emptyEngines = new ArrayList<>();
        List<BatteryBox> emptyBatteries = new ArrayList<>();

        PlayerChoicesDataStructure playerChoices = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(emptyBatteries)
                .setChosenDoubleEngines(emptyEngines)
                .build();

        card.play(playerChoices);

        // Should be disqualified due to 0 engine power
        assertEquals(1, outPlayers.size());
        assertEquals(testPlayer, outPlayers.get(0));
        assertEquals(0, movePlayerCallCount);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.CHOOSE_ENGINES, card.getFirstState());
    }

    @Test
    void testToString() {
        String result = card.toString();
        assertTrue(result.contains("FreeSpace"));
        assertTrue(result.contains("┌────────────────────────────┐"));
        assertTrue(result.contains("└────────────────────────────┘"));
    }
}