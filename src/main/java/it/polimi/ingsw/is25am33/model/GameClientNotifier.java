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

/**
 * Anche se il sistema utilizza un meccanismo di ping-pong per rilevare la disconnessione dei client,
 * manteniamo l'uso dei Future con timeout per evitare che una singola chiamata remota (RMI o socket)
 * blocchi l'intera notifica verso gli altri client.
 *
 * Questo approccio è utile per gestire casi in cui un client:
 * - È ancora considerato connesso dal ping-pong (es. appena disconnesso, ma non ancora scaduto il timeout)
 * - Risponde molto lentamente (latenze elevate o rete congestionata)
 * - Non risponde affatto, ma la RemoteException non viene immediatamente lanciata (tipico in TCP)
 *
 * In questi casi, il Future con timeout assicura che la notifica globale non venga rallentata o bloccata
 * da client non responsivi. Anche se si ignora l'eccezione a livello di logica, il timeout impedisce
 * blocchi indefiniti o rallentamenti dell'intero sistema.
 */

public class GameClientNotifier {
    private final GameModel gameModel;
    private Map<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GameClientNotifier(GameModel gameModel, ConcurrentHashMap<String, CallableOnClientController> clientControllers) {
        this.gameModel = gameModel;
        this.clientControllers = clientControllers;
    }

    public Map<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

    public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();

        clientControllers.forEach((nickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try{
                    consumer.accept(nickname, clientController);
                } catch (IOException  e) {
                    System.err.println("Errore nella notifica del client: " + nickname);
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                future.cancel(true);
            }
        });

       }

    public void notifyDisconnection(String nicknameOfDisconectedPlayer) {
        clientControllers.forEach((nicknameToNotify, clientController) -> {
                try {
                    clientController.notifyPlayerDisconnected(nicknameToNotify, nicknameOfDisconectedPlayer );
                } catch (IOException e) {}
        });
        closeAllClients();
    }

    public void closeAllClients(){
        gameModel.setStarted(false);
        clientControllers.forEach((nicknameToNotify, clientController) -> {
            try {
                clientController.forcedDisconnection(nicknameToNotify, gameModel.getGameId());
            } catch (IOException e) {}
        });
    }

    public void notifyClients(Set<String> playersNicknameToBeNotified, ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();

        clientControllers.forEach((nickname, clientController) -> {
            if(!playersNicknameToBeNotified.contains(nickname))
                return;

            Future<?> future = executor.submit(() -> {
                try {
                    consumer.accept(nickname, clientController);
                } catch (IOException e) {
                    System.err.println("Errore nella notifica del client: " + nickname);
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Timeout nella notifica del client: " + nickname);
                future.cancel(true);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

}

