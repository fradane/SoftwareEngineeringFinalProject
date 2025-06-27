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
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│            FREE SPACE              │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append("│ Engine Power:         Declare yours │\n");
        sb.append("│ Movement:             Forward only  │\n");
        sb.append("│ Advantage:            Skip occupied │\n");
        sb.append("└────────────────────────────────────┘\n");
        sb.append("Effects: Open space highway allows you to boost forward.\n");
        sb.append("Each player declares their engine power (1 for single engine,\n");
        sb.append("2 for double engine activated with battery). Move forward\n");
        sb.append("that many empty spaces, skipping occupied positions.");
        
        return sb.toString();
    }

}
