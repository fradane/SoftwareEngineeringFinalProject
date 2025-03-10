package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.Optional;

public class Planets extends AdventureCard implements cargoCubesHandler, playerMover {

    private ArrayList<Planet> availablePlanets;
    private int stepsBack;

    @Override
    public void effect(Game game){

        ArrayList<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

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
