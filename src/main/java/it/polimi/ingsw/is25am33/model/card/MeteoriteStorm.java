package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientMeteoriteStorm;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.interfaces.HowToDefend;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class MeteoriteStorm extends AdventureCard implements HowToDefend {

    /**
     * Represents the list of Meteorite objects involved in the meteorite storm.
     * This collection stores all the meteorites that are part of the current game
     * and may be used to manage their interactions, states, or configurations
     * during gameplay.
     *
     * The meteorites in this list are instances of the {@link Meteorite} class,
     * which provides specific behaviors and attributes for each meteorite.
     *
     * This field is primarily used to handle meteorite-related functionalities
     * within the {@code MeteoriteStorm} class, including initializing their states,
     * retrieving their details, and processing their behaviors during a storm event.
     */
    private List<Meteorite> meteorites;
    /**
     * Represents a list of unique identifiers associated with meteorites encountered or processed during the game.
     * This field is used to store the IDs of meteorites for identification and mapping purposes.
     * It allows operations such as retrieving meteorites by their IDs and managing their relationships within
     * the meteorite storm scenario.
     */
    private List<String> meteoriteIDs;
    /**
     * An iterator for traversing through the collection of Meteorite objects associated with the enclosing class.
     * The meteoriteIterator allows sequential access to each Meteorite in the collection, enabling operations
     * such as iteration, filtering, or custom processing on the set of meteorites.
     *
     * This iterator is especially useful when the sequence or order of processing the meteorites is essential
     * or when actions need to be carried out on each meteorite in a controlled manner.
     *
     * Its exact behavior and traversal logic depend on the implementation of the underlying collection.
     */
    private Iterator<Meteorite> meteoriteIterator;
    /**
     * A static, immutable list of predefined {@code CardState} elements that represent
     * specific states associated with the progression of the MeteoriteStorm adventure card.
     * The list is composed of the states {@code THROW_DICES}, {@code DANGEROUS_ATTACK},
     * and {@code CHECK_SHIPBOARD_AFTER_ATTACK}. These states define key phases within
     * the card's gameplay flow and dictate the actions or decisions to be performed by
     * the player during the relevant stages of the game.
     */
    private static final List<CardState> cardStates = List.of(CardState.THROW_DICES, CardState.DANGEROUS_ATTACK, CardState.CHECK_SHIPBOARD_AFTER_ATTACK);

    /**
     * Retrieves the list of meteorites associated with the MeteoriteStorm.
     *
     * @return a list of {@link Meteorite} objects that represent the meteorites
     *         involved in the storm.
     */
    @JsonIgnore
    public List<Meteorite> getMeteorites() {
        return meteorites;
    }

    /**
     * Retrieves the list of meteorite IDs associated with the meteorite storm.
     *
     * @return a list of strings representing the IDs of the meteorites.
     */
    public List<String> getMeteoriteIDs() {
        return meteoriteIDs;
    }

    /**
     * Default constructor for the MeteoriteStorm class.
     *
     * This constructor initializes a MeteoriteStorm instance and sets its cardName
     * to the class's simple name.
     */
    public MeteoriteStorm() {
        this.cardName = this.getClass().getSimpleName();
    }

    /**
     * Retrieves the first CardState from the list of card states.
     *
     * @return the first CardState in the list.
     */
    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    /**
     * Retrieves the current game model associated with this instance.
     *
     * @return the {@link GameModel} object that represents the current state of the game.
     */
    @Override
    public GameModel getGameModel() {
        return gameModel;
    }

    /**
     * Executes the play action based on the current state of the game. This method
     * handles different states and performs the corresponding actions such as
     * throwing dices, starting a meteorite attack, or checking the shipboard
     * after an attack.
     *
     * @param playerChoices the player's decisions or choices required for specific
     *                      actions during the play, such as defending against a
     *                      meteorite attack.
     * @throws UnknownStateException if the current state of the game is unrecognized or invalid.
     */
    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case THROW_DICES:
                this.throwDices();
                break;
            case DANGEROUS_ATTACK:
                ((Meteorite) gameModel.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                this.checkShipBoardAfterAttack();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    /**
     * Converts the current MeteoriteStorm instance into a ClientCard representation.
     * This method maps the server-side meteorite details to client-side objects.
     *
     * @return a ClientMeteoriteStorm object containing the name, image, and a list of
     *         ClientDangerousObjects corresponding to the meteorites in this MeteoriteStorm.
     */
    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Meteorite meteorite : meteorites) {
            clientDangerousObjects.add(new ClientDangerousObject(meteorite.getDangerousObjType(),meteorite.getDirection(), -1));
        }
        return new ClientMeteoriteStorm(this.getCardName(),this.imageName,clientDangerousObjects);
    }

    /**
     * Transforms a list of meteorite IDs into a list of meteorite objects.
     * Each ID from the meteoriteIDs field is processed through a callable
     * object retrieved from meteoriteCreator, which generates the corresponding
     * meteorite object. The resulting list of meteorites is then set to the
     * meteorites field. Additionally, an iterator for the newly created list
     * of meteorites is assigned to the meteoriteIterator field.
     *
     * The method handles any exceptions that occur during the execution of
     * the callable by wrapping and rethrowing them as RuntimeExceptions.
     *
     * This method is intended to convert and initialize meteorites from
     * their respective string-based identifiers.
     */
    public void convertIdsToMeteorites() {
        meteorites = meteoriteIDs.stream()
                .map(id -> {
                    try {
                        return meteoriteCreator.get(id).call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        this.meteoriteIterator = meteorites.iterator();
    }

    /**
     * Simulates the action of throwing dices to determine the next coordinates
     * for the current meteorite and updates the game state to reflect the impact
     * of the meteorite storm.
     *
     * The method retrieves the next meteorite from the iterator, calculates its
     * new coordinates using the dice-throwing mechanics, and sets these
     * coordinates for the meteorite. It then updates the game model to designate
     * the current meteorite as the active dangerous object and transitions the
     * game's state to indicate a dangerous attack scenario.
     *
     * This method is used internally within the game flow to handle the behavior
     * associated with meteorite movement and its impact during gameplay.
     *
     * Preconditions:
     * - The meteorite iterator must not be exhausted.
     * - The game model should be initialized and capable of handling state updates
     *   and dangerous objects.
     *
     * Postconditions:
     * - The coordinates of the current meteorite are updated.
     * - The game model's state is transitioned to the dangerous attack state.
     * - The game model is updated with the current meteorite marked as the
     *   dangerous object.
     */
    private void throwDices() {
        Meteorite currMeteorite = meteoriteIterator.next();
        currMeteorite.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currMeteorite);
        setCurrState(CardState.DANGEROUS_ATTACK);
    }

    /**
     * Sets the list of meteorite IDs for the current meteorite storm.
     *
     * @param meteoriteID the list of meteorite IDs to be assigned to this meteorite storm
     */
    public void setMeteoriteID(List<String> meteoriteID) {
        this.meteoriteIDs = meteoriteID;
    }

    /**
     * Sets the list of meteorites for the current instance and initializes the meteorite iterator.
     *
     * @param meteorites the list of meteorites to be assigned. Cannot be null and should contain valid Meteorite objects.
     */
    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
        this.meteoriteIterator = meteorites.iterator();
    }

    /**
     * Evaluates the state of the game following an attack and updates the game flow accordingly.
     *
     * This method performs the following operations in sequence:
     * 1. Notifies players with invalid ship boards about the issues in their configurations.
     * 2. Checks if all player's ship boards are correctly positioned. If not, no further actions are taken.
     * 3. If all ships are correctly positioned, proceeds to determine the next game state:
     *    - If there's a next player, advances to the next player's turn and updates the card state to DANGEROUS_ATTACK.
     *    - If there are no more players with remaining turns but the meteorite iterator has subsequent entries,
     *      resets the player iterator and changes the card state to THROW_DICES.
     *    - If no players or meteorites are remaining, transitions the game to the END_OF_CARD state, resets the
     *      player iterator, and sets the game state to CHECK_PLAYERS.
     */
    private void checkShipBoardAfterAttack(){
        gameModel.notifyInvalidShipBoards();
        if (gameModel.areAllShipsCorrect()) {

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.DANGEROUS_ATTACK);
            } else if (meteoriteIterator.hasNext()) {
                gameModel.resetPlayerIterator();
                setCurrState(CardState.THROW_DICES);
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }
    }

    /*
    public void playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(List<Coordinates> chosenShieldsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=gameModel.getCurrPlayer();
        Shield chosenShield = null;
        BatteryBox chosenBatteryBox = null;
        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();
        DangerousObj currMeteorite = gameModel.getCurrDangerousObj();

        if(!chosenShieldsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenShield = (Shield) personalBoard.getComponentAt(chosenShieldsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currMeteorite) && personalBoard.isExposed(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();

                gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (chosenShield.getDirections().stream().noneMatch(d -> d == currMeteorite.getDirection()))
                    gameModel.updateShipBoardAfterBeenHit();
                else {
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }

            } else {
                gameModel.updateShipBoardAfterBeenHit();
            }

        } else {

            if (chosenShield != null && chosenBatteryBox != null){
                chosenBatteryBox.useBattery();
                gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }

            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }
    }

    public void playerDecidedHowToDefendTheirSelvesFromBigMeteorite(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=gameModel.getCurrPlayer();
        Cannon chosenDoubleCannon = null;
        BatteryBox chosenBatteryBox = null;
        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();
        DangerousObj currMeteorite = gameModel.getCurrDangerousObj();

        if (!chosenDoubleCannonsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenDoubleCannon = (Cannon) personalBoard.getComponentAt(chosenDoubleCannonsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currMeteorite)) {

            if (chosenDoubleCannon != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();

                gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (!personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection()) && !personalBoard.isThereADoubleCannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {
                    gameModel.updateShipBoardAfterBeenHit();
                }
                else if(doubleCannonDestroyMeteorite(chosenDoubleCannonsCoords.getFirst(),chosenDoubleCannon) || personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())){
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }
                else
                    gameModel.updateShipBoardAfterBeenHit();

            } else {
                gameModel.updateShipBoardAfterBeenHit();
            }

        } else{

            if(chosenDoubleCannon != null && chosenBatteryBox != null) {
                chosenBatteryBox.useBattery();
                gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }

            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }

    }

    public String toString() {
        // Box sinistro con nome e numero meteoriti
        String firstString = String.format(""" 
       %s
       ┌────────────────────────────┐
       │       MeteoriteStorm       │
       ├────────────────────────────┤
       │ Meteorites:        x%-2d     │
       └────────────────────────────┘
       """,imageName, meteorites != null ? meteorites.size() : 0);

        StringBuilder secondString = new StringBuilder();
        if (meteorites != null && !meteorites.isEmpty()) {
            for (int i = 0; i < meteorites.size(); i++) {
                Meteorite meteorite = meteorites.get(i);
                String direction = meteorite.getDirection().name();
                String arrow = directionArrows.get(direction);
                String type;
                if (meteoriteIDs != null && i < meteoriteIDs.size()) {
                    String fullId = meteoriteIDs.get(i);
                    type = fullId.split("_")[0]; // prende solo la parte prima di "_"
                } else {
                    type = meteoriteIDs.getClass().getSimpleName();
                }
                secondString.append(String.format("Shot %d: %s %s \n", i + 1, arrow, type));
            }
        }
        return firstString + secondString;
    }

    // Mappa delle direzioni → frecce
    private static final Map<String, String> directionArrows = Map.of(
            "NORTH", "↑",
            "SOUTH", "↓",
            "EAST",  "→",
            "WEST",  "←"
    );

    private boolean doubleCannonDestroyMeteorite(Coordinates doubleCannonCoordinates, Cannon doubleCannon) {
        if(doubleCannon.getFireDirection().equals(Direction.NORTH) || doubleCannon.getFireDirection().equals(Direction.SOUTH)) {
            return doubleCannonCoordinates.getY() == gameModel.getCurrDangerousObj().getCoordinate();
        }
        else{
            return doubleCannonCoordinates.getX() == gameModel.getCurrDangerousObj().getCoordinate();
        }
    } */

}

