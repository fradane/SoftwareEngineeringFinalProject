package it.polimi.ingsw.is25am33.network;

import java.util.Map;
import java.util.concurrent.*;

public class ServerPingPongManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> pingTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pongTimeouts = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public void start(String nickname, Runnable sendPing) {

        ScheduledFuture<?> pingFuture = scheduler.scheduleAtFixedRate(() -> {
            sendPing.run(); // invia ping
            //TODO cambiare a milliseconds
        }, 1000, 7000, TimeUnit.SECONDS);

        pingTasks.put(nickname, pingFuture);

    }

    private void resetTimeout(String nickname, Runnable onTimeout) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null) pongTimeout.cancel(false);

            ScheduledFuture<?> pongFuture = scheduler.schedule(() -> {
               System.out.println("DISCONNESSIONE: Nessun pong ricevuto da " + nickname + ".");
                stop(nickname);
                onTimeout.run();
            }, 8000, TimeUnit.SECONDS); // TODO cambiare a MILLISECONDS

            pongTimeouts.put(nickname, pongFuture);
        }
    }

    public void onPongReceived(String nickname, Runnable onTimeout) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null) pongTimeout.cancel(false);
            //System.out.println("[PONG RECEIVED ON SERVER] Timeout pong cancellato per " + nickname);
            resetTimeout(nickname,onTimeout);
        }
    }

    public void stop(String nickname) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null) {
                pongTimeout.cancel(false);
                //System.out.println("[STOP] Timeout pong cancellato per " + nickname);
            } else {
                //System.out.println("[STOP] Nessun timeout pong trovato per " + nickname);
            }
            ScheduledFuture<?> pingTask = pingTasks.remove(nickname);
            if (pingTask != null) {
                pingTask.cancel(false);
                //System.out.println("[STOP] Task ping cancellato per " + nickname);
            } else {
                //System.out.println("[STOP] Nessun task ping trovato per " + nickname);
            }
        }
    }

    public void shutdown() {
        synchronized (lock) {
            pingTasks.values().forEach(task -> task.cancel(false));
            pongTimeouts.values().forEach(task -> task.cancel(false));
            scheduler.shutdownNow();
            pingTasks.clear();
            pongTimeouts.clear();
        }
    }
}
