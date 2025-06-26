package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.List;

public class ClientSlaveTraders extends ClientCard implements Serializable, CrewMalusCard {

    private int requiredFirePower;
    private int stepsBack;
    private int reward;
    private int crewMalus;

    public ClientSlaveTraders() {}

    public ClientSlaveTraders(String cardName, String imageName, int requiredFirePower, int reward, int crewMalus, int stepsBack) {
        super(cardName, imageName);
        this.requiredFirePower=requiredFirePower;
        this.reward=reward;
        this.crewMalus=crewMalus;
        this.stepsBack=stepsBack;
    }

    @Override
    public boolean hasReward() {
        return true;
    }

    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    public String getCardType() {
        return "SlaveTraders";
    }

    public int getRequiredFirePower() {
        return requiredFirePower;
    }

    public int getReward() {
        return reward;
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    public void setRequiredFirePower(int requiredFirePower) {
        this.requiredFirePower = requiredFirePower;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }
    public void setCrewMalus(int stepsBack) {
        this.crewMalus = stepsBack;
    }

    public int getStepsBack() {
        return stepsBack;
    }
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }
}
