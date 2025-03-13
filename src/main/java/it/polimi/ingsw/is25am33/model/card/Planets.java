package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;
import java.util.ArrayList;
import java.util.List;

public class Planets extends AdventureCard implements cargoCubesHandler, playerMover {

    private List<Planet> availablePlanets;
    private int stepsBack;

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    @Override
    public void effect(Game game){

        List<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

        for (Player p : playersRanking) {

            game.getController().getPreferredPlanetIndex(p, availablePlanets)
                    .ifPresent(i -> {

                        availablePlanets.get(i).noMoreAvailable();
                        movePlayer(game.getFlyingBoard(), p, stepsBack);
                        Planets.this.handleCargoCubesReward(availablePlanets.get(i).getReward(), p);

                    });

        }

    }

}
