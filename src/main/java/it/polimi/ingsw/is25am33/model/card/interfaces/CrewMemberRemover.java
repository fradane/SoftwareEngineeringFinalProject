package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.component.Cabin;

import java.util.Collections;
import java.util.List;

public interface CrewMemberRemover {

    default void removeMemberProcess(List<Cabin> chosenCabins, int crewMalus) throws IllegalArgumentException {

        chosenCabins.stream().distinct().forEach(cabin -> {
            long count = chosenCabins.stream()
                    .filter(c -> c == cabin)
                    .count();

            if (count > cabin.getInhabitants().size())
                throw new IllegalArgumentException("The number of required crew members is not enough");
        });
        chosenCabins.forEach(Cabin::removeMember);

    }

}
