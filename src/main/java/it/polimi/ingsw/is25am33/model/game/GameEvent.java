package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.component.Component;

public class GameEvent {
    private String eventId;
    private DTO gameDTO;

    public GameEvent( String eventId, DTO gameDTO) {
        this.eventId = eventId;
        this.gameDTO  = gameDTO;
    }

    public String getEventId() {
        return eventId;
    }

    public DTO getDTO() {
        return gameDTO;
    }
}
