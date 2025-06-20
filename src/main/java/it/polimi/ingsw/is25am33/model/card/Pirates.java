package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientPirates;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class Pirates extends AdvancedEnemies implements PlayerMover, DoubleCannonActivator, ShotSenderCard {

    private List<Shot> shots;
    private List<String> shotIDs;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.THROW_DICES, CardState.DANGEROUS_ATTACK, CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
    private final List<Player> defeatedPlayers = new ArrayList<>();
    private Iterator<Shot> shotIterator;
    private Iterator<Player> defeatedPlayerIterator;
    private Player currDefeatedPlayer;

    public List<String> getShotIDs() {
        return shotIDs;
    }

    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    public List<Shot> getShots() {
        return shots;
    }

    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    public Pirates(List<Shot> shots) {
        this.shots = shots;
    }

    public Pirates() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case ACCEPT_THE_REWARD:
                this.currPlayerDecidedToGetTheReward(playerChoices.hasAcceptedTheReward());
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

    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Shot shot : shots) {
            clientDangerousObjects.add(new ClientDangerousObject(shot.getDangerousObjType(),shot.getDirection(), -1));
        }
        return new ClientPirates(this.getCardName(),this.imageName,clientDangerousObjects,this.requiredFirePower,this.reward,this.stepsBack);
    }

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

    public void setMeteoriteID(List<String> meteoriteID) {
        this.shotIDs = shotIDs;
    }

    public void checkShipBoardAfterAttack(){
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
                gameModel.setCurrGameState(GameState.DRAW_CARD);
            }
        }
    }

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
        gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
        });

        if (currPlayerCannonPower > requiredFirePower) {

            setCurrState( CardState.ACCEPT_THE_REWARD);

        } else {
            defeatedPlayers.add(gameModel.getCurrPlayer());

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.CHOOSE_CANNONS);
            } else {
                if (defeatedPlayers.isEmpty()) {
                    setCurrState(CardState.END_OF_CARD);
                    gameModel.resetPlayerIterator();
                    gameModel.setCurrGameState(GameState.DRAW_CARD);
                } else {
                    defeatedPlayerIterator =defeatedPlayers.iterator();
                    currDefeatedPlayer= defeatedPlayerIterator.next();
                    gameModel.setCurrPlayer(currDefeatedPlayer);
                    setCurrState(CardState.THROW_DICES);
                }
            }
        }

    }

    private void throwDices() {

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currShot);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward){
        if (hasPlayerAcceptedTheReward) {
            gameModel.getCurrPlayer().addCredits(reward);
            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        }

        if (defeatedPlayers.isEmpty()) {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        } else {
            defeatedPlayerIterator = defeatedPlayers.iterator();
            currDefeatedPlayer= defeatedPlayerIterator.next();
            gameModel.setCurrPlayer(currDefeatedPlayer);
            setCurrState(CardState.THROW_DICES);
        }
    }

    @Override
    public void playerDecidedHowToDefendTheirSelvesFromSmallShot(List<Coordinates> chosenShieldsCoords, List<Coordinates> chosenBatteryBoxesCoords) {
        Player currentPlayer=gameModel.getCurrPlayer();
        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Shield chosenShield = null;
        BatteryBox chosenBatteryBox = null;
        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if(!chosenShieldsCoords.isEmpty() && !chosenBatteryBoxesCoords.isEmpty()) {
            chosenShield = (Shield) personalBoard.getComponentAt(chosenShieldsCoords.getFirst());
            chosenBatteryBox = (BatteryBox) personalBoard.getComponentAt(chosenBatteryBoxesCoords.getFirst());
        }

        if (personalBoard.isItGoingToHitTheShip(currShot)) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");

                chosenBatteryBox.useBattery();
                gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });

                if (chosenShield.getDirections().stream().noneMatch(d -> d == currShot.getDirection())){
                    gameModel.updateShipBoardAfterBeenHit();
                }else{
                    setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
                }

            } else {
                gameModel.updateShipBoardAfterBeenHit();
            }

        }else{
            if(chosenShield != null && chosenBatteryBox != null) {
                chosenBatteryBox.useBattery();
                gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType());
                });
            }
            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }



    }

    @Override
    public void playerIsAttackedByABigShot() {

        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();
        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip(currShot)) {
            setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
        }
        else{
            gameModel.updateShipBoardAfterBeenHit();
        }

    }

    @Override
    public String toString() {
        // Box sinistro
        String firstString = String.format("""
        %s
        ┌────────────────────────────┐
        │          Pirates           │
        ├────────────────────────────┤
        │ FirePower:         x%-2d     │
        │ Reward:            x%-2d     │
        │ StepsBack:         %-2d      │
        │ Shots:             x%-2d     │
        └────────────────────────────┘
        """,imageName, requiredFirePower, reward, stepsBack, shots != null ? shots.size() : 0);


        StringBuilder secondString = new StringBuilder();
        if (shots != null && !shots.isEmpty()) {
            for (int i = 0; i < shots.size(); i++) {
                Shot shot = shots.get(i);
                String direction = shot.getDirection().name();
                String arrow = directionArrows.get(direction);
                String type;
                if (shotIDs != null && i < shotIDs.size()) {
                    String fullId = shotIDs.get(i);
                    type = fullId.split("_")[0]; // prende solo la parte prima di "_"
                } else {
                    type = shot.getClass().getSimpleName();
                }
                secondString.append(String.format("Shot %d: %s %s \n", i + 1, arrow, type));
            }
        }
        return firstString + secondString;
    }

    // Mappa per frecce direzionali
    private static final Map<String, String> directionArrows = Map.of(
            "NORTH", "↑",
            "SOUTH", "↓",
            "EAST",  "→",
            "WEST",  "←"
    );

}