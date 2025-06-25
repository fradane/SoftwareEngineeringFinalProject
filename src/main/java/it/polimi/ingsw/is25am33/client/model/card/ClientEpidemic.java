package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

public class ClientEpidemic extends ClientCard {
    @Override
    public CardState getFirstState() {
        return CardState.EPIDEMIC;
    }

    @Override
    public String getCardType() {
        return "Epidemic";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│             EPIDEMIC               │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append("│ Crew Members Lost:        x1       │\n");
        sb.append("│ Affects:                  All      │\n");
        sb.append("│ Choice:                   Player    │\n");
        sb.append("└────────────────────────────────────┘\n");
        sb.append("Effects: A deadly disease spreads rapidly throughout all ships.\n");
        sb.append("Every player must lose exactly 1 crew member of their choice\n");
        sb.append("(either human or alien). This loss is mandatory and cannot be\n");
        sb.append("avoided. Choose wisely as reduced crew affects future gameplay.");
        
        return sb.toString();
    }
}
