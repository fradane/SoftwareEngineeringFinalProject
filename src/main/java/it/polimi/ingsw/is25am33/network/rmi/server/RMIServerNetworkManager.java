package it.polimi.ingsw.is25am33.network.rmi.server;

import it.polimi.ingsw.is25am33.controller.GameController;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.Observer;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.GameEvent;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;
import it.polimi.ingsw.is25am33.network.common.VirtualClient;
import it.polimi.ingsw.is25am33.network.common.VirtualServer;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class RMIServerNetworkManager extends UnicastRemoteObject implements VirtualServer {
    // Mappa che associa nickname ai client RMI
    private final Map<String, VirtualClient> clients;

    // Mappa che associa gameId ai controller di gioco
    private final Map<String, GameController> gameControllers;

    private final ExecutorService gameExecutor;

    public RMIServerNetworkManager() throws RemoteException {
        super();
        this.clients = new ConcurrentHashMap<>();
        this.gameControllers = new ConcurrentHashMap<>();

        // Crea un pool di thread basato sul numero di core disponibili
        int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.gameExecutor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "GameServerWorker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Chiude ordinatamente l'ExecutorService
     */
    public void shutdown() {
        System.out.println("Shutting down server executor...");
        if (gameExecutor != null && !gameExecutor.isShutdown()) {
            gameExecutor.shutdown();
            try {
                if (!gameExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    gameExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                gameExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
    public synchronized GameInfo createGame(String nickname, PlayerColor color, int numPlayers, boolean isTestFlight) throws RemoteException {
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
        controller.addPlayer(nickname, color);

        return controller.getGameInfo();
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

        if (gameInfo.getPlayersAndColors().values().contains(color)) {
            throw new RemoteException("This color has already been taken");
        }

        //TODO aggiungere: ObserverManager.addObserver(clientCallback);

        controller.addPlayer(nickname, color);
        gameInfo = controller.getGameInfo();

        // creo una copia finale di gameInfo
        final GameInfo finalGameInfo = gameInfo;

        Set<String> players = gameInfo.getConnectedPlayersNicknames();

        // Usa il nuovo metodo per notificare i giocatori
        notifyPlayers(players, (player, client) -> {
            try {
                client.notifyPlayerJoined(nickname, finalGameInfo);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Player " + nickname + " joined game " + gameId + " with color " + color);

        // Se abbiamo raggiunto il numero di giocatori, avvia la partita
        if (players.size() == gameInfo.getMaxPlayers()) {
            startGame(gameId);
        }

        return true;
    }

    private void startGame(String gameId) {
        // Esegui l'avvio della partita in un thread separato
        gameExecutor.submit(() -> {
            try {
                GameController controller = gameControllers.get(gameId);
                GameState initialState = controller.startGame();
                GameInfo gameInfo = controller.getGameInfo();

                // Usa il nuovo metodo per notificare tutti i client
                notifyPlayers(gameInfo.getConnectedPlayersNicknames(), (player, client) -> {
                    try {
                        client.notifyGameStarted(initialState);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });

                System.out.println("Game " + gameId + " started");
            } catch (Exception e) {
                System.err.println("Error starting game: " + e.getMessage());
            }
        });
    }

    /**
     * Metodo generico per inviare notifiche ai client in parallelo
     */
    private void notifyPlayers(Set<String> players, BiConsumer<String, VirtualClient> notificationFunction) {
        // Crea un CompletableFuture per ogni notifica da inviare
        List<CompletableFuture<Void>> notificationFutures = players.stream()
                .map(player -> CompletableFuture.runAsync(() -> {
                    try {
                        VirtualClient client = clients.get(player);
                        if (client != null) {
                            notificationFunction.accept(player, client);
                        }
                    } catch (Exception e) {
                        System.err.println("Error notifying player " + player + ": " + e.getMessage());
                    }
                }, gameExecutor))
                .collect(Collectors.toList());

        // Attendi che tutte le notifiche siano state inviate
        CompletableFuture.allOf(notificationFutures.toArray(new CompletableFuture[0])).join();
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
        gameExecutor.submit(() -> {
            try {
                GameController controller = gameControllers.get(gameId);
                Set<String> players = controller.getGameInfo().getConnectedPlayersNicknames();

                // Usa il nuovo metodo per notificare i giocatori
                notifyPlayers(players, (player, client) -> {
                    try {
                        client.notifyGameEnded(reason);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Rimuovi la partita
                gameControllers.remove(gameId);

                System.out.println("Game " + gameId + " ended: " + reason);
            } catch (Exception e) {
                System.err.println("Error ending game: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean isNicknameInUse(String nickname) throws RemoteException {
        return clients.containsKey(nickname);
    }

    @Override
    public boolean isColorAvailable(String gameId, PlayerColor color) throws RemoteException {
        return !gameControllers.get(gameId).getGameInfo().getPlayersAndColors().containsValue(color);
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

//    @Override
//    public void notifyClient(List<Observer> observers, GameEvent event, BiConsumer<Observer, String> biConsumer) {
//
//    }

    public static void main(String args[]) throws Exception {
        RMIServerNetworkManager server = null;
        try {
            System.out.println("Starting Galaxy Trucker Server...");

            // Crea il registry RMI
            Registry registry = LocateRegistry.createRegistry(NetworkConfiguration.RMI_PORT);

            // Crea il server
            server = new RMIServerNetworkManager();

            // Registra il server nel registry
            registry.rebind(NetworkConfiguration.RMI_SERVER_NAME, server);

            System.out.println("Galaxy Trucker Server started on port " + NetworkConfiguration.RMI_PORT);
            System.out.println("Server ready to accept connections");

            // Aggiungi un hook per lo shutdown
            final RMIServerNetworkManager finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Server shutdown hook triggered");
                finalServer.shutdown();
            }));

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            if (server != null) {
                server.shutdown();
            }
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
