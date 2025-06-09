package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import it.polimi.ingsw.is25am33.network.common.NetworkConfiguration;

import java.rmi.RemoteException;

public class ClientLauncher {

    private static Boolean isRmi = null;
    private static Boolean isGui = null;
    private static String serverAddress = NetworkConfiguration.LOCALHOST;
    private static Integer serverPort = null;

    public static void main(String[] args) {
        String titleScreen = """
                ╔═══════════════════════════════════════════════════════════════════════════╗
                ║                                                                           ║
                ║     ███████╗ █████╗ ██╗      █████╗ ██╗  ██╗██╗   ██╗                     ║
                ║    ██╔════╝ ██╔══██╗██║     ██╔══██╗╚██╗██╔╝╚██╗ ██╔╝                     ║
                ║    ██║  ███╗███████║██║     ███████║ ╚███╔╝  ╚████╔╝                      ║
                ║    ██║   ██║██╔══██║██║     ██╔══██║ ██╔██╗   ╚██╔╝                       ║
                ║    ╚██████╔╝██║  ██║███████╗██║  ██║██╔╝ ██╗   ██║                        ║
                ║     ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝                        ║
                ║                                                                           ║
                ║    ████████╗██████╗ ██╗   ██╗ ██████╗██╗  ██╗███████╗██████╗              ║
                ║    ╚══██╔══╝██╔══██╗██║   ██║██╔════╝██║ ██╔╝██╔════╝██╔══██╗             ║
                ║       ██║   ██████╔╝██║   ██║██║     █████╔╝ █████╗  ██████╔╝             ║
                ║       ██║   ██╔══██╗██║   ██║██║     ██╔═██╗ ██╔══╝  ██╔══██╗             ║
                ║       ██║   ██║  ██║╚██████╔╝╚██████╗██║  ██╗███████╗██║  ██║             ║
                ║       ╚═╝   ╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝             ║
                ║                                                                           ║
                ║  -Evita meteoriti, combatti pirati, consegna tubi... diventa leggenda!-   ║
                ║                                                                           ║
                ╚═══════════════════════════════════════════════════════════════════════════╝""";
        System.out.println(titleScreen);

        // parsing parameters
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "gui":
                    isGui = true;
                    break;
                case "cli":
                    isGui = false;
                    break;
                case "rmi":
                    isRmi = true;
                    break;
                case "socket":
                    isRmi = false;
                    break;
                case "-ip":
                    if (i + 1 < args.length) {
                        serverAddress = args[++i];
                    } else {
                        System.err.println("Missing value for -ip");
                    }
                    break;
                case "-port":
                    if (i + 1 < args.length) {
                        try {
                            serverPort = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port format");
                        }
                    } else {
                        System.err.println("Missing value for -port");
                    }
                    break;
                default:

            }
        }

        if (isRmi == null) {
            System.err.println("Invalid arguments: did not specify whether rmi or socket ");
            return;
        }

        if (serverPort == null) {
            serverPort = isRmi ? NetworkConfiguration.DEFAULT_RMI_SERVER_PORT : NetworkConfiguration.DEFAULT_SOCKET_SERVER_PORT;
        }

        if (isGui == null) {
            System.err.println("Invalid arguments: did not specify whether gui or cli ");
            return;
        }

        if (isGui) launchGui();
        else launchCli();

    }

    private static void launchCli() {
        try {
            ClientCLIView cli = new ClientCLIView();
            ClientModel clientModel = new ClientModel();
            ClientController clientController = new ClientController(clientModel, new ClientPingPongManager());
            clientController.setView(cli);
            cli.setClientController(clientController);
            cli.setClientModel(clientModel);

            CallableOnDNS dns = clientController.selectNetworkProtocol(isRmi, serverAddress, serverPort);

            if (dns == null) {
                System.err.println("Invalid parameters. Exiting...");
                return;
            }
            clientController.setDns(dns);
            cli.initialize();
            clientController.run();
        } catch (RemoteException e) {
            System.err.println("Error while connecting to the server. Exiting...");
        }
    }

    private static void launchGui() {
        new Thread(() -> {
            ClientGuiController gui;

            while (ClientGuiController.getInstance() == null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            gui = ClientGuiController.getInstance();
            gui.getInitializationDoneFuture().join();

            ClientController clientController = gui.getClientController();
            clientController.setView(gui);

            CallableOnDNS dns = clientController.selectNetworkProtocol(isRmi, serverAddress, serverPort);
            if (dns == null) {
                System.err.println("Invalid parameters. Exiting...");
                return;
            }
            clientController.setDns(dns);
            clientController.run();
        }).start();

        ClientGuiController.launch(ClientGuiController.class);
    }

}
