package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientFreeSpace;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.ArrayList;
import java.util.List;

public class FreeSpace extends AdventureCard implements PlayerMover {

    /**
     * Represents the initial states associated with the "FreeSpace" adventure card.
     * This list contains a fixed sequence of {@link CardState} starting states,
     * defining the card's flow of execution.
     */
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_ENGINES);

    /**
     * Constructs a FreeSpace object by initializing its name to the class's simple name.
     * FreeSpace represents a specific type of adventure card in the game.
     */
    public FreeSpace() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the first state from the cardStates list.
     *
     * @return the first CardState element in the cardStates list
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the player's actions based on the current state of the game.
     * The method requires player choices as input and performs appropriate actions
     * depending on the current state. Throws an exception if the state is unknown.
     *
     * @param playerChoices an instance of PlayerChoicesDataStructure containing the player's choices
     *                      such as selected double engines and battery boxes, necessary for this state.
     * @throws UnknownStateException if the current state is not a recognized or valid state for execution.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_ENGINES:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Creates and returns a ClientFreeSpace instance representing the current FreeSpace card.
     *
     * @return a new ClientFreeSpace object initialized with the card name and image name of this FreeSpace card.
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientFreeSpace(cardName,imageName);
    }

    /**
     * Handles the activation of engines and battery boxes chosen by the current player.
     * Validates the input data, checks engine power, triggers player updates, and determines the next state of the game.
     *
     * @param chosenDoubleEnginesCoords a list of coordinates representing the positions of the chosen double engines on the player's board
     * @param chosenBatteryBoxesCoords a list of coordinates representing the positions of the chosen battery boxes on the player's board
     * @throws IllegalArgumentException if either of the input lists is null or if their sizes do not match
     */
    private void currPlayerChoseEnginesToActivate(List<Coordinates> chosenDoubleEnginesCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        if (chosenDoubleEnginesCoords == null || chosenBatteryBoxesCoords == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEnginesCoords.size() != chosenBatteryBoxesCoords.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        List<Engine> chosenDoubleEngines = new ArrayList<>();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenDoubleEnginesCoord : chosenDoubleEnginesCoords) {
            chosenDoubleEngines.add((Engine) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenDoubleEnginesCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        int stepsForward = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);
        // check whether the declared engine power equals 0, in this case the player must be disqualified
        if (stepsForward == 0) {
            gameModel.getFlyingBoard().addOutPlayer(gameModel.getCurrPlayer(), false);

            if(gameModel.getFlyingBoard().getRanking().isEmpty()){ // tutti i giocatori sono stati eliminati
                gameModel.setCurrGameState(GameState.END_GAME);
            }
        } else {

            chosenBatteryBoxes.forEach(BatteryBox::useBattery);

            Player currentPlayer=gameModel.getCurrPlayer();

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
               clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
            });

            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsForward);
        }

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_ENGINES);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

}
