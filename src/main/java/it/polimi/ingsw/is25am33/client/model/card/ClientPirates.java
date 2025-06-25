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

    @Override
    public boolean hasReward() {
        return true;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│              PIRATES               │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Required Fire Power:      x%-8d │\n", requiredFirePower));
        sb.append(String.format("│ Flight Days Cost:         %-8d │\n", stepsBack));
        sb.append(String.format("│ Cosmic Credits Reward:    %-8d │\n", reward));
        sb.append(String.format("│ Number of Shots:          %-8d │\n", shots != null ? shots.size() : 0));
        sb.append("└────────────────────────────────────┘\n");
        
        if (shots != null && !shots.isEmpty()) {
            sb.append("Shot Details:\n");
            for (int i = 0; i < Math.min(shots.size(), 3); i++) {
                ClientDangerousObject shot = shots.get(i);
                sb.append(String.format("  %d. %s from %s\n", i + 1, shot.getType(), shot.getDirection()));
            }
            if (shots.size() > 3) {
                sb.append(String.format("  ... and %d more shots\n", shots.size() - 3));
            }
            sb.append("\n");
        }
        
        sb.append("Effects: Advanced enemies that attack with multiple cannon shots.\n");
        sb.append("Victory grants cosmic credits, but defeat means suffering heavy and\n");
        sb.append("light cannon attacks. Only one player can exploit this opportunity.\n");
        sb.append("You can choose to forfeit credits to avoid flight days penalty.");
        
        return sb.toString();
    }
}
