package it.polimi.ingsw.is25am33.network.rmi.server;

import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.common.VirtualClient;
import it.polimi.ingsw.is25am33.network.common.VirtualServer;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RMIServerNetworkManager extends UnicastRemoteObject implements VirtualServer {
    // Mappa che associa nickname ai client RMI
    private final Map<String, VirtualClient> clients;

    // Mappa che associa gameId ai controller di gioco
    private final Map<String, GameController> gameControllers;

    public RMIServerNetworkManager() throws RemoteException {
        super();
        this.clients = new ConcurrentHashMap<>();
        this.gameControllers = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void registerClient(String nickname, VirtualClient clientCallback) throws RemoteException {
        if (isNicknameInUse(nickname)) {
            throw new RemoteException("Nickname already in use");
        }

        clients.put(nickname, clientCallback);

        System.out.println("Client registered: " + nickname);

        try {
            clientCallback.notifyConnectionSuccessful();
        } catch (RemoteException e) {
            clients.remove(nickname);
            throw e;
        }
    }

    @Override
    public synchronized void unregisterClient(String nickname) throws RemoteException {
        clients.remove(nickname);

        System.out.println("Client unregistered: " + nickname);
    }

    @Override
    public List<GameInfo> getAvailableGames() throws RemoteException {
        return gameControllers.values().stream()
                .map(controller -> controller.getGameInfo())
                .filter(game -> !game.isStarted() && !game.isFull())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized String createGame(String nickname, PlayerColor color, int numPlayers, boolean isTestFlight) throws RemoteException {
        if (!clients.containsKey(nickname)) {
            throw new RemoteException("Client not registered");
        }

        if (numPlayers < 2 || numPlayers > 4) {
            throw new RemoteException("Invalid number of players: must be between 2 and 4");
        }

        // Genera un ID univoco per la partita
        String gameId = generateUniqueGameId();

        // Crea un nuovo controller per questa partita
        GameController controller = new GameController(gameId, numPlayers, isTestFlight);
        controller.setRMIServer(this); // Passa un riferimento al server

        // Registra il controller
        gameControllers.put(gameId, controller);

        System.out.println("GameModel created: " + gameId + " by " + nickname +
                " for " + numPlayers + " players" + (isTestFlight ? " (Test Flight)" : ""));

        // Aggiungi il player alla partita
        joinGame(gameId, nickname, color);

        return gameId;
    }

    @Override
    public synchronized boolean joinGame(String gameId, String nickname, PlayerColor color) throws RemoteException {
        if (!clients.containsKey(nickname)) {
            throw new RemoteException("Client not registered");
        }

        if (!gameControllers.containsKey(gameId)) {
            throw new RemoteException("GameModel not found");
        }

        GameController controller = gameControllers.get(gameId);
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

        //TODO aggiungere: ObserverManager.addObserver(clientCallback);

        // Aggiungi il player alla partita
        controller.addPlayer(nickname, color);
        // Aggiorno le gameModel info
        gameInfo = controller.getGameInfo();

        Set<String> players = gameInfo.getConnectedPlayersNicknames();
        // Notifica tutti i client della partita
        for (String player : players) {
            try {
                clients.get(player).notifyPlayerJoined(nickname, gameInfo);
            } catch (RemoteException e) {
                System.err.println("Error notifying player " + player + ": " + e.getMessage());
            }
        }

        System.out.println("Player " + nickname + " joined gameModel " + gameId);

        // Se abbiamo raggiunto il numero di giocatori, avvia la partita
        if (players.size() == gameInfo.getMaxPlayers()) {
            startGame(gameId);
        }

        return true;
    }

    private void startGame(String gameId) {
        try {
            GameController controller = gameControllers.get(gameId);
            GameState initialState = controller.startGame();

            GameInfo gameInfo = controller.getGameInfo();

            // Notifica tutti i client dell'inizio della partita
            for (String player : gameInfo.getConnectedPlayersNicknames()) {
                try {
                    clients.get(player).notifyGameStarted(initialState);
                } catch (RemoteException e) {
                    System.err.println("Error notifying player " + player + ": " + e.getMessage());
                }
            }

            System.out.println("GameModel " + gameId + " started");

        } catch (Exception e) {
            System.err.println("Error starting gameModel: " + e.getMessage());
        }
    }

    @Override
    public synchronized void leaveGame(String gameId, String nickname) throws RemoteException {
        if (!gameControllers.containsKey(gameId)) {
            return;
        }

        GameController controller = gameControllers.get(gameId);
        controller.removePlayer(nickname);

        GameInfo gameInfo = controller.getGameInfo();

        // Notifica tutti i client rimanenti
        Set<String> players = gameInfo.getConnectedPlayersNicknames();
        for (String player : players) {
            try {
                clients.get(player).notifyPlayerLeft(nickname, gameInfo);
            } catch (RemoteException e) {
                System.err.println("Error notifying player " + player + ": " + e.getMessage());
            }
        }

        System.out.println("Player " + nickname + " left gameModel " + gameId);

        // Se la partita è già iniziata e non ci sono abbastanza giocatori, termina la partita
        if (gameInfo.isStarted() && players.size() < 2) {
            endGame(gameId, "Not enough players");
        }

        // Se non ci sono più giocatori, rimuovi la partita
        if (players.isEmpty()) {
            gameControllers.remove(gameId);
            System.out.println("GameModel " + gameId + " removed");
        }
    }

    private void endGame(String gameId, String reason) {
        GameController controller = gameControllers.get(gameId);
        Set<String> players = controller.getGameInfo().getConnectedPlayersNicknames();

        // Notifica tutti i client della fine della partita
        for (String player : players) {
            try {
                clients.get(player).notifyGameEnded(reason);
            } catch (RemoteException e) {
                System.err.println("Error notifying player " + player + ": " + e.getMessage());
            }
        }

        // Rimuovi la partita
        gameControllers.remove(gameId);

        System.out.println("GameModel " + gameId + " ended: " + reason);
    }

    @Override
    public boolean isNicknameInUse(String nickname) throws RemoteException {
        return clients.containsKey(nickname);
    }

    /**
     * Genera un ID univoco per una partita
     */
    private String generateUniqueGameId() {
        String gameId;
        do {
            gameId = UUID.randomUUID().toString().substring(0, 8);
        } while (gameControllers.containsKey(gameId));

        return gameId;
    }

    @Override
    public String playerChoseComponentFromTable(String nickname, String coordinatesJson) throws RemoteException {
        // TODO
        return null;
    }

    @Override
    public void playerChoseToEndBuildShipBoardPhase(String nickname) throws RemoteException {
        // TODO
    }

    @Override
    public boolean playerWantsToSeeShipBoardOf(String chosenPlayerNickname, String nickname) throws RemoteException{
        // TODO
        return false;
    }

    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, String coordinatesJson) throws RemoteException {

    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) throws RemoteException {

    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) throws RemoteException {

    }

    public static void main(String args[]) throws Exception {
        try {
            System.out.println("Starting Galaxy Trucker Server...");

            // Crea il registry RMI
            Registry registry = LocateRegistry.createRegistry(NetworkConfiguration.RMI_PORT);

            // Crea il server
            RMIServerNetworkManager server = new RMIServerNetworkManager();

            // Registra il server nel registry
            registry.rebind(NetworkConfiguration.RMI_SERVER_NAME, server);

            System.out.println("Galaxy Trucker Server started on port " + NetworkConfiguration.RMI_PORT);
            System.out.println("Server ready to accept connections");

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    public notifyClients(DTO dto, List<observer> observers, Runnable callback ){
//        String message = serialize(dto);
//        for(observer o : observers) {
//            callback.run(o, message);
//        }
//    }


//    (observer, message)->{
//        observer.notifyTIleChanged(message)
//    }

}
