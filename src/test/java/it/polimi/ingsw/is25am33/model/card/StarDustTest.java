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

class StardustTest {
    private GameModel gameModel;
    private Stardust stardust;

    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        // Inizializzazione game model e giocatori reali
        gameModel = new GameModel("1234", 2, false) {
            @Override
            public void setCurrGameState(GameState state) {
                // Nessuna implementazione necessaria per test
            }
        };
        GameClientNotifier gameClientNotifier = new GameClientNotifier(gameModel, new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);
        player1 = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        player2 = new Player("marco", new Level2ShipBoard(PlayerColor.RED, gameClientNotifier, false), PlayerColor.RED);

        gameModel.getPlayers().put(player1.getNickname(), player1);
        gameModel.getPlayers().put(player2.getNickname(), player2);

        gameModel.setCurrRanking(List.of(player1, player2));
        gameModel.resetPlayerIterator();

        // Inserisci giocatori sulla FlyingBoard
        gameModel.getFlyingBoard().insertPlayer(player1);
        gameModel.getFlyingBoard().insertPlayer(player2);

        stardust = new Stardust();
        stardust.setGame(gameModel);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.STARDUST, stardust.getFirstState());
    }

    @Test
    void testPlayStardustMovesPlayersCorrectly() {
        stardust.setCurrState(CardState.STARDUST);
        // Prima dell'azione
        int initialPositionP1 = gameModel.getFlyingBoard().getPlayerPosition(player1);
        int initialPositionP2 = gameModel.getFlyingBoard().getPlayerPosition(player2);

        // Azione
        stardust.play(new PlayerChoicesDataStructure());

        assertTrue(gameModel.getFlyingBoard().getPlayerPosition(player1) <= initialPositionP1);
        assertTrue(gameModel.getFlyingBoard().getPlayerPosition(player2) <= initialPositionP2);
    }

    @Test
    void testStardustFinalStateAfterPlay() {
        stardust.setCurrState(CardState.STARDUST);

        // Esegui il play finché non arriviamo alla fine
        while (stardust.getCurrState() == CardState.STARDUST) {
            stardust.play(new PlayerChoicesDataStructure());
        }

        assertEquals(CardState.END_OF_CARD, stardust.getCurrState(),
                "Lo stato finale dovrebbe essere END_OF_CARD.");
    }

    @Test
    void testStardustStateTransition() {
        stardust.setCurrState(CardState.STARDUST);

        boolean isLastPlayer = !gameModel.hasNextPlayer();

        // Prima esecuzione - dovrebbe passare al prossimo giocatore
        stardust.play(new PlayerChoicesDataStructure());

        // Se c'è ancora un giocatore, lo stato rimane STARDUST
        if (!isLastPlayer) {
            assertEquals(CardState.STARDUST, stardust.getCurrState(),
                    "Lo stato dovrebbe rimanere STARDUST se ci sono altri giocatori");
        } else {
            assertEquals(CardState.END_OF_CARD, stardust.getCurrState(),
                    "Lo stato dovrebbe essere END_OF_CARD se non ci sono più giocatori");
        }
    }

    @Test
    void testPlayInvalidStateThrowsException() {
        stardust.setCurrState(CardState.END_OF_CARD);

        assertThrows(UnknownStateException.class,
                () -> stardust.play(new PlayerChoicesDataStructure()),
                "Si dovrebbe lanciare UnknownStateException per stati invalidi.");
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = stardust.toClientCard();
        assertTrue(clientCard instanceof ClientStarDust,
                "La carta client dovrebbe essere di tipo ClientStarDust.");
    }
}

