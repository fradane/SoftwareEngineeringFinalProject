package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.component.Cabin;

import java.util.Collections;
import java.util.List;

public interface CrewMemberRemover {

    default void removeMemberProcess(List<Cabin> chosenCabins, int crewMalus) throws IllegalArgumentException {

        if (chosenCabins.size() != crewMalus)
            throw new IllegalArgumentException("Not the right amount of crew members");

        chosenCabins.stream().distinct().forEach(cabin -> {
            if (Collections.frequency(chosenCabins, cabin) > cabin.getInhabitants().size())
                throw new IllegalArgumentException("The number of required crew members is not enough");
        });

        chosenCabins.forEach(Cabin::removeMember);

    }

}
