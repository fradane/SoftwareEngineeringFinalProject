package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.List;

public class AbandonedStation extends AdventureCard implements cargoCubesHandler {

    private int stepsBack;
    private int requiredCrewMembers;
    private List<CargoCube> reward;

    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    @Override
    public void effect(Game game) {

        ArrayList<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();

        for (Player p : playersRanking) {

            if(p.getPersonalBoard().getCrewMembers().size() >= requiredCrewMembers &&
                    game.getController().wantsToStopOnAbandonedShip(p, this)) {

                this.handleCargoCubesReward(reward, p);
                game.getFlyingBoard().move(p, stepsBack);

                break;
            }

        }

    }

}
