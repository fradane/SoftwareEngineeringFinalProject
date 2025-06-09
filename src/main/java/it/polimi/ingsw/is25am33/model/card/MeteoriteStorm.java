package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientMeteoriteStorm;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class MeteoriteStorm extends AdventureCard {

    private List<Meteorite> meteorites;
    private List<String> meteoriteIDs;
    private Iterator<Meteorite> meteoriteIterator;
    private static final List<CardState> cardStates = List.of(CardState.THROW_DICES, CardState.DANGEROUS_ATTACK, CardState.CHECK_SHIPBOARD_AFTER_ATTACK);

    public MeteoriteStorm(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
    }

    @JsonIgnore
    public List<Meteorite> getMeteorites() {
        return meteorites;
    }

    public List<String> getMeteoriteIDs() {
        return meteoriteIDs;
    }

    public MeteoriteStorm() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

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

    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Meteorite meteorite : meteorites) {
            clientDangerousObjects.add(new ClientDangerousObject(meteorite.getDangerousObjType(),meteorite.getDirection(), -1));
        }
        return new ClientMeteoriteStorm(this.getCardName(),this.imageName,clientDangerousObjects);
    }

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

    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
        this.meteoriteIterator = meteorites.iterator();
    }

    private void throwDices() {

        Meteorite currMeteorite = meteoriteIterator.next();
        //currMeteorite.setCoordinates(GameModel.throwDices());
        currMeteorite.setCoordinates(8);
        gameModel.setCurrDangerousObj(currMeteorite);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }

    public void setMeteoriteID(List<String> meteoriteID) {
        this.meteoriteIDs = meteoriteID;
    }

    public void checkShipBoardAfterAttack(){
        gameModel.notifyInvalidShipBoards();
        if(gameModel.areAllShipsCorrect()) {

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.DANGEROUS_ATTACK);
            }
            else if (meteoriteIterator.hasNext()) {
                gameModel.resetPlayerIterator();
                setCurrState(CardState.THROW_DICES);
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.DRAW_CARD);
            }
        }
    }

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

                gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (chosenShield.getDirections().stream().allMatch(d -> d != currMeteorite.getDirection()))
                    gameModel.updateShipBoardAfterBeenHit();



            } else {
                gameModel.updateShipBoardAfterBeenHit();
            }

        }
        else if(chosenShield != null && chosenBatteryBox != null){
            chosenBatteryBox.useBattery();
            gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
            });
        }
    }

    public void playerDecidedHowToDefendTheirSelvesFromBigMeteorite(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=gameModel.getCurrPlayer();
        Cannon chosenDoubleCannon = null;
        BatteryBox chosenBatteryBox = null;
        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();
        DangerousObj currMeteorite = gameModel.getCurrDangerousObj();

        if(!chosenDoubleCannonsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenDoubleCannon = (Cannon) personalBoard.getComponentAt(chosenDoubleCannonsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currMeteorite) && !personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {

            if (chosenDoubleCannon != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                if (personalBoard.isThereADoubleCannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {
                    chosenBatteryBox.useBattery();


                    gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                        clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
                    });

                } else {
                    gameModel.updateShipBoardAfterBeenHit();
                }

            } else {
                gameModel.updateShipBoardAfterBeenHit();
            }

        }
        else {

            if(chosenDoubleCannon != null && chosenBatteryBox != null) {
                chosenBatteryBox.useBattery();
                gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }
            gameModel.updateShipBoardAfterBeenHit();
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

}


