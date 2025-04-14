package it.polimi.ingsw.is25am33.Client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Implementazione dell'interfaccia ClientView per una Command Line Interface
 */
public class ClientCLIView implements ClientView {
    private final Scanner scanner;

    public ClientCLIView() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void initialize() {
        System.out.println("=== Galaxy Trucker Client ===");
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void showError(String errorMessage) {
        System.err.println("Error: " + errorMessage);
    }

    @Override
    public String askNickname() {
        System.out.print("Enter your nickname: ");
        return scanner.nextLine();
    }

    @Override
    public String askServerAddress() {
        System.out.print("Enter server address (default: localhost): ");
        String address = scanner.nextLine();
        return address.isEmpty() ? "localhost" : address;
    }

    @Override
    public void showAvailableGames(Iterable<GameInfo> games) {
        boolean hasGames = false;

        System.out.println("Available games:");
        for (GameInfo game : games) {
            hasGames = true;
            System.out.println("ID: " + game.getGameId() +
                    " | Players: " + game.getConnectedPlayersNicknames().size() + "/" + game.getMaxPlayers() +
                    " | Test Flight: " + (game.isTestFlight() ? "Yes" : "No"));
        }

        if (!hasGames) {
            System.out.println("No games available.");
        }
    }

    @Override
    public int[] askCreateGame() {
        int[] result = new int[3]; // [numPlayers, isTestFlight, colorChoice]

        // Chiedi numero di giocatori
        while (true) {
            try {
                System.out.print("Number of players (2-4): ");
                int numPlayers = Integer.parseInt(scanner.nextLine());
                if (numPlayers >= 2 && numPlayers <= 4) {
                    result[0] = numPlayers;
                    break;
                } else {
                    System.out.println("Invalid number. Must be between 2 and 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        // Chiedi se è un volo di prova
        System.out.print("Test flight (y/n): ");
        result[1] = scanner.nextLine().equalsIgnoreCase("y") ? 1 : 0;

        // Scegli il colore
        result[2] = Integer.parseInt(askPlayerColor());

        return result;
    }

    @Override
    public String[] askJoinGame(Iterable<GameInfo> games) {
        showAvailableGames(games);

        String[] result = new String[2]; // [gameIndex, colorChoice]

        // Chiedi quale gioco
        System.out.print("Enter game ID to join: ");
        String gameId = scanner.nextLine();
        result[0] = gameId; // Usa l'ID come stringa, non un indice

        // Scegli il colore
        result[1] = askPlayerColor();

        return result;
    }

    private String askPlayerColor() {
        while (true) {
            try {
                System.out.println("Choose your color: ");
                System.out.println("1. RED");
                System.out.println("2. BLUE");
                System.out.println("3. GREEN");
                System.out.println("4. YELLOW");
                System.out.print("Your choice: ");
                int colorChoice = Integer.parseInt(scanner.nextLine());
                if (colorChoice >= 1 && colorChoice <= 4) {
                    return Integer.toString(colorChoice);
                } else {
                    System.out.println("Invalid choice. Please select 1-4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    @Override
    public int showMainMenu() {
        while (true) {
            try {
                System.out.println("\nChoose an option:");
                System.out.println("1. List available games");
                System.out.println("2. Create a new game");
                System.out.println("3. Join a game");
                System.out.println("4. Exit");
                System.out.print("Your choice: ");
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 1 && choice <= 4) {
                    return choice;
                } else {
                    System.out.println("Invalid choice. Please select 1-4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    @Override
    public int showGameMenu() {
        while (true) {
            try {
                System.out.println("\nGame options:");
                System.out.println("1. Wait for game to start");
                System.out.println("2. Leave game");
                System.out.print("Your choice: ");
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 1 && choice <= 2) {
                    return choice;
                } else {
                    System.out.println("Invalid choice. Please select 1-2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {
        System.out.println(nickname + " joined the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {
        System.out.println(nickname + " left the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyGameStarted(GameState gameState) {
        System.out.println("Game started! Initial state: " + gameState);
        System.out.println("The game is now in progress...");
    }

    @Override
    public void notifyGameEnded(String reason) {
        System.out.println("Game ended. Reason: " + reason);
    }
}