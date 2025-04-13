package it.polimi.ingsw.is25am33.model;

import java.util.ArrayList;
import java.util.List;

public class GameContext {
    private final String gameId;
    private final List<Observer> observers = new ArrayList<>();
    private final VirtualServer virtualServer;

    public GameContext(String gameId, VirtualServer virtualServer) {
        this.gameId = gameId;
        this.virtualServer = virtualServer;
    }

    public String getGameId() {
        return gameId;
    }

    public VirtualServer getVirtualServer(){
        return virtualServer;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public Observer getObserver(String observerId){
        for (Observer observer : observers) {
            if(observer.getId().equals(observerId)){
                return observer;
            }
        }
        return null;
    }


}

