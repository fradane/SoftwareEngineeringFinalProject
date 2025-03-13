package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;

public class MeteoriteStorm extends AdventureCard{

    private List<Meteorite> meteorites;

    public void setMeteorites(List<Meteorite> meteorites) {
        this.meteorites = meteorites;
    }


    @Override
    public void effect(Game game) {

        for (Meteorite m : meteorites) {

            m.setCoordinates(Game.throwDices());
            game.getPlayers()
                    .stream()
                    .forEach(p -> p.getPersonalBoard().handleDangerousObj(m));

        }

    }
}
