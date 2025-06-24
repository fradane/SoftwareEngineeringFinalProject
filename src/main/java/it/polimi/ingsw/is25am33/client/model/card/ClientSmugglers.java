package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.util.List;

public class ClientSmugglers extends ClientCard {

    private int requiredFirePower;
    private List<CargoCube> reward;
    private int stepsBack;
    private int cubeMalus;

    public ClientSmugglers() {}

    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    public ClientSmugglers(String cardName, String imageName, int requiredFirePower, List<CargoCube> reward, int stepsBack, int cubeMalus) {
        super(cardName, imageName);
        this.requiredFirePower=requiredFirePower;
        this.reward=reward;
        this.cubeMalus=cubeMalus;
        this.stepsBack=stepsBack;
    }
    @Override
    public String getCardType() {
        return "Smugglers";
    }

    public int getCubeMalus() {
        return cubeMalus;
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public int getRequiredFirePower() {
        return requiredFirePower;
    }

    public void setRequiredFirePower(int requiredFirePower) {
        this.requiredFirePower = requiredFirePower;
    }

    public List<CargoCube> getReward() {
        return reward;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    @Override
    public boolean hasReward() {
        return true;
    }

}
