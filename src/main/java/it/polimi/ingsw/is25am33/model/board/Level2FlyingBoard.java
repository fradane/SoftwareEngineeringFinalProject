package it.polimi.ingsw.is25am33.model.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.rmi.RemoteException;
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

    private static final int prettiestShipReward = 4;

    /**
     * A fixed list of credits awarded to players based on their final ranking.
     * The first player receives the first value, the second receives the second, and so on.
     */
    private final static List<Integer> credits = List.of(12, 9, 6, 3);

    private Iterator<Integer> initialPositionIterator;
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
        return credits.get(index);
    }

    @Override
    public void insertPlayer(Player player){
        try{
            ranking.put(player, initialPositionIterator.next());

            for (String s : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(s).notifyFlyingBoardUpdate(s,this);
            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }
    }
}
