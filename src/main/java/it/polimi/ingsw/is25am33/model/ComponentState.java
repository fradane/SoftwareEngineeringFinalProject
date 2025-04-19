package it.polimi.ingsw.is25am33.model;

public enum ComponentState {
    VISIBLE,    // it is visible on the board and pickable by any player
    BOOKED,     // it is booked on a specific player's shipBoard
    USED,       // TODO non so cosa sia
    HIDDEN;     // it is hidden on the table, pickable by any player
}
