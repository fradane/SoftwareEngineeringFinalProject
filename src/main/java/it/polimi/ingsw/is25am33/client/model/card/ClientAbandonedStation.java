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
        String firstString = String.format("""
           %s
           ┌────────────────────────────┐
           │     Abandoned Station      │
           ├────────────────────────────┤
           │ Required Crew:     x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """, imageName, requiredCrewMembers, stepsBack);

        StringBuilder secondString = new StringBuilder("   ");
        if (reward != null && !reward.isEmpty()) {
            secondString.append("Station Rewards:\n");
            String cubes = reward
                    .stream()
                    .map(Enum::name)
                    .toList()
                    .toString()
                    .replaceAll("[\\[\\]]", "");
            secondString.append(String.format("   Cargo: %s%n", cubes));
        }

        return firstString + secondString;
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