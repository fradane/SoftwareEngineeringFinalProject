package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentHashMap;


public class GameModel {
    private final String gameId;
    private final boolean isTestFlight;
    private final int maxPlayers;
    private boolean isStarted;

    private AdventureCard currAdventureCard;
    private final FlyingBoard flyingBoard;
    private final ConcurrentHashMap<String, Player> players;
    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator;
    private DangerousObj currDangerousObj;
    private GameState currGameState;
    private Deck deck;
    private ComponentTable componentTable;
    private GameContext gameContext;

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
        componentTable = new ComponentTable();
    }

    public void setObservers(Map<String, CallableOnClientController> clientControllers){

    }
    public void setStarted(boolean started) {
        isStarted = started;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public ComponentTable getComponentTable() {
        return componentTable;
    }

    public void setComponentTable(ComponentTable componentTable) {
        this.componentTable = componentTable;
    }

    public void createGameContext(Map<String, CallableOnClientController> clientControllers) {
        this.gameContext= new GameContext(clientControllers);
        deck.setGameContext(gameContext);
        flyingBoard.setGameContext(gameContext);
        componentTable.setGameContext(gameContext);
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
        try {
            this.currGameState = currGameState;

            for (String nickname : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(nickname).notifyGameState(nickname, currGameState);
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }
    }

    public GameContext getGameContext() {
        return gameContext;
    }

    public GameState getCurrGameState() {
        return currGameState;
    }

    public Deck getDeck() {
        return deck;
    }

    public static int throwDices() {
        return (int) (Math.random() * 12) + 1;
    }

    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    public void setCurrDangerousObj(DangerousObj dangerousObj) {

        try{
            this.currDangerousObj = dangerousObj;

            for(String nickname : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(nickname).notifyDangerousObjAttack(nickname, currDangerousObj);
            }
        } catch(RemoteException e) {
            System.err.println("Remote Exception");
        }

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

    public void setCurrPlayer(Player player){
        try{
            this.currPlayer = player;

            for(String nickname: gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(nickname).notifyCurrPlayerChanged(nickname, player.getNickname());
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }

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

        try {
            this.currAdventureCard = currAdventureCard;

            for (String nickname : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(nickname).notifyCurrAdventureCard(nickname, currAdventureCard);
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }
    }

    public AdventureCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    /**
     * Starts the current card phase: updates the gameModel's currState and the card's currState to
     * the first of the actual card, sets the currPlayer to the first based on the provided
     * ranking.
     *
     * @throws IllegalStateException if the card phase is not started yet.
     */
    public void startCard() throws IllegalStateException {

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
                                        default -> {
                                            return 0;
                                        }
                                    }
                                }).sum();

            credits -= player.getPersonalBoard().getNotActiveComponents().size();

            player.setOwnedCredits(credits);
        });

    }

    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController){
        try{
            gameContext.getClientControllers().put(nickname, clientController);
            ShipBoard shipBoard = isTestFlight ? new Level1ShipBoard(color) : new Level2ShipBoard(color);
            shipBoard.setGameContext(gameContext);
            Player player = new Player(nickname, shipBoard, color);
            player.setGameContext(gameContext);
            players.put(nickname, player);
            shipBoard.setPlayer(player);

            for(String nicknameToNotify : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(nicknameToNotify).notifyShipBoardUpdate(nicknameToNotify, player.getNickname(), shipBoard.getShipMatrix());
                gameContext.getClientControllers().get(nicknameToNotify).notifyPlayerCredits(nicknameToNotify, player.getNickname(), 0);
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }

    }

    public void removePlayer(String nickname) {
        players.remove(nickname);
    }

}
