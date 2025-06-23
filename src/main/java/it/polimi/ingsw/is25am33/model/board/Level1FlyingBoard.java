package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.Iterator;
import java.util.List;

/**
 * The {@code Level1FlyingBoard} class extends {@code FlyingBoard} to represent
 * a basic flying board used in the first level of the gameModel.
 * <p>
 * It features a fixed credit distribution and no hourglass mechanic.
 */
public class Level1FlyingBoard extends FlyingBoard {

    /**
     * A fixed list of credits awarded to players based on their final ranking.
     */
    private static final List<Integer> credits = List.of(4, 3, 2, 1);
    /**
     * Represents the fixed number of credits awarded to the player whose
     * ship is voted as the "prettiest" in the {@code Level1FlyingBoard}.
     * This reward is included as part of the scoring mechanism in the first-level
     * flying board gameplay.
     */
    private static final int prettiestShipReward = 2;

    /**
     * An iterator over a predefined list of initial positions used for assigning
     * positions to players when they are inserted into the ranking of a Level 1
     * flying board.
     *
     * This variable is initialized in the constructor with an iterator created from
     * {@code initialPositions}, a fixed list of integers. It is used to ensure that
     * players are assigned positions consistently and sequentially from the predefined
     * list of initial values.
     *
     * This iterator is primarily utilized in the {@code insertPlayer} method to determine
     * the initial position of a new player being added to the board's ranking.
     */
    private final Iterator<Integer> initialPositionIterator;
    /**
     * A fixed list of starting positions for players in the ranking.
     * This list determines the initial placement of players in the game
     * ranking when they are first added to the board. The order of the
     * integers in the list represents predefined positions, with each
     * value corresponding to a specific rank.
     */
    private final static List<Integer> initialPositions = List.of(4, 2, 1, 0);

    /**
     * Constructs a new {@code Level1FlyingBoard} with:
     * <ul>
     *     <li>A run length of 18 units (passed to the superclass constructor).</li>
     *     <li>A fixed list of credits awarded based on player ranking: 4, 3, 2, 1.</li>
     * </ul>
     */
    public Level1FlyingBoard() {
        super(18);
        initialPositionIterator = initialPositions.iterator();
    }

    /**
     * Returns the reward value associated with the prettiest ship in the game.
     * This value represents the amount of credits assigned to the player
     * who achieves the prettiest ship recognition.
     *
     * @return the reward value for the prettiest ship as an integer
     */
    @Override
    public int getPrettiestShipReward() {
        return prettiestShipReward;
    }

    /**
     * Returns the number of credits assigned to a player based on their current ranking.
     *
     * @param player the player whose credit score is to be retrieved
     * @return the number of credits assigned to the player
     * @throws IndexOutOfBoundsException if the player is not in the current ranking
     */
    @Override
    public int getCreditsForPosition(Player player) {
        int index = getCurrentRanking().indexOf(player);
        return credits.get(index);
    }

    /**
     * Inserts a player into the ranking system, assigns an initial position to the player,
     * notifies all connected clients about the ranking update, and returns the updated
     * size of the ranking. This method is synchronized on the ranking object to ensure
     * thread safety while modifying the ranking.
     *
     * @param player the player to be inserted into the ranking system
     * @return the updated size of the ranking after the player is inserted
     */
    @Override
    public int insertPlayer(Player player) {

        synchronized (ranking) {
            int initialPosition = initialPositionIterator.next();
            ranking.put(player, initialPosition);

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRankingUpdate(nicknameToNotify,player.getNickname(),initialPosition);
        });

            return ranking.size();

        }

    }
}
