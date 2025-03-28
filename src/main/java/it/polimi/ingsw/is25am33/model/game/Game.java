package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game {

    private AdventureCard currAdventureCard;
    private GameState currState;
    private final FlyingBoard flyingBoard;
    private final List<Player> players;
    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator;
    private DangerousObj currDangerousObj;

    public Game(FlyingBoard flyingBoard, List<Player> players) {
        this.flyingBoard = flyingBoard;
        currAdventureCard = null;
        currState = null;
        currRanking = new ArrayList<>();
        currPlayer = null;
        currDangerousObj = null;
        this.players = players;
    }

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

    public void setCurrRanking(List<Player> currRanking) {
        this.currRanking = currRanking;
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

    public FlyingBoard getFlyingBoard() {
        return flyingBoard;
    }

    public List<Player> getPlayers() {
        return currRanking;
    }

    public GameState getCurrState() {
        return currState;
    }

    public void setCurrAdventureCard(AdventureCard currAdventureCard) {
        this.currAdventureCard = currAdventureCard;
    }

    /**
     * Starts the current card phase: updates the game's currState and the card's currState to
     * the first of the actual card, sets the currPlayer to the first based on the provided
     * ranking.
     *
     * @throws IllegalStateException if the card phase is not started yet.
     */
    public void startCard() throws IllegalStateException{

        if (currAdventureCard == null || currState != GameState.START_CARD)
            throw new IllegalStateException("Not the right state");

        currState = currAdventureCard.getFirstState();
        playerIterator = currRanking.iterator();
        currPlayer = playerIterator.next();
        currAdventureCard.setCurrState(currState);

    }

}
