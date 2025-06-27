package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;

public class ClientStarDust extends ClientCard implements Serializable {
    public ClientStarDust() {}
    public ClientStarDust(String cardName, String imageName) {
        super(cardName, imageName);
    }

    public CardState getFirstState() {
        return CardState.STARDUST;
    }

    public String getCardType() {
        return "StarDust";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│             STAR DUST              │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append("│ Penalty:              Per Connector │\n");
        sb.append("│ Flight Days Lost:     1 per exposed │\n");
        sb.append("│ Affects:              All players   │\n");
        sb.append("└────────────────────────────────────┘\n");
        sb.append("Effects: Cosmic dust interferes with exposed ship connectors.\n");
        sb.append("Each player loses 1 flight day for every exposed connector\n");
        sb.append("on their ship (single, double, or universal). Well-built\n");
        sb.append("ships with few exposed connectors suffer minimal penalties.");
        
        return sb.toString();
    }

}
