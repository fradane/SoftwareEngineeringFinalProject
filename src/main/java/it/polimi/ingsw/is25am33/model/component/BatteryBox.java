package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.Map;

/**
 * Represents a battery container component that stores energy units used to power other components.
 */
public class BatteryBox extends Component {

    /** The maximum battery capacity of this battery box. */
    private int maxBatteryCapacity;

    /** The number of currently available battery units. */
    private int availableBattery;

    /**
     * Default constructor for {@code Battery}.
     */
    public BatteryBox(){
        type = "BatteryBox";
    }

    /**
     * Constructs a BatteryBox component with specified connectors and maximum battery capacity.
     *
     * @param connectors          a map associating directions with connector types
     * @param maxBatteryCapacity  the maximum number of battery units this battery box can hold
     */
    public BatteryBox(Map<Direction, ConnectorType> connectors, int maxBatteryCapacity) {
        super(connectors);
        this.maxBatteryCapacity = maxBatteryCapacity;
        this.availableBattery = maxBatteryCapacity;
    }

    @Override
    public String toString() {
        return "BatteryBox{" +
                "connectors" + this.getConnectors() +
                "maxBatteryCapacity=" + maxBatteryCapacity +
                ", availableBattery=" + availableBattery +
                '}';
    }

    /**
     * Returns the maximum battery capacity of this battery box.
     *
     * @return the maximum battery capacity
     */
    public int getMaxBatteryCapacity() {
        return maxBatteryCapacity;
    }

    /**
     * Returns the number of battery units currently available in this battery box.
     *
     * @return the available battery units
     */
    public int getAvailableBattery() {
        return availableBattery;
    }

    /**
     * Consumes one unit of battery from the battery box.
     *
     * @throws IllegalStateException if no battery units are available
     */

    /**
     * Sets the maximum battery capacity.
     *
     * @param maxBatteryCapacity the maximum capacity of the battery
     */
    public void setMaxBatteryCapacity(int maxBatteryCapacity) {
        this.maxBatteryCapacity = maxBatteryCapacity;
    }

    /**
     * Uses one unit of the available battery.
     *
     * @throws IllegalStateException if the battery is empty (availableBattery == 0)
     */
    public void useBattery() throws IllegalStateException {
        if (availableBattery == 0) {
            throw new IllegalStateException("empty battery box");
        }
        availableBattery--;
//        notifyObservers(new ComponentEvent(this, "avaiableBattery", availableBattery ));
    }

}
