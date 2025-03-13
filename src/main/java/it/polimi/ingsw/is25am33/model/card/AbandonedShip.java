package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.game.Game;


public class AbandonedShip extends AdventureCard implements playerMover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    @Override
    public


    @Override
    public void effect(Game game){

        List<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

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
