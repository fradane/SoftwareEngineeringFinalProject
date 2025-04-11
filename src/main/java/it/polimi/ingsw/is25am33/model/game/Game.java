package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
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
    private ObserverManager observerManager;
    private VirtualServer virtualServer;

    public void setCurrGameState(GameState currGameState) {
        this.currGameState = currGameState;
    }

    public GameState getCurrGameState() {
        return currGameState;
    }

    public Deck getDeck() {
        return deck;
    }


    public Game(FlyingBoard flyingBoard, List<Player> players, List<Component> components) {
        this.flyingBoard = flyingBoard;
        currAdventureCard = null;
        currRanking = new ArrayList<>();
        currPlayer = null;
        currDangerousObj = null;
        this.players = players;
        deck = new Deck();
        componentTable = new ComponentTable(components);
        observerManager = new ObserverManager();
    }

    public void chooseFocusComponent(Player player, Coordinates coordinates){
        Component component = componentTable.getComponent(coordinates);

        player.getPersonalBoard().setFocusedComponent(component);
        component.setCurrState(ComponentState.USED);

        DTO dto = new DTO();
        dto.setComponentTable(componentTable);
        ObserverManager.getInstance().notifyAll(new GameEvent( "pickUpCoveredComponent", dto ));

        DTO dto1 = new DTO();
        dto.setPlayer(player);
        dto.setComponent(component);
        ObserverManager.getInstance().notifyObserver(player.getNickname(), new GameEvent( "showFocusComponent", dto ));
    }

    public void releaseComponentWithFocus(Player player){
        Component component = player.getPersonalBoard().getFocusedComponent();
        component.setCurrState(ComponentState.FREE);
        player.getPersonalBoard().setFocusedComponent(null);

        DTO dto = new DTO();
        dto.setComponentTable(componentTable);
        ObserverManager.getInstance().notifyAll(new GameEvent( "replaceFocusedComponentOnTable", dto ));

        DTO dto1 = new DTO();
        dto.setPlayer(player);
        ObserverManager.getInstance().notifyObserver(player.getNickname(), new GameEvent( "releaseFocusComponent", dto ));
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
        ObserverManager.getInstance().notifyAll(new GameEvent( "dangerousObjAttack", dto ));
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

        ObserverManager.getInstance().notifyAll(new GameEvent( "currPlayerUpdate", dto ));
    }

    public void watchVisibleDeck(Player player, int index){

        DTO dto = new DTO();
        dto.setPlayer(player);
        dto.setNum(index);
        ObserverManager.getInstance().notifyAll(new GameEvent( "playerWatchesLittleDeck", dto ));

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

        BiConsumer<Observer,String> notify= (observer, message)-> {
            observer.notifyCurrAdventureCard(message);
        };

        virtualServer.notifyClient(observerManager.getObservers(), new GameEvent( "drawnCard", dto ), notify);
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

}
