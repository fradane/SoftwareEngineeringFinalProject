package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;


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

    public String getComponentName() {
        return "BatteryBox";
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

    @Override
    @NotNull
    @JsonIgnore
    public Integer getGuiHash() {
        return Objects.hash(imageName, remainingBatteries, getRotation());
    }
}
