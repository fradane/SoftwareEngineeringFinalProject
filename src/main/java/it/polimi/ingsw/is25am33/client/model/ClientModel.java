package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.ERROR;

public class ClientModel {

    private String myNickname;
    private final Map<String, PlayerClientData> playerClientData = new ConcurrentHashMap<>();
    private ClientCard currAdventureCard;
    private CardState currCardState;
    private GameState gameState;
    private ClientDangerousObject currDangerousObj;
    private String currentPlayer;
    private Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private List<List<ClientCard>> littleVisibleDecks = new ArrayList<>();
    private boolean isMyTurn;
    private Hourglass hourglass;
    private ModelFxAdapter modelFxAdapter;
    private List<PrefabShipInfo> availablePrefabShips = new ArrayList<>();
    private final Object modelFxAdapterLock = new Object();

    public void setModelFxAdapter(ModelFxAdapter modelFxAdapter) {
        synchronized (modelFxAdapterLock) {
            this.modelFxAdapter = modelFxAdapter;
            modelFxAdapterLock.notifyAll();
        }
    }

    public void refreshShipBoardOf(String nickname) {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshShipBoardOf(nickname);
        }
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

    /**
     * Sets the current adventure card for the client model.
     * If the GUI adapter (modelFxAdapter) is available and ready (i.e., isCardAdapter returns true),
     * it schedules an asynchronous check using a Timer to update the GUI once the adapter is ready.
     * <p>
     *     This ensures that the GUI update is non-blocking and runs only when the adapter is in the correct state.
     * </p>
     *
     * @param currAdventureCard the new current adventure card to be set
     */
    public void setCurrAdventureCard(ClientCard currAdventureCard) {

        System.out.println(currAdventureCard.getImageName());

        if (this.currAdventureCard != null && this.currAdventureCard.getImageName().equals(currAdventureCard.getImageName())) {
            this.currAdventureCard = currAdventureCard;
            return;
        }

        this.currAdventureCard = currAdventureCard;

        // Synchronize on the lock to ensure thread-safe access to modelFxAdapter
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null) {
                //System.err.println("ModelFxAdapter not null");

                // Create a Timer to periodically check if the adapter is ready
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (modelFxAdapterLock) {
                            // When the adapter becomes ready, update the GUI and cancel the timer
                            if (modelFxAdapter != null && modelFxAdapter.isCardAdapter()) {
                                Platform.runLater(() -> {
                                    modelFxAdapter.refreshCurrAdventureCard();
                                    //System.err.println("refreshing current Adventure Card");
                                });
                                timer.cancel();
                            }
                        }
                    }
                }, 0, 100); // Check every 100 ms
            }
        }
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
                .forEach(nickname -> {
                    PlayerClientData currPlayerClientData = this.playerClientData.get(nickname);
                    ranking.put(
                            currPlayerClientData.getColor(),
                            currPlayerClientData.isOut() || currPlayerClientData.isLanded() ?
                                    Integer.MIN_VALUE : currPlayerClientData.getFlyingBoardPosition()
                    );
                });

        return ranking;
    }

    public int getMyCosmicCredits() {
        if (myNickname != null && playerClientData.containsKey(myNickname)) {
            return playerClientData.get(myNickname).getCredits();
        }
        return 0;
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

    public List<String> getOutPlayers() {
        return playerClientData.keySet()
                .stream()
                .filter(player -> playerClientData.get(player).isOut())
                .toList();
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

    public List<List<ClientCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    public void setLittleVisibleDeck(List<List<ClientCard>> littleVisibleDecks) {
        this.littleVisibleDecks = littleVisibleDecks;
    }

    public void updatePlayerCredits(String nickname, int newOwnedCredits) {
        playerClientData.get(nickname).setCredits(newOwnedCredits);
        if (nickname.equals(myNickname)) {
            refreshCosmicCredits();
        }
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
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshVisibleComponents();
        }
    }

    public void refreshRanking() {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshRanking();
        }
    }

    public void refreshCosmicCredits() {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null) {
                modelFxAdapter.refreshCosmicCredits();
            }
        }
    }

    /**
     * Extracts the information about cube rewards from the current card.
     * Uses the ClientCard object properties directly instead of string parsing.
     *
     * @return List of CargoCube that represent the rewards of the current card
     */
    public List<CargoCube> extractCubeRewardsFromCurrentCard() throws IllegalStateException {
        List<CargoCube> cubes = new ArrayList<>();
        ClientCard card = currAdventureCard;

        if (card == null)
            throw new IllegalStateException("No active card available.");

        // Extract rewards based on a card type
        if (card instanceof ClientPlanets planets) {
            return planets.getPlayerReward(myNickname);
        }
        else if (card instanceof ClientAbandonedStation) {
            // AbandonedStation has a direct list of rewards
            return new ArrayList<>(((ClientAbandonedStation) card).getReward());
        }
        else if (card instanceof ClientSmugglers) {
            // Smugglers have a direct list of rewards
            return new ArrayList<>(((ClientSmugglers) card).getReward());
        }

        return cubes;
    }

}
