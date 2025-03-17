package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class LifeSupport extends Component{
    private final ColorLifeSupport color;
    public LifeSupport(Map<Direction, ConnectorType> connectors, ColorLifeSupport color) {
        super(connectors);
        this.color = color;
    }
    public ColorLifeSupport getLifeSupportColor() {
        return color;
    }
}
