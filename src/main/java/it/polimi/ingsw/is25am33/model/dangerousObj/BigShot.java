package it.polimi.ingsw.is25am33.model.dangerousObj;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;

import java.util.function.BiConsumer;

public class BigShot extends Shot {

    public BigShot(Direction direction) {
        super(direction);
        this.dangerousObjType = "bigShot";
    }

    public BigShot() {
        super();
        this.dangerousObjType = "bigShot";
    }

    @Override
    public void startAttack(PlayerChoicesDataStructure playerChoices, Pirates pirates) {
        pirates.playerIsAttackedByABigShot();
    }

    @Override
    public String getDangerousObjType() {
        return dangerousObjType;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view) {
        return view.showBigShotMenu();
    }

}
