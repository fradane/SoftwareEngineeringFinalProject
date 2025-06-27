package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientWarField;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import javafx.util.Pair;

import java.util.*;

public class WarField extends AdventureCard implements PlayerMover, DoubleCannonActivator, CrewMemberRemover, HowToDefend {

    /**
     * Represents the malus applied to the cubes in the WarField context.
     * Determines the negative impact associated with cube-related actions or elements during gameplay.
     */
    private int cubeMalus;
    /**
     * Represents the number of steps a player or entity needs to move backward
     * during gameplay in the WarField class.
     * This variable is typically used to manage penalties, movements, or specific
     * game scenarios that involve reversing progress.
     */
    private int stepsBack;
    /**
     * Represents the penalty associated with the crew in the context of the game.
     * This value is used to determine the malus applied to the crew under certain conditions.
     */
    private int crewMalus;
    /**
     * Represents the collection of shots available in the WarField.
     * Each shot in the list corresponds to an instance of the Shot class, which is used
     * to model actions or mechanisms related to attacks or defenses in the game.
     *
     * The shots stored in this list can be manipulated or retrieved for gameplay mechanics,
     * such as initiating attacks, evaluating defenses, or resolving game state changes.
     */
    private List<Shot> shots;
    /**
     * A list of strings that represent the unique identifiers for shots in the game.
     * It is used to track, manage, and reference specific shots that are relevant
     * during the gameplay in the WarField context.
     */
    private List<String> shotIDs;
    /**
     * Represents the mapping of {@link CardState} transitions to their subsequent {@link CardState}s.
     * This mapping defines the flow or progression of states within the game behavior.
     * The map is deserialized as a {@link LinkedHashMap} to preserve the order of insertion.
     */
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<CardState, CardState> categories;
    /**
     * Holds the player with the least resources and their corresponding resource value.
     * Represents a pair where the first element is a {@link Player} object
     * and the second is a {@code Double} representing the player's resource level.
     * This is used to determine the most resource-deprived player in the context of the game.
     *
     * Initialized to {@code null}. The value needs to be updated during the game's progression
     * based on player statuses and resource calculations.
     */
    private Pair<Player, Double> leastResourcedPlayer = null;
    /**
     * An iterator over the phases of a WarField card. This iterator allows
     * traversal through the defined states of the card, represented as
     * {@link CardState} enums, in a sequential manner.
     *
     * It is used internally to manage the progression of the card's various phases
     * during the game's execution. The phases represent specific actions or
     * events tied to the WarField card's behavior and logic.
     *
     * The iteration provides a mechanism to move through each state in the order
     * determined by the card's lifecycle and gameplay process.
     */
    private Iterator<CardState> phasesIterator;
    /**
     * An iterator used to traverse or process the collection of {@link Shot} instances
     * contained within the WarField class.
     * This variable allows for sequential access to elements in the Shot collection,
     * enabling operations such as iteration over the shots for specific game mechanics
     * or manipulations.
     */
    private Iterator<Shot> shotIterator;

    /**
     * Default constructor for the WarField class.
     * This initializes the card name of the WarField instance to the simple class name.
     */
    public WarField() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the first state from the collection of phases managed in the current object.
     * The collection is iterated in the order of its keys, and this method returns the first state.
     *
     * @return the first {@code CardState} in the sequence of phases.
     */
    @Override
    @JsonIgnore
    public CardState getFirstState() {
        phasesIterator = categories.keySet().iterator();
        return phasesIterator.next();
    }

    /**
     * Retrieves the game model associated with this instance of the WarField class.
     *
     * @return the GameModel object that represents the current state or configuration of the game.
     */
    @Override
    public GameModel getGameModel() {
        return gameModel;
    }

    /**
     * Retrieves the cube malus associated with the war field.
     *
     * @return the cube malus as an integer value
     */
    public int getCubeMalus() {
        return cubeMalus;
    }

    /**
     * Sets the cube malus value for the WarField.
     *
     * @param cubeMalus the value to set as the cube malus
     */
    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    /**
     * Sets the mapping of card states to their corresponding card states.
     *
     * @param categories a map representing the association between a {@code CardState}
     *                   and another {@code CardState}, defining specific transitions
     *                   or relationships between the states.
     */
    public void setCategories(Map<CardState, CardState> categories) {
        this.categories = categories;
    }

    /**
     * Retrieves the list of shot IDs associated with the WarField.
     *
     * @return a list of strings representing the shot IDs.
     */
    public List<String> getShotIDs() {
        return shotIDs;
    }

    /**
     * Sets the list of shot IDs associated with this WarField instance.
     *
     * @param shotIDs the list of shot IDs to set
     */
    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    /**
     * Retrieves the number of steps the player needs to move back on the game board.
     *
     * @return the number of steps to move back
     */
    public int getStepsBack() {
        return stepsBack;
    }

    /**
     * Retrieves the malus value applied to the crew.
     *
     * @return the crew malus as an integer
     */
    public int getCrewMalus() {
        return crewMalus;
    }

    /**
     * Retrieves the list of shots associated with this WarField.
     *
     * @return a list of Shot objects representing the shots in the WarField
     */
    public List<Shot> getShots() {
        return shots;
    }

    /**
     * Retrieves the map of card states associated with their corresponding categories.
     *
     * @return a map where the key is a CardState representing the current state,
     *         and the value is a CardState representing the associated category state
     */
    public Map<CardState, CardState> getCategories() {
        return categories;
    }

    /**
     * Sets the number of steps to move back in the game.
     *
     * @param stepsBack the number of steps to move back
     */
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    /**
     * Sets the malus value associated with the crew.
     *
     * @param crewMalus the malus value to be assigned to the crew
     */
    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    /**
     * Sets the list of shots associated with the WarField.
     *
     * @param shots the list of Shot objects to be set; each Shot represents an element
     *              used in gameplay, such as attacks or defenses.
     */
    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    /**
     * Executes gameplay logic based on the current state of the card and the choices made by the player.
     *
     * @param playerChoices the structure containing the player's choices such as selected cannons, engines, crew members,
     *                      battery boxes, storage, or cabins, depending on the current game state.
     *                      The provided choices will be used to perform the corresponding actions for the current state.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case CHOOSE_ENGINES:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case EVALUATE_CREW_MEMBERS:
                this.countCrewMembers();

                if (gameModel.hasNextPlayer()) {
                    gameModel.nextPlayer();
                    setCurrState(CardState.EVALUATE_CREW_MEMBERS);
                } else {
                    gameModel.resetPlayerIterator();
                    gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                        clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least members");
                    });
                    handleMalus();
                }

                break;
            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;
            case HANDLE_CUBES_MALUS:
                this.currPlayerChoseStorageToRemove(playerChoices.getChosenStorage().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
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
     * Converts the current WarField instance into its corresponding ClientWarField object.
     * This conversion includes transferring relevant properties such as card name, image name,
     * malus values (crew, steps back, and cubes), and a list of dangerous objects.
     *
     * The dangerous objects are constructed by iterating through the shots in the WarField and
     * creating corresponding ClientDangerousObject instances, which encapsulate the object type,
     * direction, and a default coordinate value of -1.
     *
     * @return a ClientWarField object representing the client-side version of the WarField.
     */
    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Shot shot : shots) {
            clientDangerousObjects.add(new ClientDangerousObject(shot.getDangerousObjType(),shot.getDirection(), -1));
        }
        return new ClientWarField(cardName,imageName, crewMalus, stepsBack, cubeMalus, clientDangerousObjects);
    }

    /**
     * Converts a list of shot IDs into corresponding Shot objects and initializes the shot iterator.
     *
     * This method processes the shot IDs stored in the `shotIDs` field, converts each ID into a Shot
     * object using the `shotCreator.get(id).call()` method, and stores the resulting Shot objects
     * into the `shots` field. If an exception occurs during the conversion of any ID, the method
     * wraps and rethrows the exception as a RuntimeException.
     *
     * After the conversion, the method initializes the `shotIterator` field to iterate over the newly created shots.
     *
     * Throws:
     * - RuntimeException: If an exception occurs while creating Shot objects from IDs.
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
     * Evaluates the state of ship boards after an attack and determines the next state of the game.
     *
     * This method performs the following tasks:
     * 1. Notifies players with invalid ship boards about the issues in their configurations
     *    by calling the {@code gameModel.notifyInvalidShipBoards()} method.
     * 2. Checks whether all ships are correctly positioned using {@code gameModel.areAllShipsCorrect()}.
     * 3. Based on the current game state, it transitions to the next state:
     *    - If there are remaining shots to be processed, the state transitions to {@code CardState.THROW_DICES}.
     *    - If the current phase is complete but there are more phases remaining, the player
     *      iterator is reset and the state transitions to the next phase.
     *    - If all phases and shots have been handled, the state transitions to {@code CardState.END_OF_CARD},
     *      resets the player iterator, and sets the game state to {@code GameState.CHECK_PLAYERS}.
     *
     * This method interacts with the game model and other components to ensure the
     * game progresses appropriately after handling ship board evaluations.
     */
    public void checkShipBoardAfterAttack(){
        gameModel.notifyInvalidShipBoards();
        if(gameModel.areAllShipsCorrect()) {

            if (shotIterator.hasNext()) {
                setCurrState(CardState.THROW_DICES);
            } else if(phasesIterator.hasNext()){
                gameModel.resetPlayerIterator();
                setCurrState(phasesIterator.next());
            }
            else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }
    }

    /**
     * Handles the selection and activation of double cannons and battery boxes for the current player.
     * Validates the chosen components and performs actions to calculate the current player's cannon power.
     * Updates the game state depending on the result of the activation process.
     *
     * @param chosenDoubleCannonsCoords the list of coordinates corresponding to the double cannons chosen by the current player
     * @param chosenBatteryBoxesCoords the list of coordinates corresponding to the battery boxes chosen by the current player
     * @throws IllegalArgumentException if any of the chosen coordinates are invalid or do not map to the correct components
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

        if (leastResourcedPlayer == null || currPlayerCannonPower < leastResourcedPlayer.getValue() ||
                (currPlayerCannonPower == leastResourcedPlayer.getValue() &&
                        gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey())<gameModel.getFlyingBoard().getPlayerPosition(currentPlayer)))
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), (double) currPlayerCannonPower);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        }
        else{
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least fire power");
            });
            handleMalus();
        }
    }

    /**
     * Handles the logic for the current player choosing the engines to activate,
     * ensuring that the number of selected engines matches the number of battery boxes.
     * The method updates the engine power based on the player's choices.
     *
     * @param chosenDoubleEnginesCoords the list of coordinates representing the locations
     *                                  of the double engines on the player's board that the player wants to activate
     * @param chosenBatteryBoxesCoords  the list of coordinates representing the locations
     *                                  of the battery boxes on the player's board that the player wants to use for activation
     * @throws IllegalArgumentException if any of the provided lists are null or if the sizes of the two lists do not match
     */
    private void currPlayerChoseEnginesToActivate(List<Coordinates> chosenDoubleEnginesCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        Player currentPlayer=gameModel.getCurrPlayer();
        if (chosenDoubleEnginesCoords == null || chosenBatteryBoxesCoords == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEnginesCoords.size() != chosenBatteryBoxesCoords.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        List<Engine> chosenDoubleEngines = new ArrayList<>();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenDoubleEnginesCoord : chosenDoubleEnginesCoords) {
            chosenDoubleEngines.add((Engine) currentPlayer.getPersonalBoard().getComponentAt(chosenDoubleEnginesCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });
        int currPlayerEnginePower = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);

        if (leastResourcedPlayer == null || currPlayerEnginePower < leastResourcedPlayer.getValue() ||
                (currPlayerEnginePower == leastResourcedPlayer.getValue() &&
                gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey())<gameModel.getFlyingBoard().getPlayerPosition(currentPlayer)))
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), (double) currPlayerEnginePower);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_ENGINES);
        }
        else{
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least engine power of all");
            });
            handleMalus();
        }

    }

    /**
     * Analyzes the crew members of the current player and determines if they are
     * the player with the least resources. If the current player qualifies as
     * the least resourced based on the number of crew members or their position
     * on the flying board (in case of a tie), updates the corresponding information.
     *
     * Notifies all connected clients about the player with the least resources.
     * The notification includes the nickname of the player identified as having
     * the least crew members.
     *
     * The evaluation considers:
     * - The number of crew members owned by the current player compared to
     *   other players.
     * - The position of the player on the flying board to break ties between
     *   players with the same number of crew members.
     */
    private void countCrewMembers() {

        Player player =  gameModel.getCurrPlayer();

        if (
                leastResourcedPlayer == null ||
                leastResourcedPlayer.getValue() > player.getPersonalBoard().getCrewMembers().size() ||
                (leastResourcedPlayer.getValue() == player.getPersonalBoard().getCrewMembers().size() &&
                        gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey()) < gameModel.getFlyingBoard().getPlayerPosition(player))
        )
            leastResourcedPlayer = new Pair<>(player, (double) player.getPersonalBoard().getCrewMembers().size());

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least crew members");
        });

    }

    /**
     * Handles the logic for applying the malus effect based on the current game state.
     *
     * Depending on the current state of the card and the resource distribution among players,
     * this method will either move the least resourced player backward by a specified number of steps
     * or transition the game into a new state for further actions.
     *
     * Behavior includes:
     * 1. If the current card state is STEPS_BACK:
     *    - Moves the least resourced player back on the board.
     *    - If there are remaining phases in the game progression, updates to the next phase.
     *    - Otherwise, transitions to the END_OF_CARD state and prepares the game to check player statuses.
     * 2. For other card states:
     *    - Sets the least resourced player as the current player.
     *    - Transitions to a new state, either THROW_DICES for dangerous attacks or a state corresponding
     *      to the current card phase.
     *
     * At the end, the least resourced player reference is cleared to indicate the process is complete.
     */
    private void handleMalus() {

        if (categories.get(currState) == CardState.STEPS_BACK) {
            movePlayer(gameModel.getFlyingBoard(), leastResourcedPlayer.getKey(), stepsBack);
            if (phasesIterator.hasNext()) {
                gameModel.resetPlayerIterator();
                setCurrState(phasesIterator.next());
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        } else {
            gameModel.setCurrPlayer(leastResourcedPlayer.getKey());
            if (categories.get(currState) == CardState.DANGEROUS_ATTACK)
                setCurrState(CardState.THROW_DICES);
            else
                setCurrState(categories.get(currState));
        }
        leastResourcedPlayer=null;
    }

    /**
     * Handles the process where the current player selects crew members located in specified cabin coordinates
     * to be removed from their ship during the game. Validates and processes the provided coordinates
     * and updates the state of the game accordingly.
     *
     * @param chosenCabinsCoordinate the list of {@link Coordinates} representing the positions of cabins
     *                                selected by the current player for crew member removal.
     * @throws IllegalArgumentException if the provided coordinates are invalid or do not correspond
     *                                  to valid cabin components on the ship board.
     */
    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException {
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Player currentPlayer=gameModel.getCurrPlayer();
        //no check is performed to ensure they are all cabins because it's already handled on the client side
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        removeMemberProcess(chosenCabins, crewMalus);
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (phasesIterator.hasNext()) {
            gameModel.resetPlayerIterator();
            setCurrState(phasesIterator.next());

        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

    /**
     * Simulates the action of throwing dices to determine the outcome of a scenario.
     * Updates the current shot's coordinates based on the dice roll result.
     * Sets the current dangerous object and updates the game state to indicate
     * a dangerous attack.
     *
     * This method relies on an iterator to obtain the next shot from a predefined
     * sequence of shots. If the iterator has not been initialized, it is set up
     * using the predefined collection of shots.
     *
     * The method interacts with the game model to update game state and dynamic
     * entities such as the dangerous object and shot coordinates.
     */
    private void throwDices() {

        if (shotIterator == null) shotIterator = shots.iterator();

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currShot);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }


    /**
     * Handles the removal of selected storage and battery box components from the current player's ship board.
     * The method processes the provided storage and battery boxes coordinates, removes the most valuable cargo cube
     * from each chosen storage, and activates each chosen battery box. Afterwards, it notifies all clients of the updates
     * and manages the transition to the next game state or phase.
     *
     * @param chosenStorageCoords a list of coordinates indicating the storage components chosen for removal.
     * @param chosenBatteryBoxesCoords a list of coordinates indicating the battery boxes chosen for activation.
     * @throws IllegalArgumentException if invalid coordinates are provided or if an unexpected error occurs during the processing.
     */
    private void currPlayerChoseStorageToRemove(List<Coordinates> chosenStorageCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        // no check is performed to ensure they are all storage because it's already handled on the client side
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

        if (phasesIterator.hasNext()) {
            gameModel.resetPlayerIterator();
            setCurrState(phasesIterator.next());
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

}
