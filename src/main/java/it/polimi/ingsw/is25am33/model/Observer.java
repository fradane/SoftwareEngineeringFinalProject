package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.game.GameEvent;

public interface Observer {
    void notify(GameEvent event);
    void notifyCurrAdventureCard(String message);
    String getId();
}
