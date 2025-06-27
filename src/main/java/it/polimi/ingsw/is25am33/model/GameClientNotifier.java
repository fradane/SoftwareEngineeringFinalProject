package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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
    /**
     * A thread-safe map that stores references to client controllers by their associated player nicknames.
     * Each entry in the map associates a player's nickname (key) with a corresponding implementation
     * of the {@link CallableOnClientController} interface (value).
     *
     * This map facilitates communication between the server and clients by allowing the server to notify
     * or interact with specific clients using their unique identifiers (nicknames).
     *
     * Thread safety is ensured through the use of a {@link ConcurrentHashMap}, permitting
     * concurrent operations without external synchronization.
     */
    private Map<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
    /**
     * An {@code ExecutorService} instance used to manage and handle the execution of tasks in a cached thread pool.
     * This thread pool dynamically adjusts the number of threads based on the current workload, creating new threads
     * as needed and reusing previously constructed threads when possible.
     *
     * The {@code executor} is primarily utilized for managing client notification tasks
     * within the {@code GameClientNotifier} class, enabling asynchronous execution and efficient resource utilization.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Constructs a new instance of the GameClientNotifier class.
     * This class is responsible for managing and notifying registered client controllers
     * in the game environment.
     *
     * @param clientControllers a thread-safe map that associates unique player nicknames (String)
     *                          with their corresponding client controllers of type CallableOnClientController.
     */
    public GameClientNotifier(ConcurrentHashMap<String, CallableOnClientController> clientControllers) {
        this.clientControllers = clientControllers;
    }

    /**
     * Retrieves the map of client controllers associated with player nicknames.
     *
     * @return a map where the keys are player nicknames represented as strings,
     *         and the values are instances of CallableOnClientController, which handle communication
     *         with the respective clients.
     */
    public Map<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

    /**
     * Notifies all connected client controllers by executing the given operation on each of them.
     * The execution is performed asynchronously for each client and waits for a maximum of 5 seconds
     * for each operation to complete. If an operation on any client exceeds the timeout, it is cancelled.
     *
     * @param consumer A throwing bi-consumer that defines the operation to perform on each
     *                 client. It accepts a client's nickname and the corresponding client controller
     *                 and performs a specific operation, potentially throwing an IOException.
     */
    public void notifyAllClients(ThrowingBiConsumer<String, CallableOnClientController, IOException> consumer) {
        Map<Future<?>, String> futureNicknames = new HashMap<>();

        clientControllers.forEach((nickname, clientController) -> {
            Future<?> future = executor.submit(() -> {
                try{
                    consumer.accept(nickname, clientController);
                } catch (IOException  e) {
                    System.out.println("ERRORE notifyAllClients: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            futureNicknames.put(future, nickname);
        });

        futureNicknames.forEach((future, nickname) -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                System.err.println("TIMEOUT: Client " + nickname + " non risponde dopo 5 secondi");
                future.cancel(true);
            }
        });

    }

    /**
     * Notifies a set of clients as specified by their nicknames using a provided consumer.
     * This method allows performing custom actions on the client controllers associated with
     * the nicknames provided. The notifications are executed asynchronously, with a timeout
     * for each notification.
     *
     * @param playersNicknameToBeNotified a set containing the nicknames of the players to be notified
     * @param consumer a functional interface that defines the action to be performed for
     *                 each player to be notified, taking the player's nickname and their
     *                 corresponding client controller as parameters
     */
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
                future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                System.err.println("Timeout nella notifica del client: " + nickname);
                future.cancel(true);
            }
        });
    }

    /**
     * Notifies all active client controllers about the disconnection of a specific player
     * and proceeds to close all client connections associated with the provided game model.
     *
     * @param nicknameOfDisconectedPlayer the nickname of the player who has disconnected
     * @param gameModel the instance of the game model associated with the current game session
     */
    public void notifyDisconnection(String nicknameOfDisconectedPlayer, GameModel gameModel) {
        clientControllers.forEach((nicknameToNotify, clientController) -> {
            try {
                clientController.notifyPlayerDisconnected(nicknameToNotify, nicknameOfDisconectedPlayer );
            } catch (IOException e) {}
        });
        closeAllClients(gameModel);
    }

    /**
     * Closes all client connections associated with the current game model and marks the game as not started.
     * Each client is notified about a forced disconnection.
     *
     * @param gameModel The game model associated with the clients to be closed. This parameter is used to
     *                  mark the game as not started and provide the associated game ID for client notifications.
     */
    public void closeAllClients(GameModel gameModel){
        gameModel.setStarted(false);
        clientControllers.forEach((nicknameToNotify, clientController) -> {
            try {
                clientController.forcedDisconnection(nicknameToNotify, gameModel.getGameId());
            } catch (IOException e) {}
        });
    }

}

