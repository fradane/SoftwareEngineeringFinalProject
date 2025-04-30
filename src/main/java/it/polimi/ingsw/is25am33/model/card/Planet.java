package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.util.Iterator;
import java.util.List;

public class Planet {

    private boolean isBusy = false;
    private List<CargoCube> reward;
    private Iterator<CargoCube> rewardIterator;

    public Planet(List<CargoCube> cargoCubes) {
        this.isBusy = false;
        this.reward = cargoCubes;
        rewardIterator = reward.iterator();
    }

    public Planet() {}

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    @JsonIgnore
    public CargoCube getCurrent() {
        return rewardIterator.next();
    }

    public boolean hasNext() {
        return rewardIterator.hasNext();
    }

    public void isNoMoreAvailable() {
        isBusy = true;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public List<CargoCube> getReward() {
        return reward;
    }
    
}
