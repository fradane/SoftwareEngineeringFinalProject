package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.DNS;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final Map<String, CallableOnClientController> clients = new ConcurrentHashMap<>();
    private final DNS dns;

    public ConnectionManager(DNS dns) {
        this.dns = dns;
    }

    public synchronized boolean registerWithNickname(String nickname, CallableOnClientController controller) {
        if (clients.containsKey(nickname)) return false;
        clients.put(nickname, controller);
        return true;
    }

    public GameInfo createGame(PlayerColor color, int numPlayers, boolean isTestFlight, String nickname) throws RemoteException {
        if (!clients.containsKey(nickname)) {
            throw new RemoteException("Client not registered");
        }

        if (numPlayers < 2 || numPlayers > 4) {
            throw new RemoteException("Invalid number of players: must be between 2 and 4");
        }

        // unique ID for this gameModel
        String gameId = generateUniqueGameId();

        // creates a new controller for this gameModel
        GameController controller = new GameController(gameId, numPlayers, isTestFlight, dns);
        synchronized (controller) {
            DNS.gameControllers.put(gameId, controller);

            System.out.println("GameModel created: " + gameId + " by " + nickname +
                    " for " + numPlayers + " players" + (isTestFlight ? " (Test Flight)" : ""));



            // Aggiungi il player alla partita
            controller.addPlayer(nickname, color, clients.get(nickname));
            System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

            //succede tanta roba
            return controller.getGameInfo();
        }
    }

    public Map<String, CallableOnClientController> getClients() {
        return clients;
    }

    public boolean joinGame(String gameId, String nickname, PlayerColor color) throws RemoteException {
        if (!clients.containsKey(nickname)) {
            throw new RemoteException("Client not registered");
        }

        // TODO
        if (!DNS.gameControllers.containsKey(gameId)) {
            throw new RemoteException("GameModel not found");
        }

        GameController controller = DNS.gameControllers.get(gameId);
        synchronized (controller) {
            GameInfo gameInfo = controller.getGameInfo();

            if (gameInfo.isStarted()) {
                throw new RemoteException("GameModel already started");
            }

            if (gameInfo.isFull()) {
                throw new RemoteException("GameModel is full");
            }

            if (gameInfo.getConnectedPlayersNicknames().contains(nickname)) {
                throw new RemoteException("You are already in this gameModel");
            }

            if (gameInfo.getConnectedPlayers().containsValue(color)) {
                return false;
            }

            controller.addPlayer(nickname, color, clients.get(nickname));
            gameInfo = controller.getGameInfo();

            final GameInfo finalGameInfo = gameInfo;

            Set<String> players = gameInfo.getConnectedPlayersNicknames();

            controller.notifyNewPlayerJoined(gameId, nickname, color).start();

            System.out.println("[" + gameId + "] " + nickname + " joined game with color " + color);

            // if the game is completed, it starts automatically
            if (players.size() == gameInfo.getMaxPlayers()) {
                controller.startGame();
            }

            return true;
        }
    }

    private String generateUniqueGameId() {
        String gameId;
        do {
            gameId = UUID.randomUUID().toString().substring(0, 8);
        } while (DNS.gameControllers.containsKey(gameId));

        return gameId;
    }

}
