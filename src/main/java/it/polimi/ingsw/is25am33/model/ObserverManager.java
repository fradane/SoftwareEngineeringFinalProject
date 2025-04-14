package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.model.game.GameEvent;

import javax.management.remote.rmi.RMIServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObserverManager {
    private static ObserverManager instance;
    private Map<String, GameContext> games;

    private ObserverManager() {
        games = new HashMap<>();
    }

    public static ObserverManager getInstance() {
        if (instance == null)
            instance = new ObserverManager();
        return instance;
    }

    public void registerGame(GameContext context) {
        games.put(context.getGameId(), context);
    }

    public GameContext getGameContext(String gameId) {
        return games.get(gameId);
    }
}


