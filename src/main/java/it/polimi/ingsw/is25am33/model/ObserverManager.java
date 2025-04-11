package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.model.game.GameEvent;

import javax.management.remote.rmi.RMIServer;
import java.util.ArrayList;
import java.util.List;

public class ObserverManager {
    private List<Observer> observers;

    private ObserverManager() {
        observers = new ArrayList<>();
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public Observer getObserver(int index) {
        return observers.get(index);
    }

    public List<Observer> getObservers() {
        return observers;
    }


    public void notifyObserver(String id,GameEvent event) {
        for (Observer observer : observers) {
            if(observer.getId().equals(id)) {
                observer.notify(event);
            }
        }
    }
}

