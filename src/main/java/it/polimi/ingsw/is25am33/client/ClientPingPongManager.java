package it.polimi.ingsw.is25am33.client;
import java.util.concurrent.*;

public class ClientPingPongManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> pingTask = null;
    private ScheduledFuture<?> pongTimeout = null;
    private final Object lock = new Object();

    public void start(Runnable sendPing) {
        pingTask = scheduler.scheduleAtFixedRate(() -> {
            sendPing.run(); // invia ping
        }, 1000, 7000, TimeUnit.MILLISECONDS);
    }

    private void resetTimeout(Runnable onTimeout) {
        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);

            pongTimeout = scheduler.schedule(() -> {
               System.out.println("DISCONNESIONE Nessun pong ricevuto dal server .");
                stop();
                onTimeout.run();
            }, 8000, TimeUnit.MILLISECONDS); // TODO cambiare a MILLISECONDS
        }

    }

    public void onPongReceived(Runnable onTimeout) {
        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);
            //System.out.println("[PONG RECEIVED ON CLIENT] Timeout pong cancellato.");
            resetTimeout(onTimeout);
        }
    }

    public void stop() {
        synchronized (lock) {
            if (pongTimeout != null) pongTimeout.cancel(false);
            if (pingTask != null) pingTask.cancel(false);
        }
    }

    public void shutdown() {
        synchronized (lock) {
            pongTimeout.cancel(false);
            pingTask.cancel(false);
            scheduler.shutdownNow();
        }
    }
}
