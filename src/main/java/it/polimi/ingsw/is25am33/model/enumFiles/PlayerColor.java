package it.polimi.ingsw.is25am33.model.enumFiles;

/**
 * Represents the different colors that can be assigned to players in the game.
 * Each color is associated with a unique integer value.
 */
public enum PlayerColor {
    /**
     * Red player color with value 1.
     */
    RED(1),

    /**
     * Blue player color with value 2.
     */
    BLUE(2),

    /**
     * Green player color with value 3.
     */
    GREEN(3),

    /**
     * Yellow player color with value 4.
     */
    YELLOW(4);

    private final int number;

    /**
     * Constructs a player color with the specified numeric value.
     *
     * @param number the numeric value associated with this player color
     */
    PlayerColor(int number) {
        this.number = number;
    }

    /**
     * Returns the numeric value associated with this player color.
     *
     * @return the numeric value of this player color
     */
    public int getNumber() {
        return number;
    }

    /**
     * Retrieves a player color based on its numeric value.
     *
     * @param number the numeric value to search for
     * @return the PlayerColor associated with the given number, or null if no match is found
     */
    public static PlayerColor getPlayerColor(int number){
        for(PlayerColor playerColor : PlayerColor.values()){
            if(playerColor.getNumber() == number)
                return playerColor;
        }
        return null;
    }
}