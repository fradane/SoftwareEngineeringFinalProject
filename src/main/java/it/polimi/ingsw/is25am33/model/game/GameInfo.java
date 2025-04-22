package it.polimi.ingsw.is25am33.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class GameInfo implements Serializable {
    private String gameId;
    private CallableOnGameController gameController;
    private int maxPlayers;
    private Map<String, PlayerColor> connectedPlayers;
    private boolean isStarted;
    private boolean isTestFlight;

    public GameInfo(String gameId, CallableOnGameController gameController, int maxPlayers, Map<String, PlayerColor> connectedPlayers, boolean isStarted, boolean isTestFlight) {
        this.gameId = gameId;
        this.gameController = gameController;
        this.maxPlayers = maxPlayers;
        this.connectedPlayers = connectedPlayers;
        this.isStarted = isStarted;
        this.isTestFlight = isTestFlight;
    }

    public GameInfo() {}

    public Map<String, PlayerColor> getConnectedPlayers() {
        return connectedPlayers;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setConnectedPlayers(Map<String, PlayerColor> connectedPlayers) {
        this.connectedPlayers = connectedPlayers;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public void setTestFlight(boolean testFlight) {
        isTestFlight = testFlight;
    }

    public String getGameId() {
        return gameId;
    }

    @JsonIgnore
    public CallableOnGameController getGameController() {
        return gameController;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    @JsonIgnore
    public Set<String> getConnectedPlayersNicknames() {
        return connectedPlayers.keySet();
    }

    public boolean isTestFlight() {
        return isTestFlight;
    }

    public boolean isStarted() {
        return isStarted;
    }

    @JsonIgnore
    public boolean isFull() {
        return connectedPlayers.size() >= maxPlayers;
    }

}
