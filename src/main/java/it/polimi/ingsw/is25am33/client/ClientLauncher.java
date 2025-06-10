package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.tui.ClientCLIView;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.network.CallableOnDNS;
import it.polimi.ingsw.is25am33.serializationLayer.SocketMessage;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientDeserializer;
import it.polimi.ingsw.is25am33.serializationLayer.client.ClientSerializer;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import static it.polimi.ingsw.is25am33.model.enumFiles.Direction.*;

public class ClientLauncher {
    public static void main(String[] args) throws IOException {
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

//        SocketMessage message = new SocketMessage();
//        Map<Direction,ConnectorType> connectors = new HashMap<>();
//        connectors.put(NORTH,ConnectorType.EMPTY);
//        connectors.put(EAST,ConnectorType.EMPTY);
//        connectors.put(SOUTH,ConnectorType.UNIVERSAL);
//        connectors.put(WEST,ConnectorType.SINGLE);
//
//        Shield cannon = new Shield(connectors);
//
//        message.setParamComponent(cannon);

//        List<Coordinates> doubleCannonCoords = new ArrayList<>();
//        doubleCannonCoords.add(new Coordinates(0, 0));
//        doubleCannonCoords.add(new Coordinates(1, 1));
//
//        List<Coordinates> batteryBoxCoords = new ArrayList<>();
//        batteryBoxCoords.add(new Coordinates(0,    1));
//        batteryBoxCoords.add(new Coordinates(1, 2));
//
//        PlayerChoicesDataStructure playerChoiceDataStructure = new PlayerChoicesDataStructure
//                .Builder()
//                .setChosenDoubleCannons(doubleCannonCoords)
//                .setChosenBatteryBoxes(batteryBoxCoords)
//                .build();
//
//        message.setParamChoice(playerChoiceDataStructure);

//        //System.out.println(cannon.getDirections());
//        System.out.println(message.getParamComponent());
//
//        String messaggioSerializzato = ServerSerializer.serialize(message);
//        System.out.println(messaggioSerializzato);
//
//        Component messaggioDeserializzato = ClientDeserializer.deserialize(messaggioSerializzato, SocketMessage.class).getParamComponent();
//        System.out.println(messaggioDeserializzato);


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
