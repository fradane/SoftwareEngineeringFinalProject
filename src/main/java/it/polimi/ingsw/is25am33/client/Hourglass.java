package it.polimi.ingsw.is25am33.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Hourglass {

    private ScheduledExecutorService scheduler;
    private int timeLeft;
    private int flipsLeft;

    public Hourglass(int durationInSeconds, int flipsLeft) {
        this.timeLeft = durationInSeconds;
        this.flipsLeft = flipsLeft;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public int getFlipsLeft() {
        return flipsLeft;
    }

    public void start() {
        flipsLeft--;
        scheduler.scheduleAtFixedRate(() -> {
            if (timeLeft > 0) {
                timeLeft--;
            } else {
                scheduler.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

}
