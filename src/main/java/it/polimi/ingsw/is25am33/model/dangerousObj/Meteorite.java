package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public abstract class Meteorite extends DangerousObj {
    public Meteorite(Direction direction) {
        super(direction);
    }

    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm meteoriteStorm);
}
