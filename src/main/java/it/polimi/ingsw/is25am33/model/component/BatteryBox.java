package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class BatteryBox extends Component {
    private final int maxBatteryCapacity;
    private int availableBattery;
    public BatteryBox(Map<Direction, ConnectorType> connectors,int maxBatteryCapacity) {
        super(connectors);
        this.maxBatteryCapacity = maxBatteryCapacity;
        availableBattery = maxBatteryCapacity;
    }
    public int getMaxBatteryCapacity() {
        return maxBatteryCapacity;
    }

    public int getAvailableBattery() {
        return availableBattery;
    }
    public void useBattery() throws IllegalStateException {
        if(availableBattery==0) throw new IllegalStateException("empty battery box");
        availableBattery--;
    }
}
