package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.card.WarField;

public abstract class Shot extends DangerousObj {

    public Shot(Direction direction) {
        super(direction);
    }

    public abstract void startAttack(PlayerChoicesDataStructure playerChoices, Pirates pirates);

}
