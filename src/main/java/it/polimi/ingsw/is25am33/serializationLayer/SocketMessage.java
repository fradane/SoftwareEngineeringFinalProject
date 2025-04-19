package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SocketMessage {

    private String senderNickname;
    private String actions;
    private String paramString;
    private String paramGameId;
    private Coordinates paramCoordinates;
    private List<GameInfo> paramGameInfo;
    private Integer paramInt;
    private Boolean paramBoolean;
    private PlayerColor paramPlayerColor;
    private GameState paramGameState;

    public SocketMessage(String senderNickname, String actions) {
        this.senderNickname = senderNickname;
        this.actions = actions;
        this.paramString = "";
        this.paramCoordinates = new Coordinates();
        this.paramGameInfo = new ArrayList<>();
        this.paramInt = 0;
        this.paramBoolean = false;
        this.paramPlayerColor = PlayerColor.GREEN;
    }

    public SocketMessage() {

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
