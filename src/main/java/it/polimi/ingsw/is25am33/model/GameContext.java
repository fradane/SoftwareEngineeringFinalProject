package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.network.socket.SocketServerManager;

import java.io.IOException;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class GameContext {
    private final GameModel gameModel;
    private Map<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GameContext(GameModel gameModel, ConcurrentHashMap<String, CallableOnClientController> clientControllers) {
        this.gameModel = gameModel;
        this.clientControllers = clientControllers;
    }

    public Map<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

    public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<String> clientsDisconnected = new ArrayList<>();
        clientControllers.forEach((nickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try{
                    consumer.accept(nickname, clientController);
                } catch (IOException  e) {
                    clientsDisconnected.add(nickname);
                    System.out.println(e.getMessage());
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                clientsDisconnected.add(nickname);
            }
        });

        if(!clientsDisconnected.isEmpty()) {
            clientsDisconnected.forEach(clientControllers::remove);
            notifyDisconnection(clientsDisconnected);
        }

    }

    private void notifyDisconnection(List<String> disconnectedClients) {
        for (String disconnected : disconnectedClients) {
            clientControllers.forEach((nicknameToNotify, clientController) -> {
                try {
                    clientController.notifyPlayerDisconnected(nicknameToNotify, disconnected);
                } catch (IOException e) {
                    System.err.println("Errore durante la notifica a " + nicknameToNotify + ": " + e.getMessage());
                }
            });
        }
    }

    public void notifyClients(Set<String> playersNicknameToBeNotified, BiConsumer<String, CallableOnClientController> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();

        clientControllers.forEach((nickname, clientController) -> {
            if(!playersNicknameToBeNotified.contains(nickname))
                return;

            Future<?> future = executor.submit(() -> {
                try {
                    consumer.accept(nickname, clientController);
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Timeout nella notifica del client: " + nickname);
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Errore nella notifica del client: " + nickname);
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

}

