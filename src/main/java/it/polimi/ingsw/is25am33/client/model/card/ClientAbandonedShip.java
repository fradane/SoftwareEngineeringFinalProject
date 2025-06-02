package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientAbandonedShip extends ClientCard implements Serializable, CrewMalusCard {
    private int crewMalus;
    private int stepsBack;
    private int reward;

    public ClientAbandonedShip() {
        super();
    }

    public ClientAbandonedShip(String cardName, String imageName, int crewMalus, int stepsBack, int reward) {
        super(cardName, imageName);
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.reward = reward;
    }

    // Getters
    @Override
    public int getCrewMalus() {
        return crewMalus;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getReward() {
        return reward;
    }

    // Setters
    @Override
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
    public CardState getFirstState() {
        return CardState.VISIT_LOCATION;
    }

    @Override
    public String getCardType() {
        return "AbandonedShip";
    }

    @Override
    public boolean hasReward() {
        return true;
    }

    @Override
    public boolean hasStepsBack() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("""
           %s
           ┌────────────────────────────┐
           │       Abandoned Ship       │
           ├────────────────────────────┤
           │ Crew Lost:         x%-2d     │
           │ Reward:            x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """, imageName, crewMalus, reward, stepsBack);
    }
}
