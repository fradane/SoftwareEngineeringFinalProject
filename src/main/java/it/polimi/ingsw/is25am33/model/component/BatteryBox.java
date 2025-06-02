package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.io.Serializable;
import java.util.Map;


/**
 * Represents a battery container component that stores energy units used to power other components.
 */
public class BatteryBox extends Component{

    /** The maximum battery capacity of this battery box. */
    private int maxBatteryCapacity;

    /** The number of currently available battery units. */
    private int remainingBatteries;

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
        this.remainingBatteries = maxBatteryCapacity;
    }


    @Override
    public String toString() {
        String north = getConnectors().get(Direction.NORTH) != null
                ? String.valueOf(getConnectors().get(Direction.NORTH).fromConnectorTypeToValue())
                : " ";
        String south = getConnectors().get(Direction.SOUTH) != null
                ? String.valueOf(getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue())
                : " ";
        String west  = getConnectors().get(Direction.WEST) != null
                ? String.valueOf(getConnectors().get(Direction.WEST).fromConnectorTypeToValue())
                : " ";
        String east  = getConnectors().get(Direction.EAST) != null
                ? String.valueOf(getConnectors().get(Direction.EAST).fromConnectorTypeToValue())
                : " ";

        return String.format("""
            %s
            BatteryBox
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            maxBatteryCapacity: %d
            """, imageName, north, west, east, south, maxBatteryCapacity);
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
    public int getRemainingBatteries() {
        return remainingBatteries;
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
        if (remainingBatteries == 0) {
            throw new IllegalStateException("empty battery box");
        }
        remainingBatteries--;
//        notifyObservers(new ComponentEvent(this, "availableBattery", availableBattery ));
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "BBX";
    }

    @Override
    @JsonIgnore
    public String getMainAttribute() {
        return Integer.toString(remainingBatteries);
    }

}
