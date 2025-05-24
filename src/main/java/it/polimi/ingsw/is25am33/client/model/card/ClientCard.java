package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import javax.smartcardio.Card;
import java.io.Serializable;

public abstract class ClientCard implements Serializable{
    protected String cardName;
    protected String imageName;
    protected CardState currState;

    public ClientCard() {
    }

    public ClientCard(String cardName, String imageName) {
        this.cardName = cardName;
        this.imageName = imageName;
    }

    // Getters
    public String getCardName() {
        return cardName;
    }

    public String getImageName() {
        return imageName;
    }

    public CardState getCurrState() {
        return currState;
    }

    // Setters
    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setCurrState(CardState currState) {
        this.currState = currState;
    }

    // Methods to be implemented by subclasses
    public abstract CardState getFirstState();
    public abstract String getCardType();

    // Feature flags - override in subclasses as needed
    public boolean hasReward() {
        return false;
    }

    public boolean hasStepsBack() {
        return false;
    }

    public boolean hasCubeReward() {
        return false;
    }

    public static void setCommonProperties(ClientCard clientCard, AdventureCard serverCard) {
        clientCard.setCardName(serverCard.getCardName());
        clientCard.setImageName(serverCard.getImageName());

        // Handle null currState
        CardState currState = serverCard.getCurrState();
        if (currState != null) {
            clientCard.setCurrState(currState);
        }
    }
}