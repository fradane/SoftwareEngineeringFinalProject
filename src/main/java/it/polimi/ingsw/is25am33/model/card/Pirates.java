package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.model.GameState;
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
    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_CANNONS, GameState.ACCEPT_THE_REWARD, GameState.THROW_DICES, GameState.DANGEROUS_ATTACK);
    private final List<Player> defeatedPlayers = new ArrayList<>();
    private Iterator<Shot> shotIterator;
    private Iterator<Player> playerIterator;

    public Pirates(List<Shot> shots, Game game) {
        super(game);
        this.shots = shots;
    }

    @Override
    public GameState getFirstState() {
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

    public void setShots(List<Shot> shots) {
        this.shots = shots;
        shotIterator = shots.iterator();
    }

    private void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException {

        int currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, game.getCurrPlayer());

        if (currPlayerCannonPower > requiredFirePower) {

            currState = GameState.ACCEPT_THE_REWARD;
            game.setCurrState(currState);

        } else {

            if (currPlayerCannonPower < requiredFirePower) defeatedPlayers.add(game.getCurrPlayer());

            if (game.hasNextPlayer()) {
                game.nextPlayer();
            } else {

                if (defeatedPlayers.isEmpty()) {
                    game.setCurrState(GameState.END_OF_CARD);
                } else {
                    currState = GameState.THROW_DICES;
                    game.setCurrState(currState);
                }

            }

        }

    }

    private void throwDices() {

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currShot);
        currState = GameState.DANGEROUS_ATTACK;
        game.setCurrState(currState);

    }

    private void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) {

        if (hasPlayerAcceptedTheReward) {
            game.getCurrPlayer().addCredits(reward);
            movePlayer(game.getFlyingBoard(), game.getCurrPlayer(), stepsBack);
        }

        if (defeatedPlayers.isEmpty()) {
            game.setCurrState(GameState.END_OF_CARD);
        } else {
            currState = GameState.THROW_DICES;
            game.setCurrState(currState);
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
            currState = GameState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
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
            currState = GameState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

}
