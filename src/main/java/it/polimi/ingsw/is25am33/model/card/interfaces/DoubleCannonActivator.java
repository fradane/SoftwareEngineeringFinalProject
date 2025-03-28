package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.Collections;
import java.util.List;

public interface DoubleCannonActivator {

    default double activateDoubleCannonsProcess(List<Cannon> chosenDoubleCannons, List<BatteryBox> chosenBatteryBoxes, Player player) throws IllegalArgumentException, IllegalStateException {

        if (chosenDoubleCannons.size() != chosenBatteryBoxes.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        chosenBatteryBoxes.stream().distinct().forEach(box -> {
            if (Collections.frequency(chosenDoubleCannons, box) > box.getAvailableBattery())
                throw new IllegalArgumentException("The number of required batteries is not enough");
        });

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);

        return player.getPersonalBoard().countTotalFirePower(chosenDoubleCannons);

    }

}
