package it.polimi.ingsw.is25am33.model;

import java.util.Comparator;

public enum CargoCube {

    RED(4),
    YELLOW(3),
    GREEN(2),
    BLUE(1);

    private final int value;

    CargoCube(int value){
        this.value = value;
    }

    public int getValue() {
        return this.value;
    };

    public static final Comparator<CargoCube> byValue = Comparator.comparing(CargoCube::getValue);

}
