package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.AlienColor;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class LifeSupport extends Component{
    private AlienColor componentColor;
    public LifeSupport(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }
    public AlienColor getComponentColor() {
        return componentColor;
    }
    public void setComponentColor(AlienColor componentColor) {
        this.componentColor = componentColor;
    }
}
