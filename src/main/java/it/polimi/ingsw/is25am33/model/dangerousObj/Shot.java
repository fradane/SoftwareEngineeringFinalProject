package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public abstract class Shot extends DangerousObj {

    public Shot(Direction direction) {
        super(direction);
    }

    public Shot() {
        super();
    }

    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, Pirates pirates);

}
