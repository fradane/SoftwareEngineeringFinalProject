package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientModel {

    private String nickname;
    private Set<String> playersNickname;
    private AdventureCard currAdventureCard;
    private CardState currCardState;
    private GameState gameState;
    private Map<String, ShipBoardClient> Shipboards = new HashMap<>();
    private Map<String, Integer> Credits = new HashMap<>();
    private DangerousObj currentDangerousObj;
    private String currentPlayer;
    private Map<Integer, Component> visibleComponents;
    private FlyingBoard flyingBoard;
    private List<List<AdventureCard>> littleVisibleDecks;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setCardState(CardState cardState){
        this.currCardState=cardState;
    }

    public CardState getCurrCardState() {
        return currCardState;
    }

    public String getNickname() {
        return nickname;
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

    public Map<String, ShipBoardClient> getShipboards() {
        return Shipboards;
    }

    public Map<String, Integer> getCredits() {
        return Credits;
    }

    public Set<String> getPlayersNickname() {
        return playersNickname;
    }

    public void setPlayersNickname(Set<String> playersNickname) {
        this.playersNickname = playersNickname;
    }

    public void setCurrentDangerousObj(DangerousObj currentDangerousObj) {
        this.currentDangerousObj = currentDangerousObj;
    }

    public void setCredits(Map<String, Integer> credits) {
        Credits = credits;
    }

    public void setCurrCardState(CardState currCardState) {
        this.currCardState = currCardState;
    }

    public void setShipboards(Map<String, ShipBoardClient> shipboards) {
        Shipboards = shipboards;
    }

    public void setFlyingBoard (FlyingBoard flyingBoard){
        this.flyingBoard=flyingBoard;
    }

    public void setCurrentPlayer(String  currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void setVisibleComponents(Map<Integer, Component> visibleComponents) {
        this.visibleComponents = visibleComponents;
    }

    public DangerousObj getCurrentDangerousObj() {
        return currentDangerousObj;
    }

    public List<List<AdventureCard>> getLittleVisibleDecks() {
        return littleVisibleDecks;
    }

    public void setLittleVisibleDeck(List<List<AdventureCard>> littleVisibleDecks) {
        this.littleVisibleDecks = littleVisibleDecks;
    }
}
