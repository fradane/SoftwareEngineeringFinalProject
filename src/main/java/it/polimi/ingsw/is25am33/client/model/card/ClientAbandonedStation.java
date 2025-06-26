package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// implementa crewMalusCard per generalizzare il metodo playerWantsToVisitLocation che richiede un numero minimo di crew per visitare una stazione/nave
//TODO cambiare nome all'interfaccia crewMalusCard per renderla più appropriata anche a questa classe
public class ClientAbandonedStation extends ClientCard implements Serializable, CrewMalusCard {
    private int requiredCrewMembers;
    private int stepsBack;
    private List<CargoCube> reward;

    public ClientAbandonedStation() {
        super();
        this.reward = new ArrayList<>();
    }

    public ClientAbandonedStation(String cardName, String imageName, int requiredCrewMembers, List<CargoCube> reward, int stepsBack) {
        super(cardName, imageName);
        this.requiredCrewMembers = requiredCrewMembers;
        this.reward = reward != null ? new ArrayList<>(reward) : new ArrayList<>();
        this.stepsBack = stepsBack;
    }

    // Getters
    public int getRequiredCrewMembers() {
        return requiredCrewMembers;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public List<CargoCube> getReward() {
        return reward != null ? new ArrayList<>(reward) : new ArrayList<>();
    }

    // Setters
    public void setRequiredCrewMembers(int requiredCrewMembers) {
        this.requiredCrewMembers = requiredCrewMembers;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward != null ? new ArrayList<>(reward) : new ArrayList<>();
    }

    @Override
    public CardState getFirstState() {
        return CardState.VISIT_LOCATION;
    }

    @Override
    public String getCardType() {
        return "AbandonedStation";
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
    public boolean hasCubeReward() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│        ABANDONED STATION           │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Required Crew Members:    x%-8d │\n", requiredCrewMembers));
        sb.append(String.format("│ Flight Days Cost:         %-8d │\n", stepsBack));
        sb.append("└────────────────────────────────────┘\n");
        
        if (reward != null && !reward.isEmpty()) {
            sb.append("Cargo Cube Reward:\n");
            String cubes = reward.stream()
                    .map(Enum::name)
                    .toList()
                    .toString()
                    .replaceAll("[\\[\\]]", "");
            sb.append(String.format("  %s\n\n", cubes));
        }
        
        sb.append("Effects: Requires minimum crew to dock at this abandoned station.\n");
        sb.append("Only one player can exploit it. Docking loads goods that can be\n");
        sb.append("redistributed or discarded, then you move back the specified\n");
        sb.append("flight days. No crew members are lost in this process.");
        
        return sb.toString();
    }

    @Override
    public int getCrewMalus() {
        return requiredCrewMembers;
    }

    @Override
    public void setCrewMalus(int crewMalus) {
        requiredCrewMembers = crewMalus;
    }
}