package it.polimi.ingsw.is25am33.model.component;

/**
 * Interface representing a component that can be activated using a battery.
 */
public interface Activable {
    /**
     * Activates the component using the provided BatteryBox.
     *
     * <p>Default implementation consumes a battery from the {@link BatteryBox}.</p>
     *
     * @param batteryBox the battery container supplying power to the component.
     */
    default void turnOn(BatteryBox batteryBox){
        batteryBox.useBattery();
    }
}
