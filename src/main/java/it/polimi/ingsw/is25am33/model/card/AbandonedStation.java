package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientAbandonedStation;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CubesRedistributionHandler;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbandonedStation extends AdventureCard implements PlayerMover, CubesRedistributionHandler {

    /**
     * Represents the number of steps a player must move back when interacting with the "AbandonedStation" card.
     * This variable determines specific movement penalties or effects applied during gameplay.
     */
    private int stepsBack;
    /**
     * Represents the number of crew members required to interact with the abandoned station card.
     * This variable determines the minimum crew size needed to proceed with actions or effects
     * associated with this card.
     */
    private int requiredCrewMembers;
    /**
     * Represents the list of CargoCube rewards that can be obtained
     * when encountering and resolving this specific AbandonedStation card.
     * The reward may vary and typically depends on the game's state or player actions.
     */
    private List<CargoCube> reward;
    /**
     * Represents a predefined sequence of states specific to an abandoned station card.
     * These states determine the flow of operations and interactions available during the gameplay.
     * The list is immutable and includes the following states:
     * - VISIT_LOCATION: Represents the action or phase where a player can choose to visit the abandoned station.
     * - HANDLE_CUBES_REWARD: Represents the phase where a player interacts with reward-related cargo cubes.
     */
    private static final List<CardState> cardStates = List.of(CardState.VISIT_LOCATION, CardState.HANDLE_CUBES_REWARD);

    /**
     * Retrieves the first state from the list of card states.
     *
     * @return the first CardState in the cardStates list
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Executes the logic of the gameplay interaction based on the current state
     * and the player's choices provided in the input.
     *
     * @param playerChoices the choices made by the player, encapsulated in a
     *                      PlayerChoicesDataStructure object. Provides information
     *                      about the actions the player intends to take, such as
     *                      whether to visit a location or where to store reward cubes.
     * @throws UnknownStateException if the current state of the game does not match
     *                               any known or expected states, preventing proper
     *                               execution of the method.
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

            case HANDLE_CUBES_REWARD:
                if (playerChoices.getStorageUpdates().isPresent()) {
                    this.handleStorageUpdates(playerChoices.getStorageUpdates().orElseThrow());
                } else {
                    this.currPlayerChoseCargoCubeStorage(playerChoices.getChosenStorage().orElseThrow());
                }
                break;

            default:
                throw new UnknownStateException("Unknown current state");

        }

    }

    /**
     * Converts this AbandonedStation instance into its client-facing representation as a
     * ClientAbandonedStation object. The returned ClientAbandonedStation will contain all
     * relevant information, including card name, image name, required crew members, reward,
     * and steps back.
     *
     * @return a ClientAbandonedStation object containing the client-side representation of this card.
     */
    @Override
    public ClientCard toClientCard() {
        return new ClientAbandonedStation(cardName, imageName, requiredCrewMembers, reward, stepsBack);
    }

    /**
     * Sets the level of the AbandonedStation.
     *
     * @param level the level to be set
     */
    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Sets the number of crew members required for the abandoned station.
     *
     * @param requiredCrewMembers the number of crew members needed
     *                            to interact with the abandoned station
     */
    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    /**
     * Sets the reward for the abandoned station.
     *
     * @param reward the list of CargoCube objects representing the reward.
     */
    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    /**
     * Sets the number of steps to move back on the board.
     *
     * @param stepsBack the number of steps to move back
     */
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    /**
     * Retrieves the number of steps back assigned to this AbandonedStation instance.
     *
     * @return the number of steps back as an integer
     */
    public int getStepsBack() {
        return stepsBack;
    }

    /**
     * Retrieves the number of crew members required for this AbandonedStation.
     *
     * @return the required number of crew members.
     */
    public int getRequiredCrewMembers() {
        return requiredCrewMembers;
    }

    /**
     * Retrieves the list of cargo cubes that constitute the reward.
     *
     * @return a List of CargoCube objects representing the reward.
     */
    public List<CargoCube> getReward() {
        return reward;
    }

    /**
     * Default constructor for the AbandonedStation class.
     * Initializes the card name to the class's simple name.
     */
    public AbandonedStation() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Handles the current player's decision whether to visit a specific location.
     * Depending on the player's decision and game state, this method validates
     * the choice, updates the game state, and determines the next actions.
     *
     * @param wantsToVisit a boolean indicating the current player's decision:
     *                     true if the player wants to visit the location, false otherwise
     * @throws IllegalDecisionException if the*/
    private void currPlayerWantsToVisit(boolean wantsToVisit) throws IllegalDecisionException {
        try {
            if (wantsToVisit) {
                if (gameModel.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < requiredCrewMembers)
                    throw new IllegalDecisionException("Player has not enough crew members");
                setCurrState(CardState.HANDLE_CUBES_REWARD);
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
     * Handles the selection of storage locations by the current player for storing cargo cubes.
     * The chosen storage locations are validated and updated with the assigned rewards if possible.
     *
     * @param chosenStorageCoords a list of coordinates representing the storages chosen by the player.
     *                            Each coordinate corresponds to a potential storage location on the player's ship board.
     *                            Invalid coordinates or non-storage components will result in null being added to the internal list of storages.
     */
    private void currPlayerChoseCargoCubeStorage(List<Coordinates> chosenStorageCoords) {
        List<CargoCube> stationRewards = new ArrayList<>(reward);

        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Storage> chosenStorages = new ArrayList();
        for (Coordinates coords : chosenStorageCoords) {
            if (coords.isCoordinateInvalid()) {
                chosenStorages.add(null);
            } else {
                Component component = shipBoard.getComponentAt(coords);
                if (component instanceof Storage) {
                    chosenStorages.add((Storage) component);
                } else {
                    chosenStorages.add(null);
                }
            }
        }

        if (chosenStorages.isEmpty()) {
            proceedToNextPlayerOrEndCard();
            return;
        }

        if (chosenStorages.size() < stationRewards.size()) {
            List<CargoCube> rewardsToProcess = stationRewards.subList(0, chosenStorages.size());
            List<CargoCube> discardedRewards = stationRewards.subList(chosenStorages.size(), stationRewards.size());
            stationRewards = rewardsToProcess;
        }

        if (chosenStorages.size() > stationRewards.size()) {
            chosenStorages = chosenStorages.subList(0, stationRewards.size());
        }

        for (int i = 0; i < Math.min(chosenStorages.size(), stationRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = stationRewards.get(i);

            if (storage == null) {
                continue;
            }

            if (!(storage instanceof SpecialStorage) && cube == CargoCube.RED) {
                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
            }
        }

        for (int i = 0; i < Math.min(chosenStorages.size(), stationRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = stationRewards.get(i);

            if (storage == null) {
                continue;
            }

            if (storage.isFull()) {
                List<CargoCube> sortedStorage = new ArrayList<>(storage.getStockedCubes());
                sortedStorage.sort(CargoCube.byValue);
                CargoCube lessValuableCargoCube = sortedStorage.get(0);
                storage.removeCube(lessValuableCargoCube);
            }

            storage.addCube(cube);
        }

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(), gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
        });

        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        setCurrState(CardState.END_OF_CARD);
        gameModel.resetPlayerIterator();
        gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
    }

    /**
     * Determines the next step in the game's progression based on the availability of additional players.
     * This method either moves the game to the next player or concludes the current card's execution
     * if there are no more players remaining in the sequence.
     *
     * If there is another player available, it advances the iterator to the next player,
     * sets the card state to {@code VISIT_LOCATION}, and continues processing for the current card.
     *
     * If no additional players are available:
     * - The card state is updated to {@code END_OF_CARD}, marking the conclusion
     *   of the card's activities.
     * - The player iterator is reset to*/
    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.VISIT_LOCATION);
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
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
            });
            
            // Muovi il giocatore indietro
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);
            
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyRankingUpdate(nicknameToNotify, gameModel.getCurrPlayer().getNickname(), 
                    gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
            });
            
            proceedToNextPlayerOrEndCard();
            
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
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
            });
            
            // Rimani in HANDLE_CUBES_REWARD per il retry
        }
    }

}
