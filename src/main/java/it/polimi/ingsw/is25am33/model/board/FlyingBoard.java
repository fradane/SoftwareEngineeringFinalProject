package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class representing a FlyingBoard that keeps track of players and their positions.
 */
public abstract class FlyingBoard {
    private Set<Player> outPlayers;
    protected int runLenght;
    protected Map<Player, Integer> ranking;
    protected GameClientNotifier gameClientNotifier;

    /**
     * Constructor to initialize runLength, outPlayers, and ranking.
     *
     * @param runLength The length difference after which a player is considered doubled.
     */
    public FlyingBoard(int runLength) {
        this.runLenght = runLength;
        this.outPlayers = new HashSet<>();
        this.ranking = new HashMap<>();
    }

    public void setGameContext(GameClientNotifier gameClientNotifier) {
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
     * Adds a player to the outPlayers set and removes them from the ranking.
     *
     * @param player The player to be marked as out.
     */
    public void addOutPlayer(Player player) {
        outPlayers.add(player);
        ranking.remove(player);

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyEliminatedPlayer(nicknameToNotify, player.getNickname());
        });
    }

    @JsonIgnore
    public int getRunLenght() {
        return runLenght;
    }

    public Map<Player, Integer> getRanking() {
        return ranking;
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
                .collect(Collectors.toList());

        playersToRemove.forEach(this::addOutPlayer);
        ranking.keySet().removeAll(playersToRemove);
    }

    /**
     * Moves a player by a specified offset, ensuring the new position is not already occupied.
     *
     * @param player The player to be moved.
     * @param offset The number of positions to move (can be positive or negative).
     */
    public void movePlayer(Player player, int offset){
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
    public boolean checkPosition(int newPosition) {
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

    @JsonIgnore
    public abstract int getCreditsForPosition(Player player);

    @JsonIgnore
    public abstract int getPrettiestShipReward();

}
