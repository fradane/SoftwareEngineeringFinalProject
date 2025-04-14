package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;
import it.polimi.ingsw.is25am33.network.rmi.client.RMIClientNetworkManager;

import java.rmi.RemoteException;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws RemoteException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Galaxy Trucker Client ===");

        // Selezione dell'interfaccia utente
        ClientView view = selectUserInterface(scanner);

        //String nickname = view.askNickname();

        // Creiamo il controller
        ClientController controller = new ClientController(view);

        // Selezione del protocollo di rete
        ClientNetworkManager networkManager = selectNetworkProtocol(scanner, controller);

        // Avviamo l'applicazione
        controller.setNetworkManager(networkManager);
        controller.start();
    }

    /**
     * Permette all'utente di selezionare l'interfaccia utente
     * @param scanner Scanner per leggere l'input dell'utente
     * @return L'implementazione di ClientView scelta
     */
    private static ClientView selectUserInterface(Scanner scanner) {
        System.out.println("Select user interface:");
        System.out.println("1. Command Line Interface (CLI)");
        System.out.println("2. Graphical User Interface (GUI)");

        while (true) {
            System.out.print("Your choice: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        return new ClientCLIView();
                    case 2:
                        //return new ClientGUIView();
                    default:
                        System.out.println("Invalid choice. Please enter 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    /**
     * Permette all'utente di selezionare il protocollo di rete
     * @param scanner Scanner per leggere l'input dell'utente
     * @return L'implementazione di NetworkManager scelta
     */
    private static ClientNetworkManager selectNetworkProtocol(Scanner scanner, ClientController controller) throws RemoteException {
        System.out.println("Select network protocol:");
        System.out.println("1. RMI (Remote Method Invocation)");
        System.out.println("2. Socket TCP/IP");

        while (true) {
            System.out.print("Your choice: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        return new RMIClientNetworkManager(controller);
                    case 2:
                        //return new SocketNetworkManager(nickname);
                    default:
                        System.out.println("Invalid choice. Please enter 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
