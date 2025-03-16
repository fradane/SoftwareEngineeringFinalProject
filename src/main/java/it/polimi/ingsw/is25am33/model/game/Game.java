package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.Iterator;
import java.util.List;

public abstract class Game {

    private AdventureCard currAdventureCard;
    private GameState currState;

    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator = currRanking.iterator();

    private DangerousObj currDangerousObj;

    public static int throwDices() {
        double random = Math.random();
        return (int) (Math.random() * 12) + 1;
    }

    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    public void setCurrDangerousObj(DangerousObj dangerousObj) {
        this.currDangerousObj = dangerousObj;
    }

    public Boolean hasNextPlayer() {
        if (playerIterator.hasNext()) return true;
        return false;
    }

    public void resetPlayerIterator() {
        playerIterator = currRanking.iterator();
        currPlayer = playerIterator.next();
    }

    public void setCurrPlayer(Player player) {
        this.currPlayer = player;
    }

    public void nextPlayer() {
        currPlayer = playerIterator.next();
    }

    public void setCurrState(GameState currState) {
        this.currState = currState;
    }

    public Player getCurrPlayer() {
        return currPlayer;
    }

}
