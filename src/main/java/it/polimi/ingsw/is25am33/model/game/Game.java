package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import javax.management.remote.rmi.RMIServer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class Game {

    // TODO aggiungere gameState
    private AdventureCard currAdventureCard;
    private final FlyingBoard flyingBoard;
    private final List<Player> players;
    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator;
    private DangerousObj currDangerousObj;
    private GameState currGameState = GameState.SETUP;
    private Deck deck;
    private ComponentTable componentTable;
    private GameContext gameContext;

    public void setCurrGameState(GameState currGameState) {
        this.currGameState = currGameState;
    }

    public GameState getCurrGameState() {
        return currGameState;
    }

    public Deck getDeck() {
        return deck;
    }


    public Game(VirtualServer virtualServer, FlyingBoard flyingBoard, List<Player> players, List<Component> components, String gameId) {
        this.flyingBoard = flyingBoard;
        currAdventureCard = null;
        currRanking = new ArrayList<>();
        currPlayer = null;
        currDangerousObj = null;
        this.players = players;
        deck = new Deck();
        componentTable = new ComponentTable(components);
        gameContext = new GameContext(gameId, virtualServer);
        flyingBoard.setGameContext(gameContext);
        ObserverManager.getInstance().registerGame(gameContext);
    }

    public GameContext getGameContext() {
        return gameContext;
    }

    public void setFocusComponent(Player player, Coordinates coordinates){
        Component component = componentTable.getComponent(coordinates);

        player.getPersonalBoard().setFocusedComponent(component);
        component.setCurrState(ComponentState.USED);

        DTO dto = new DTO();
        dto.setComponentTable(componentTable);

        BiConsumer<Observer,String> notifyComponentTable= Observer::notifyComponentTableChanged;

        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "componentTableUpdate", dto ), notifyComponentTable);

        DTO dto1 = new DTO();
        dto.setComponent(component);

        BiConsumer<Observer,String> notifyComponent= Observer::notifyChoosenComponent;

        gameContext.getVirtualServer().notifyClient(List.of(ObserverManager.getInstance().getGameContext(gameContext.getGameId()).getObserver(player.getNickname())), new GameEvent( "showFocusComponent", dto ), notifyComponent);


    }

    public void releaseComponentWithFocus(Player player){
        player.getPersonalBoard().getFocusedComponent().setCurrState(ComponentState.FREE);
        player.getPersonalBoard().setFocusedComponent(null);

        DTO dto = new DTO();
        dto.setComponentTable(componentTable);

        BiConsumer<Observer,String> notifyComponentTable= Observer::notifyComponentTableChanged;
        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "componentTableUpdate", dto ), notifyComponentTable);

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

        DTO dto = new DTO();
        dto.setDangerousObj(dangerousObj);

        BiConsumer<Observer,String> notifyAttack= Observer::notifyDangerousObjAttack;

        virtualServer.notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "dangerousObjAttack", dto ), notifyAttack);
    }

    public void watchVisibileDeck(Player player, int index){
        DTO dto = new DTO();
        dto.setLittleDeck(deck.getVisibleDeck(index));

        BiConsumer<Observer,String> notifyLittleDeck= Observer::notifyChoosenLittleDeck;

        virtualServer.notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()).getObserver(player.getNickname()), new GameEvent("playerWatchesLittleDeck", dto), notifyLittleDeck)
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

        DTO dto = new DTO();
        dto.setPlayer(player);

        BiConsumer<Observer,String> notifyCurrPlayer= Observer::notifyCurrPlayerChanged;

        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "currPlayerUpdate", dto ), notifyCurrPlayer);

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

    public List<Player> getPlayers() {
        return currRanking;
    }

    public void setCurrAdventureCard(AdventureCard currAdventureCard) {
        this.currAdventureCard = currAdventureCard;

        DTO dto = new DTO();
        dto.setAdventureCard(currAdventureCard);

        BiConsumer<Observer,String> notifyAdventureCard= Observer::notifyCurrAdventureCard;

        gameContext.getVirtualServer().notifyClient(ObserverManager.getInstance().getGameContext(gameContext.getGameId()), new GameEvent( "drawnCard", dto ), notifyAdventureCard);

}

    /**
     * Starts the current card phase: updates the game's currState and the card's currState to
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

        players.forEach(player -> {
            x.put(player, player.getPersonalBoard().countExposed());
        });

        Integer minValue = Collections.min(x.values());

        return x.keySet().stream().filter(player -> x.get(player).equals(minValue)).toList();

    }

    public void calculatePlayersCredits() {

        players.forEach(player -> {

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
        shipBoard.setGameContext(gameContext);
        Player player = new Player(nickname, shipBoard);
        player.setGameContext(gameContext);
        players.put(nickname, player);
    }

}
