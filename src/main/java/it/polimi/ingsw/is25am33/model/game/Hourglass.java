package it.polimi.ingsw.is25am33.model.game;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Hourglass {

    // TODO aggiungere game context o simili da cui fare le notify broadcast, e completare il codice

    private ScheduledExecutorService scheduler;
    private int timeLeft;
    private int flipsLeft;
    // game context

    public Hourglass(/*game context, */ int durationInSeconds, int flipsLeft) {
        // game context
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
                // gameContext.notifyTimeUpdate(timeLeft);
            } else {
                // gameContext.notifyTimeExpired();
                scheduler.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

}
