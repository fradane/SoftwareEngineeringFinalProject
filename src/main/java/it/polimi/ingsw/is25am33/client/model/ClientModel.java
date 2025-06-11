package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientModel {

    private String myNickname;
    private final Map<String, PlayerClientData> playerClientData = new ConcurrentHashMap<>();
    private ClientCard currAdventureCard;
    private CardState currCardState;
    private GameState gameState;
    private ClientDangerousObject currDangerousObj;
    private String currentPlayer;
    private Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private List<List<String>> littleVisibleDecks = new ArrayList<>();
    private boolean isMyTurn;
    private Hourglass hourglass;
    private List<PrefabShipInfo> availablePrefabShips = new ArrayList<>();

    public List<PrefabShipInfo> getAvailablePrefabShips() {
        return availablePrefabShips;
    }

    public void setAvailablePrefabShips(List<PrefabShipInfo> availablePrefabShips) {
        this.availablePrefabShips = availablePrefabShips;
    }

    public Hourglass getHourglass() {
        return hourglass;
    }

    public void setHourglass(Hourglass hourglass) {
        this.hourglass = hourglass;
    }

    public void eliminatePlayer(String nickname) {
        playerClientData.get(nickname).setOut(true);
    }

    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }

    public boolean isMyTurn() {
        return myNickname.equals(currentPlayer);
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

    public void setCurrAdventureCard(ClientCard currAdventureCard) {
        this.currAdventureCard = currAdventureCard;
    }

    public ClientCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    public void setCurrDangerousObj(ClientDangerousObject currDangerousObj) {
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
                .filter(player -> !playerClientData.get(player).isOut())
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

    public ClientDangerousObject getCurrDangerousObj() {
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

    public void addPlayer(String nickname, PlayerColor color, boolean isTestFlight) {
        playerClientData.put(nickname, new PlayerClientData(nickname, color, isTestFlight));
    }

    public ShipBoardClient getShipboardOf(String nickname) {
        return playerClientData.get(nickname).getShipBoard();
    }

    public Set<String> getPlayersNickname() {
        return new HashSet<>(playerClientData.keySet());
    }

    public Set<String> getEliminatedPlayers() {
        return playerClientData.keySet()
                .stream()
                .filter(player -> playerClientData.get(player).isOut())
                .collect(Collectors.toSet());
    }

    public void setNickname(String nickname) {
        this.myNickname = nickname;
    }
}
