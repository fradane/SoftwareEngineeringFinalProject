package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientStarDust;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import net.bytebuddy.matcher.StringSetMatcher;

import java.util.List;

public class Stardust extends AdventureCard implements PlayerMover {

    private static final List<CardState> cardStates = List.of(CardState.STARDUST);

    public Stardust() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case STARDUST:
                this.moveNotCorrectlyAssembledShips();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        return new ClientStarDust(cardName,imageName);
    }

    private void moveNotCorrectlyAssembledShips() {

        gameModel.getCurrRanking()
                .reversed()
                .forEach(p -> movePlayer(gameModel.getFlyingBoard(), p, p.getPersonalBoard().countExposed() * -1));
        setCurrState( CardState.END_OF_CARD);
        gameModel.resetPlayerIterator();
        gameModel.setCurrGameState(GameState.DRAW_CARD);
    }
}
