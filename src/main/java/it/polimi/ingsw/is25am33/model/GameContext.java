package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;

import java.util.ArrayList;
import java.util.List;

public class GameContext {
    private final String gameId;
    private final List<Observer> observers = new ArrayList<>();
    private final CallableOnClientController clientController;

    public GameContext(String gameId, CallableOnClientController clientController) {
        this.gameId = gameId;
        this.clientController = clientController;
    }

    public String getGameId() {
        return gameId;
    }

    public CallableOnClientController getVirtualServer(){
        return clientController;
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

