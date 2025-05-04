package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.Hourglass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientModel {

    private String myNickname;
    private final Map<String, PlayerClientData> playerClientData = new ConcurrentHashMap<>();
    private AdventureCard currAdventureCard;
    private CardState currCardState;
    private GameState gameState;
    private DangerousObj currDangerousObj;
    private String currentPlayer;
    private Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private List<List<String>> littleVisibleDecks = new ArrayList<>();
    private boolean isMyTurn;
    private Hourglass hourglass;

    public Hourglass getHourglass() {
        return hourglass;
    }

    public void setHourglass(Hourglass hourglass) {
        this.hourglass = hourglass;
    }

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

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setCurrAdventureCard(AdventureCard card) {
        this.currAdventureCard = card;
    }

    public AdventureCard getCurrAdventureCard() {
        return currAdventureCard;
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

    public Map<String, PlayerClientData> getPlayerClientData() {
        return playerClientData;
    }

    /**
     * Retrieves a sorted list of player nicknames based on their flying board position in ascending order,
     * with the list then being reversed to get a descending order ranking.
     *
     * @return a list of player nicknames representing the sorted ranking in descending order of their flying board position.
     */
    public List<String> getSortedRanking() {
        return playerClientData.keySet()
                .stream()
                .sorted((a, b) -> Integer.compare(playerClientData.get(a).getFlyingBoardPosition(), playerClientData.get(b).getFlyingBoardPosition()))
                .toList()
                .reversed();
    }

    public void setCurrCardState(CardState currCardState) {
        this.currCardState = currCardState;
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

    public List<List<String>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    public void setLittleVisibleDeck(List<List<String>> littleVisibleDecks) {
        this.littleVisibleDecks = littleVisibleDecks;
    }

    public void updatePlayerCredits(String nickname, int newOwnedCredits) {
        playerClientData.get(nickname).setCredits(newOwnedCredits);
    }

    public void updatePlayerPosition(String nickname, int newPosition) {
        playerClientData.get(nickname).setFlyingBoardPosition(newPosition);
    }

    public void addPlayer(String nickname, PlayerColor color) {
        playerClientData.put(nickname, new PlayerClientData(nickname, color));
    }

    public ShipBoardClient getShipboardOf(String nickname) {
        return playerClientData.get(nickname).getShipBoard();
    }

    public List<String> getPlayersNickname() {
        return new ArrayList<>(playerClientData.keySet());
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
