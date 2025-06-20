package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.model.component.Component;
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
    private ModelFxAdapter modelFxAdapter;
    private List<PrefabShipInfo> availablePrefabShips = new ArrayList<>();

    public void setModelFxAdapter(ModelFxAdapter modelFxAdapter) {
        this.modelFxAdapter = modelFxAdapter;
    }

    public void refreshShipBoardOf(String nickname) {
        if (modelFxAdapter != null)
            modelFxAdapter.refreshShipBoardOf(nickname);
    }

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
        playerClientData.get(nickname).setLanded(true);
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
        if (modelFxAdapter != null)
            modelFxAdapter.refreshCurrAdventureCard();
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

    public ShipBoardClient getMyShipboard() {
        return playerClientData.get(myNickname).getShipBoard();
    }

    public Map<PlayerColor, Integer> getColorRanking() {
        Map<PlayerColor, Integer> ranking = new HashMap<>();

        playerClientData.keySet()
                .stream()
                .filter(player -> !playerClientData.get(player).isLanded())
                .forEach(nickname -> ranking.put(
                        playerClientData.get(nickname).getColor(),
                        playerClientData.get(nickname).getFlyingBoardPosition()
                ));

        return ranking;
    }

    /**
     * Retrieves a sorted list of player nicknames based on their flying board position.
     * Players still flying are sorted in descending order by position.
     * Landed players are placed at the end of the list.
     *
     * @return a list of all player nicknames with flying players first (sorted by position)
     *         and landed players last
     */
    public List<String> getSortedRanking() {
        // Separate players into two groups
        List<String> flyingPlayers = playerClientData.keySet()
                .stream()
                .filter(player -> !playerClientData.get(player).isLanded())
                .sorted((a, b) -> Integer.compare(
                        playerClientData.get(a).getFlyingBoardPosition(),
                        playerClientData.get(b).getFlyingBoardPosition()
                ))
                .toList()
                .reversed();

        List<String> landedPlayers = playerClientData.keySet()
                .stream()
                .filter(player -> playerClientData.get(player).isLanded())
                .toList();

        // Combine the lists: flying players first, then landed players
        List<String> combinedRanking = new ArrayList<>();
        combinedRanking.addAll(flyingPlayers);
        combinedRanking.addAll(landedPlayers);

        return combinedRanking;
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

    public void addPlayer(String nickname, PlayerColor color, boolean isTestFlight, boolean isGui) {
        playerClientData.put(nickname, new PlayerClientData(nickname, color, isTestFlight, isGui));
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
                .filter(player -> playerClientData.get(player).isLanded())
                .collect(Collectors.toSet());
    }

    public void setNickname(String nickname) {
        this.myNickname = nickname;
    }

    public void refreshVisibleComponents() {
        if (modelFxAdapter != null)
            modelFxAdapter.refreshVisibleComponents();
    }

    public void refreshRanking() {
        if (modelFxAdapter != null)
            modelFxAdapter.refreshRanking();
    }
}
