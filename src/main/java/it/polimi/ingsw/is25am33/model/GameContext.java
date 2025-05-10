package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class GameContext {
    private final Map<String, CallableOnClientController> clientControllers;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GameContext(Map<String, CallableOnClientController> clientControllers) {
        this.clientControllers = clientControllers;
    }

    public Map<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

    public void notifyAllClients(BiConsumer<String, CallableOnClientController> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();

        clientControllers.forEach((nickname, clientController) -> {
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

