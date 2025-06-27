package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientSmugglers;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CubesRedistributionHandler;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class Smugglers extends Enemies implements PlayerMover, DoubleCannonActivator, CubesRedistributionHandler {

    /**
     * Represents the penalty in terms of cargo cubes that the player incurs when interacting with this card.
     *
     * This variable typically holds the number of cargo cubes deducted or affected
     * during specific gameplay mechanics as determined by the player's decisions or card effects.
     */
    private int cubeMalus;
    /**
     * Represents the reward for a player consisting of a list of CargoCubes.
     * Each CargoCube in the list has a specific value tied to its color.
     * The reward is determined by the game logic and can be used for various gameplay decisions.
     */
    private List<CargoCube> reward;
    /**
     * A constant, unmodifiable list of predefined {@link CardState} values representing
     * the sequential states of the "Smugglers" card's lifecycle during a game.
     * This list defines the following states:
     *
     * 1. {@link CardState#CHOOSE_CANNONS} - Requires the player to select cannons to activate.
     * 2. {@link CardState#ACCEPT_THE_REWARD} - Allows the player to accept or decline a reward.
     * 3. {@link CardState#HANDLE_CUBES_REWARD} - Requires the player to manage cubes obtained as a reward.
     * 4. {@link CardState#HANDLE_CUBES_MALUS} - Requires the player to handle penalties involving cubes.
     *
     * This variable encapsulates the transition logic for these specific states and ensures the
     * states are processed in a predefined order. It is intended to guide the sequence of actions
     * and menus presented to the player for this particular card.
     */
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.HANDLE_CUBES_REWARD, CardState.HANDLE_CUBES_MALUS);

    /**
     * Constructs a new instance of the Smugglers card.
     * This constructor sets the card's name to the simple name of its class.
     */
    public Smugglers() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Sets the malus value for the cube.
     *
     * @param cubeMalus the malus value to set for the cube
     */
    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    /**
     * Sets the reward for the smuggler, represented as a list of CargoCube objects.
     *
     * @param reward the list of CargoCube objects to be set as the smuggler's reward
     */
    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    /**
     * Returns the initial state of the card.
     *
     * @return the first {@code CardState}, which is {@code CHOOSE_CANNONS}.
     */
    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    /**
     * Executes the appropriate action based on the current state of the game
     * and the player's choices.
     *
     * @param playerChoices an instance of PlayerChoicesDataStructure containing
     *        the player's decisions such as chosen cannons, storage locations,
     *        and reward acceptance.
     * @throws UnknownStateException if the current state is not recognized.
     * @throws IllegalArgumentException if required choices within playerChoices
     *         are not provided for specific states.
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
            case HANDLE_CUBES_MALUS:
                this.currPlayerChoseStorageToRemove(playerChoices.getChosenStorage().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case HANDLE_CUBES_REWARD:
                this.handleStorageUpdates(playerChoices.getStorageUpdates().orElseThrow());
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }
    }

    /**
     * Converts the current instance of the Smugglers card into a corresponding ClientSmugglers object.
     *
     * @return an instance of ClientSmugglers, containing the properties of the current Smugglers card,
     * including card name, image name, required firepower, reward, steps back, and cube malus.
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientSmugglers(cardName, imageName, requiredFirePower, reward, stepsBack, cubeMalus );
    }

    /**
     * Processes the current player's selection of cannons and battery boxes to activate,
     * determines the resulting cannon power, and updates the game state accordingly.
     *
     * @param chosenDoubleCannonsCoords a list of coordinates representing the double cannons
     *                                  chosen by the current player to activate.
     * @param chosenBatteryBoxesCoords  a list of coordinates representing the battery boxes
     *                                  chosen by the current player to activate.
     * @throws IllegalArgumentException if any of the provided coordinates are invalid or refer
     *                                  to components that cannot be activated.
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
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (currPlayerCannonPower > requiredFirePower)
            setCurrState(CardState.ACCEPT_THE_REWARD);
        else if(currPlayerCannonPower == requiredFirePower){
            if(gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.CHOOSE_CANNONS);
            }else{
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }else
            setCurrState(CardState.HANDLE_CUBES_MALUS);

    }

    /**
     * Handles the decision of the current player regarding whether they accept the reward.
     * If the player accepts the reward, the game transitions to the state for handling cube rewards.
     * Otherwise, it ends the card phase and resets the player iterator, setting the game state to draw a new card.
     *
     * @param hasPlayerAcceptedTheReward true if the player decided to accept the reward, false otherwise
     */
    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {
        if (hasPlayerAcceptedTheReward)
            setCurrState(CardState.HANDLE_CUBES_REWARD);
        else{
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    /**
     * Handles the transition to the next player's turn or concludes the current card process if no more players remain in the sequence.
     *
     * The method first checks if there is another player available in the queue by invoking {@code gameModel.hasNextPlayer}.
     * If a next player exists, the current player is updated using {@code gameModel.nextPlayer()} and the card state
     * transitions to {@code CardState.VISIT_LOCATION}, enabling the next player to take their turn.
     *
     * If no next player is available, the card process is finalized by setting the card state to {@code CardState.END_OF_CARD}.
     * Additionally, the player iterator is reset using {@code gameModel.resetPlayerIterator()}, and the game state transitions
     * to {@code GameState.CHECK_PlAYERS} to set up the next game round or activity.
     */
    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    /**
     * Processes the removal of the most valuable CargoCube from chosen storages or activates
     * the chosen BatteryBoxes by the current player. It also moves the game to the next
     * player or updates the game to the next state if all players have completed their actions.
     *
     * @param chosenStorageCoords the list of coordinates indicating the storages selected
     *        by the current player from which to remove the most valuable CargoCubes
     * @param chosenBatteryBoxesCoords the list of coordinates indicating the battery boxes
     *        selected by the current player to be activated
     * @throws IllegalArgumentException if any of the provided coordinates are invalid or
     *         do not refer to existing components
     */
    private void currPlayerChoseStorageToRemove(List<Coordinates> chosenStorageCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        Player currentPlayer=gameModel.getCurrPlayer();
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Storage> chosenStorages = new ArrayList();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenStorageCoord : chosenStorageCoords) {
            chosenStorages.add((Storage) currentPlayer.getPersonalBoard().getComponentAt(chosenStorageCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        if(!chosenStorages.isEmpty()) {

            chosenStorages.forEach(storage -> {
                List<CargoCube> sortedStorage = storage.getStockedCubes();
                sortedStorage.sort(CargoCube.byValue);
                CargoCube moreValuableCargoCube = sortedStorage.getLast();
                storage.removeCube(moreValuableCargoCube);
            });

        }

        if(!chosenBatteryBoxes.isEmpty())
            chosenBatteryBoxes.forEach(BatteryBox::useBattery);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
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

    /**
     * Gestisce gli aggiornamenti degli storage tramite la nuova struttura dati.
     * 
     * @param storageUpdates mappa degli aggiornamenti degli storage
     */
    private void handleStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
        try {
            validateStorageUpdates(storageUpdates, gameModel);
            applyStorageUpdates(storageUpdates, gameModel);
            
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });
            
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            
        } catch (IllegalArgumentException e) {
            // Gestione errore con retry
            String currentPlayer = gameModel.getCurrPlayer().getNickname();
            gameModel.getGameClientNotifier().notifyClients(
                Set.of(currentPlayer),
                (nickname, clientController) -> {
                    clientController.notifyStorageError(nickname, e.getMessage());
                }
            );
            
            // Ripristina stato shipboard
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), 
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType(),
                    gameModel.getCurrPlayer().getPersonalBoard().getNotActiveComponents());
            });
            
            // Rimani in HANDLE_CUBES_REWARD per il retry
        }
    }

}
