package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;

import java.util.ArrayList;
import java.util.List;

public class Planet {

    private boolean isBusy;
    private final List<CargoCube> reward;

    public Planet(List<CargoCube> cargoCubes) {
        this.isBusy = false;
        this.reward = cargoCubes;
    }

    public void noMoreAvailable() {
        isBusy = true;
    }

    public List<CargoCube> getReward() {
        return reward;
    }
}
