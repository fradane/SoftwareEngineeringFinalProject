package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientSlaveTraders;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.ArrayList;
import java.util.List;

public class SlaveTraders extends AdvancedEnemies implements PlayerMover, CrewMemberRemover, DoubleCannonActivator {
    /**
     * Represents the penalty applied to the crew when interacting with the
     * SlaveTraders card. This value indicates the number of crew members
     * affected negatively, such as being removed or incapacitated,
     * depending on the current game state. The specific effects and behavior
     * are determined by the game logic and state transitions.
     */
    private int crewMalus;

    /**
     * Represents a predefined sequence of states in the game for the "SlaveTraders" card.
     * The states dictate the progression of actions or decisions a player must take when interacting with this card.
     * This immutable list includes the following specific stages:
     * CHOOSE_CANNONS: The player selects cannons to activate.
     * ACCEPT_THE_REWARD: The player decides whether to accept a reward.
     * REMOVE_CREW_MEMBERS: The player selects crew members to remove.
     */
    private final static List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.REMOVE_CREW_MEMBERS);

    /**
     * Default constructor for the SlaveTraders class.
     * Initializes the card's name using the class's simple name.
     */
    public SlaveTraders() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the crew malus value associated with this instance.
     *
     * @return the crew malus value as an integer
     */
    public int getCrewMalus() {
        return crewMalus;
    }

    /**
     * Sets the crew malus value for the instance of the SlaveTraders class.
     * This method assigns the specified crewMalus value to the crewMalus field.
     */
    public void setCrewMalus() {
        this.crewMalus = crewMalus;
    }

    /**
     * Retrieves the first CardState from the list of card states.
     *
     * @return the first CardState in the cardStates list.
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the game's logic depending on the current state and the player's choices.
     * The method determines the appropriate action based on the internal state of the object and the provided player choices.
     *
     * @param playerChoices the data structure encapsulating the player's choices, including optional selections for cannons, rewards, and crew members.
     *                      It must contain valid data relevant to the current state; otherwise, appropriate exceptions will be thrown.
     * @throws UnknownStateException if the current state does not match any expected, predefined states.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case ACCEPT_THE_REWARD:
                this.currPlayerDecidedToGetTheReward(playerChoices.isHasAcceptedTheReward());
                break;
            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Converts this SlaveTraders instance into a ClientSlaveTraders instance.
     *
     * @return a new ClientSlaveTraders object initialized with this instance's properties,
     *         such as card name, image name, required firepower, reward, crew malus, and steps back.
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientSlaveTraders(this.cardName, this.imageName,this.requiredFirePower, this.reward, this.crewMalus, this.stepsBack);
    }

    /**
     * Handles the activation of the double cannons and battery boxes chosen by the current player
     * and determines the subsequent game state based on the player's cannon power. Updates the
     * game state and notifies all clients about the ship board status update.
     *
     * @param chosenDoubleCannonsCoords the coordinates of the double cannons chosen by the current player
     * @param chosenBatteryBoxesCoords the coordinates of the battery boxes chosen by the current player
     * @throws IllegalArgumentException if the provided coordinates are invalid or cannot be associated with valid components
     */
    private void currPlayerChoseCannonsToActivate(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {
        Player currentPlayer=gameModel.getCurrPlayer();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();
        List<Cannon> chosenDoubleCannons = new ArrayList<>();

        for(Coordinates chosenDoubleCannonCoord : chosenDoubleCannonsCoords) {
            chosenDoubleCannons.add((Cannon) currentPlayer.getPersonalBoard().getComponentAt(chosenDoubleCannonCoord));
        }

        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
        });

        if (currPlayerCannonPower > requiredFirePower) {

            setCurrState(CardState.ACCEPT_THE_REWARD);

        } else if (currPlayerCannonPower == requiredFirePower) {

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState( CardState.CHOOSE_CANNONS);
            } else {
                setCurrState( CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }

        } else {
            setCurrState(CardState.REMOVE_CREW_MEMBERS);
        }

    }

    /**
     * Executes the logic for when the current player decides whether to accept the reward or not.
     * If the player accepts the reward, credits are added to their account,
     * they are moved backwards on the game board by a specified number of steps,
     * and the card transitions to the end state. The game proceeds to the next phase.
     *
     * @param hasPlayerAcceptedTheReward true if the current player has chosen to accept the reward; false otherwise
     */
    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            gameModel.getCurrPlayer().addCredits(reward);
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
        }

        setCurrState(CardState.END_OF_CARD);
        gameModel.resetPlayerIterator();
        gameModel.setCurrGameState(GameState.DRAW_CARD);
    }


    /**
     * Handles the process where the current player selects crew members to be removed from
     * their personal ship board. It identifies the cabins based on the provided coordinates,
     * processes the removal, and notifies all clients of the updated ship board. Depending on
     * the game's state, it determines the next step in the game flow, either transitioning to
     * the next player's turn or advancing to the end-of-card state.
     *
     * @param chosenCabinsCoordinate A list of {@code Coordinates} representing the positions
     *                               of the selected cabins (crew members) to be removed on
     *                               the player's personal ship board. These coordinates must
     *                               correspond to valid cabin locations.
     * @throws IllegalArgumentException if any of the provided coordinates do not correspond
     *                                  to valid or removable cabins on the ship board.
     */
    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException{
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        removeMemberProcess(chosenCabins, crewMalus);
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getCurrPlayer().getPersonalBoardAsMatrix(), gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
        });

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

}