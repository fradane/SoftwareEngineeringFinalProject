package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.common.ClientNetworkManager;
import it.polimi.ingsw.is25am33.network.rmi.client.RMIClientNetworkManager;
import it.polimi.ingsw.is25am33.network.rmi.server.RMIServerNetworkManager;

import java.rmi.RemoteException;
import java.util.List;

public class ClientController {
    private final ClientView view;
    private ClientNetworkManager networkManager;
    private String nickname;
    private String currentGameId;
    private boolean inGame;
    private boolean gameStarted;
    private GameState currGameState;
    private CardState currCardState;

    /**
     * Costruttore del controller
     * @param view La view da utilizzare (CLI o GUI)
     */
    public ClientController(ClientView view){
        super();
        this.view = view;
        this.inGame = false;
        this.gameStarted = false;
    }

    public void setNetworkManager(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public void setCurrGameState(GameState currGameState) {
        this.currGameState = currGameState;
        showNewGameState(currGameState.toString());
    }

    public void setCurrCardState(CardState currCardState) {
        this.currCardState = currCardState;
        showNewGameState(currCardState.toString());
    }

    /**
     * Starts the application execution.
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

    public String getValidNickname() throws RemoteException {
        while (true) {
            String attemptedNickname = view.askNickname();
            try {
                if (networkManager.isNicknameAvailable(attemptedNickname)) {
                    return attemptedNickname;
                } else {
                    view.showError("Nickname already in use");
                }
            } catch (RemoteException e) {
                view.showError("Error checking nickname: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Sets up the connection to the server.
     *
     * @throws RemoteException in case of communication errors
     */
    private void setupConnection() throws RemoteException {
        String serverAddress = view.askServerAddress();

        // Crea un'implementazione RMI con callback per le notifiche
        try {
            networkManager.connectToServer(serverAddress);

            // Ottenere un nickname valido dopo aver stabilito la connessione
            this.nickname = getValidNickname();
            networkManager.registerWithNickname(this.nickname);

            view.showMessage("Connected to server as " + nickname);
        } catch (RemoteException e) {
            view.showError("Connection failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Handles the main menu.
     *
     * @return true to continue execution, false to exit
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
     * Handles the game state.
     *
     * @return true to continue execution, false to exit
     */
    private boolean handleGameState() {

        if (gameStarted) {

            setCurrGameState(GameState.BUILD_SHIPBOARD);
            view.setLatestComponentTable(new ComponentTable());
            view.showComponentTable();
            buildShipBoardPhase();

            // TODO logica di gioco

            view.showMessage("Game in progress...");
            return true;
        }

        int choice = view.showGameMenu();

        try {
            switch (choice) {
                case 0:
                    gameStarted = true;
                    return true;
                case 1: // Lascia il gioco
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
     * Displays the list of available games.
     */
    private void listAvailableGames() throws RemoteException {
        List<GameInfo> games = networkManager.getAvailableGames();
        view.showAvailableGames(games);
    }

    /**
     * Creates a new game.
     */
    private void createGame() throws RemoteException {
        int[] gameSettings = view.askCreateGame();
        int numPlayers = gameSettings[0];
        boolean isTestFlight = gameSettings[1] == 1;
        PlayerColor color = view.intToPlayerColor(gameSettings[2]);

        GameInfo gameInfo = networkManager.createGame(color, numPlayers, isTestFlight);
        currentGameId = gameInfo.getGameId();
        inGame = true;

        view.notifyGameCreated(currentGameId);
        view.notifyPlayerJoined(this.nickname, gameInfo);
        view.showMessage("Waiting for other players to join...");
    }

    /**
     * Joins an existing game.
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

            while(!networkManager.isColorAvailable(gameId, color)){
                view.showError("Color already in use");
                joinSettings[1] = view.askPlayerColor();
                colorChoice = Integer.parseInt(joinSettings[1]);
                color = view.intToPlayerColor(colorChoice);
            }

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
     * Leaves the current game.
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
     * Disconnects the client from the server.
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

    public String askNickname() throws RemoteException {
        return view.askNickname();
    }

    public void showError(String error) throws RemoteException {
        view.showError(error);
    }

    public void notifyGameStarted(GameState gameState) throws RemoteException {
        gameStarted = true;
        view.notifyGameStarted(gameState);
        // Se la view è di tipo ClientCLIView, possiamo interrompere l'attesa
        if (view instanceof ClientCLIView) {
            ((ClientCLIView) view).cancelInputWaiting();
        }
    }

    /**
     * Informs the view to display the latest game state as a string.
     *
     * @param gameState the string representation of the new game state
     */
    public void showNewGameState(String gameState) {
        view.showNewGameState(gameState);
    }

    /**
     * Updates the view with a new ComponentTable.
     * If no previous table exists, it is shown immediately.
     *
     * @param componentTable the updated component table to display
     */
    public void componentTableUpdate(ComponentTable componentTable) {

        if (view.getLatestComponentTable() == null) {
            view.setLatestComponentTable(componentTable);
            view.showComponentTable();
        } else {
            view.setLatestComponentTable(componentTable);
        }

    }

    /**
     * Initiates the ship board building phase by showing the build menu
     * and executing the selected action on the server using the current nickname.
     */
    public void buildShipBoardPhase() {

        boolean hasPlayerEndedThisPhase = false;

        while(currGameState == GameState.BUILD_SHIPBOARD && !hasPlayerEndedThisPhase) {
            hasPlayerEndedThisPhase = view.showBuildShipBoardMenu()
                    .apply((RMIServerNetworkManager) ((RMIClientNetworkManager) networkManager).getServer(), nickname);

        }

        while(currGameState == GameState.BUILD_SHIPBOARD) {
            view.showShipBoardsMenu();
        }

    }

    public void setCurrAdventureCard(AdventureCard card) {
        view.setCurrAdventureCard(card);
    }

    public void cardPhase() {
        // TODO
    }


}
