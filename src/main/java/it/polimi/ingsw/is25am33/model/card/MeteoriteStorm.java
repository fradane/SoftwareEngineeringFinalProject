package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.CardState;
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
    private List<String> meteoriteIDs;
    private Iterator<Meteorite> meteoriteIterator;
    private static final List<CardState> cardStates = List.of(CardState.THROW_DICES, CardState.DANGEROUS_ATTACK);

    public MeteoriteStorm(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
        this.meteoriteIterator = meteorites.iterator();
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
                ((Meteorite) game.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

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

    }

    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
        this.meteoriteIterator = meteorites.iterator();
    }

    private void throwDices() {

        Meteorite currMeteorite = meteoriteIterator.next();
        currMeteorite.setCoordinates(Game.throwDices());
        game.setCurrDangerousObj(currMeteorite);
        currState = CardState.DANGEROUS_ATTACK;

    }

    public void setMeteoriteID(List<String> meteoriteID) {
        this.meteoriteIDs = meteoriteID;
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
            currState = CardState.THROW_DICES;
            game.resetPlayerIterator();
        } else {
            currState = CardState.END_OF_CARD;
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
            currState = CardState.THROW_DICES;
            game.resetPlayerIterator();
        } else {
            currState = CardState.END_OF_CARD;
        }

    }


}
