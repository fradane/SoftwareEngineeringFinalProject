package it.polimi.ingsw.is25am33.network.rmi;

import it.polimi.ingsw.is25am33.network.DNS;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RMIServerRunnable implements Runnable {

    private final DNS dns;
    private final String serverIP;

    public RMIServerRunnable(DNS dns, String serverIP) {
        this.dns = dns;
        this.serverIP = serverIP;
    }

    @Override
    public void run() {

        try {

            //configuration system properties for cross-platform compatibility
            System.setProperty("java.rmi.server.hostname", serverIP);
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.rmi.server.useLocalHostname", "true");
            
            System.out.println("[RMI] Configured system properties for cross-platform compatibility");
            System.out.println("[RMI] Using hostname: " + serverIP);

            // create/obtain the RMI registry on a default port
            Registry registry = LocateRegistry.createRegistry(NetworkConfiguration.DEFAULT_RMI_SERVER_PORT);

            //binding stup with the desired name
            registry.rebind(NetworkConfiguration.DNS_NAME, dns);

            System.out.println("[RMI] Server RMI ready on " + serverIP + ":" + NetworkConfiguration.DEFAULT_RMI_SERVER_PORT);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
