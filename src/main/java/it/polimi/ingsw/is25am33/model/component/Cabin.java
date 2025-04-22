package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents a cabin component capable of housing crew members.
 */
public class Cabin extends Component {

    /** List containing crew members currently inhabiting this cabin. */
    private List<CrewMember> inhabitants;

    /**
     * Default constructor for {@code Cabin}.
     */
    public Cabin() {
        type = "Cabin";
    }

    /**
     * Constructs a Cabin with specified connectors and initializes an empty inhabitants list.
     *
     * @param connectors a map associating directions with connector types
     */
    public Cabin(Map<Direction, ConnectorType> connectors) {
        super(connectors);
        inhabitants = new ArrayList<CrewMember>();
    }

    /**
     * Retrieves the list of crew members currently inhabiting the cabin.
     *
     * @return the list of inhabitants
     */
    public List<CrewMember> getInhabitants() {
        return inhabitants;
    }

    /**
     * Adds a crew member to the cabin. If the crew member is HUMAN, two members are added instead of one.
     *
     * @param member the crew member to add to the cabin
     */
    public void fillCabin(CrewMember member){
        if (member.equals(CrewMember.HUMAN)) {
            inhabitants.add(member);
            inhabitants.add(member);
        } else {
            inhabitants.add(member);
        }
//        notifyObservers(new ComponentEvent(this, "inhabitants", inhabitants ));
    }

    /**
     * Removes the first crew member from the cabin's inhabitants list.
     *
     * @throws java.util.NoSuchElementException if the cabin has no inhabitants
     */
    public void removeMember() throws NoSuchElementException {

        if (inhabitants.isEmpty())
            throw new NoSuchElementException("Empty cabin");

        inhabitants.removeFirst();
//        notifyObservers(new ComponentEvent(this, "inhabitants", inhabitants ));
    }

    /**
     * Checks whether the cabin currently has any inhabitants.
     *
     * @return {@code true} if the cabin has at least one inhabitant; {@code false} otherwise
     */
    public boolean hasInhabitants() {
        return !inhabitants.isEmpty();
    }
}
