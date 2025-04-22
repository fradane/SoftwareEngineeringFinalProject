package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.List;
import java.util.Set;

public class ClientModel {

    private Set<String> playersNickname;
    private AdventureCard currAdventureCard;
    private GameState gameState;
    private ShipBoard shipBoard;


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

    public void setPlayersNickname(Set<String> playersNickname) {
        this.playersNickname = playersNickname;
    }

    public Set<String> getPlayersNickname() {
        return playersNickname;
    }



}
