package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract class representing a FlyingBoard that keeps track of players and their positions.
 */
public abstract class FlyingBoard {
    /**
     * A set containing players who are no longer participating in the game.
     * Players added to this set are considered out of the active gameplay.
     * This could occur due to various reasons such as exceeding the predefined
     * run length or voluntary/involuntary actions leading them out of the game.
     *
     * This data structure is primarily used to manage and keep track of excluded players,
     * ensuring that they do not interfere with ongoing game mechanics and ranking.
     *
     * It supports operations such as adding players who are deemed out,
     * retrieving the collection of excluded players, and facilitating the
     * proper management of their game state.
     */
    private final Set<Player> outPlayers;
    /**
     * The `runLenght` variable defines the threshold distance or difference
     * between player positions in the ranking, beyond which a player is
     * considered "doubled" and removed from active competition.
     *
     * It is used as a key parameter in methods that determine player status,
     * such as identifying and handling outplayers based on the position
     * difference from the highest-ranked player.
     *
     * This value is initialized during object construction and plays a role
     * in managing the player ranking and game flow.
     */
    protected int runLenght;
    /**
     * A mapping of players to their respective ranks or scores in the game.
     * This structure is used to track the current standing of all active players
     * based on their performance or position. Each player is associated with
     * an integer value representing their rank or score.
     *
     * The {@code ranking} map is updated as the game progresses and is used
     * to determine various gameplay outcomes, such as identifying players who
     * are out of the game or determining the current order of players.
     *
     * This field is immutable to ensure that the reference to the map cannot be
     * changed after initialization, although the contents of the map itself can be
     * modified.
     */
    protected final Map<Player, Integer> ranking;
    /**
     * The {@code gameClientNotifier} is a protected field within the {@code FlyingBoard} class.
     * It serves as a communication handler for notifying game clients about various game events,
     * such as player actions, disconnections, or other updates during the game lifecycle.
     *
     * The {@code GameClientNotifier} utilizes a combination of asynchronous operations and
     * timeout mechanisms to ensure efficient, non-blocking notifications to all clients,
     * even in cases where some clients are unresponsive or slow to respond.
     *
     * This field is essential for maintaining synchronized communication across clients
     * and updating their state according to the server's state changes during gameplay.
     */
    protected GameClientNotifier gameClientNotifier;

    /**
     * Constructor to initialize runLength, outPlayers, and ranking.
     *
     * @param runLength The length difference after which a player is considered doubled.
     */
    public FlyingBoard(int runLength) {
        this.runLenght = runLength;
        this.outPlayers = new HashSet<>();
        this.ranking = new ConcurrentHashMap<>();  // Thread-safe collection for concurrent access
    }

    /**
     * Sets the GameClientNotifier instance for this class.
     * This notifier is used to manage and notify game clients about game events,
     * such as player actions, disconnections, or other updates.
     *
     * @param gameClientNotifier An instance of GameClientNotifier used to handle
     *                           communication with game clients.
     */
    public void setGameClientNotifier(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    /**
     * Inserts a player into the ranking with a specified position.
     *
     * @param player The player to be inserted.
     * @return
     */
    public abstract int insertPlayer(Player player);

    /**
     * Retrieves the current position of the specified player in the ranking.
     *
     * @param player The player whose position is to be retrieved.
     * @return The integer position of the given player.
     */
    public int getPlayerPosition(Player player) {
        return ranking.get(player);
    }

    /**
     * Returns the set of players who have been doubled.
     *
     * @return A set of players who are out of the gameModel.
     */
    public Set<Player> getOutPlayers() {
        return outPlayers;
    }

    /**
     * Retrieves the current ranking of players.
     * The ranking is represented as a mapping between players and their corresponding
     * integer positions or scores. The players are mapped to their rank values in the game.
     *
     * @return A {@code Map<Player, Integer>} where {@code Player} represents a player entity
     *         and {@code Integer} represents the player's position or score in the ranking.
     */
    public Map<Player, Integer> getRanking() {
        return ranking;
    }

    /**
     * Adds a player to the outPlayers set and removes them from the ranking.
     *
     * @param player The player to be marked as out.
     */
    public void addOutPlayer(Player player, boolean isLandingVoluntarily) {
        outPlayers.add(player);
        ranking.remove(player);
        player.setEarlyLanded(true);

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            if(isLandingVoluntarily)
                clientController.notifyPlayerEarlyLanded(nicknameToNotify, player.getNickname());
            else
                clientController.notifyEliminatedPlayer(nicknameToNotify, player.getNickname());
        });
    }

    /**
     * Identifies and removes players from the ranking whose position difference
     * from the top-ranked player exceeds the predefined run length.
     * These players are added to the `outPlayers` set.
     * <p>
     * The method performs the following:
     * 1. Determines the maximum position in the ranking.
     * 2. Filters players whose position difference from the maximum position
     *    is greater than the `runLenght`.
     * 3. Adds the filtered players to the `outPlayers` set.
     * 4. Removes the filtered players from the ranking map.
     */
    public void getDoubledPlayers() {
        int maxPosition = Collections.max(ranking.values());

        List<Player> playersToRemove = ranking.keySet()
                .stream()
                .filter(player -> maxPosition - ranking.get(player) > runLenght)
                .toList();

        playersToRemove.forEach(player -> addOutPlayer(player, false));
        playersToRemove.forEach(ranking.keySet()::remove);
    }

    /**
     * Moves a player by a specified offset, ensuring the new position is not already occupied.
     * This method is synchronized to prevent race conditions when multiple threads attempt
     * to move players simultaneously.
     *
     * @param player The player to be moved.
     * @param offset The number of positions to move (can be positive or negative).
     */
    public synchronized void movePlayer(Player player, int offset){
        int currentPosition = ranking.get(player);
        int newPosition = currentPosition + offset;

        while (checkPosition(newPosition)) {
            if (offset > 0) {
                newPosition++;
            } else {
                newPosition--;
            }
        }
        ranking.put(player, newPosition);

        int finalNewPosition = newPosition;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRankingUpdate(nicknameToNotify,player.getNickname(), finalNewPosition);
        });

    }

    /**
     * Checks if a specified position is already occupied by another player.
     *
     * @param newPosition The position to check.
     * @return True if the position is occupied, false otherwise.
     */
    private boolean checkPosition(int newPosition) {
        return ranking.containsValue(newPosition);
    }

    /**
     * Returns the current ranking of players sorted in descending order based on their positions.
     *
     * @return A list of players sorted from highest to lowest position.
     */
    public List<Player> getCurrentRanking() {
        return ranking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Calculates and retrieves the credits assigned to a player based on their position
     * or rank on the board. This method determines the appropriate credit allocation
     * for the specified player.
     *
     * @param player The player whose credits are to be determined based on their position.
     * @return The number of credits assigned to the given player.
     */
    @JsonIgnore
    public abstract int getCreditsForPosition(Player player);

    /**
     * Retrieves the reward associated with the prettiest ship in the game.
     * This method determines and returns an integer value representing
     * the reward, which may be used in the game dynamics for incentives
     * or ranking purposes.
     *
     * @return An integer value representing the reward for the prettiest ship.
     */
    @JsonIgnore
    public abstract int getPrettiestShipReward();

}
