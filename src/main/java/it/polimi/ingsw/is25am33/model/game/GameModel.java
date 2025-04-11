package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.polimi.ingsw.is25am33.model.CargoCube.*;

public class GameModel {
    private final String gameId;
    private final boolean isTestFlight;
    private final int maxPlayers;
    private boolean isStarted;

    // TODO aggiungere gameState
    private AdventureCard currAdventureCard;
    private final FlyingBoard flyingBoard;
    private final ConcurrentHashMap<String, Player> players;
    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator;
    private DangerousObj currDangerousObj;
    private GameState currGameState;
    private Deck deck;

    public GameModel(String gameId, int maxPlayers, boolean isTestFlight) {
        this.gameId = gameId;
        this.maxPlayers = maxPlayers;
        this.isTestFlight = isTestFlight;
        this.flyingBoard = isTestFlight ? new Level1FlyingBoard() : new Level2FlyingBoard();
        currAdventureCard = null;
        currRanking = new ArrayList<>();
        currPlayer = null;
        currDangerousObj = null;
        currGameState = GameState.SETUP;
        this.players = new ConcurrentHashMap<>();
        deck = new Deck();
        isStarted = false;
    }

    public String getGameId() {
        return gameId;
    }

    public ConcurrentHashMap<String, Player> getPlayers() {
        return players;
    }

    public boolean isTestFlight() {
        return isTestFlight;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setCurrGameState(GameState currGameState) {
        this.currGameState = currGameState;
    }

    public GameState getCurrGameState() {
        return currGameState;
    }

    public Deck getDeck() {
        return deck;
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

    public Player getCurrPlayer() {
        return currPlayer;
    }

    public FlyingBoard getFlyingBoard() {
        return flyingBoard;
    }

    public List<Player> getCurrRanking() {
        return currRanking;
    }

    public void setCurrAdventureCard(AdventureCard currAdventureCard) {
        this.currAdventureCard = currAdventureCard;
    }

    /**
     * Starts the current card phase: updates the gameModel's currState and the card's currState to
     * the first of the actual card, sets the currPlayer to the first based on the provided
     * ranking.
     *
     * @throws IllegalStateException if the card phase is not started yet.
     */
    public void startCard() throws IllegalStateException{

        if (currAdventureCard == null || currAdventureCard.getCurrState() != CardState.START_CARD)
            throw new IllegalStateException("Not the right state");

        setCurrRanking(flyingBoard.getCurrentRanking());
        currAdventureCard.setGame(this);
        playerIterator = currRanking.iterator();
        currPlayer = playerIterator.next();
        currAdventureCard.setCurrState(currAdventureCard.getFirstState());

    }

    public List<Player> getPlayerWithPrettiestShip() {

        Map<Player, Integer> x = new HashMap<>();

        players.values().forEach(player -> {
            x.put(player, player.getPersonalBoard().countExposed());
        });

        Integer minValue = Collections.min(x.values());

        return x.keySet().stream().filter(player -> x.get(player).equals(minValue)).toList();

    }

    public void calculatePlayersCredits() {

        players.values().forEach(player -> {

            int credits = player.getOwnedCredits();

            credits += flyingBoard.getCreditsForPosition(player);

            if (getPlayerWithPrettiestShip().contains(player))
                credits += flyingBoard.getPrettiestShipReward();

            credits += player.getPersonalBoard().getStorages()
                                .stream()
                                .flatMap(storage -> storage.getStockedCubes().stream())
                                .mapToInt(stockedCube -> {
                                    switch (stockedCube) {
                                        case BLUE -> {
                                            return 1;
                                        }
                                        case GREEN -> {
                                            return 2;
                                        }
                                        case YELLOW -> {
                                            return 3;
                                        }
                                        case RED -> {
                                            return 4;
                                        }
                                        default -> {return 0;}
                                    }
                                }).sum();

            credits -= player.getPersonalBoard().getNotActiveComponents().size();

            player.setOwnedCredits(credits);
        });



    }

    public void addPlayer(String nickname, PlayerColor color) {
        ShipBoard shipBoard = isTestFlight ? new Level1ShipBoard(color) : new Level2ShipBoard(color);
        Player player = new Player(nickname, shipBoard);
        players.put(nickname, player);
    }

    public void removePlayer(String nickname) {
        players.remove(nickname);
    }
}
