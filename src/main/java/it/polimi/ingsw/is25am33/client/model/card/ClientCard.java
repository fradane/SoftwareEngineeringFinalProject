package it.polimi.ingsw.is25am33.client.model.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "cardType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientPlanets.class, name = "Planets"),
        @JsonSubTypes.Type(value = ClientAbandonedShip.class, name = "AbandonedShip"),
        @JsonSubTypes.Type(value = ClientStarDust.class, name = "Stardust"),
        @JsonSubTypes.Type(value = ClientMeteoriteStorm.class, name = "MeteoriteStorm"),
        @JsonSubTypes.Type(value = ClientFreeSpace.class, name = "FreeSpace"),
        @JsonSubTypes.Type(value = ClientPirates.class, name = "Pirates")
        // Aggiungi tutte le altre sottoclassi di ClientCard
})
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