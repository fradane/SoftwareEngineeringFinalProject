package it.polimi.ingsw.is25am33.client.model;

import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.view.ClientView;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents an Hourglass timer with a fixed duration and limited flips.
 * The Hourglass can be started and provides notifications through a client view.
 */
public class Hourglass {

    private ScheduledExecutorService scheduler;
    private int timeLeft;
    private int flipsLeft;
    private boolean isRunning;
    private final ClientController controller;

    /**
     * Constructs an Hourglass instance with a specified mode indicating if it is for a test flight.
     *
     * @param isTestFlight if true, the Hourglass is initialized for a test flight with zero flips left;
     *                     if false, it is initialized with two flips left.
     */
    public Hourglass(boolean isTestFlight, ClientController controller) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = false;
        this.controller = controller;
        this.flipsLeft = isTestFlight ? 1 : 3;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getFlipsLeft() {
        return flipsLeft;
    }

    /**
     * Starts the hourglass timer and updates the client view with the remaining time
     * and flip count. The timer counts down from 60 seconds and notifies the client
     * view when the timer ends. If a timer is already running, it will shut down
     * before starting a new one.
     *
     * @param view the client view used to update the remaining time
     *             and notify when the timer ends
     */
    public void start(ClientView view, String nickname) {

        if (scheduler != null) {
            scheduler.shutdown();
        }

        flipsLeft--;

        view.notifyHourglassStarted(flipsLeft, nickname);

        scheduler = Executors.newScheduledThreadPool(1);

        isRunning = true;
        timeLeft = 1; //TODO ricambiare a 10 secondi
        scheduler.scheduleAtFixedRate(() -> {

            if (timeLeft > 0) {
                view.updateTimeLeft(timeLeft);
                timeLeft--;
            } else {
                scheduler.shutdown();
                view.notifyTimerEnded(flipsLeft);

                try {
                    controller.notifyHourglassEnded();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                isRunning = false;
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

}
