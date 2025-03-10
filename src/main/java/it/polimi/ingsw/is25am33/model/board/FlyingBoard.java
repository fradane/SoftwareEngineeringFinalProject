package it.polimi.ingsw.is25am33.model.board;

import java.util.*;
import java.util.stream.Collectors;

public abstract class FlyingBoard {
    private Set<Player> outPlayers;  // Set to store players who have been 'doubled'
    private int runLenght;
    private Map<Player, Integer> ranking;  // A map to store players and their positions in the ranking

    // Constructor to initialize runLength, outPlayers, and ranking
    // Constructor to initialize runLength, outPlayers, and ranking
    public FlyingBoard(int runLength) {
        this.runLenght = runLength;
        this.outPlayers = new HashSet<>();
        this.ranking = new HashMap<>();
    }

    public void insertPlayer (Player player, Integer pos){
        ranking.put(player, pos);
    }

    // Returns the set of outPlayers (players who have been doubled)
    public Set<Player> getOutPlayers() {
        return outPlayers;
    }

    // Adds a player to the outPlayers set and returns the updated set
    public void addOutPlayer(Player player) {
        outPlayers.add(player);
        ranking.remove(player);
    }

    // Returns the players who have been "doubled" based on their positions in the ranking, which are added to outPlayer
    public List<Player> getDoubledPlayers() {
        // Find the maximum position in the ranking
        int maxPosition = Collections.max(ranking.values());

        // Find the players who have been doubled by comparing their positions with the max position
        List<Player> playersToRemove = ranking.keySet()
                .stream()
                .filter(player -> maxPosition - ranking.get(player) > runLenght)
                .collect(Collectors.toList());
        outPlayers.addAll(playersToRemove);  // Add the doubled players to outPlayers
        ranking.keySet().removeAll(playersToRemove);  // Remove the doubled players from the ranking

        return playersToRemove;  // Return the new players that are out of the game
    }

    public void movePlayer(Player player, int offset) {
        int currentPosition = ranking.get(player);
        int newPosition = currentPosition + offset;
        ranking.put(player, newPosition);
    }

    public ArrayList<Player> getCurrentRanking() {
        return new ArrayList<>(ranking.keySet().stream()
                .sorted(Comparator.comparing(ranking::get).reversed())
                .collect(Collectors.toList()));
    }
}




