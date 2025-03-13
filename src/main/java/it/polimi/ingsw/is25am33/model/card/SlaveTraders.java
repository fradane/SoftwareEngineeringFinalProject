package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;

public class SlaveTraders extends AdvancedEnemies implements playerMover {

    private int crewMalus;

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    @Override
    public void effect(Game game) {

        List<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

        for (Player p : playersRanking) {

            int currPlayerTotalCannonPower = p.getPersonalBoard().countTotalCannonPower();

            if(currPlayerTotalCannonPower > requiredFirePower) {

                movePlayer(game.getFlyingBoard(), p, stepsBack);
                p.addCredits(reward);
                break;

            } else if(currPlayerTotalCannonPower < requiredFirePower) {

                p.getPersonalBoard().removeCrewMembers(crewMalus);

            }

        }
    }
}
