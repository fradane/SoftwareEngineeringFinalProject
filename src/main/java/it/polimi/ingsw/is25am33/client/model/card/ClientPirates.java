package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.List;

public class ClientPirates extends ClientCard implements Serializable {

    private List<ClientDangerousObject> shots;
    private int requiredFirePower;
    private int reward;
    private int stepsBack;

    public ClientPirates() {}

    public ClientPirates(String cardName, String imageName, List<ClientDangerousObject> shots, int requiredFirePower, int reward, int stepsBack) {
        super(cardName, imageName);
        this.shots=shots;
        this.requiredFirePower=requiredFirePower;
        this.reward=reward;
        this.stepsBack=stepsBack;
    }

    public List<ClientDangerousObject> getShots() {
        return shots;
    }

    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    public String getCardType() {
        return "Pirates";
    }

    public int getDangerousObjCount() {
        return shots.size();
    }

    public int getRequiredFirePower() {
        return requiredFirePower;
    }

    public int getReward() {
        return reward;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public void setShots(List<ClientDangerousObject> shots) {
        this.shots = shots;
    }
    public void setRequiredFirePower(int requiredFirePower) {
        this.requiredFirePower = requiredFirePower;
    }
    public void setReward(int reward) {
        this.reward = reward;
    }
    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }
}
