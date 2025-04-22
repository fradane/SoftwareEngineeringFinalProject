package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.util.List;

public class DTO {

    private CardState cardState;
    private GameState gameState;
    private ComponentState componentState;
    private Player player;
    private Component component;
    private Coordinates coordinates;
    private Integer num;
    private Boolean shipboardOK;
    private FlyingBoard flyingBoard;
    private ShipBoard shipBoard;
    private AdventureCard adventureCard;
    private List<AdventureCard> littleDeck;
    private DangerousObj dangerousObj;
    private ComponentTable componentTable;

    public List<AdventureCard> getLittleDeck() {
        return littleDeck;
    }

    public void setLittleDeck(List<AdventureCard> littleDeck) {
        this.littleDeck = littleDeck;
    }

    public ComponentTable getComponentTable() {
        return componentTable;
    }

    public void setComponentTable(ComponentTable componentTable) {
        this.componentTable = componentTable;
    }

    public DangerousObj getDangerousObj() {
        return dangerousObj;
    }

    public void setDangerousObj(DangerousObj dangerousObj) {
        this.dangerousObj = dangerousObj;
    }

    public CardState getCardState() {
        return cardState;
    }

    public void setCardState(CardState cardState) {
        this.cardState = cardState;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public ComponentState getComponentState() {
        return componentState;
    }

    public void setComponentState(ComponentState componentState) {
        this.componentState = componentState;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public FlyingBoard getFlyingBoard() {
        return flyingBoard;
    }

    public void setFlyingBoard(FlyingBoard flyingBoard) {
        this.flyingBoard = flyingBoard;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public ShipBoard getShipBoard() {
        return shipBoard;
    }

    public void setShipBoard(ShipBoard shipBoard) {
        this.shipBoard = shipBoard;
    }

    public Boolean getShipboardOK() {
        return shipboardOK;
    }

    public void setShipboardOK(Boolean shipboardOK) {
        this.shipboardOK = shipboardOK;
    }

    public AdventureCard getAdventureCard() {
        return adventureCard;
    }

    public void setAdventureCard(AdventureCard adventureCard) {
        this.adventureCard = adventureCard;
    }
}
