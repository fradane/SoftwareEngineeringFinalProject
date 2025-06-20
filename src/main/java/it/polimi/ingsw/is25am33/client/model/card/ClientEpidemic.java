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
}
