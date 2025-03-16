package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class DoubleEngine extends Engine implements Activable{
    public DoubleEngine(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }
}
