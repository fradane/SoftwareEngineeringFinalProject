package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;

import java.util.ArrayList;

public class Planet {

    private boolean isBusy;
    private ArrayList<CargoCube> reward;

    public void noMoreAvailable() {
        isBusy = true;
    }

    public ArrayList<CargoCube> getReward() {
        return reward;
    }
}
