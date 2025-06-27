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

    @Override
    public boolean hasReward() {
        return true;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│           SLAVE TRADERS            │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Required Fire Power:      x%-8d │\n", requiredFirePower));
        sb.append(String.format("│ Flight Days Cost:         %-8d │\n", stepsBack));
        sb.append(String.format("│ Cosmic Credits Reward:    %-8d │\n", reward));
        sb.append(String.format("│ Crew Members Lost:        x%-8d │\n", crewMalus));
        sb.append("└────────────────────────────────────┘\n");
        sb.append("Effects: Advanced enemies that enslave crew members if you lose.\n");
        sb.append("Victory grants cosmic credits, but defeat forces you to give up\n");
        sb.append("crew members of your choice. Only one player can exploit this\n");
        sb.append("opportunity. You can forfeit credits to avoid flight days penalty.");
        
        return sb.toString();
    }
}
