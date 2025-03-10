package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;

public class AbandonedShip extends AdventureCard implements playerMover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    @Override
    public void effect(Game game){

        ArrayList<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

        for (Player p : playersRanking) {

            if(game.getController().wantsToStopOnAbandonedShip(p, this)) {

                p.addCredits(reward);
                p.getPersonalBoard().removeCrewMembers(crewMalus);
                movePlayer(game.getFlyingBoard(), p, stepsBack);

                break;
            }

        }

    }

}
