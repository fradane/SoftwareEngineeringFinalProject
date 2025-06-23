package it.polimi.ingsw.is25am33.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.io.Serializable;
import java.util.List;

public class PlayerFinalData implements Serializable {
    private final String nickname;
    /**
     * Represents the total credits earned by the player at the end of the game.
     * This value is finalized when the game concludes and includes any bonuses or penalties
     * accrued during gameplay. It is a read-only value that reflects the player's financial
     * performance in the game session.
     */
    private final int totalCredits;
    /**
     * Indicates whether the player has landed early in the game.
     * This flag is used to track or manage game mechanics related to early landing events.
     */
    private final boolean isEarlyLanded;
    /**
     * Represents the collection of CargoCube owned by a player at the end of the game.
     * This list stores all the cubes collected by the player during gameplay, providing
     * insight into the resources accumulated and contributing to the player's final score.
     *
     * The list contains instances of the CargoCube enum, which categorizes the cubes by
     * type and assigns each a specific value.
     */
    private final List<CargoCube> allOwnedCubes;
    /**
     * Represents the total number of ship components that were lost by the player
     * during gameplay. This number reflects components that were destroyed,
     * misplaced, or otherwise removed from the player's possession.
     *
     * It is used to track and calculate game outcomes, penalties, or performance ratings
     * for the player based on the state of their ship at the end of the game.
     */
    private final int lostComponents;

    /**
     * Constructs a PlayerFinalData object with the specified total credits, early landing status,
     * owned cargo cubes, and number of lost components.
     *
     * @param totalCredits the total number of credits owned by the player
     * @param isEarlyLanded a boolean indicating whether the player finished their game with an early landing
     * @param allOwnedCubes the list of cargo cubes owned by the player
     * @param lostComponents the number of ship components lost by the player
     */
    @JsonCreator
    public PlayerFinalData(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("totalCredits") int totalCredits,
            @JsonProperty("isEarlyLanded") boolean isEarlyLanded,
            @JsonProperty("allOwnedCubes") List<CargoCube> allOwnedCubes,
            @JsonProperty("lostComponents") int lostComponents
    ) {
        this.nickname = nickname;
        this.totalCredits = totalCredits;
        this.allOwnedCubes = allOwnedCubes;
        this.lostComponents = lostComponents;
        this.isEarlyLanded = isEarlyLanded;
    }

    /**
     * Checks if the player has landed early.
     *
     * @return true if the player landed early, otherwise false.
     */
    @JsonProperty("isEarlyLanded")
    public boolean isEarlyLanded() {
        return isEarlyLanded;
    }

    /**
     * Retrieves the list of all CargoCubes currently owned.
     *
     * @return a list of CargoCube objects representing the owned cubes.
     */
    public List<CargoCube> getAllOwnedCubes() {
        return allOwnedCubes;
    }

    /**
     * Retrieves the number of lost components associated with the player.
     *
     * @return the number of lost components as an integer.
     */
    public int getLostComponents() {
        return lostComponents;
    }

    /**
     * Retrieves the total number of credits accumulated by the player at the end of the game.
     *
     * @return the total number of credits as an integer
     */
    public int getTotalCredits() {
        return totalCredits;
    }

    public String getNickname() {
        return nickname;
    }


}
