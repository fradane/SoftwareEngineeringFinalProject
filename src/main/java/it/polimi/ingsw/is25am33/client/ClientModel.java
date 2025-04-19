package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.List;

public class ClientModel {

    private List<String> playersNickname;
    private AdventureCard currAdventureCard;
    private GameState gameState;


    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setCurrAdventureCard(AdventureCard card) {
        this.currAdventureCard = card;
    }

    public AdventureCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    public void setPlayersNickname(List<String> playersNickname) {
        this.playersNickname = playersNickname;
    }

    public List<String> getPlayersNickname() {
        return playersNickname;
    }



}
