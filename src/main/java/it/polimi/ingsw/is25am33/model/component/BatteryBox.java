package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

/**
 * Represents a battery container component that stores energy units used to power other components.
 */
public class BatteryBox extends Component {

    /** The maximum battery capacity of this battery box. */
    private final int maxBatteryCapacity;

    /** The number of currently available battery units. */
    private int availableBattery;

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
    public void useBattery() throws IllegalStateException {
        if (availableBattery == 0) {
            throw new IllegalStateException("empty battery box");
        }
        availableBattery--;
    }
}
