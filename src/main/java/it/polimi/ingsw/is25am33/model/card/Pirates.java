package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.DoubleCannonActivator;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.game.Game;
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
                ((Shot) game.getCurrDangerousObj()).startAttack(playerChoices, this);
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

    public List<Shot> getShots() {
        return shots;
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

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, game.getCurrPlayer());

        if (currPlayerCannonPower > requiredFirePower) {

            currState = CardState.ACCEPT_THE_REWARD;

        } else {

            if (currPlayerCannonPower < requiredFirePower) defeatedPlayers.add(game.getCurrPlayer());

            if (game.hasNextPlayer()) {
                game.nextPlayer();
            } else {

                if (defeatedPlayers.isEmpty()) {
                    currState = CardState.END_OF_CARD;
                } else {
                    currState = CardState.THROW_DICES;
                }

            }

        }

    }

    private void throwDices() {

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currShot);
        currState = CardState.DANGEROUS_ATTACK;

    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            game.getCurrPlayer().addCredits(reward);
            movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);
        }

        if (defeatedPlayers.isEmpty()) {
            currState = CardState.END_OF_CARD;
        } else {
            currState = CardState.THROW_DICES;
        }

    }

    public void playerDecidedHowToDefendTheirSelvesFromSmallShot(Shield chosenShield, BatteryBox chosenBatteryBox) {

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip(currShot)) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getAvailableBattery() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenShield.getDirections().stream().anyMatch(d -> d == currShot.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                chosenBatteryBox.useBattery();

            } else {
                personalBoard.handleDangerousObject(currShot);
            }

        }

        if(playerIterator.hasNext()) {
            game.nextPlayer();
        } else if (shotIterator.hasNext()) {
            currState = CardState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            currState = CardState.END_OF_CARD;
        }

    }

    public void playerIsAttackedByABigShot() {

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = game.getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip(currShot)) {
            personalBoard.handleDangerousObject(currShot);
        }

        if(playerIterator.hasNext()) {
            game.nextPlayer();
        } else if (shotIterator.hasNext()) {
            currState = CardState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            currState = CardState.END_OF_CARD;
        }

    }

}
