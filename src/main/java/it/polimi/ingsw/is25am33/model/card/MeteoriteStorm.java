package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.*;

public class MeteoriteStorm extends AdventureCard{

    private List<Meteorite> meteorites;
    private Iterator<Meteorite> meteoriteIterator = meteorites.iterator();
    private static final List<GameState> cardStates = List.of(GameState.THROW_DICES, GameState.DANGEROUS_ATTACK);

    public MeteoriteStorm(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
    }

    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
    }

    public void throwDices() {

        if (currState != GameState.THROW_DICES) throw new IllegalStateException("Not the right state");

        Meteorite currMeteorite = meteoriteIterator.next();
        currMeteorite.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currMeteorite);
        currState = GameState.DANGEROUS_ATTACK;
        game.setCurrState(currState);

    }

    public void playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(Optional<Shield> chosenShield, Optional<BatteryBox> chosenBatteryBox) {

        if (currState != GameState.DANGEROUS_ATTACK) throw new IllegalStateException("Not the right state");

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currMeteorite = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip() && personalBoard.isExposed(currMeteorite.getCoordinates(), currMeteorite.getDirection())) {

            if (chosenShield.isPresent() && chosenBatteryBox.isPresent()) {

                Shield selectedShield = chosenShield.get();
                BatteryBox selectedBatteryBox = chosenBatteryBox.get();

                if (selectedBatteryBox.getAvailableBattery() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (selectedShield.getDirections().stream().anyMatch(d -> d == currMeteorite.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                selectedBatteryBox.useBattery();

            } else {
                personalBoard.handleAttack(currMeteorite);
            }

        }

        if(game.hasNextPlayer()) {
            game.nextPlayer();
        } else if (meteoriteIterator.hasNext()) {
            currState = GameState.THROW_DICES;
            game.resetPlayerIterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

    public void playerDecidedHowToDefendTheirSelvesFromBigMeteorite(Optional<DoubleCannon> chosenDoubleCannon, Optional<BatteryBox> chosenBatteryBox) {

        if (currState != GameState.DANGEROUS_ATTACK) throw new IllegalStateException("Not the right state");

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currMeteorite = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip() && !personalBoard.canDefendItselfWithSingleCannons()) {

            if (chosenDoubleCannon.isPresent() && chosenBatteryBox.isPresent()) {

                DoubleCannon selectedDoubleCannon = chosenDoubleCannon.get();
                BatteryBox selectedBatteryBox = chosenBatteryBox.get();

                if (selectedBatteryBox.getAvailableBattery() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (selectedDoubleCannon.getFireDirection() != currMeteorite.getDirection())
                    throw new IllegalArgumentException("Not correct direction");

                if (canDefendWith(selectedDoubleCannon, currMeteorite)) {
                    selectedBatteryBox.useBattery();
                } else {
                    personalBoard.handleAttack(currMeteorite);
                }

            } else {
                personalBoard.handleAttack(currMeteorite);
            }

        }

        if(game.hasNextPlayer()) {
            game.nextPlayer();
        } else if (meteoriteIterator.hasNext()) {
            currState = GameState.THROW_DICES;
            game.resetPlayerIterator();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }


}
