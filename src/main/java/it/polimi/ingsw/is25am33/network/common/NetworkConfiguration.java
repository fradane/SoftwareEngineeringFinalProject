package it.polimi.ingsw.is25am33.network.common;

/**
 * Contains network configuration constants used throughout the application.
 * This class provides default ports, addresses, and DNS names required for
 * establishing network connections via both RMI and Socket protocols.
 */
public class NetworkConfiguration {
    /**
     * The default port used for RMI server connections.
     */
    public static final int DEFAULT_RMI_SERVER_PORT = 1099;

    /**
     * The default port used for Socket server connections.
     */
    public static final int DEFAULT_SOCKET_SERVER_PORT = 3000;

    /**
     * The localhost address string.
     */
    public static final String LOCALHOST = "localhost";

    /**
     * The DNS name used for server identification.
     */
    public static final String DNS_NAME = "ServerDNS";
}