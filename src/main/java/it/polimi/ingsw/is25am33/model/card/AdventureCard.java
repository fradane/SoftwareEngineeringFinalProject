package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.Serializable;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public abstract class AdventureCard implements Serializable {

    protected String cardName;
    protected int level;
    protected CardState currState;
    protected GameModel gameModel;

    /**
     * A static map that associates string identifiers with factory functions
     * for creating specific types of {@link Meteorite} objects.
     * <p>
     * This is used by cards that contain meteorites, allowing them to dynamically
     * instantiate the appropriate meteorite object based on a given ID (e.g., "big_north").
     */
    protected static Map<String, Callable<Meteorite>> meteoriteCreator = Map.of(
            "big_north", () -> new BigMeteorite(Direction.NORTH),
            "big_east", () -> new BigMeteorite(Direction.EAST),
            "big_west", () -> new BigMeteorite(Direction.WEST),
            "big_south", () -> new BigMeteorite(Direction.SOUTH),
            "small_north", () -> new SmallMeteorite(Direction.NORTH),
            "small_south", () -> new SmallMeteorite(Direction.SOUTH),
            "small_west", () -> new SmallMeteorite(Direction.WEST),
            "small_east", () -> new SmallMeteorite(Direction.EAST)
    );

    /**
     * A static map that associates string identifiers with factory functions
     * for creating specific types of {@link Shot} objects.
     * <p>
     * This is used by cards that involve shots, allowing them to dynamically
     * instantiate the correct shot object based on a given ID (e.g., "small_east").
     */
    protected static Map<String, Callable<Shot>> shotCreator = Map.of(
            "big_north", () -> new BigShot(Direction.NORTH),
            "big_east", () -> new BigShot(Direction.EAST),
            "big_west", () -> new BigShot(Direction.WEST),
            "big_south", () -> new BigShot(Direction.SOUTH),
            "small_north", () -> new SmallShot(Direction.NORTH),
            "small_south", () -> new SmallShot(Direction.SOUTH),
            "small_west", () -> new SmallShot(Direction.WEST),
            "small_east", () -> new SmallShot(Direction.EAST)
    );

    public void setLevel(int level) {
        this.level = level;
    }

    public String getCardName() {
        return cardName;
    }

    public int getLevel() {
        return level;
    }

    @JsonIgnore
    public abstract CardState getFirstState();

    public void setCurrState(CardState currState)  {
        try {
            this.currState = currState;

            for (String s : gameModel.getGameContext().getClientControllers().keySet()) {
                gameModel.getGameContext().getClientControllers().get(s).notifyCardState(s, currState);
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }

    }

    @JsonIgnore
    public CardState getCurrState() {
        return currState;
    }

    public void setGame(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public abstract void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException;

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    @Override
    public String toString() {
        return "AdventureCard{" +
                "cardName='" + cardName + '\'' +
                ", level=" + level +
                '}';
    }
}

