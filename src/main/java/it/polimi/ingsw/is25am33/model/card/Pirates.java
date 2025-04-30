package it.polimi.ingsw.is25am33.model.card;


import com.fasterxml.jackson.annotation.JsonIgnore;
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
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

public class Pirates extends AdvancedEnemies implements PlayerMover, DoubleCannonActivator {

    private List<Shot> shots;
    private List<String> shotIDs;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_CANNONS, CardState.ACCEPT_THE_REWARD, CardState.THROW_DICES, CardState.DANGEROUS_ATTACK);
    private final List<Player> defeatedPlayers = new ArrayList<>();
    private Iterator<Shot> shotIterator;
    private Iterator<Player> playerIterator;

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
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    public void setShots(List<Shot> shots) {
        this.shots = shots;
        shotIterator = shots.iterator();
    }

    @JsonIgnore
    public List<Shot> getShots() {
        return shots;
    }

    public List<String> getShotIDs() {
        return shotIDs;
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

    }

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());

        if (currPlayerCannonPower > requiredFirePower) {

            setCurrState( CardState.ACCEPT_THE_REWARD);

        } else {

            if (currPlayerCannonPower < requiredFirePower) defeatedPlayers.add(gameModel.getCurrPlayer());

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
            } else {

                if (defeatedPlayers.isEmpty()) {
                    setCurrState( CardState.END_OF_CARD);
                } else {
                    setCurrState(CardState.THROW_DICES );
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
        } else {
            setCurrState(CardState.THROW_DICES);
        }
    }

    public void playerDecidedHowToDefendTheirSelvesFromSmallShot(Shield chosenShield, BatteryBox chosenBatteryBox) {

        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip(currShot)) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getRemainingBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenShield.getDirections().stream().anyMatch(d -> d == currShot.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                chosenBatteryBox.useBattery();

            } else {
                personalBoard.handleDangerousObject(currShot);
            }

        }

        if(playerIterator.hasNext()) {
            gameModel.nextPlayer();
        } else if (shotIterator.hasNext()) {
            setCurrState( CardState.THROW_DICES);
            playerIterator = defeatedPlayers.iterator();
        } else {
            setCurrState( CardState.END_OF_CARD);
        }

    }

    public void playerIsAttackedByABigShot() {

        ShipBoard personalBoard = gameModel.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = gameModel.getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip(currShot)) {
            personalBoard.handleDangerousObject(currShot);
        }

        if(playerIterator.hasNext()) {
            gameModel.nextPlayer();
        } else if (shotIterator.hasNext()) {
            setCurrState( CardState.THROW_DICES);
            playerIterator = defeatedPlayers.iterator();
        } else {
            setCurrState( CardState.END_OF_CARD);
        }

    }

}
