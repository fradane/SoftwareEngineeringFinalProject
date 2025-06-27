package it.polimi.ingsw.is25am33.network.rmi;

import it.polimi.ingsw.is25am33.network.DNS;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


/**
 * A runnable class that initializes and starts an RMI server.
 * This class configures the necessary system properties, creates an RMI registry,
 * and binds the DNS object to make it accessible to remote clients.
 */
public class RMIServerRunnable implements Runnable {

    private final DNS dns;
    private final String serverIP;

    /**
     * Constructs an RMI server runnable with the specified DNS and server IP address.
     *
     * @param dns the DNS object to be bound in the RMI registry
     * @param serverIP the IP address on which the RMI server will run
     */
    public RMIServerRunnable(DNS dns, String serverIP) {
        this.dns = dns;
        this.serverIP = serverIP;
    }

    /**
     * Executes the RMI server initialization and startup process.
     * This method sets the necessary system properties for cross-platform compatibility,
     * creates an RMI registry on the default port, and binds the DNS object with
     * the configured name. Error messages are printed to standard output in case of exceptions.
     */
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