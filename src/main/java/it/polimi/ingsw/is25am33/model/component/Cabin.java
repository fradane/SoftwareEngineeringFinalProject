package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.EnumMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a cabin component capable of housing crew members.
 */
public class Cabin extends Component{

    /** List containing crew members currently inhabiting this cabin. */
    private List<CrewMember> inhabitants = new ArrayList<>();

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
        inhabitants = new ArrayList<>();
    }

    public String getComponentName() {
        return "Cabin";
    }

    /**
     * Generates a hash code for the GUI representation of the cabin.
     * The hash code is computed based on the cabin's image name, rotation,
     * and its list of inhabitants (if not null).
     *
     * @return the hash code used for GUI representation
     */
    @Override
    @JsonIgnore
    @NotNull
    public Integer getGuiHash() {
        return inhabitants ==  null ?
                Objects.hash(imageName, getRotation()) :
                Objects.hash(imageName, inhabitants, getRotation());
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
    }

    /**
     * Removes the first member from the list of inhabitants currently in the cabin.
     *
     * This method checks if the list of inhabitants is empty before attempting to
     * remove a member. If the list is empty, a {@code NoSuchElementException} is thrown
     * to indicate that no members can be removed.
     *
     * @throws NoSuchElementException if the cabin has no inhabitants
     */
    public void removeMember() throws NoSuchElementException {

        if (inhabitants.isEmpty())
            throw new NoSuchElementException("Empty cabin");

        inhabitants.removeFirst();
    }

    /**
     * Checks whether the cabin currently has any inhabitants.
     *
     * @return {@code true} if the cabin has at least one inhabitant; {@code false} otherwise
     */
    public boolean hasInhabitants() {
        return !inhabitants.isEmpty();
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        return "CAB";
    }

    /**
     * Retrieves a string representation of the main attribute for the cabin component.
     * The main attribute represents the inhabitants of the cabin.
     * If the cabin is empty, the method returns "--".
     * Otherwise, it concatenates a mapping of crew member types defined as:
     * HUMAN -> "H", PURPLE_ALIEN -> "P", BROWN_ALIEN -> "B".
     *
     * @return a string representing the main attribute of the cabin
     */
    @Override
    @JsonIgnore
    public String getMainAttribute() {
        if (!hasInhabitants()) return "--";
        return inhabitants.stream()
                .map(crewMember -> switch (crewMember) {
                    case HUMAN -> "H";
                    case PURPLE_ALIEN -> "P";
                    case BROWN_ALIEN -> "B";
                })
                .collect(Collectors.joining());
    }

}
