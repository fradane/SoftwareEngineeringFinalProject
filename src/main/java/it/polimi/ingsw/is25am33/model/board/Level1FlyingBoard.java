package it.polimi.ingsw.is25am33.model.board;

import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The {@code Level1FlyingBoard} class extends {@code FlyingBoard} to represent
 * a basic flying board used in the first level of the game.
 * <p>
 * It features a fixed credit distribution and no hourglass mechanic.
 */
public class Level1FlyingBoard extends FlyingBoard {

    /**
     * A fixed list of credits awarded to players based on their final ranking.
     */
    private static final List<Integer> credits = List.of(4, 3, 2, 1);
    private static final int prettiestShipReward = 2;

    private Iterator<Integer> initialPositionIterator;
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
    }

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

    @Override
    public void insertPlayer(Player player) {
        ranking.put(player, initialPositionIterator.next());
    }
}
