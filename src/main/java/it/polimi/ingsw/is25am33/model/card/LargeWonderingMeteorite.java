package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;

import java.util.List;

public class LargeWonderingMeteorite extends MeteoriteStorm{

    public LargeWonderingMeteorite(List<Meteorite> meteorites) {
        super(meteorites);
        this.cardName = this.getClass().getSimpleName();
    }

}
