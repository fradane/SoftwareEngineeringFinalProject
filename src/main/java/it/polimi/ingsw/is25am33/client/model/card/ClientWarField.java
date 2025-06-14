package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.List;

public class ClientWarField extends ClientCard implements Serializable, CrewMalusCard {
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

    public int getCrewMalus() {
        return crewMalus;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getCubeMalus() {
        return cubeMalus;
    }

    public List<ClientDangerousObject> getShots() {
        return shots;
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setShots(List<ClientDangerousObject> shots) {
        this.shots = shots;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public CardState getFirstState() {
        return CardState.CHOOSE_CANNONS;
    }

    public String getCardType() {
        return "WarField";
    }

}
