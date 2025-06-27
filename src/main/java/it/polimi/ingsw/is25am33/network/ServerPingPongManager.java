package it.polimi.ingsw.is25am33.network;

import java.util.Map;
import java.util.concurrent.*;

public class ServerPingPongManager {
    /**
     * A ScheduledExecutorService responsible for managing and scheduling periodic or delayed tasks
     * for sending "ping" messages and handling connection timeouts for a server-client communication.
     *
     * This executor service uses a fixed thread pool size of 2 to execute scheduled tasks.
     * It supports scheduling tasks at fixed intervals or after a specified delay, enabling efficient
     * management of timed operations such as sending pings or monitoring the receipt of pongs.
     *
     * The scheduler coordinates the execution of tasks in a thread-safe manner, ensuring proper interaction
     * with other components that rely on scheduled ping and pong operations within the containing class.
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    /**
     * A mapping of nicknames to their corresponding scheduled ping tasks.
     * Each entry represents a scheduled recurring task that sends ping messages
     * to a specific client or user identified by their nickname.
     *
     * This map is used to manage and track ping tasks associated with different
     * nicknames, allowing for their addition, cancellation, and retrieval to
     * ensure timely monitoring and communication.
     *
     * Thread-safe operations are ensured through the use of a {@code ConcurrentHashMap}.
     */
    private final Map<String, ScheduledFuture<?>> pingTasks = new ConcurrentHashMap<>();
    /**
     * A thread-safe map that associates a user identifier (nickname) with a scheduled
     * future task representing a timeout for receiving a "pong" message.
     * This map is used to manage and track timeout tasks to ensure a user
     * responds within a specific time frame.
     *
     * Key: The string identifier (e.g., nickname) of the user.
     * Value: The scheduled future representing the timeout task for the associated user.
     *
     * This field is part of the ServerPingPongManager class for managing "ping-pong"
     * operations in a server context, ensuring timely responses from users and
     * handling disconnections in case of response timeouts.
     */
    private final Map<String, ScheduledFuture<?>> pongTimeouts = new ConcurrentHashMap<>();
    /**
     * An object used as a synchronization lock to ensure thread-safe access
     * and modification of shared resources in the context of managing ping-pong
     * tasks and timeouts for a server-client communication system.
     */
    private final Object lock = new Object();

    /**
     * Starts a repetitive ping task for the specified user. The task sends periodic
     * ping messages at fixed intervals and manages the scheduling using a {@link ScheduledExecutorService}.
     * The task is associated with the given nickname and stored for later management.
     *
     * @param nickname the unique identifier for the user associated with the ping task
     * @param sendPing a {@link Runnable} task responsible for sending a ping message
     */
    public void start(String nickname, Runnable sendPing) {
        synchronized (lock) {
            ScheduledFuture<?> pingFuture = scheduler.scheduleAtFixedRate(() -> {
                sendPing.run(); // invia ping
            }, 1000, 3000, TimeUnit.MILLISECONDS);

            pingTasks.put(nickname, pingFuture);
        }
    }

    /**
     * Resets the timeout for a user by cancelling any existing timeout task and scheduling a new one.
     * If the timeout expires, the user is marked as disconnected, and the provided onTimeout action is executed.
     *
     * @param nickname the nickname of the user for whom the timeout is being reset
     * @param onTimeout a {@code Runnable} action to execute when the timeout expires
     */
    private void resetTimeout(String nickname, Runnable onTimeout) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null) pongTimeout.cancel(false);

            ScheduledFuture<?> pongFuture = scheduler.schedule(() -> {
               System.out.println("DISCONNECTION: No pong received from " + nickname + ".");
                stop(nickname);
                onTimeout.run();
            }, 9500, TimeUnit.MILLISECONDS);

            pongTimeouts.put(nickname, pongFuture);
        }
    }

    /**
     * Handles the reception of a "pong" message from a specific client. This method cancels
     * any previously scheduled timeout for the client and resets the timeout for the next expected "pong".
     *
     * @param nickname the unique identifier of the client from which the "pong" was received.
     * @param onTimeout the callback to execute if the next "pong" is not received within the timeout period.
     */
    public void onPongReceived(String nickname, Runnable onTimeout) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null) pongTimeout.cancel(false);
            resetTimeout(nickname,onTimeout);
        }
    }

    /**
     * Stops the ping-pong monitoring for the specified nickname by canceling any associated
     * ping tasks and pong timeout tasks if they exist.
     *
     * @param nickname the identifier for the client whose ping-pong monitoring tasks are to be canceled.
     */
    public void stop(String nickname) {
        synchronized (lock) {
            ScheduledFuture<?> pongTimeout = pongTimeouts.remove(nickname);
            if (pongTimeout != null)
                pongTimeout.cancel(false);
            ScheduledFuture<?> pingTask = pingTasks.remove(nickname);
            if (pingTask != null)
                pingTask.cancel(false);
        }
    }

}
