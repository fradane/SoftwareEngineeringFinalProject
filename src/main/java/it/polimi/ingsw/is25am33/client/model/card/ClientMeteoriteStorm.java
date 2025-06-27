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
        this.meteorites = meteorites;
    }

    public List<ClientDangerousObject> getMeteorites() {
        return meteorites;
    }

    public void setMeteorites(List<ClientDangerousObject> meteorites) {
        this.meteorites = meteorites;
    }

    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_ENGINES;
    }

    public String getCardType() {
        return "MeteoriteStorm";
    }

    public int getDangerousObjCount() {
      return meteorites.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│          METEORITE STORM           │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Number of Meteorites:     x%-8d │\n", meteorites != null ? meteorites.size() : 0));
        sb.append("└────────────────────────────────────┘\n");
        
        if (meteorites != null && !meteorites.isEmpty()) {
            sb.append("Meteorite Details:\n");
            for (int i = 0; i < Math.min(meteorites.size(), 5); i++) {
                ClientDangerousObject meteorite = meteorites.get(i);
                sb.append(String.format("  %d. %s from %s\n", i + 1, meteorite.getType(), meteorite.getDirection()));
            }
            if (meteorites.size() > 5) {
                sb.append(String.format("  ... and %d more meteorites\n", meteorites.size() - 5));
            }
            sb.append("\n");
        }
        
        sb.append("Effects: Meteorites rain down from various directions. Small meteorites\n");
        sb.append("can be blocked by shields (1 battery cost), while large meteorites must\n");
        sb.append("be destroyed by cannons pointing in the correct direction. Level I storms\n");
        sb.append("come from sides and behind, Level II can also come from the front.");
        
        return sb.toString();
    }

}
