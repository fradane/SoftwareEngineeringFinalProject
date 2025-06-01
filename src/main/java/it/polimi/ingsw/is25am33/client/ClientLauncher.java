package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import it.polimi.ingsw.is25am33.serializationLayer.SocketMessage;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientSerializer;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class ClientLauncher {
    public static void main(String[] args) {
        String titleScreen ="╔═══════════════════════════════════════════════════════════════════════════╗\n" +
                            "║                                                                           ║\n" +
                            "║     ███████╗ █████╗ ██╗      █████╗ ██╗  ██╗██╗   ██╗                     ║\n" +
                            "║    ██╔════╝ ██╔══██╗██║     ██╔══██╗╚██╗██╔╝╚██╗ ██╔╝                     ║\n" +
                            "║    ██║  ███╗███████║██║     ███████║ ╚███╔╝  ╚████╔╝                      ║\n" +
                            "║    ██║   ██║██╔══██║██║     ██╔══██║ ██╔██╗   ╚██╔╝                       ║\n" +
                            "║    ╚██████╔╝██║  ██║███████╗██║  ██║██╔╝ ██╗   ██║                        ║\n" +
                            "║     ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝                        ║\n" +
                            "║                                                                           ║\n" +
                            "║    ████████╗██████╗ ██╗   ██╗ ██████╗██╗  ██╗███████╗██████╗              ║\n" +
                            "║    ╚══██╔══╝██╔══██╗██║   ██║██╔════╝██║ ██╔╝██╔════╝██╔══██╗             ║\n" +
                            "║       ██║   ██████╔╝██║   ██║██║     █████╔╝ █████╗  ██████╔╝             ║\n" +
                            "║       ██║   ██╔══██╗██║   ██║██║     ██╔═██╗ ██╔══╝  ██╔══██╗             ║\n" +
                            "║       ██║   ██║  ██║╚██████╔╝╚██████╗██║  ██╗███████╗██║  ██║             ║\n" +
                            "║       ╚═╝   ╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝             ║\n" +
                            "║                                                                           ║\n" +
                            "║  -Evita meteoriti, combatti pirati, consegna tubi... diventa leggenda!-   ║\n" +
                            "║                                                                           ║\n" +
                            "╚═══════════════════════════════════════════════════════════════════════════╝";
        System.out.println(titleScreen);

        SocketMessage message = new SocketMessage();
        Map<Coordinates, CrewMember> crewMembers = new HashMap<>();
        crewMembers.put(new Coordinates(2, 1), CrewMember.HUMAN);
        message.setParamCrewChoices(crewMembers);

        String prova = ClientSerializer.serialize(message);
        System.out.println(prova);


        if (args.length != 2) {
            System.err.println("Invalid parameters. Exiting...");
            return;
        }

        if (args[0].equals("gui")) {

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

                CallableOnDNS dns = clientController.selectNetworkProtocol(args[1]);
                if (dns == null) {
                    System.err.println("Invalid parameters. Exiting...");
                    return;
                }
                clientController.setDns(dns);
                clientController.run();
            }).start();

            ClientGuiController.launch(ClientGuiController.class);

        } else if (args[0].equals("cli")) {

            try {
                ClientView cli = new ClientCLIView();
                ClientModel clientModel = new ClientModel();
                ClientController clientController = new ClientController(clientModel);
                clientController.setView(cli);
                ((ClientCLIView) cli).setClientController(clientController);
                ((ClientCLIView) cli).setClientModel(clientModel);

                CallableOnDNS dns = clientController.selectNetworkProtocol(args[1]);
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
        } else {
            System.err.println("Invalid parameters. Exiting...");
        }

    }

}
