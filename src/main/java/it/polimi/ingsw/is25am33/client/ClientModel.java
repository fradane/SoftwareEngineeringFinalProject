package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.Map;
import java.util.Set;

public class ClientModel {

    private Set<String> playersNickname;
    private AdventureCard currAdventureCard;
    private GameState gameState;
    private ShipBoard shipBoard;
    private CardState cardState;
    private boolean isMyTurn;
    private String nickname;
    private DangerousObj currDangerousObj;


    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

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

    public CardState getCardState() {
        return cardState;
    }

    public ShipBoard getShipBoard() {
        return shipBoard;
    }

    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    public void setCurrDangerousObj(DangerousObj currDangerousObj) {
        this.currDangerousObj = currDangerousObj;
    }

}
