package it.polimi.ingsw.is25am33.model.card.interfaces;

import it.polimi.ingsw.is25am33.model.component.Cabin;
import java.util.List;

public interface CrewMemberRemover {

    /**
     * Removes crew members from the specified cabins while ensuring there are enough crew members
     * in each cabin to allow the operation. If the removal count for any cabin exceeds its inhabitants,
     * an {@code IllegalArgumentException} is thrown.
     *
     * @param chosenCabins the list of cabins from which crew members will be removed
     * @param crewMalus an integer representing a penalty or additional constraint applied during the process
     * @throws IllegalArgumentException if the required number of crew members in a cabin is insufficient
     */
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
