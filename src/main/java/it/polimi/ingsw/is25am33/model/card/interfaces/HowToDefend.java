package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.List;

public interface HowToDefend {

    /**
     * Retrieves the current instance of the game model.
     *
     * @return the current GameModel object representing the state of the game
     */
    GameModel getGameModel();

    /**
     * Sets the current state of the card in the game.
     * This method updates the state of the card to guide the game's flow and manage actions
     * or events associated with the specified state.
     *
     * @param currState the desired state of the card represented by an instance of CardState
     */
    void setCurrState(CardState currState);

    /**
     * Handles the player’s decision on how to defend themselves from a small meteorite.
     * This method processes the chosen shield and battery box coordinates to decide and apply
     * defense mechanisms. If valid components are chosen, it updates the game state accordingly,
     * such as reducing batteries or updating the ship's board if hit by the meteorite.
     *
     * @param chosenShieldsCoords a list of coordinates representing the chosen shield locations on the player’s ship board.
     * @param chosenBatteryBoxesCoords a list of coordinates representing the chosen battery box locations on the player’s ship board.
     * @throws IllegalStateException if no batteries are available in the chosen battery box.
     */
    default void playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(List<Coordinates> chosenShieldsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=getGameModel().getCurrPlayer();
        Shield chosenShield = null;
        BatteryBox chosenBatteryBox = null;
        ShipBoard personalBoard = getGameModel().getCurrPlayer().getPersonalBoard();
        DangerousObj currMeteorite = getGameModel().getCurrDangerousObj();

        if(!chosenShieldsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenShield = (Shield) personalBoard.getComponentAt(chosenShieldsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currMeteorite) && personalBoard.isExposed(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();

                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (chosenShield.getDirections().stream().noneMatch(d -> d == currMeteorite.getDirection()))
                    getGameModel().updateShipBoardAfterBeenHit();
                else {
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }

            } else {
                getGameModel().updateShipBoardAfterBeenHit();
            }

        }
        else{

            if(chosenShield != null && chosenBatteryBox != null){
                chosenBatteryBox.useBattery();
                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }

            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }
    }

    /**
     * Handles the decision process for the player to defend against a big meteorite.
     * The method evaluates the situation of the player's ship board and
     * determines if the chosen double cannons and battery boxes can prevent damage caused by the meteorite.
     * It updates the game state and notifies all connected clients of any relevant changes.
     *
     * @param chosenDoubleCannonsCoords the list of coordinates representing the chosen double cannons on the player's ship board
     * @param chosenBatteryBoxesCoords the list of coordinates representing the chosen battery boxes on the player's ship board
     */
    default void playerDecidedHowToDefendTheirSelvesFromBigMeteorite(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=getGameModel().getCurrPlayer();
        Cannon chosenDoubleCannon = null;
        BatteryBox chosenBatteryBox = null;
        ShipBoard personalBoard = getGameModel().getCurrPlayer().getPersonalBoard();
        DangerousObj currMeteorite = getGameModel().getCurrDangerousObj();

        if(!chosenDoubleCannonsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenDoubleCannon = (Cannon) personalBoard.getComponentAt(chosenDoubleCannonsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currMeteorite)) {

            if (chosenDoubleCannon != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();

                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (!personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection()) && !personalBoard.isThereADoubleCannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {
                    getGameModel().updateShipBoardAfterBeenHit();
                }
                else if(doubleCannonDestroyMeteorite(chosenDoubleCannonsCoords.getFirst(),chosenDoubleCannon) || personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())){
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }
                else
                    getGameModel().updateShipBoardAfterBeenHit();

            } else {
                getGameModel().updateShipBoardAfterBeenHit();
            }

        } else{

            if(chosenDoubleCannon != null && chosenBatteryBox != null) {
                chosenBatteryBox.useBattery();
                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }

            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }

    }

    /**
     * Determines if a double cannon successfully destroys a meteorite
     * based on the cannon's firing direction and its coordinates.
     *
     * @param doubleCannonCoordinates the coordinates of the double cannon
     * @param doubleCannon the double cannon being evaluated
     * @return true if the double cannon's firing direction aligns with the meteorite's
     * position, false otherwise
     */
    private boolean doubleCannonDestroyMeteorite(Coordinates doubleCannonCoordinates, Cannon doubleCannon) {
        if(doubleCannon.getFireDirection().equals(Direction.NORTH) || doubleCannon.getFireDirection().equals(Direction.SOUTH)) {
            return doubleCannonCoordinates.getY() == getGameModel().getCurrDangerousObj().getCoordinate();
        }
        else{
            return doubleCannonCoordinates.getX() == getGameModel().getCurrDangerousObj().getCoordinate();
        }
    }

    /**
     * Handles the player's decision on how to defend themselves from a small shot.
     * This method processes the chosen shield and battery box coordinates to determine
     * and apply the appropriate defensive actions. It evaluates the components on the
     * player's ship board and decides whether the shot is mitigated or if the ship
     * board takes damage. Additionally, the game state and client notifications are
     * updated accordingly.
     *
     * @param chosenShieldsCoords a list of coordinates representing the chosen shield locations on the player's ship board
     * @param chosenBatteryBoxesCoords a list of coordinates representing the chosen battery box locations on the player's ship board
     * @throws IllegalStateException if the selected battery box does not have enough batteries available
     */
    default void playerDecidedHowToDefendTheirSelvesFromSmallShot(List<Coordinates> chosenShieldsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=getGameModel().getCurrPlayer();
        ShipBoard personalBoard = getGameModel().getCurrPlayer().getPersonalBoard();
        Shield chosenShield = null;
        BatteryBox chosenBatteryBox = null;
        DangerousObj currShot = getGameModel().getCurrDangerousObj();

        if(!chosenShieldsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenShield = (Shield) personalBoard.getComponentAt(chosenShieldsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currShot)) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();
                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (chosenShield.getDirections().stream().noneMatch(d -> d == currShot.getDirection())){
                    getGameModel().updateShipBoardAfterBeenHit();
                }else{
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }

            } else {
                getGameModel().updateShipBoardAfterBeenHit();
            }

        }else{
            if(chosenShield != null && chosenBatteryBox != null) {
                chosenBatteryBox.useBattery();
                getGameModel().getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }
            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }

    }

    /**
     * Handles the scenario where the current player is attacked by a big shot in the game.
     *
     * This method checks if the dangerous object, referred to as a "big shot," will hit the player's ship board.
     * If the shot is not going to hit the ship, it transitions the game state to check the shipboard
     * for any changes after the attack. If the shot does hit the ship, the shipboard state is updated
     * to reflect the damage caused by the big shot.
     *
     * The method relies on the game model to evaluate the player's shipboard and to update the game state
     * accordingly based on the outcome of the attack.
     */
    default void playerIsAttackedByABigShot() {

        ShipBoard personalBoard = getGameModel().getCurrPlayer().getPersonalBoard();
        DangerousObj currShot = getGameModel().getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip(currShot)) {
            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }
        else{
            getGameModel().updateShipBoardAfterBeenHit();
        }

    }
}
