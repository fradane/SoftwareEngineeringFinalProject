package it.polimi.ingsw.is25am33.model.enumFiles;

import java.util.Comparator;

/**
 * Represents the different types of cargo cubes in the game.
 * Each cube has an associated value, with higher values typically
 * indicating greater rarity or worth in the game.
 */
public enum CargoCube {

    /**
     * Red cargo cube with the highest value of 4.
     */
    RED(4),

    /**
     * Yellow cargo cube with value 3.
     */
    YELLOW(3),

    /**
     * Green cargo cube with value 2.
     */
    GREEN(2),

    /**
     * Blue cargo cube with the lowest value of 1.
     */
    BLUE(1);

    private final int value;

    /**
     * Constructs a cargo cube with the specified value.
     *
     * @param value the numerical value of this cargo cube
     */
    CargoCube(int value){
        this.value = value;
    }

    /**
     * Returns the numerical value of this cargo cube.
     *
     * @return the value associated with this cargo cube
     */
    public int getValue() {
        return this.value;
    };

    /**
     * A comparator that orders cargo cubes based on their numerical values.
     * Can be used to sort collections of cargo cubes.
     */
    public static final Comparator<CargoCube> byValue = Comparator.comparing(CargoCube::getValue);

}