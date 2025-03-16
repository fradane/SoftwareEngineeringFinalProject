package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.*;

public class Pirates extends AdvancedEnemies implements playerMover {

    private List<Shot> shots;
    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_CANNONS, GameState.ACCEPT_THE_REWARD, GameState.THROW_DICES, GameState.DANGEROUS_ATTACK);
    private final List<Player> defeatedPlayers = new ArrayList<>();
    private Iterator<Shot> shotIterator;
    private Iterator<Player> playerIterator;

    public void setShots(List<Shot> shots) {
        this.shots = shots;
        shotIterator = shots.iterator();
    }

    public void currPlayerChoseCannonsToActivate(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes) throws IllegalArgumentException, IllegalStateException {

        if (currState != GameState.CHOOSE_CANNONS) throw new IllegalStateException("Not the right state");

        if (chosenDoubleCannons.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenDoubleCannons, box) > box.getAvailableBatteries())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(box -> box.useBattery());

        int currPlayerCannonPower = game.getCurrPlayer().getPersonalBoard().computeTotalCannonPower(chosenDoubleCannons);

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

    public void throwDices() {

        if (currState != GameState.THROW_DICES) throw new IllegalStateException("Not the right state");

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currShot);
        currState = GameState.DANGEROUS_ATTACK;
        game.setCurrState(currState);

    }

    public void currPlayerDecidedToGetTheReward(boolean hasPlayerAcceptedTheReward) throws IllegalStateException {

        if (currState != GameState.ACCEPT_THE_REWARD) throw new IllegalStateException("Not the right state");

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

    public void playerDecidedHowToDefendTheirSelvesFromSmallShot(Optional<Shield> chosenShield, Optional<BatteryBox> chosenBatteryBox) {

        if (currState != GameState.DANGEROUS_ATTACK) throw new IllegalStateException("Not the right state");

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip()) {

            if (chosenShield.isPresent() && chosenBatteryBox.isPresent()) {

                Shield selectedShield = chosenShield.get();
                BatteryBox selectedBatteryBox = chosenBatteryBox.get();

                if (selectedBatteryBox.getAvailableBatteries() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenShield.getDirections().stream().anyMatch(d -> d == currShot.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                chosenBatteryBox.useBattery();

            } else {
                personalBoard.handleAttack(currShot);
            }

        }

        if(playerIterator.hasNext()) {
            game.setCurrPlayer = playerIterator.next();
        } else if (shotIterator.hasNext()) {
            currState = GameState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

    public void playerIsAttackedByABigShot() {

        if (currState != GameState.DANGEROUS_ATTACK) throw new IllegalStateException("Not the right state");

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currShot = game.getCurrDangerousObj();

        if (!personalBoard.isItGoingToHitTheShip()) {
            personalBoard.handleAttack(currShot);
        }

        if(playerIterator.hasNext()) {
            game.setCurrPlayer = playerIterator.next();
        } else if (shotIterator.hasNext()) {
            currState = GameState.THROW_DICES;
            playerIterator = defeatedPlayers.iterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

}
