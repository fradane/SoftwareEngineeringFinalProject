package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class representing a FlyingBoard that keeps track of players and their positions.
 */
public abstract class FlyingBoard {
    private Set<Player> outPlayers;  // Set to store players who have been 'doubled'
    private int runLenght;
    private Map<Player, Integer> ranking;  // A map to store players and their positions in the ranking

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

    /**
     * Inserts a player into the ranking with a specified position.
     *
     * @param player The player to be inserted.
     * @param pos The position at which the player is placed.
     */
    public void insertPlayer(Player player, Integer pos) {
        ranking.put(player, pos);
    }

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
     * @return A set of players who are out of the game.
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
    }

    /**
     * Identifies and returns the players who have been doubled, based on their position.
     * Doubled players are added to the outPlayers set and removed from the ranking.
     *
     * @return A list of players who are out of the game.
     */
    public List<Player> getDoubledPlayers() {
        int maxPosition = Collections.max(ranking.values());

        List<Player> playersToRemove = ranking.keySet()
                .stream()
                .filter(player -> maxPosition - ranking.get(player) > runLenght)
                .collect(Collectors.toList());

        outPlayers.addAll(playersToRemove);
        ranking.keySet().removeAll(playersToRemove);

        return playersToRemove;
    }

    /**
     * Moves a player by a specified offset, ensuring the new position is not already occupied.
     *
     * @param player The player to be moved.
     * @param offset The number of positions to move (can be positive or negative).
     */
    public void movePlayer(Player player, int offset) {
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
    public ArrayList<Player> getCurrentRanking() {
        return new ArrayList<>(ranking.keySet().stream()
                .sorted(Comparator.comparing(ranking::get).reversed())
                .collect(Collectors.toList()));
    }
}
