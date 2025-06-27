package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientStarDust;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;
import net.bytebuddy.matcher.StringSetMatcher;

import java.util.List;

public class Stardust extends AdventureCard implements PlayerMover {

    private static final List<CardState> cardStates = List.of(CardState.STARDUST);

    /**
     * Constructs a new Stardust card.
     * Initializes the card name to the simple name of this class.
     */
    public Stardust() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Returns the first state of this Stardust card.
     *
     * @return the first CardState in the card's state list
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the Stardust card's behavior based on its current state.
     * In STARDUST state, it moves incorrectly assembled ships and handles player transitions.
     * The method will advance to the next player if available or reset player iterator
     * and change the game state when all players have been processed.
     *
     * @param playerChoices the data structure containing player choices
     * @throws UnknownStateException if the card is in an unknown or invalid state
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case STARDUST:
                this.moveNotCorrectlyAssembledShips();

                if (gameModel.hasNextPlayer()) {
                    gameModel.nextPlayer();
                    setCurrState(CardState.STARDUST);
                } else {
                    setCurrState(CardState.END_OF_CARD);
                    gameModel.resetPlayerIterator();
                    gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
                }

                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Converts this server-side Stardust card to its client-side representation.
     *
     * @return a ClientStarDust object representing this card on the client side
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientStarDust(cardName,imageName);
    }

    private void moveNotCorrectlyAssembledShips() {

        Player currPlayer = gameModel.getCurrPlayer();
        movePlayer(gameModel.getFlyingBoard(), currPlayer, currPlayer.getPersonalBoard().countExposed() * -1);

    }
}