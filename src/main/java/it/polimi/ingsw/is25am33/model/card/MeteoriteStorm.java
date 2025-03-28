package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.*;

public class MeteoriteStorm extends AdventureCard {

    private List<Meteorite> meteorites;
    private final Iterator<Meteorite> meteoriteIterator;
    private static final List<GameState> cardStates = List.of(GameState.THROW_DICES, GameState.DANGEROUS_ATTACK);

    public MeteoriteStorm(List<Meteorite> meteorites, Game game) {
        super(game);
        this.meteorites = meteorites;
        this.meteoriteIterator = meteorites.iterator();
    }

    @Override
    public GameState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case THROW_DICES:
                this.throwDices();
                break;
            case DANGEROUS_ATTACK:
                ((Meteorite) game.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
    }

    private void throwDices() {

        Meteorite currMeteorite = meteoriteIterator.next();
        currMeteorite.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currMeteorite);
        currState = GameState.DANGEROUS_ATTACK;
        game.setCurrState(currState);

    }

    public void playerDecidedHowToDefendTheirSelvesFromSmallMeteorite(Shield chosenShield, BatteryBox chosenBatteryBox) {

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currMeteorite = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip(currMeteorite) &&
                personalBoard.isExposed(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {

            if (chosenShield != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getAvailableBattery() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenShield.getDirections().stream().anyMatch(d -> d == currMeteorite.getDirection()))
                    throw new IllegalArgumentException("Not correct direction");

                chosenBatteryBox.useBattery();

            } else {
                personalBoard.handleDangerousObject(currMeteorite);
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

    public void playerDecidedHowToDefendTheirSelvesFromBigMeteorite(DoubleCannon chosenDoubleCannon, BatteryBox chosenBatteryBox) {

        ShipBoard personalBoard = game.getCurrPlayer().getPersonalBoard();

        DangerousObj currMeteorite = game.getCurrDangerousObj();

        if (personalBoard.isItGoingToHitTheShip(currMeteorite) && !personalBoard.isThereACannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {

            if (chosenDoubleCannon != null && chosenBatteryBox != null) {

                if (chosenBatteryBox.getAvailableBattery() == 0)
                    throw new IllegalStateException("Not enough batteries");
                if (chosenDoubleCannon.getFireDirection() != currMeteorite.getDirection())
                    throw new IllegalArgumentException("Not correct direction");

                if (personalBoard.isThereADoubleCannon(currMeteorite.getCoordinate(), currMeteorite.getDirection())) {
                    chosenBatteryBox.useBattery();
                } else {
                    personalBoard.handleDangerousObject(currMeteorite);
                }

            } else {
                personalBoard.handleDangerousObject(currMeteorite);
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
