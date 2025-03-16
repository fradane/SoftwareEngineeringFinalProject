package it.polimi.ingsw.is25am33.model.crew;
import it.polimi.ingsw.is25am33.model.component.Cabin;

public abstract class CrewMember {
    private Cabin itsCabin;
    public CrewMember(Cabin itsCabin) {
        this.itsCabin = itsCabin;
    }
}
