package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.*;

/**
 * The {@code Level2FlyingBoard} class extends {@code FlyingBoard} to represent
 * a specialized flying board with an additional hourglass mechanic.
 * <p>
 * This class manages the state and behavior of a level 2 flying board,
 * including position tracking for the hourglass and a custom credit distribution
 * system based on player ranking.
 */
public class Level2FlyingBoard extends FlyingBoard {

    /**
     * The current position of the hourglass on this board.
     */
    private int hourglassPosition;

    /**
     * The constant reward value assigned for having the "prettiest ship" in the game.
     * This value is used in determining additional credits or perks for players
     * based on aesthetic judgment criteria.
     *
     * It signifies a fixed reward amount of 4, emphasizing consistency across all
     * evaluations related to this designation.
     */
    private static final int prettiestShipReward = 4;

    /**
     * A fixed list of credits awarded to players based on their final ranking.
     * The first player receives the first value, the second receives the second, and so on.
     */
    private final static List<Integer> credits = List.of(12, 9, 6, 3);

    /**
     * An iterator over the initial positions for the {@code Level2FlyingBoard}.
     * Used to provide sequential access to predefined starting positions
     * for players or other components in the game.
     * This iterator ensures controlled and ordered traversal,
     * adhering to the board's initial configuration setup.
     */
    private final Iterator<Integer> initialPositionIterator;
    /**
     * A fixed list of initial positions that are used to set up certain game elements
     * or players at the start of the game in the {@code Level2FlyingBoard}.
     * The list contains predefined integer values representing specific starting positions.
     */
    private final static List<Integer> initialPositions = List.of(6, 3, 1, 0);

    /**
     * Constructs a new {@code Level2FlyingBoard} with:
     * <ul>
     *     <li>A run length of 34 units (passed to the superclass constructor).</li>
     *     <li>The hourglass positioned at the start (position 0).</li>
     *     <li>A fixed list of credits awarded based on player ranking: 12, 9, 6, 3.</li>
     * </ul>
     */
    public Level2FlyingBoard() {
        super(24);
        this.hourglassPosition = 0;
        this.initialPositionIterator = initialPositions.iterator();
    }

    /**
     * Retrieves the reward value for the prettiest ship in the game.
     *
     * @return the reward value as an integer for the prettiest ship.
     */
    @JsonIgnore
    @Override
    public int getPrettiestShipReward() {
        return prettiestShipReward;
    }

    /**
     * Returns the number of credits assigned to a player based on their current ranking.
     * The ranking is determined by the {@code getCurrentRanking()} method.
     *
     * @param player the player whose credit score is to be retrieved
     * @return the number of credits assigned to the player based on their ranking
     * @throws IndexOutOfBoundsException if the player is not in the current ranking
     */
    @Override
    public int getCreditsForPosition(Player player) {
        int index = getCurrentRanking().indexOf(player);
        return index >= 0 ? credits.get(index) : 0;
    }

    /**
     * Inserts a player into the ranking system and assigns them an initial position.
     * Notifies all connected game clients about the updated ranking.
     *
     * @param player the {@code Player} object to be inserted into the ranking
     * @return the current size of the ranking after the player is added
     */
    @Override
    public int insertPlayer(Player player) {

        synchronized (ranking) {
            int initialPosition = initialPositionIterator.next();
            ranking.put(player, initialPosition);

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyRankingUpdate(nicknameToNotify, player.getNickname(), initialPosition);
            });

            return ranking.size();
        }

    }

}
