package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.CrewMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cabin extends Component{
    private final List<CrewMember> inhabitants;

    public Cabin(Map<Direction, ConnectorType> connectors) {
        super(connectors);
        inhabitants = new ArrayList<CrewMember>();
    }

    public List<CrewMember> getInhabitants() {
        return inhabitants;
    }

    public void fillCabin(CrewMember member){
        if(member.equals(CrewMember.HUMAN)) {
            inhabitants.add(member);
            inhabitants.add(member);
        }
        else inhabitants.add(member);
    }

    public void removeMember() {
        inhabitants.removeFirst();
    }

    public boolean hasInhabitants() {
        return !inhabitants.isEmpty();
    }

}
