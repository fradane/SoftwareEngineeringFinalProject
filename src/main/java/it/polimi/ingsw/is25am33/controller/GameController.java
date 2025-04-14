package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.network.rmi.server.RMIServerNetworkManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameController {
    private final GameModel gameModel;
    private RMIServerNetworkManager rmiServer;

    public GameController(String gameId, int maxPlayers, boolean isTestFlight) {
        this.gameModel = new GameModel(gameId, maxPlayers, isTestFlight);
    }

    /**
     * Imposta il riferimento al server RMI per inviare notifiche
     */
    public void setRMIServer(RMIServerNetworkManager rmiServer) {
        this.rmiServer = rmiServer;
    }

    public void addPlayer(String nickname, PlayerColor color) {
        gameModel.addPlayer(nickname, color);
    }

    public void removePlayer(String nickname) {
        gameModel.removePlayer(nickname);
    }

    public GameInfo getGameInfo() {
        Collection<Player> players = gameModel.getPlayers().values();
        Map<String, PlayerColor> playerAndColors = new HashMap<>();
        for (Player player : players) {
            playerAndColors.put(player.getNickname(), player.getPlayerColor());
        }

        return new GameInfo(
                gameModel.getGameId(),
                gameModel.getMaxPlayers(),
                playerAndColors,
                gameModel.isStarted(),
                gameModel.isTestFlight()

        );
    }

    public GameState startGame() {
        return gameModel.getCurrGameState();
    }
}
