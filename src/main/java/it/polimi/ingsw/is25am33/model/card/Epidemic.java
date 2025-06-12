package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientEpidemic;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.List;
import java.util.Set;

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

                if (gameModel.hasNextPlayer()) {
                    gameModel.nextPlayer();
                    setCurrState(CardState.EPIDEMIC);
                } else {
                    setCurrState(CardState.END_OF_CARD);
                    gameModel.resetPlayerIterator();
                    gameModel.setCurrGameState(GameState.DRAW_CARD);
                }
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
        Player currPlayer = gameModel.getCurrPlayer();
        ShipBoard currShipBoard = currPlayer.getPersonalBoard();
        Set<Coordinates> cabinCoordinatesWithNeighbors = currShipBoard.getCabinCoordinatesWithNeighbors();

        cabinCoordinatesWithNeighbors.forEach(coord -> ((Cabin)currShipBoard.getShipMatrix()[coord.getX()][coord.getY()]).removeMember());

        gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currPlayer.getNickname(), currShipBoard.getShipMatrix(), currShipBoard.getComponentsPerType());
        });

        gameModel.getGameContext().notifyClients(Set.of(currPlayer.getNickname()), (nicknameToNotify, clientController) -> {
            try {
                clientController.notifyInfectedCrewMembersRemoved(nicknameToNotify, cabinCoordinatesWithNeighbors);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
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
