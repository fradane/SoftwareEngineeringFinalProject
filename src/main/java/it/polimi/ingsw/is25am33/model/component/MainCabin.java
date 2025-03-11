package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.PlayerColor;

import java.util.Map;

public class MainCabin extends Cabin {
    private PlayerColor color;
    public MainCabin(Map<Direction, ConnectorType>connectors, PlayerColor color) {
        super(connectors);
        this.color = color;
    }
}
