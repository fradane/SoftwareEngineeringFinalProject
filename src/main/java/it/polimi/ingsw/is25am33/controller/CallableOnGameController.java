package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CallableOnGameController extends Remote {

    /**
     * Displays a message to the user.
     *
     * @param s the message to be displayed
     * @throws IOException if an I/O error occurs during the operation
     */
    void showMessage(String s) throws IOException;

    /**
     * Allows a player identified by their nickname to pick a hidden component during the game.
     *
     * @param nickname The nickname of the player who wants to pick a hidden component.
     * @throws IOException If an input or output exception occurs during the operation.
     */
    void playerPicksHiddenComponent(String nickname) throws IOException;

    /**
     * Allows the player to place a focused component on the game board at a specified position
     * with a given rotation. The placement is performed based on the player's nickname, the coordinates
     * of the placement, and the desired rotation angle.
     *
     * @param nickname    the nickname of the player initiating the placement
     * @param coordinates the coordinates where the component should be placed
     * @param rotation    the rotation of the component in degrees
     * @throws IOException if an I/O error occurs during the communication or processing of the request
     */
    void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) throws IOException;

    /**
     * Handles the player's request to reserve the currently focused component during the game.
     * This action allows the player to temporarily mark a specific component for their use.
     *
     * @param nickname the nickname of the player making the request
     * @throws IOException if an input or output error occurs during processing
     */
    void playerWantsToReserveFocusedComponent(String nickname) throws IOException;

    /**
     * Handles the player's action to release focus from the currently focused component in the game.
     *
     * @param nickname the nickname of the player requesting to release the focused component
     * @throws IOException if an I/O error occurs while processing the request
     */
    void playerWantsToReleaseFocusedComponent(String nickname) throws IOException;

    /**
     * Signals that the player identified by their nickname has completed the build ship board phase in the game.
     *
     * @param nickname the unique identifier of the player completing the build ship board phase
     * @throws IOException if an input or output exception occurs
     */
    void playerEndsBuildShipBoardPhase(String nickname) throws IOException;

    /**
     * Allows a player to select a visible component in the game.
     *
     * @param nickname the nickname of the player making the selection
     * @param choice the ID or index of the visible component being selected by the player
     * @throws IOException if an input-output error occurs during the execution of the method
     */
    void playerPicksVisibleComponent(String nickname, Integer choice) throws IOException;

    /**
     * Handles a player's decision regarding visiting a specific location in the game.
     *
     * @param nickname The player's unique identifier or username.
     * @param choice A Boolean value indicating whether the player wants to visit the location (true) or not (false).
     * @throws IOException If an input or output exception occurs during the operation.
     */
    void playerWantsToVisitLocation(String nickname, Boolean choice) throws IOException;

    /**
     * Allows a player to indicate their desire to throw dices during the game.
     *
     * @param nickname the nickname of the player who wants to throw dices
     * @throws IOException if an I/O error occurs while processing the request
     */
    void playerWantsToThrowDices(String nickname) throws IOException;

    /**
     * Handles the player's decision to choose double engines for their ship.
     * The method receives the nickname of the player and the coordinates of the
     * double engines as well as the battery boxes required for their operation.
     *
     * @param nickname the nickname of the player making the choice
     * @param doubleEnginesCoords a list of {@code Coordinates} representing the positions
     *                             of the double engines selected by the player
     * @param batteryBoxesCoords a list of {@code Coordinates} representing the positions
     *                            of the battery boxes chosen by the player to power the double engines
     * @throws IOException if there is an error during the process
     */
    void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws IOException;

    /**
     * Handles the player's choice of double cannons on their ship.
     * This method is triggered when a player selects the placement of double cannon components
     * and their respective battery box components.
     *
     * @param nickname the player's unique identifier or nickname
     * @param doubleCannonsCoords a list of {@link Coordinates} specifying the locations of the double cannon components
     * @param batteryBoxesCoords a list of {@link Coordinates} specifying the locations of the battery box components
     * @throws IOException if any connectivity or communication issue occurs
     */
    void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws IOException;

    /**
     * This method is invoked when a player chooses the cabin components for their ship.
     * It allows the player to specify the locations of the cabin components on the ship board.
     *
     * @param nickname the unique identifier or name of the player who is making the selection
     * @param cabinCoords a list of coordinates representing the positions of the chosen cabin components on the ship board
     * @throws IOException if an I/O error occurs during the operation
     */
    void playerChoseCabins(String nickname, List<Coordinates> cabinCoords) throws IOException;

    /**
     * Handles the player's decision to visit a specific planet during the game.
     *
     * @param nickname the nickname of the player making the decision
     * @param choice an integer representing the player's choice regarding the planet
     * @throws IOException if an input or output exception occurs
     */
    void playerWantsToVisitPlanet(String nickname, int choice) throws IOException;

    /**
     * Handles the player's decision on whether to accept a given reward.
     *
     * @param nickname the nickname of the player making the decision.
     * @param choice the player's decision, where true indicates acceptance and false indicates refusal.
     * @throws IOException if an I/O error occurs during the processing of the player's decision.
     */
    void playerWantsToAcceptTheReward(String nickname, Boolean choice) throws IOException;

    /**
     * Handles the small dangerous objects encountered by a player in the game.
     * The method allows the player to specify shields and battery boxes to handle the event.
     *
     * @param nickname the nickname of the player who is handling the small dangerous objects
     * @param shieldCoords a list of coordinates representing the positions of shields to activate
     * @param batteryBoxCoords a list of coordinates representing the positions of battery boxes to use
     * @throws IOException if an I/O error occurs during the processing of the player's actions
     */
    void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords) throws IOException;

    /**
     * Handles the player's response to a big meteorite event in the game.
     *
     * @param nickname the nickname of the player taking the action
     * @param doubleCannonCoords a list of coordinates indicating the positions
     *                           of double cannons the player intends to use
     * @param batteryBoxCoords a list of coordinates specifying the locations
     *                         of the battery boxes related to the player's action
     * @throws IOException if an I/O error occurs during the operation
     */
    void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords) throws IOException;

    /**
     * Handles the event where the player specified by the given nickname reacts to a "big shot" scenario in the game.
     * This method generally involves managing the player's actions or choices in high-impact game events.
     *
     * @param nickname the unique nickname of the player who is taking action during the "big shot" event
     * @throws IOException if an input or output exception occurs during communication or data handling
     */
    void playerHandleBigShot(String nickname) throws IOException;

    /**
     * Handles the player's action of choosing storage coordinates.
     *
     * @param nickname the nickname of the player making the choice
     * @param storageCoords a list of {@link Coordinates} representing the locations on the shipBoard
     *                      where the player has chosen to place storage components
     * @throws IOException if an I/O error occurs during the process
     */
    void playerChoseStorage(String nickname, List<Coordinates> storageCoords) throws IOException;

    /**
     * Allows the specified player to trigger an epidemic event within the game.
     *
     * @param nickname the nickname of the player initiating the epidemic event
     * @throws IOException if an input or output error occurs during the execution
     */
    void spreadEpidemic(String nickname) throws IOException;

    /**
     * Handles the stardust event action for a specific player identified by their nickname.
     *
     * @param nickname the nickname of the player invoking the stardust event
     * @throws IOException if an I/O error occurs during the execution of the stardust event
     */
    void stardustEvent(String nickname) throws IOException;

    /**
     * Allows the player with the specified nickname to restart the hourglass timer.
     *
     * @param nickname The nickname of the player who wants to restart the hourglass.
     * @throws IOException If an I/O error occurs during the operation.
     */
    void playerWantsToRestartHourglass(String nickname) throws IOException;

    /**
     * Notifies the system that the hourglass timer has ended for a specific player.
     *
     * @param nickname the nickname of the player whose hourglass has ended
     * @throws IOException if an I/O error occurs while processing the notification
     */
    void notifyHourglassEnded(String nickname) throws IOException;

    /**
     * Allows a player to leave the game immediately after the game creation phase.
     *
     * @param nickname the nickname of the player who wishes to leave the game.
     * @throws IOException if an I/O error occurs during the process.
     */
    void leaveGameAfterCreation(String nickname) throws IOException;

    /**
     * Allows a player to remove a specific component from their shipboard at the specified coordinates.
     *
     * @param nickname the nickname of the player requesting the removal of the component
     * @param coordinate the coordinates of the component to be removed
     * @throws IOException if an I/O error occurs during the operation
     */
    void playerWantsToRemoveComponent(String nickname, Coordinates coordinate) throws IOException;

    /**
     * Allows a player to choose parts of the ship during the ship-building phase.
     * The chosen ship parts are provided as a set of coordinates corresponding to their positions on the ship board.
     *
     * @param nickname the nickname of the player making the selection
     * @param shipPart a set of coordinates representing the ship parts selected by the player
     * @throws IOException if an I/O error occurs during the operation
     */
    void playerChoseShipPart(String nickname, Set<Coordinates> shipPart) throws IOException;

    /**
     * Allows a player to focus on a reserved game component.
     *
     * @param nickname the nickname of the player making the request
     * @param choice an integer representing the specific reserved component to focus on
     * @throws IOException if there is a communication issue during the process
     */
    void playerWantsToFocusReservedComponent(String nickname, int choice) throws IOException;

    /**
     * Allows a player to place their pawn in the game. This method sends a request
     * based on the player's specified nickname to perform the action of placing the pawn.
     *
     * @param nickname the unique identifier of the player placing the pawn
     * @throws IOException if an I/O error occurs while processing the request
     */
    void playerPlacesPawn(String nickname) throws IOException;

    /**
     * Handles the client's choice made during the game. This method processes the
     * player's decision based on the provided choice data structure.
     *
     * @param nickname the nickname of the player making the choice
     * @param choice   the data structure representing the player's choice
     * @throws IOException if an I/O error occurs during the processing of the choice
     */
    void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException;

    /**
     * Submits the crew choices made by a player.
     *
     * @param nickname the nickname of the player making the choices
     * @param choices a map containing the coordinates of ship components as keys
     *                and the crew member assigned to those coordinates as values
     * @throws IOException if there is an issue with communication or data transmission
     */
    void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException;

    /**
     * Sends a request to retrieve the list of available prefab ships for the specified player.
     *
     * @param nickname The nickname of the player requesting the prefab ships.
     * @throws IOException If there is an error during the communication or data retrieval process.
     */
    void requestPrefabShips(String nickname) throws IOException;

    /**
     * Requests the selection of a prefab ship for the specified player.
     *
     * @param nickname      The nickname of the player making the request.
     * @param prefabShipId  The unique identifier of the prefab ship to be selected.
     * @throws IOException  If an input or output error occurs during the request.
     */
    void requestSelectPrefabShip(String nickname, String prefabShipId) throws IOException;

    /**
     * Signals that a player, identified by their nickname, intends to land in the game.
     * This method handles the player's request to perform the landing action
     * during the appropriate phase of the game.
     *
     * @param nickname the unique identifier of the player who wants to land
     * @throws IOException if an input or output exception occurs during the execution of the request
     */
    void playerWantsToLand(String nickname) throws IOException;

    /**
     * Initiates the process of checking the ship's board state after an attack has occurred.
     *
     * @param nickname the nickname of the player whose ship's board needs to be checked
     * @throws IOException if there is an error in the communication process
     */
    void startCheckShipBoardAfterAttack(String nickname) throws IOException;

    /**
     * Skips the game to the last card in the current sequence of cards.
     * This method is used for debugging purposes and allows for quickly
     * advancing the game state to the final stage of the card sequence.
     *
     * @throws IOException if an I/O error occurs during the operation
     */
    void debugSkipToLastCard() throws IOException;

    /**
     * Evaluates the crew members for the player specified by the given nickname.
     *
     * @param nickname the nickname of the player whose crew members are to be evaluated
     * @throws IOException if an I/O error occurs during the evaluation process
     */
    void evaluatedCrewMembers(String nickname) throws IOException;

}