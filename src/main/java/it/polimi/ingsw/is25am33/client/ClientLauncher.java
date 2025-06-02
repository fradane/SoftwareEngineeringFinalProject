package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import it.polimi.ingsw.is25am33.serializationLayer.SocketMessage;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientSerializer;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ClientLauncher {
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
                ║  -Evita meteoriti, combatti pirati, consegna cubi... diventa leggenda!-   ║
                ║                                                                           ║
                ╚═══════════════════════════════════════════════════════════════════════════╝""";
        System.out.println(titleScreen);

        SocketMessage message = new SocketMessage();
        List<Coordinates> cabinCoords = new ArrayList<>();
        cabinCoords.add(new Coordinates(0, 0));
        cabinCoords.add(new Coordinates(1, 1));
        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
                .Builder()
                .setChosenCabins(cabinCoords)
                .build();
        message.setParamChoice(playerChoiceDataStructure);

        String messaggioSerializzato = ClientSerializer.serialize(message);
        System.out.println(messaggioSerializzato);

        PlayerChoicesDataStructure messaggioDeserializzato = ServerDeserializer.deserializeObj(messaggioSerializzato, SocketMessage.class).getParamChoice();
        System.out.println(messaggioDeserializzato);


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
                ClientController clientController = new ClientController(clientModel, new ClientPingPongManager());
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
