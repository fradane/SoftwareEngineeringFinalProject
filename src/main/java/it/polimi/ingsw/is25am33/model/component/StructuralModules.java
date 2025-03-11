package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class StructuralModules extends Component {
    public StructuralModules(Map<Direction, ConnectorType> connectors) {
        super(connectors);
    }
}
