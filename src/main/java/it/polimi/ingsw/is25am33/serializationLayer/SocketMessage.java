package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.dangerousObj.BigShot;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.*;

public class SocketMessage {

    private String senderNickname;
    private String actions;
    private String paramString;
    private String paramGameId;
    private Coordinates paramCoordinates;
    private List<Coordinates> paramActivableCoordinates;
    private List<Coordinates> paramCabinCoordinates;
    private List<Coordinates> paramBatteryBoxCoordinates;
    private List<GameInfo> paramGameInfo;
    private Integer paramInt;
    private Boolean paramBoolean;
    private PlayerColor paramPlayerColor;
    private GameState paramGameState;
    private CardState paramCardState;
    private Component paramComponent;
    private Component[][] paramShipBoardAsMatrix;
    private DangerousObj paramDangerousObj;
    private List<List<String>> paramLittleVisibleDecks;
    private Map<Integer, Component> paramVisibleComponents;
    private Set<Coordinates> paramIncorrectlyPositionedCoordinates;
    private Set<Set<Coordinates>> paramShipParts;
    private Set<Coordinates> paramShipPart;

    public SocketMessage(String senderNickname, String actions) {
        this.senderNickname = senderNickname;
        this.actions = actions;
        this.paramString = "";
        this.paramCoordinates = new Coordinates();
        this.paramGameInfo = new ArrayList<>();
        this.paramInt = 0;
        this.paramBoolean = false;
        this.paramPlayerColor = PlayerColor.GREEN;
        this.paramShipBoardAsMatrix = new Component[0][0];
        this.paramGameState = GameState.SETUP;
        this.paramComponent = new BatteryBox(new HashMap<>(), 0);
        this.paramGameId = "";
        this.paramDangerousObj = new BigShot(Direction.NORTH);
        this.paramActivableCoordinates = new ArrayList<>();
        this.paramBatteryBoxCoordinates = new ArrayList<>();
        this.paramCabinCoordinates = new ArrayList<>();
        this.paramVisibleComponents = new HashMap<>();
        this.paramLittleVisibleDecks = new ArrayList<>();
        this.paramCardState=CardState.START_CARD;
    }

    public SocketMessage() {
    }

    public Map<Integer, Component> getParamVisibleComponents() {
        return paramVisibleComponents;
    }

    public void setParamVisibleComponents(Map<Integer, Component> paramVisibleComponents) {
        this.paramVisibleComponents = paramVisibleComponents;
    }

    public List<Coordinates> getParamActivableCoordinates() {
        return paramActivableCoordinates;
    }

    public void setParamActivableCoordinates(List<Coordinates> paramActivableCoordinates) {
        this.paramActivableCoordinates = paramActivableCoordinates;
    }

    public List<Coordinates> getParamCabinCoordinates(){ return paramCabinCoordinates; }

    public void setParamCabinCoordinates(List<Coordinates> paramCabinCoordinates){
        this.paramCabinCoordinates = paramCabinCoordinates;
    }
    public List<Coordinates> getParamBatteryBoxCoordinates() { return paramBatteryBoxCoordinates; }

    public void setParamBatteryBoxCoordinates(List<Coordinates> paramBatteryBoxCoordinates) {
        this.paramBatteryBoxCoordinates = paramBatteryBoxCoordinates;
    }

    public Component[][] getParamShipBoardAsMatrix() {
        return paramShipBoardAsMatrix;
    }

    public void setParamShipBoardAsMatrix(Component[][] paramShipBoardAsMatrix) {
        this.paramShipBoardAsMatrix = paramShipBoardAsMatrix;
    }

    public GameState getParamGameState() {
        return paramGameState;
    }

    public void setParamGameState(GameState paramGameState) {
        this.paramGameState = paramGameState;
    }

    public String getParamGameId() {
        return paramGameId;
    }

    public void setParamGameId(String paramGameId) {
        this.paramGameId = paramGameId;
    }

    public PlayerColor getParamPlayerColor() {
        return paramPlayerColor;
    }

    public void setParamPlayerColor(PlayerColor paramPlayerColor) {
        this.paramPlayerColor = paramPlayerColor;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public Boolean getParamBoolean() {
        return paramBoolean;
    }

    public void setParamBoolean(Boolean paramBoolean) {
        this.paramBoolean = paramBoolean;
    }

    public Integer getParamInt() {
        return paramInt;
    }

    public void setParamInt(Integer paramInt) {
        this.paramInt = paramInt;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public String getActions() {
        return actions;
    }

    public String getParamString() {
        return paramString;
    }

    public void setParamString(String paramString) {
        this.paramString = paramString;
    }

    public Coordinates getParamCoordinates() {
        return paramCoordinates;
    }

    public void setParamCoordinates(Coordinates paramCoordinates) {
        this.paramCoordinates = paramCoordinates;
    }

    public List<GameInfo> getParamGameInfo() {
        return paramGameInfo;
    }

    public void setParamGameInfo(List<GameInfo> paramGameInfo) {
        this.paramGameInfo = paramGameInfo;
    }

    public Component getParamComponent() {
        return paramComponent;
    }

    public void setParamComponent(Component paramComponent) {
        this.paramComponent = paramComponent;
    }

    public CardState getParamCardState() {
        return paramCardState;
    }

    public void setParamCardState(CardState paramCardState) {
        this.paramCardState = paramCardState;
    }

    public void setParamDangerousObj(DangerousObj paramDangerousObj) {
        this.paramDangerousObj = paramDangerousObj;
    }

    public DangerousObj getParamDangerousObj() {
        return paramDangerousObj;
    }

    public List<List<String>> getParamLittleVisibleDecks() {
        return paramLittleVisibleDecks;
    }

    public void setParamLittleVisibleDecks(List<List<String>> paramLittleVisibleDecks) {
        this.paramLittleVisibleDecks = paramLittleVisibleDecks;
    }

    public Set<Coordinates> getParamIncorrectlyPositionedCoordinates() {
        return paramIncorrectlyPositionedCoordinates;
    }

    public void setParamIncorrectlyPositionedCoordinates(Set<Coordinates> paramIncorrectlyPositionedCoordinates) {
        this.paramIncorrectlyPositionedCoordinates = paramIncorrectlyPositionedCoordinates;
    }

    public Set<Set<Coordinates>> getParamShipParts() {
        return paramShipParts;
    }

    public void setParamShipParts(Set<Set<Coordinates>> paramShipParts) {
        this.paramShipParts = paramShipParts;
    }

    public Set<Coordinates> getParamShipPart() {
        return paramShipPart;
    }

    public void setParamShipPart(Set<Coordinates> paramShipPart) {
        this.paramShipPart = paramShipPart;
    }

    @JsonIgnore
    public Map<String, String> getMessageAsMap() {
        Map<String, String> messageAsMap = new HashMap<>();
        if (senderNickname != null) {
            messageAsMap.put("nickname", senderNickname);
        }
        if (actions != null) {
            messageAsMap.put("actions", actions);
        }
        if (paramString != null) {
            messageAsMap.put("paramString", paramString);
        }
        if (paramCoordinates != null) {
            messageAsMap.put("paramCoordinates", paramCoordinates.toString());
        }
        if (paramGameInfo != null) {
            messageAsMap.put("paramGameInfo", paramGameInfo.toString());
        }
        return messageAsMap;
    }

}
