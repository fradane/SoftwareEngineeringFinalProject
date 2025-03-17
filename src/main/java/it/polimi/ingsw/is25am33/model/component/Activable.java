package it.polimi.ingsw.is25am33.model.component;

public interface Activable {
    default void turnOn(BatteryBox batteryBox){
        batteryBox.useBattery();
    }
}
