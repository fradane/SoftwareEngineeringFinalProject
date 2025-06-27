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
    private Timer cardAdapterTimer;  // Track current timer to prevent memory leaks

    /**
     * Sets the ModelFxAdapter for this client model.
     *
     * @param modelFxAdapter the adapter to be set
     */
    public void setModelFxAdapter(ModelFxAdapter modelFxAdapter) {
        synchronized (modelFxAdapterLock) {
            this.modelFxAdapter = modelFxAdapter;
            modelFxAdapterLock.notifyAll();
        }
    }

    /**
     * Refreshes the ship board display for a specific player.
     *
     * @param nickname the nickname of the player whose ship board needs to be refreshed
     */
    public void refreshShipBoardOf(String nickname) {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshShipBoardOf(nickname);
        }
    }

    /**
     * Returns the list of available prefabricated ships.
     *
     * @return list of available prefabricated ship information
     */
    public List<PrefabShipInfo> getAvailablePrefabShips() {
        return availablePrefabShips;
    }

    /**
     * Sets the list of available prefabricated ships.
     *
     * @param availablePrefabShips list of prefabricated ship information to be set
     */
    public void setAvailablePrefabShips(List<PrefabShipInfo> availablePrefabShips) {
        this.availablePrefabShips = availablePrefabShips;
    }

    /**
     * Gets the hourglass timer object.
     *
     * @return the hourglass timer
     */
    public Hourglass getHourglass() {
        return hourglass;
    }

    /**
     * Sets the hourglass timer.
     *
     * @param hourglass the hourglass timer to be set
     */
    public void setHourglass(Hourglass hourglass) {
        this.hourglass = hourglass;
    }

    /**
     * Marks a player as eliminated from the game.
     *
     * @param nickname the nickname of the player to be eliminated
     */
    public synchronized void eliminatePlayer(String nickname) {
        PlayerClientData playerData = playerClientData.get(nickname);
        if (playerData != null) {
            playerData.setLanded(true);
        }
    }

    /**
     * Sets the nickname for the current player.
     *
     * @param myNickname the nickname to be set
     */
    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }

    /**
     * Checks if it's currently this player's turn.
     *
     * @return true if it's this player's turn, false otherwise
     */
    public boolean isMyTurn() {
        return myNickname.equals(currentPlayer);
    }

    /**
     * Sets whether it's currently this player's turn.
     *
     * @param myTurn true if it's this player's turn, false otherwise
     */
    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    /**
     * Sets the current state of the card.
     *
     * @param cardState the state to set for the current card
     */
    public void setCardState(CardState cardState){
        this.currCardState = cardState;
    }

    /**
     * Gets the current state of the card.
     *
     * @return the current card state
     */
    public CardState getCurrCardState() {
        return currCardState;
    }

    /**
     * Gets the nickname of the current player.
     *
     * @return the player's nickname
     */
    public String getMyNickname() {
        return myNickname;
    }

    /**
     * Gets the current state of the game.
     *
     * @return the current game state
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the current state of the game.
     *
     * @param gameState the game state to set
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Sets the current adventure card for the client model.
     * If the GUI adapter (modelFxAdapter) is available and ready (i.e., isCardAdapter returns true),
     * it schedules an asynchronous check using a Timer to update the GUI once the adapter is ready.
     * Implements proper timer cleanup to prevent memory leaks.
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
            // Cancel any existing timer to prevent memory leaks
            if (cardAdapterTimer != null) {
                cardAdapterTimer.cancel();
                cardAdapterTimer = null;
            }
            
            if (modelFxAdapter != null) {
                // Create a new Timer to periodically check if the adapter is ready
                cardAdapterTimer = new Timer("CardAdapterTimer", true);  // daemon timer
                final int maxAttempts = 100;  // Maximum 10 seconds (100 * 100ms)
                
                cardAdapterTimer.schedule(new TimerTask() {
                    private int attempts = 0;
                    
                    @Override
                    public void run() {
                        synchronized (modelFxAdapterLock) {
                            attempts++;
                            
                            // Check if adapter is ready or if we've exceeded max attempts
                            if (modelFxAdapter != null && modelFxAdapter.isCardAdapter()) {
                                Platform.runLater(() -> {
                                    modelFxAdapter.refreshCurrAdventureCard();
                                });
                                cleanupTimer();
                            } else if (attempts >= maxAttempts) {
                                // Timeout: stop trying to avoid infinite loop
                                System.err.println("CardAdapter timeout: adapter not ready after " + maxAttempts + " attempts");
                                cleanupTimer();
                            }
                        }
                    }
                    
                    private void cleanupTimer() {
                        synchronized (modelFxAdapterLock) {
                            if (cardAdapterTimer != null) {
                                cardAdapterTimer.cancel();
                                cardAdapterTimer = null;
                            }
                        }
                    }
                }, 0, 100); // Check every 100 ms
            }
        }
    }

    /**
     * Gets the current adventure card.
     *
     * @return the current adventure card
     */
    public ClientCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    /**
     * Sets the current dangerous object.
     *
     * @param currDangerousObj the dangerous object to set
     */
    public void setCurrDangerousObj(ClientDangerousObject currDangerousObj) {
        this.currDangerousObj = currDangerousObj;
    }

    /**
     * Gets the map of visible components.
     *
     * @return map of component IDs to components
     */
    public Map<Integer, Component> getVisibleComponents() {
        return visibleComponents;
    }

    /**
     * Gets the nickname of the player whose turn it currently is.
     *
     * @return the current player's nickname
     */
    public String getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Gets the map of all player data.
     *
     * @return map of player nicknames to their client data
     */
    public Map<String, PlayerClientData> getPlayerClientData() {
        return playerClientData;
    }

    /**
     * Gets the ship board for the current player.
     *
     * @return the current player's ship board
     */
    public ShipBoardClient getMyShipboard() {
        return playerClientData.get(myNickname).getShipBoard();
    }

    /**
     * Gets the ranking of players by their colors and positions.
     *
     * @return map of player colors to their ranking positions
     */
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

    /**
     * Gets the number of cosmic credits owned by the current player.
     *
     * @return the current player's cosmic credits
     */
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

    /**
     * Gets the list of players who are out of the game.
     *
     * @return list of nicknames of players who are out
     */
    public List<String> getOutPlayers() {
        return playerClientData.keySet()
                .stream()
                .filter(player -> playerClientData.get(player).isOut())
                .toList();
    }

    /**
     * Sets the current state of the card.
     *
     * @param currCardState the card state to set
     */
    public void setCurrCardState(CardState currCardState) {
        this.currCardState = currCardState;
    }

    /**
     * Sets the current active player.
     *
     * @param currentPlayer nickname of the player to set as current
     */
    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    /**
     * Sets the map of visible components.
     *
     * @param visibleComponents map of component IDs to Components to set
     */
    public void setVisibleComponents(Map<Integer, Component> visibleComponents) {
        this.visibleComponents = visibleComponents;
    }

    /**
     * Gets the current dangerous object.
     *
     * @return the current dangerous object
     */
    public ClientDangerousObject getCurrDangerousObj() {
        return currDangerousObj;
    }

    /**
     * Gets the list of visible card decks.
     *
     * @return list of visible card decks
     */
    public List<List<ClientCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    /**
     * Sets the list of visible card decks.
     *
     * @param littleVisibleDecks list of visible card decks to set
     */
    public void setLittleVisibleDeck(List<List<ClientCard>> littleVisibleDecks) {
        this.littleVisibleDecks = littleVisibleDecks;
    }

    /**
     * Updates the cosmic credits for a player.
     * If the updated player is the current player, refreshes the cosmic credits display.
     *
     * @param nickname        nickname of the player to update
     * @param newOwnedCredits new amount of credits for the player
     */
    public synchronized void updatePlayerCredits(String nickname, int newOwnedCredits) {
        PlayerClientData playerData = playerClientData.get(nickname);
        if (playerData != null) {
            playerData.setCredits(newOwnedCredits);
            if (nickname.equals(myNickname)) {
                refreshCosmicCredits();
            }
        }
    }

    /**
     * Updates the board position for a player.
     *
     * @param nickname    nickname of the player to update
     * @param newPosition new position for the player
     */
    public synchronized void updatePlayerPosition(String nickname, int newPosition) {
        PlayerClientData playerData = playerClientData.get(nickname);
        if (playerData != null) {
            playerData.setFlyingBoardPosition(newPosition);
        }
    }

    /**
     * Adds a new player to the game.
     *
     * @param nickname     nickname of the player to add
     * @param color        color assigned to the player
     * @param isTestFlight whether the player is in test flight mode
     * @param isGui        whether the player is using GUI interface
     */
    public void addPlayer(String nickname, PlayerColor color, boolean isTestFlight, boolean isGui) {
        playerClientData.put(nickname, new PlayerClientData(nickname, color, isTestFlight, isGui));
    }

    /**
     * Gets the shipboard for a specific player.
     *
     * @param nickname nickname of the player
     * @return the player's shipboard
     */
    public ShipBoardClient getShipboardOf(String nickname) {
        return playerClientData.get(nickname).getShipBoard();
    }

    /**
     * Gets the set of all player nicknames.
     *
     * @return set of player nicknames
     */
    public Set<String> getPlayersNickname() {
        return new HashSet<>(playerClientData.keySet());
    }

    /**
     * Gets the set of players who have been eliminated.
     *
     * @return set of eliminated player nicknames
     */
    public synchronized Set<String> getEliminatedPlayers() {
        return playerClientData.keySet()
                .stream()
                .filter(player -> {
                    PlayerClientData playerData = playerClientData.get(player);
                    return playerData != null && playerData.isLanded();
                })
                .collect(Collectors.toSet());
    }

    /**
     * Sets the nickname for this client.
     *
     * @param nickname nickname to set
     */
    public void setNickname(String nickname) {
        this.myNickname = nickname;
    }

    /**
     * Refreshes the display of visible components.
     */
    public void refreshVisibleComponents() {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshVisibleComponents();
        }
    }

    /**
     * Refreshes the ranking display.
     */
    public void refreshRanking() {
        synchronized (modelFxAdapterLock) {
            if (modelFxAdapter != null)
                modelFxAdapter.refreshRanking();
        }
    }

    /**
     * Refreshes the cosmic credits display.
     */
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
     * @throws IllegalStateException if no active card is available
     */
    public List<CargoCube> extractCubeRewardsFromCurrentCard() throws IllegalStateException {
        List<CargoCube> cubes = new ArrayList<>();
        ClientCard card = currAdventureCard;

        // Extract rewards based on a card type
        return switch (card) {
            case null -> throw new IllegalStateException("No active card available.");
            case ClientPlanets planets -> planets.getPlayerReward(myNickname);
            case ClientAbandonedStation clientAbandonedStation ->
                // AbandonedStation has a direct list of rewards
                    new ArrayList<>(clientAbandonedStation.getReward());
            case ClientSmugglers clientSmugglers ->
                // Smugglers have a direct list of rewards
                    new ArrayList<>(clientSmugglers.getReward());
            default -> cubes;
        };

    }

}