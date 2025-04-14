package it.polimi.ingsw.is25am33.Client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Implementazione dell'interfaccia ClientView per una Command Line Interface
 */
public class ClientCLIView implements ClientView {
    private final Scanner scanner;
    private volatile boolean waitingForGameStart = false;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean waitingForInput = false;


    // Definizione di un colore rosso ANSI per gli errori (funziona nei terminali che supportano i colori ANSI)
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";



    public ClientCLIView() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void initialize() {
        // Avvia il thread di input
        Thread inputThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Leggi l'input e mettilo nella coda
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine();
                        inputQueue.put(input);
                    }
                    // Piccola pausa per evitare uso eccessivo della CPU
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        inputThread.setDaemon(true); // Termina quando il thread principale termina
        inputThread.start();

        System.out.println("=== Galaxy Trucker Client ===");
    }

    /**
     * Richiede input all'utente in modo non bloccante
     * @param prompt Il messaggio da mostrare
     * @return L'input dell'utente o stringa vuota in caso di interruzione
     */
    private String askForInput(String prompt) {
        System.out.print(prompt);
        waitingForInput = true;

        try {
            String input = null;
            // Controlla periodicamente se è arrivato input
            while (input == null && waitingForInput) {
                input = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                // Qui puoi inserire codice per gestire notifiche dal server
            }

            return input != null ? input : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            waitingForInput = false;
        }
    }

    /**
     * Metodo per interrompere l'attesa dell'input in caso di eventi importanti
     * Questo metodo va chiamato quando arriva una notifica importante dal server
     */
    public void cancelInputWaiting() {
        waitingForInput = false;
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void showError(String errorMessage) {
        System.out.println(ANSI_RED + "Error: " + errorMessage + ANSI_RESET);
    }

    @Override
    public String askNickname() {
        return askForInput("Enter your nickname: ");
    }

    @Override
    public String askServerAddress() {
        String address = askForInput("Enter server address (default: localhost): ");
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
            String input = askForInput("Number of players (2-4): ");
            try {
                int numPlayers = Integer.parseInt(input);
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
        String isTest = askForInput("Test flight (y/n): ");
        result[1] = isTest.equalsIgnoreCase("y") ? 1 : 0;

        // Scegli il colore
        result[2] = Integer.parseInt(askPlayerColor());

        return result;
    }

    @Override
    public String[] askJoinGame(Iterable<GameInfo> games) {
        showAvailableGames(games);

        String[] result = new String[2]; // [gameId, colorChoice]
        result[0] = askForInput("Enter game ID to join: ");
        result[1] = askPlayerColor();

        return result;
    }

    public String askPlayerColor() {
        System.out.println("Choose your color: ");
        System.out.println("1. RED");
        System.out.println("2. BLUE");
        System.out.println("3. GREEN");
        System.out.println("4. YELLOW");

        while (true) {
            String input = askForInput("Your choice: ");
            try {
                int colorChoice = Integer.parseInt(input);
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
            System.out.println("\nChoose an option:");
            System.out.println("1. List available games");
            System.out.println("2. Create a new game");
            System.out.println("3. Join a game");
            System.out.println("4. Exit");

            String input = askForInput("Your choice: ");
            try {
                int choice = Integer.parseInt(input);
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
        System.out.println("\nEnter 1 if you want to leave the game otherwise wait for the game to start...\n");
        waitingForGameStart = true;

        try {
            while (waitingForGameStart) {
                // Controlla l'input per 500ms alla volta
                String input = inputQueue.poll(500, TimeUnit.MILLISECONDS);
                if (input != null && input.equals("1")) {
                    waitingForGameStart = false;
                    return 1; // Lascia il gioco
                }

                // Se il gioco è iniziato nel frattempo (tramite notifica dal server)
                if (!waitingForGameStart) {
                    return 0; // Il gioco è iniziato
                }
            }

            return 0; // Il gioco è iniziato
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 1; // In caso di interruzione, esci
        }
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {
        System.out.println(nickname + " joined the game with color "+ gameInfo.getPlayersAndColors().get(nickname) + ". Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {
        System.out.println(nickname + " left the game. Players: " +
                gameInfo.getConnectedPlayersNicknames().size() + "/" +
                gameInfo.getMaxPlayers());
    }

    public void notifyGameCreated(String gameId) {
        System.out.println("Game created! ID: " + gameId);
    }

    @Override
    public void notifyGameStarted(GameState gameState) {
        waitingForGameStart = false;
        System.out.println("Game started! Initial state: " + gameState);
        System.out.println("The game is now in progress...");
    }

    @Override
    public void notifyGameEnded(String reason) {
        System.out.println("Game ended. Reason: " + reason);
    }
}