package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientAbandonedShip;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.List;


public class AbandonedShip extends AdventureCard implements PlayerMover, CrewMemberRemover {

    /**
     * Represents the penalty in terms of the number of crew members
     * that are negatively affected when encountering an abandoned ship.
     * This penalty is applied during gameplay mechanics where the
     * number of available crew members can influence the overall
     * strategy and outcome of the game.
     */
    private int crewMalus;
    /**
     * Represents the number of steps a player moves backward upon encountering the AbandonedShip card.
     * The value determines the penalty applied in terms of movement on the game board.
     */
    private int stepsBack;
    /**
     * The reward represents a point or value granted to the current player when a specific
     * condition or event occurs within the game.
     * This variable is typically used to store the benefit or gain that a player receives
     * upon successfully completing or interacting with the AbandonedShip card. The reward
     * value can influence gameplay by providing an incentive to visit or make specific
     * decisions related to the AbandonedShip card.
     * The reward should be positive or zero, aligning with the mechanics of the game to
     * enhance player strategy and progression.
     */
    private int reward;

    /**
     * Represents a predefined, immutable list of {@link CardState} objects relevant to the behavior of the "AbandonedShip" class.
     * This list determines the sequential states that an "AbandonedShip" card can traverse during gameplay.
     * The included states are:
     * - {@code VISIT_LOCATION}: Represents the action of visiting a specific location.
     * - {@code REMOVE_CREW_MEMBERS}: Represents the action of removing crew members from the ship.
     */
    private static final List<CardState> cardStates = List.of(CardState.VISIT_LOCATION, CardState.REMOVE_CREW_MEMBERS);

    /**
     * Default constructor for the AbandonedShip class.
     *
     * This constructor initializes the card name using the class's simple name.
     */
    public AbandonedShip() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the crew malus value associated with this AbandonedShip.
     *
     * @return the crew malus value as an integer
     */
    public int getCrewMalus() {
        return crewMalus;
    }

    /**
     * Retrieves the number of steps to move back associated with this instance.
     *
     * @return the number of steps to move back
     */
    public int getStepsBack() {
        return stepsBack;
    }

    /**
     * Retrieves the reward associated with the abandoned ship.
     *
     * @return the reward value
     */
    public int getReward() {
        return reward;
    }

    /**
     * Sets the level of the abandoned ship.
     *
     * @param level the level to be set
     */
    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Sets the penalty value for the crew, which may represent the negative impact
     * or disadvantage applied in the game scenario where crew members are involved.
     *
     * @param crewMalus the penalty value to be set for the crew.
     *                  This value determines how the crew's performance or effect
     *                  is negatively influenced during gameplay.
     */
    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    /**
     * Sets the number of steps the player is required to move back
     * during the AbandonedShip card's play.
     *
     * @param stepsBack the number of steps to move back; must be a non-negative integer.
     */
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    /**
     * Sets the reward value for the abandoned ship.
     *
     * @param reward the reward value to be set
     */
    public void setReward(int reward) {
        this.reward = reward;
    }

    /**
     * Retrieves the first CardState from the list of card states associated with this
     * AbandonedShip object.
     *
     * @return the first CardState in the list.
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the actions based on the current state of the game and the choices made by the player.
     *
     * @param playerChoices the data structure containing the choices made by the current player,
     *                      such as whether the player wants to visit or which crew members to remove.
     * @throws UnknownStateException if the game is in an unknown or unsupported state.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case VISIT_LOCATION:
                try {
                    this.currPlayerWantsToVisit(playerChoices.isWantsToVisit());
                } catch (IllegalDecisionException e) {
                    e.printStackTrace();
                }
                break;

            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Converts the current AbandonedShip instance to its corresponding ClientCard representation.
     *
     * @return the ClientAbandonedShip object created based on the properties of the current AbandonedShip instance.
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientAbandonedShip(cardName, imageName, crewMalus, stepsBack, reward);
    }

    /**
     * Manages the current player's decision to either visit the abandoned ship or skip it. Updates
     * the game state and player sequence based on the player's choice.
     *
     * @param wantsToVisit a boolean indicating whether the current player wants to visit the abandoned ship.
     *                     If true, the method checks if the player has enough crew members to proceed.
     *                     If false, the game moves to the next player or the next phase if no players remain.
     * @throws IllegalDecisionException if the player chooses to visit but does not have enough crew members
     *                                  to meet the required condition.
     */
    private void currPlayerWantsToVisit(boolean wantsToVisit) throws IllegalDecisionException {
        try {
            if (wantsToVisit) {
                if (gameModel.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < crewMalus)
                    throw new IllegalDecisionException("Player has not enough crew members");
                setCurrState(CardState.REMOVE_CREW_MEMBERS);
            } else if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.VISIT_LOCATION);
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        } catch (Exception e) {
            System.err.println("Error in currPlayerWantsToVisit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the selection of removable crew members by the current player based on the provided coordinates.
     * The chosen cabins corresponding to the input coordinates are processed to remove crew members,
     * and the state and player progress are updated accordingly.
     *
     * @param chosenCabinsCoordinate a list of {@link Coordinates} indicating the locations of the cabins
     *                               on the current player's ship where crew members are to be removed.
     *                               Must not contain invalid or null elements.
     * @throws IllegalArgumentException if the coordinates do not correspond to valid cabins on the ship board
     *                                  or if the cabins are not eligible for crew member removal.
     */
    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException {
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();


        removeMemberProcess(chosenCabins, crewMalus);

        String currPlayerNickname = gameModel.getCurrPlayer().getNickname();
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currPlayerNickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType(), shipBoard.getNotActiveComponents());
        });

        gameModel.getCurrPlayer().addCredits(reward);
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        setCurrState(CardState.END_OF_CARD);
        gameModel.setCurrGameState(GameState.CHECK_PLAYERS);

    }

}
