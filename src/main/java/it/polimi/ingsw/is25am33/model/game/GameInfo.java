package it.polimi.ingsw.is25am33.model.game;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class GameInfo implements Serializable {
    private final String gameId;
    private final int maxPlayers;
    private final Set<String> connectedPlayers;
    private final boolean isStarted;
    private final boolean isTestFlight;

    public GameInfo(String gameId, int maxPlayers, Set<String> connectedPlayers, boolean isStarted, boolean isTestFlight) {
        this.gameId = gameId;
        this.maxPlayers = maxPlayers;
        this.connectedPlayers = connectedPlayers;
        this.isStarted = isStarted;
        this.isTestFlight = isTestFlight;
    }

    public String getGameId() {
        return gameId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Set<String> getConnectedPlayersNicknames() {
        return connectedPlayers;
    }

    public boolean isTestFlight() {
        return isTestFlight;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isFull() {
        return connectedPlayers.size() >= maxPlayers;
    }
}
