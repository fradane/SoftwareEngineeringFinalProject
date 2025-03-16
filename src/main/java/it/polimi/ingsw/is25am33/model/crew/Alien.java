package it.polimi.ingsw.is25am33.model.crew;
import it.polimi.ingsw.is25am33.model.AlienColor;
import it.polimi.ingsw.is25am33.model.component.Cabin;

public class Alien extends CrewMember{
    private AlienColor color;
    public Alien(Cabin cabin, AlienColor color) {
        super(cabin);
        this.color = color;
    }
}
