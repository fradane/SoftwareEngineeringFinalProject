package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientModel {

    private String myNickname;
    private Set<String> playersNickname;
    private AdventureCard currAdventureCard;
    private CardState currCardState;
    private GameState gameState;
    private Map<String, ShipBoardClient> shipboards = new ConcurrentHashMap<>();
    private Map<String, Integer> credits = new ConcurrentHashMap<>();
    private DangerousObj currDangerousObj;
    private String currentPlayer;
    private Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private final Map<String, Integer> ranking = new ConcurrentHashMap<>();
    private List<List<AdventureCard>> littleVisibleDecks;
    private boolean isMyTurn;

    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    public void setCardState(CardState cardState){
        this.currCardState = cardState;
    }

    public CardState getCurrCardState() {
        return currCardState;
    }

    public String getMyNickname() {
        return myNickname;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void updatePlayerPosition(String nickname, int newPosition) {
        ranking.put(nickname, newPosition);
    }

    public Map<String, Integer> getRanking() {
        return ranking;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setCurrAdventureCard(AdventureCard card) {
        this.currAdventureCard = card;
    }

    public AdventureCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    public Map<String, ShipBoardClient> getShipboards() {
        return shipboards;
    }

    public Map<String, Integer> getCredits() {
        return credits;
    }

    public Set<String> getPlayersNickname() {
        return playersNickname;
    }

    public void setPlayersNickname(Set<String> playersNickname) {
        this.playersNickname = playersNickname;
    }

    public void setCurrDangerousObj(DangerousObj currDangerousObj) {
        this.currDangerousObj = currDangerousObj;
    }

    public Map<Integer, Component> getVisibleComponents() {
        return visibleComponents;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public List<String> getSortedRanking() {
        return ranking.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public void setCredits(Map<String, Integer> credits) {
        this.credits = credits;
    }

    public void setCurrCardState(CardState currCardState) {
        this.currCardState = currCardState;
    }

    public void setShipboards(Map<String, ShipBoardClient> shipboards) {
        this.shipboards = shipboards;
    }

    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void setVisibleComponents(Map<Integer, Component> visibleComponents) {
        this.visibleComponents = visibleComponents;
    }

    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    public List<List<AdventureCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    public void setLittleVisibleDeck(List<List<AdventureCard>> littleVisibleDecks) {
        this.littleVisibleDecks = littleVisibleDecks;
    }

    /*
    * TODO:
    *  - notificare solo il cambiamento della shipboard tramite due notify del tipo removeComponent(coordinate) e una notifyAddComponent(component, coordinate),
    *       notifyAddComponent deve chiamare il metodo addComponent della shipBoard in modo che venga aggiunto tutto anche nella mappa che serve per altre cose
    *  - notificare solo il cambiamento delle visible component in modo simile a quello di prima, non passare tutta la mappa intero componente, ma solo cosa togliere o aggiungere
    *  - mettere Override sui metodi di cui fai override
    *  - la flyingBoard non va passata tutta ma solo i cambiamenti della mappa ranking del client model quindi tipo notifyNewPosition(nickname, newPosition) tramite il metodo del model updatePlayerPosition
    *  - completare la classe hourglass con le tue notify
    * */



}
