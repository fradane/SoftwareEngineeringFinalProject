package it.polimi.ingsw.is25am33.network.rmi;

import it.polimi.ingsw.is25am33.network.DNS;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RMIServerRunnable implements Runnable {

    private final DNS dns;

    public RMIServerRunnable(DNS dns) {
        this.dns = dns;
    }

    @Override
    public void run() {

        try {

            // Crea/ottieni il registro RMI su una porta default
            Registry registry = LocateRegistry.createRegistry(NetworkConfiguration.DEFAULT_RMI_SERVER_PORT);

            // Binding dello stub col nome desiderato
            registry.rebind(NetworkConfiguration.DNS_NAME, dns);

            System.out.println("[RMI] Server ready");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
