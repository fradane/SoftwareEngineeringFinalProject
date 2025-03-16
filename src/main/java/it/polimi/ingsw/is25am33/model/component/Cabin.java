package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.crew.CrewMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cabin extends Component{
    private List<CrewMember> inhabitants;

    public Cabin(Map<Direction, ConnectorType> connectors) {
        super(connectors);
        inhabitants = new ArrayList<CrewMember>();
    }
    public List<CrewMember> getInhabitants() {
        return inhabitants;
    }
    public void addMember(CrewMember member) throws IllegalStateException {
        if(inhabitants.size()==2)
            throw new IllegalStateException("Full cabin");
        else inhabitants.add(member);
    }
    public void removeMember(CrewMember member) {
        inhabitants.remove(member);
    }
    public boolean hasInhabitants() {
        return !inhabitants.isEmpty();
    }
}
