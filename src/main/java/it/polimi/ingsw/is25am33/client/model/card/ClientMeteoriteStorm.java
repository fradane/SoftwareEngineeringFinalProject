package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.dangerousObj.Meteorite;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;
import java.util.List;

public class ClientMeteoriteStorm extends ClientCard implements Serializable {
    private List<ClientDangerousObject> meteorites;

    public ClientMeteoriteStorm() {}

    public ClientMeteoriteStorm(String cardName, String imageName, List<ClientDangerousObject> meteorites) {
        super(cardName, imageName);
        this.meteorites=meteorites;
    }

    public List<ClientDangerousObject> getMeteorites() {
        return meteorites;
    }
    public void setMeteorites(List<ClientDangerousObject> meteorites) {
        this.meteorites = meteorites;
    }

    public CardState getFirstState() {
        return CardState.CHOOSE_ENGINES;
    }

    public String getCardType() {
        return "MeteoriteStorm";
    }

    public int getDangerousObjCount() {
      return meteorites.size();
    }

}
