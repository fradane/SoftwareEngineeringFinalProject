package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.component.Component;

public class GameEvent {
    private String attribute;
    private DTO gameDTO;

    public GameEvent( String attribute, DTO gameDTO) {
        this.attribute = attribute;
        this.gameDTO  = gameDTO;
    }

    public String getAttribute() {
        return attribute;
    }

    public DTO getDTO() {
        return gameDTO;
    }
}
