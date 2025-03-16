package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.Direction;

public interface Activable {
    default void turnOn(BatteryBox batteryBox){
        batteryBox.useBattery();
    }
}
