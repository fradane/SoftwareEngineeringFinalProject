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
        StringBuilder sb = new StringBuilder("Star Dust Card: ");
        sb.append(cardName).append("\n");
        sb.append("If you have built a good ship, you are at peace :)");
        return sb.toString();
    }

}
