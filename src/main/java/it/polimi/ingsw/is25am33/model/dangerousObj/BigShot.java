package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

public class BigShot extends Shot {

    public BigShot(Direction direction) {
        super(direction);
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, Pirates pirates) {
        pirates.playerIsAttackedByABigShot();
    }

}
