package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientEpidemic;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.component.Cabin;

import java.util.List;

public class Epidemic extends AdventureCard{

    private static final List<CardState> cardStates = List.of(CardState.EPIDEMIC, CardState.WAIT_FOR_CONFIRM_REMOVAL_HANDLED);

    public Epidemic() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case EPIDEMIC:
                this.removeInfectedCrewMembers();
                // After processing epidemic, move to confirmation state
                this.currState = CardState.WAIT_FOR_CONFIRM_REMOVAL_HANDLED;
                break;
            case WAIT_FOR_CONFIRM_REMOVAL_HANDLED:
                // Client confirmed handling - proceed to next card
                setCurrState(CardState.END_OF_CARD);
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        ClientEpidemic clientEpidemic = new ClientEpidemic();
        ClientCard.setCommonProperties(clientEpidemic, this);
        return clientEpidemic;
    }

    public void removeInfectedCrewMembers() {

        //TODO per ogni giocatore ottenere una lista di cabine abitate che sono collegate con altre cabine abitate e togliere un abitante da ciascuna
        //TODO per ogni shipboard a cui si tolgono abitatni bisogna fare la notify a tutti i giocatori usando - notifyInfectedCrewMembersRemoved.
        // in caso non si fosse tolto nessun abitante semplicemente si passa una lista vuota
    }

    @Override
    public String toString() {
        return String.format("""
           %s
           ┌────────────────────────────┐
           │          Epidemic          │
           └────────────────────────────┘
           """, imageName);
    }

}
