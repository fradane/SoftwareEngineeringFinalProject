package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Planet {

    private boolean isBusy;
    private final List<CargoCube> reward;
    private final Iterator<CargoCube> rewardIterator;

    public Planet(List<CargoCube> cargoCubes) {
        this.isBusy = false;
        this.reward = cargoCubes;
        rewardIterator = reward.iterator();
    }

    public CargoCube getCurrent() {
        return rewardIterator.next();
    }

    public boolean hasNext() {
        return rewardIterator.hasNext();
    }

    public void noMoreAvailable() {
        isBusy = true;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public List<CargoCube> getReward() {
        return reward;
    }
}
