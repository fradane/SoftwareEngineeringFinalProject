package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.component.Component;

public class GameEvent {
    private final String eventID;
    private final DTO gameDTO;

    public GameEvent(String eventID, DTO gameDTO) {
        this.eventID = eventID;
        this.gameDTO  = gameDTO;
    }

    public String getEventID() {
        return eventID;
    }

    public DTO getDTO() {
        return gameDTO;
    }
}