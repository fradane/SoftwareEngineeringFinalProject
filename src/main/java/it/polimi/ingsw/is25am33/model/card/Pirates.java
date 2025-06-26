package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientPirates;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.interfaces.HowToDefend;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class Pirates extends AdvancedEnemies implements PlayerMover, DoubleCannonActivator, HowToDefend {

    /**
     * A collection of {@code Shot} objects representing the shots fired during a game.
     * Each {@code Shot} encapsulates details about its direction and attack behavior.
     * This field is used to manage and track all the shots involved in the current state
     * of the game, including their creation and usage during gameplay.
     */
    private List<Shot> shots;
    /**
     * A list of unique string identifiers representing the IDs of shots executed during the game.
     * Each ID corresponds to a specific shot performed by a player or game logic.
     */
    private List<String> shotIDs;
    /**
     * Represents a predefined list of game card states relevant to the "Pirates" game functionality.
     * This list is immutable and contains the following card states:
     * CHOOSE_CANNONS, ACCEPT_THE_REWARD, THROW_DICES, DANGEROUS_ATTACK, CHECK_SHIPBOARD_AFTER_ATTACK.
     * These states determine the sequence of actions or events in the gameplay.
     */
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.THROW_DICES, CardState.DANGEROUS_ATTACK, CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
    /**
     * A list that keeps track of players who have been defeated during the game.
     * This collection stores Player instances and is used to manage the state of
     * players who are no longer actively participating in the game.
     *
     * Once a player is defeated, their corresponding Player object is added to this list.
     * It helps in tracking game progress and determining the outcome of the game
     * in scenarios where the state of each player matters.
     *
     * This list is immutable after its initialization, but players can be added
     * dynamically to it during the game.
     */
    private final List<Player> defeatedPlayers = new ArrayList<>();
    /**
     * An iterator that allows traversal of the collection of {@link Shot} objects
     * associated with the game logic in the Pirates context.
     *
     * The iterator facilitates operations such as iterating over the available
     * shots, enabling their sequential processing or handling within game mechanics.
     *
     * It is initialized based on the current list of shots available and tied to
     * the state and behavior of the Pirates class.
     */
    private Iterator<Shot> shotIterator;
    /**
     * An iterator over the collection of defeated players within the game.
     * It allows sequential access to the players who have been defeated
     * during the gameplay, providing a mechanism to traverse through the
     * list of defeated players.
     *
     * This iterator can be used to perform operations such as checking
     * the state, retrieving specific details, or manipulating the defeated
     * players' data.
     *
     * The iterator is private to the class and directly interacts with
     * the internal representation of defeated players in the game.
     */
    private Iterator<Player> defeatedPlayerIterator;
    /**
     * Represents the most recently defeated player in the current game context.
     * This variable stores a reference to the player who was last defeated during the game,
     * allowing for tracking and further interactions with that player (e.g., handling rewards,
     * penalties, or game state changes related to their defeat).
     */
    private Player currDefeatedPlayer;

    /**
     * Retrieves the list of shot IDs associated with the current state of the object.
     *
     * @return a list of strings representing the shot IDs.
     */
    public List<String> getShotIDs() {
        return shotIDs;
    }

    /**
     * Sets the list of shot IDs associated with the current instance.
     *
     * @param shotIDs the list of shot IDs to be set
     */
    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    /**
     * Retrieves the list of Shot objects associated with the current instance.
     *
     * @return a List of Shot objects.
     */
    public List<Shot> getShots() {
        return shots;
    }

    /**
     * Sets the list of shots and initializes the iterator for the shots.
     *
     * @param shots the list of {@code Shot} objects to be set and iterated over.
     */
    public void setShots(List<Shot> shots) {

        this.shots = shots;
        shotIterator = shots.iterator();
    }

    /**
     * Default constructor for the Pirates class.
     * Initializes the card with its name derived from its class name.
     */
    public Pirates() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the first state of the card from the list of card states.
     *
     * @return the first CardState in the list of card states
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Retrieves the current game model instance.
     *
     * @return the current instance of GameModel representing the game state.
     */
    @Override
    public GameModel getGameModel() {
        return gameModel;
    }

    /**
     * Executes the current player's action based on the game's state and the choices provided.
     *
     * @param playerChoices A data structure containing the player's action choices, such as selected cannons,
     *                      battery boxes, dice rolls, reward decisions, or attack details, depending on the current game state.
     * @throws IllegalArgumentException If required choices for the current state are missing or invalid.
     * @throws UnknownStateException    If the current game state is unrecognized.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case ACCEPT_THE_REWARD:
                this.currPlayerDecidedToGetTheReward(playerChoices.isHasAcceptedTheReward());
                break;
            case THROW_DICES:
                this.throwDices();
                break;
            case DANGEROUS_ATTACK:
                ((Shot) gameModel.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                this.checkShipBoardAfterAttack();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Converts the current Pirates instance into a {@link ClientPirates} object.
     * The method transforms the list of {@link Shot} objects associated with this Pirates instance
     * into a list of corresponding {@link ClientDangerousObject} instances. These objects are then
     * included in the constructed {@link ClientPirates} object along with other properties
     * such as the card name, image name, required firepower, reward, and steps back.
     *
     * @return a {@link ClientPirates} object containing the client-side representation of the card,
     *         including its dangerous objects, firepower requirements, reward details, and movement data.
     */
    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Shot shot : shots) {
            clientDangerousObjects.add(new ClientDangerousObject(shot.getDangerousObjType(),shot.getDirection(), -1));
        }
        return new ClientPirates(this.getCardName(),this.imageName,clientDangerousObjects,this.requiredFirePower,this.reward,this.stepsBack);
    }

    /**
     * Converts a list of shot IDs into Shot objects and initializes an iterator over the resulting list of shots.
     *
     * This method processes the shotIDs field by mapping each ID to a corresponding Shot object
     * using the shotCreator dependency. The mapping operation involves invoking the call method on
     * the retrieved object associated with each ID. If an exception occurs during the conversion
     * process, it wraps the exception in a RuntimeException and throws it.
     *
     * Once the conversion is complete, the generated list of shots is assigned to the shots field.
     * Additionally, the method initializes the shotIterator field with an iterator for the shots list.
     *
     * Note: The method assumes that all IDs in the shotIDs list can be successfully converted
     * and does not provide recovery mechanisms for individual conversion failures.
     */
    public void convertIdsToShots() {

        shots = shotIDs.stream()
                .map(id -> {
                    try {
                        return shotCreator.get(id).call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        this.shotIterator = shots.iterator();
    }

    /**
     * Validates and updates the game state after a shipboard attack in the game.
     * This method checks the state of the ship boards and determines subsequent actions
     * based on the outcome of the attack, the defeated players, and the current game rules.
     *
     * The method performs the following:
     * 1. Notifies the game model if ship boards are invalid.
     * 2. Checks if all ships are in a valid or correct state.
     * 3. Updates the current player and game state based on the sequence of defeated players
     *    or the availability of shots for the current turn.
     * 4. Resets the game state if no further actions are possible.
     */
    private void checkShipBoardAfterAttack(){
        gameModel.notifyInvalidShipBoards();
        if(gameModel.areAllShipsCorrect()) {

            if (defeatedPlayerIterator.hasNext()) {
                currDefeatedPlayer= defeatedPlayerIterator.next();
                gameModel.setCurrPlayer(currDefeatedPlayer);
                setCurrState(CardState.DANGEROUS_ATTACK);
            }
            else if (shotIterator.hasNext()) {
                defeatedPlayerIterator =defeatedPlayers.iterator();
                currDefeatedPlayer= defeatedPlayerIterator.next();
                gameModel.setCurrPlayer(currDefeatedPlayer);
                setCurrState(CardState.THROW_DICES);
            } else {
                setCurrState(CardState.END_OF_CARD);
                defeatedPlayers.clear();
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }
    }

    /**
     * Handles the activation process of cannons and battery boxes chosen by the current player.
     * Validates the chosen components, calculates the current player's cannon power, and determines the next game state based on the result.
     *
     * @param chosenDoubleCannonsCoords the coordinates of the double cannons chosen by the current player
     * @param chosenBatteryBoxesCoords the coordinates of the battery boxes chosen by the current player
     * @throws IllegalArgumentException if invalid coordinates or components are provided
     */
    private void currPlayerChoseCannonsToActivate(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {
        Player currentPlayer = gameModel.getCurrPlayer();
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
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (currPlayerCannonPower > requiredFirePower) {

            setCurrState( CardState.ACCEPT_THE_REWARD);

        } else if(currPlayerCannonPower < requiredFirePower){
            defeatedPlayers.add(gameModel.getCurrPlayer());

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.CHOOSE_CANNONS);
            } else {
                if (defeatedPlayers.isEmpty()) {
                    setCurrState(CardState.END_OF_CARD);
                    gameModel.resetPlayerIterator();
                    gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
                } else {
                    defeatedPlayerIterator =defeatedPlayers.iterator();
                    currDefeatedPlayer= defeatedPlayerIterator.next();
                    gameModel.setCurrPlayer(currDefeatedPlayer);
                    setCurrState(CardState.THROW_DICES);
                }
            }
        }else{
            if(gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.CHOOSE_CANNONS);
            }
            else{
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }

    }

    /**
     * Executes the action of throwing dices and updates the current game state accordingly.
     * This method retrieves the next shot from the shot iterator, assigns coordinates
     * to it by simulating a dice throw, updates the current dangerous object in the game,
     * and changes the current state of the game to indicate a dangerous attack.
     *
     * The process includes:
     * - Retrieving the next shot object.
     * - Setting the coordinates of the shot using a dice throw.
     * - Updating the game model with the current dangerous object.
     * - Setting the current state to indicate a dangerous attack.
     */
    private void throwDices() {

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currShot);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }

    /**
     * Handles the decision of the current player to accept or decline the reward.
     * If the reward is accepted, credits are added to the current player and
     * they are moved back by a specified number of steps. The state of the card
     * and game is updated based on the state of defeated players.
     *
     * @param hasPlayerAcceptedTheReward A boolean indicating if the current player
     *                                   has decided to accept the reward. If true,
     *                                   the player will receive the reward and
     *                                   their position will be updated accordingly.
     */
    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward){
        if (hasPlayerAcceptedTheReward) {
            gameModel.getCurrPlayer().addCredits(reward);
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
        }
        if (defeatedPlayers.isEmpty()) {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        } else {
            defeatedPlayerIterator = defeatedPlayers.iterator();
            currDefeatedPlayer= defeatedPlayerIterator.next();
            gameModel.setCurrPlayer(currDefeatedPlayer);
            setCurrState(CardState.THROW_DICES);
        }
    }


}