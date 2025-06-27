package it.polimi.ingsw.is25am33.client;
import java.util.concurrent.*;

public class ClientPingPongManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> pingTask = null;
    private ScheduledFuture<?> pongTimeout = null;
    private final Object lock = new Object();

    /**
     * Starts the ping-pong mechanism by scheduling periodic ping messages.
     * The first ping is sent after 1 second, then every 5 seconds.
     *
     * @param sendPing the runnable that implements the ping sending logic
     */
    public void start(Runnable sendPing) {
        pingTask = scheduler.scheduleAtFixedRate(() -> {
            sendPing.run(); // invia ping
        }, 1000, 2000, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the timeout timer for pong responses.
     * If a pong is not received within 9 seconds, the timeout handler is executed.
     *
     * @param onTimeout the runnable to execute when timeout occurs
     */
    private void resetTimeout(Runnable onTimeout) {

        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);

            pongTimeout = scheduler.schedule(() -> {
                stop();
                onTimeout.run();
            }, 9000, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * Handles the reception of a pong message.
     * Cancels the current timeout and starts a new timeout period.
     *
     * @param onTimeout the runnable to execute if no pong is received within the timeout period
     */
    public void onPongReceived(Runnable onTimeout) {
        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);
            //System.out.println("[PONG RECEIVED ON CLIENT]");
            resetTimeout(onTimeout);
        }
    }

    /**
     * Stops the ping-pong mechanism by canceling both ping and pong timeout tasks.
     * Does not shut down the scheduler.
     */
    public void stop() {
        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);
            if (pingTask != null) pingTask.cancel(false);
        }
    }

    /**
     * Performs a complete shutdown of the ping-pong mechanism.
     * Cancels all tasks and shuts down the scheduler immediately.
     */
    public void shutdown() {
        synchronized (lock) {
            pongTimeout.cancel(false);
            pingTask.cancel(false);
            scheduler.shutdownNow();
        }
    }
}
