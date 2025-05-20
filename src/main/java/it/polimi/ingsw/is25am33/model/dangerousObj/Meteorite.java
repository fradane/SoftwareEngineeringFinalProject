package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.card.interfaces.ShotSenderCard;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public abstract class Meteorite extends DangerousObj {

    public Meteorite() {
        super();
    }

    public Meteorite(Direction direction) {
        super(direction);
    }

    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, MeteoriteStorm card);
}
