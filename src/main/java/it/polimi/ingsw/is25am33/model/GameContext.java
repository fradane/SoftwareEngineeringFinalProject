package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

    public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, RemoteException> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();
        List<String> clientsDisconnected = new ArrayList<>();
        clientControllers.forEach((nickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try{
                    consumer.accept(nickname, clientController);
                } catch (RemoteException e) {
                    clientsDisconnected.add(nickname);
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                clientsDisconnected.add(nickname);
            }
        });

        if(!clientsDisconnected.isEmpty()) {
            clientsDisconnected.forEach(clientControllers::remove);
            notifyDisconnection(clientsDisconnected);
            gameModel.setCurrGameState(GameState.END_GAME);
        }

    }

    private void notifyDisconnection(List<String> disconnectedClients) {
        for (String disconnected : disconnectedClients) {
            clientControllers.forEach((nicknameToNotify, clientController) -> {
                try {
                    clientController.notifyPlayerDisconnected(nicknameToNotify, disconnected);
                } catch (RemoteException e) {
                    System.err.println("Errore durante la notifica a " + nicknameToNotify + ": " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

}

