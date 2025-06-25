package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientEpidemic;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.List;
import java.util.Set;

public class Epidemic extends AdventureCard{

    /**
     * A predefined list of {@link CardState} instances that represents the sequence of states
     * relevant for the "Epidemic" card. This list is used to manage the progression through the
     * states during the card's lifecycle.
     *
     * The states in this list include:
     * - {@code CardState.EPIDEMIC}: Represents the initial state where the epidemic is triggered.
     * - {@code CardState.WAIT_FOR_CONFIRM_REMOVAL_HANDLED}: Represents the state where the confirmation
     *   for processing the removal of infected crew members is awaited.
     *
     * This constant is immutable and cannot be modified at runtime.
     */
    private static final List<CardState> cardStates = List.of(CardState.EPIDEMIC, CardState.WAIT_FOR_CONFIRM_REMOVAL_HANDLED);

    /**
     * Constructs a new instance of the Epidemic card, which is an extension of the AdventureCard.
     * This constructor initializes the card's name by setting it to the class's simple name.
     * The Epidemic card represents an in-game scenario where infected crew members need to be removed
     * from a player's ship as part of the game's mechanics.
     */
    public Epidemic() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the first state from the list of card states associated with the card.
     *
     * @return the first CardState in the list of predefined states for the card
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the play logic for the Epidemic card based on the current card state.
     * Performs specific actions such as removing infected crew members during the epidemic
     * phase and transitions to subsequent game states as required.
     *
     * @param playerChoices represents the choices made by the current player during this phase.
     *                       It provides the data required for processing the card's logic.
     * @throws UnknownStateException if the current card state is unrecognized or unsupported.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case EPIDEMIC:
                this.removeInfectedCrewMembers();
                // After processing the epidemic, move to confirmation state

                if (gameModel.hasNextPlayer()) {
                    gameModel.nextPlayer();
                    setCurrState(CardState.EPIDEMIC);
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
     * Converts the current Epidemic instance to its corresponding client-side representation,
     * encapsulated within a {@code ClientEpidemic} object. Common properties
     * are copied from the server object to the client object during this conversion.
     *
     * @return A {@code ClientEpidemic} object representing the current Epidemic instance.
     */
    @Override
    public ClientCard toClientCard() {
        ClientEpidemic clientEpidemic = new ClientEpidemic();
        ClientCard.setCommonProperties(clientEpidemic, this);
        return clientEpidemic;
    }

    /**
     * Removes infected crew members from the current player's ship board cabins.
     *
     * The method identifies all cabin coordinates on the player's ship board, along with their neighboring coordinates,
     * and removes crew members from those affected cabin locations.
     *
     * Once the removal of infected crew members is completed:
     * - An update is sent to all connected clients, notifying them about the changes in the ship board state.
     * - A targeted notification is sent to the current player, specifying which cabin coordinates had crew members removed.
     *
     * This method ensures proper synchronization of the game state across all clients
     * and updates the current player's ship board data to reflect the removal of infected crew.
     */
    private void removeInfectedCrewMembers() {
        FlyingBoard flyingBoard = gameModel.getFlyingBoard();
        Player currPlayer = gameModel.getCurrPlayer();
        ShipBoard currShipBoard = currPlayer.getPersonalBoard();
        Set<Coordinates> cabinCoordinatesWithNeighbors = currShipBoard.getCabinCoordinatesWithNeighbors();

        cabinCoordinatesWithNeighbors.forEach(coord -> ((Cabin)currShipBoard.getShipMatrix()[coord.getX()][coord.getY()]).removeMember());

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currPlayer.getNickname(), currShipBoard.getShipMatrix(), currShipBoard.getComponentsPerType());
        });

        gameModel.getGameClientNotifier().notifyClients(Set.of(currPlayer.getNickname()), (nicknameToNotify, clientController) -> {
            clientController.notifyInfectedCrewMembersRemoved(nicknameToNotify, cabinCoordinatesWithNeighbors);
        });
    }

}
