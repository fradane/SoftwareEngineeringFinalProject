package it.polimi.ingsw.is25am33.model.card;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.card.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.*;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.model.UnknownStateException;

class EpidemicTest {

    private GameModel gameModel;
    private AdventureCard epidemic;
    private GameClientNotifier gameClientNotifier;

    @BeforeEach
    void setUp() {
        // Inizializzazione base
        gameModel = new GameModel("1234", 2, false){
            @Override
            public void setCurrGameState(GameState gameState){
            }
        };
        gameClientNotifier = new GameClientNotifier(gameModel,new ConcurrentHashMap<>());
        gameModel.setGameClientNotifier(gameClientNotifier);
        epidemic = new Epidemic();
        epidemic.setGame(gameModel);

        // Aggiungiamo un giocatore
        Player player = new Player("luca", new Level2ShipBoard(PlayerColor.YELLOW, gameClientNotifier, false), PlayerColor.YELLOW);
        gameModel.getPlayers().put("luca", player);
        gameModel.setCurrPlayer(player);

        gameModel.getFlyingBoard().setGameClientNotifier(gameClientNotifier);
        gameModel.getFlyingBoard().insertPlayer(player);
    }

    @Test
    void testGetFirstState() {
        assertEquals(CardState.EPIDEMIC, epidemic.getFirstState());
    }

    @Test
    void testPlayEpidemicMovesToNextPlayer() {
        epidemic.setCurrState(CardState.EPIDEMIC);
        Player secondPlayer = new Player("francesco", new Level2ShipBoard(PlayerColor.GREEN, gameClientNotifier, false), PlayerColor.GREEN);
        gameModel.getPlayers().put("francesco", secondPlayer);
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        epidemic.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.EPIDEMIC, epidemic.getCurrState());
    }

    @Test
    void testPlayEpidemicEndsWithLastPlayer() {
        gameModel.setCurrRanking(gameModel.getPlayers().values().stream().toList());
        gameModel.resetPlayerIterator();
        epidemic.setCurrState(CardState.EPIDEMIC);
        // Nessun secondo giocatore
        epidemic.play(new PlayerChoicesDataStructure());

        assertEquals(CardState.END_OF_CARD, epidemic.getCurrState());
    }

    @Test
    void testPlayUnknownStateException() {
        epidemic.setCurrState(CardState.WAIT_FOR_CONFIRM_REMOVAL_HANDLED);
        assertThrows(UnknownStateException.class, () -> epidemic.play(new PlayerChoicesDataStructure()));
    }

    @Test
    void testToClientCard() {
        ClientCard clientCard = epidemic.toClientCard();
        assertTrue(clientCard instanceof ClientEpidemic);
        assertEquals("Epidemic", clientCard.getCardName());
    }

}
