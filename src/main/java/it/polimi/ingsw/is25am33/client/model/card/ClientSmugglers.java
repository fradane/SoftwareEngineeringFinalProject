package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.util.List;

public class ClientSmugglers extends ClientCard implements CubeMalusCard {

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

    @Override
    public int getCubeMalus() {
        return cubeMalus;
    }

    @Override
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│             SMUGGLERS              │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Required Fire Power:      x%-8d │\n", requiredFirePower));
        sb.append(String.format("│ Flight Days Cost:         %-8d │\n", stepsBack));
        sb.append(String.format("│ Cargo Cubes Penalty:     x%-8d │\n", cubeMalus));
        sb.append("└────────────────────────────────────┘\n");
        
        if (reward != null && !reward.isEmpty()) {
            sb.append("Victory Reward (Cargo Cubes):\n");
            String rewardStr = reward.toString().replaceAll("[\\[\\]]", "");
            sb.append(String.format("  %s\n\n", rewardStr));
        }
        
        sb.append("Effects: Enemies attack players in route order starting from the leader.\n");
        sb.append("Compare your fire power against the required amount. Higher power means\n");
        sb.append("victory and reward, lower power means defeat and penalty. Equal power\n");
        sb.append("results in a draw with no effects.");
        
        return sb.toString();
    }

}
