package it.polimi.ingsw.is25am33.network.common;

import it.polimi.ingsw.is25am33.client.ClientController;
import javafx.util.Pair;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class ConnectionManager extends UnicastRemoteObject {

    private Map<String, ClientController> clients;
    private Map<String, List<String>> clientsPerGame;

    public ConnectionManager() throws RemoteException {
        super();
    }

    public ClientController getControllerOf(String nickname) {
        return clients.get(nickname);
    }

    public List<Pair<String, ClientController>> getControllerPerClientPerGame(String gameId) {
        return clientsPerGame.get(gameId)
                .stream()
                .map(nickname -> new Pair<>(nickname, getControllerOf(nickname)))
                .toList();
    }

    public Map<String, ClientController> getClients() {
        return clients;
    }

    public Map<String, List<String>> getClientsPerGame() {
        return clientsPerGame;
    }

}
