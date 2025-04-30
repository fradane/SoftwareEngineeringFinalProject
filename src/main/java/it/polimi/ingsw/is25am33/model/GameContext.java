package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameContext {
    private  Map<String, CallableOnClientController> clientControllers;

    public GameContext(Map<String, CallableOnClientController> clientControllers) {
        this.clientControllers = clientControllers;
    }

    public Map<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

}

