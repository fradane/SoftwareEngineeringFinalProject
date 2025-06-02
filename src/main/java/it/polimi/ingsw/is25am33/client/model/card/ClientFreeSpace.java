package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;

public class ClientFreeSpace extends ClientCard implements Serializable {

    public ClientFreeSpace() {}

    public ClientFreeSpace(String cardName, String imageName) {
        super(cardName, imageName);
    }

    public CardState getFirstState() {
        return CardState.CHOOSE_ENGINES;
    }

    public String getCardType() {
        return "FreeSpace";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Free Space Card: ");
        sb.append(cardName).append("\n");
        sb.append("Increase your engine power and advance in the rankings");
        return sb.toString();
    }

}
