package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.List;

public class ClientWarField extends ClientCard implements Serializable, CrewMalusCard, CubeMalusCard {
    private int crewMalus;
    private int stepsBack;
    private int cubeMalus;
    private List<ClientDangerousObject> shots;

    public ClientWarField(){}

    public ClientWarField(String cardName, String imageName, int crewMalus, int stepsBack, int cubeMalus, List<ClientDangerousObject> shots) {
        super(cardName, imageName);
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.cubeMalus = cubeMalus;
        this.shots = shots;
    }

    @Override
    public int getCrewMalus() {
        return crewMalus;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    @Override
    public int getCubeMalus() {
        return cubeMalus;
    }

    public List<ClientDangerousObject> getShots() {
        return shots;
    }

    @Override
    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setShots(List<ClientDangerousObject> shots) {
        this.shots = shots;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    @Override
    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    @Override
    public String getCardType() {
        return "WarField";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│             WAR FIELD              │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Flight Days Penalty:      %-8d │\n", stepsBack));
        sb.append(String.format("│ Crew Members Lost:        x%-8d │\n", crewMalus));
        sb.append(String.format("│ Cargo Cubes Lost:         x%-8d │\n", cubeMalus));
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
        
        sb.append("Effects: Three-phase war zone with sequential penalties.\n");
        sb.append("Phase 1: Player with least crew loses flight days.\n");
        sb.append("Phase 2: Player with least engine power loses crew members.\n");
        sb.append("Phase 3: Player with least fire power suffers cannon attacks.\n");
        sb.append("In case of ties, the player furthest ahead on the route suffers.");
        
        return sb.toString();
    }

}
