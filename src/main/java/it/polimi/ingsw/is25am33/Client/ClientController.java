package it.polimi.ingsw.is25am33.Client;

import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;

import java.rmi.RemoteException;
import java.util.List;

public class ClientController {
    private final ClientView view;
    private ClientNetworkManager networkManager;
    private String nickname;
    private String currentGameId;
    private boolean inGame;
    private boolean gameStarted;

    /**
     * Costruttore del controller
     * @param view La view da utilizzare (CLI o GUI)
     */
    public ClientController(ClientView view, ClientNetworkManager networkManager) {
        this.view = view;
        this.inGame = false;
        this.gameStarted = false;
        this.networkManager = networkManager;
    }

    /**
     * Inizia l'esecuzione dell'applicazione
     */
    public void start() {
        view.initialize();

        try {
            // Setup iniziale
            setupConnection();

            // Loop principale dell'applicazione
            boolean running = true;
            while (running) {
                if (inGame) {
                    running = handleGameState();
                } else {
                    running = handleMainMenu();
                }
            }

            // Disconnessione
            disconnect();

        } catch (Exception e) {
            view.showError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configura la connessione al server
     * @throws RemoteException in caso di errori di comunicazione
     */
    private void setupConnection() throws RemoteException {
        String serverAddress = view.askServerAddress();

        // Crea un'implementazione RMI con callback per le notifiche
        try {
            networkManager.connectToServer(serverAddress);
            view.showMessage("Connected to server as " + nickname);
        } catch (RemoteException e) {
            view.showError("Connection failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Gestisce il menu principale
     * @return true per continuare l'esecuzione, false per terminare
     */
    private boolean handleMainMenu() {
        int choice = view.showMainMenu();

        try {
            switch (choice) {
                case 1: // Lista giochi
                    listAvailableGames();
                    return true;

                case 2: // Crea gioco
                    createGame();
                    return true;

                case 3: // Unisciti a un gioco
                    joinGame();
                    return true;

                case 4: // Esci
                    return false;

                default:
                    view.showError("Invalid choice");
                    return true;
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
            return true;
        }
    }

    /**
     * Gestisce lo stato di gioco
     * @return true per continuare l'esecuzione, false per terminare
     */
    private boolean handleGameState() {
        if (gameStarted) {
            // Qui andrebbe la logica di gioco effettiva
            view.showMessage("Game in progress...");
            return true;
        }

        int choice = view.showGameMenu();

        try {
            switch (choice) {
                case 1: // Attendi inizio gioco
                    view.showMessage("Waiting for game to start...");
                    return true;

                case 2: // Lascia il gioco
                    leaveGame();
                    return true;

                default:
                    view.showError("Invalid choice");
                    return true;
            }
        } catch (Exception e) {
            view.showError(e.getMessage());
            return true;
        }
    }

    /**
     * Mostra i giochi disponibili
     */
    private void listAvailableGames() throws RemoteException {
        List<GameInfo> games = networkManager.getAvailableGames();
        view.showAvailableGames(games);
    }

    /**
     * Crea un nuovo gioco
     */
    private void createGame() throws RemoteException {
        int[] gameSettings = view.askCreateGame();
        int numPlayers = gameSettings[0];
        boolean isTestFlight = gameSettings[1] == 1;
        PlayerColor color = view.intToPlayerColor(gameSettings[2]);

        String gameId = networkManager.createGame(color, numPlayers, isTestFlight);
        currentGameId = gameId;
        inGame = true;

        view.showMessage("Game created! ID: " + gameId);
        view.showMessage("Waiting for other players to join...");
    }

    /**
     * Unisciti a un gioco esistente
     */
    private void joinGame() throws RemoteException {
        try {
            List<GameInfo> games = networkManager.getAvailableGames();

            if (games.isEmpty()) {
                view.showMessage("No games available.");
                return;
            }

            String[] joinSettings = view.askJoinGame(games);
            String gameId = joinSettings[0];

            // Break down the conversion to make it clearer for the compiler
            int colorChoice = Integer.parseInt(joinSettings[1]);
            PlayerColor color = view.intToPlayerColor(colorChoice);

            boolean success = networkManager.joinGame(gameId, color);

            if (success) {
                currentGameId = gameId;
                inGame = true;
                view.showMessage("Successfully joined game!");
                view.showMessage("Waiting for game to start...");
            } else {
                view.showError("Failed to join game.");
            }
        } catch (NumberFormatException e) {
            view.showError("Invalid color choice: " + e.getMessage());
        } catch (RemoteException e) {
            view.showError("Error joining game: " + e.getMessage());
        } catch (Exception e) {
            view.showError("Error joining game: " + e.getMessage());
        }
    }

    /**
     * Lascia il gioco corrente
     */
    private void leaveGame() {
        try {
            if (currentGameId != null) {
                networkManager.leaveGame(currentGameId);
                inGame = false;
                gameStarted = false;
                currentGameId = null;
                view.showMessage("Left the game.");
            }
        } catch (Exception e) {
            view.showError("Error leaving game: " + e.getMessage());
        }
    }

    /**
     * Disconnette il client dal server
     */
    private void disconnect() {
        if (networkManager != null) {
            try{
                networkManager.disconnect();
                view.showMessage("Disconnected from server.");
            }catch (RemoteException e){
                view.showError("Error disconnecting from server: " + e.getMessage());
            }

        }
    }
}
