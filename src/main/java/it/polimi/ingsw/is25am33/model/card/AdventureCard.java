package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.game.Game;

public abstract class AdventureCard {

    protected int level;
    protected GameState currState;
    protected static Game game;

    public AdventureCard(Game game) {
        this.game = game;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public abstract GameState getFirstState();

    public void setCurrState(GameState currState) {
        this.currState = currState;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public abstract void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException;

}
