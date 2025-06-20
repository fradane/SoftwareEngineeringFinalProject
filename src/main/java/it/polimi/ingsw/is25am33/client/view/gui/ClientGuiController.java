package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.ClientPingPongManager;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.*;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.client.view.tui.MessageType;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientGuiController extends Application implements ClientView {

    final ClientModel clientModel;
    final ClientController clientController;
    private static ClientGuiController instance;

    private StartViewController startViewController;
    private MainMenuViewController mainMenuViewController;
    private BuildAndCheckShipBoardController buildAndCheckShipBoardController;
    private CardPhaseController cardPhaseController;

    private final Object loaderLock = new Object();

    // Task queue system for pending operations
    private final Map<String, Queue<Runnable>> pendingTasks = new HashMap<>();
    private final Set<String> loadingControllers = new HashSet<>();
    private final Set<String> processingTasks = new HashSet<>();

    // Controller type constants
    private static final String START_CONTROLLER = "StartViewController";
    private static final String MAIN_MENU_CONTROLLER = "MainMenuViewController";
    private static final String BUILD_SHIPBOARD_CONTROLLER = "BuildAndCheckShipBoardController";
    private static final String CARD_PHASE_CONTROLLER = "CardPhaseController";

    public static ClientGuiController getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        // Initialize task queues
        initializeTaskQueues();

        // Icona per barra del titolo
        primaryStage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/galaxy_trucker_icon.png"))
        ));

        // Icona per la taskbar/dock
        try {
            if (Taskbar.isTaskbarSupported()) {
                java.awt.Image dockIcon = ImageIO.read(Objects.requireNonNull(getClass().getResource("/gui/graphics/galaxy_trucker_icon.png")));
                Taskbar.getTaskbar().setIconImage(dockIcon);
            }
        } catch (Exception e) {
            System.err.println("Failed to set Taskbar icon: not supported by current OS");
        }

        // Carica il container principale, ma imposta manualmente il controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainContainer.fxml"));
        loader.setController(this);  // Imposta questo oggetto come controller
        Scene scene = new Scene(loader.load(), 1200, 700);

        // Configura il model e il controller
        GuiController.setClientModel(clientModel);
        GuiController.setClientController(clientController);

        // Imposta il titolo e mostra la finestra
        primaryStage.setTitle("Galaxy Trucker");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");

        // Carica la schermata iniziale
        loadStartView();

        initializationDone.complete(null);
    }

    // Nuovo container principale
    @FXML
    private StackPane mainContainer;

    // Elementi attuali
    private Parent currentView;
    private GuiController currentController;

    private static final CompletableFuture<Void> initializationDone = new CompletableFuture<>();

    public ClientGuiController() throws RemoteException {
        clientModel = new ClientModel();
        clientController = new ClientController(clientModel, new ClientPingPongManager());
    }

    /**
     * Initialize task queues for all controller types
     */
    private void initializeTaskQueues() {
        pendingTasks.put(START_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(MAIN_MENU_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(BUILD_SHIPBOARD_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(CARD_PHASE_CONTROLLER, new ConcurrentLinkedQueue<>());
    }

    /**
     * Loads a view and handles task execution
     */
    private <T extends GuiController> void loadView(String fxmlPath, String controllerType) {
        synchronized (loaderLock) {
            // Check if already loading this controller
            if (loadingControllers.contains(controllerType)) {
                return; // Will be handled by pending tasks
            }

            // Check if controller already exists and is correct type
            T existingController = getCurrentControllerByType(controllerType);
            if (existingController != null) {
                return;
            }

            // Mark as loading
            loadingControllers.add(controllerType);

            try {
                // Load the new view
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent newView = loader.load();
                T controller = loader.getController();

                // Verify mainContainer is not null
                if (mainContainer == null) {
                    System.err.println("Error: mainContainer is null. Cannot load view: " + fxmlPath);
                    loadingControllers.remove(controllerType);
                    return;
                }

                // Remove previous view
                if (currentView != null) {
                    mainContainer.getChildren().remove(currentView);
                }

                // Add new view
                mainContainer.getChildren().add(newView);
                currentView = newView;
                currentController = controller;

                // Update specific controller references
                updateControllerReference(controller, controllerType);

                // Mark as loaded and process pending tasks
                loadingControllers.remove(controllerType);
                processPendingTasks(controllerType);

            } catch (IOException e) {
                System.err.println("Error loading view: " + fxmlPath);
                e.printStackTrace();
                loadingControllers.remove(controllerType);
            }
        }
    }

    /**
     * Updates the specific controller reference based on type
     */
    private <T extends GuiController> void updateControllerReference(T controller, String controllerType) {
        switch (controllerType) {
            case START_CONTROLLER:
                startViewController = (StartViewController) controller;
                break;
            case MAIN_MENU_CONTROLLER:
                mainMenuViewController = (MainMenuViewController) controller;
                break;
            case BUILD_SHIPBOARD_CONTROLLER:
                buildAndCheckShipBoardController = (BuildAndCheckShipBoardController) controller;
                break;
            case CARD_PHASE_CONTROLLER:
                cardPhaseController = (CardPhaseController) controller;
                break;
        }
    }

    /**
     * Gets current controller by type
     */
    @SuppressWarnings("unchecked")
    private <T extends GuiController> T getCurrentControllerByType(String controllerType) {
        return switch (controllerType) {
            case START_CONTROLLER -> (T) startViewController;
            case MAIN_MENU_CONTROLLER -> (T) mainMenuViewController;
            case BUILD_SHIPBOARD_CONTROLLER -> (T) buildAndCheckShipBoardController;
            case CARD_PHASE_CONTROLLER -> (T) cardPhaseController;
            default -> null;
        };
    }

    /**
     * Processes all pending tasks for a specific controller type
     */
    private void processPendingTasks(String controllerType) {
        // Mark as processing to prevent new tasks from jumping the queue
        processingTasks.add(controllerType);

        Queue<Runnable> tasks = pendingTasks.get(controllerType);
        if (tasks != null) {
            if (Platform.isFxApplicationThread()) {
                // Execute tasks sequentially on FX thread
                Runnable task;
                while ((task = tasks.poll()) != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        System.err.println("Error executing pending task: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                // Mark as done processing
                processingTasks.remove(controllerType);
            } else {
                // Not on FX thread, schedule sequential processing
                Platform.runLater(() -> {
                    Runnable task;
                    while ((task = tasks.poll()) != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.err.println("Error executing pending task: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    // Mark as done processing
                    processingTasks.remove(controllerType);
                });
            }
        } else {
            // No tasks to process
            processingTasks.remove(controllerType);
        }
    }

    /**
     * Checks if a controller is ready and not processing pending tasks
     */
    private boolean isControllerReadyForImmediateExecution(String controllerType) {
        return getCurrentControllerByType(controllerType) != null &&
                !processingTasks.contains(controllerType) &&
                !loadingControllers.contains(controllerType);
    }

    /**
     * Executes a task with a specific controller, queueing if necessary
     */
    private void executeWithController(String controllerType, Runnable task) {

        String fxmlPath = "";

        switch (controllerType) {
            case START_CONTROLLER -> fxmlPath = "/gui/StartView.fxml";
            case MAIN_MENU_CONTROLLER -> fxmlPath = "/gui/MainMenuView.fxml";
            case BUILD_SHIPBOARD_CONTROLLER -> fxmlPath = "/gui/BuildShipBoardView.fxml";
            case CARD_PHASE_CONTROLLER ->  fxmlPath = "/gui/CardPhaseView.fxml";
        }

        synchronized (loaderLock) {
            // Check if controller is ready for immediate execution
            if (isControllerReadyForImmediateExecution(controllerType)) {
                // Controller exists and no pending operations, execute immediately
                if (Platform.isFxApplicationThread()) {
                    task.run();
                } else {
                    Platform.runLater(task);
                }
                return;
            }

            // Controller doesn't exist OR we're processing pending tasks, queue the task
            Queue<Runnable> tasks = pendingTasks.get(controllerType);
            if (tasks != null) {
                tasks.offer(task);
            }

            // Start loading if not already loading
            if (!loadingControllers.contains(controllerType)) {
                if (Platform.isFxApplicationThread()) {
                    loadView(fxmlPath, controllerType);
                } else {
                    String finalFxmlPath = fxmlPath;
                    Platform.runLater(() -> loadView(finalFxmlPath, controllerType));
                }
            }
        }
    }

    // Updated methods using the task queue system

    @Override
    public void showCrewPlacementMenu() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showCrewPlacementMenu(false)
        );
    }

    @Override
    public void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips) {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showPrefabShipBoards(prefabShips)
        );
    }

    @Override
    public void showInvalidShipBoardMenu() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showInvalidComponents()
        );
    }

    @Override
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts) {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showShipParts(shipParts)
        );
    }

    @Override
    public void showThrowDicesMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showThrowDicesMenu()
        );
    }

    @Override
    public void showChoosePlanetMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showChoosePlanetMenu()
        );
    }

    @Override
    public void showPickedComponentAndMenu() {
        if (buildAndCheckShipBoardController != null) {
            buildAndCheckShipBoardController.showFocusComponent();
        } else {
            executeWithController(
                    BUILD_SHIPBOARD_CONTROLLER,
                    () -> buildAndCheckShipBoardController.showFocusComponent()
            );
        }
    }

    @Override
    public void showFirstToEnter() {
        if (buildAndCheckShipBoardController != null) {
            buildAndCheckShipBoardController.showFirstToEnterButton();
        } else {
            executeWithController(
                    BUILD_SHIPBOARD_CONTROLLER,
                    () -> buildAndCheckShipBoardController.showFirstToEnterButton()
            );
        }
    }

    @Override
    public void showBuildShipBoardMenu() {
        // Only load if not already loaded
        if (buildAndCheckShipBoardController == null) {
            Platform.runLater(() -> loadView("/gui/BuildAndCheckShipBoardView.fxml", BUILD_SHIPBOARD_CONTROLLER));
        }
    }

    @Override
    public void showMainMenu() {
        executeWithController(
                MAIN_MENU_CONTROLLER,
                () -> mainMenuViewController.setAvailableGames()
        );
    }

    @Override
    public void showNewGameState() {
        if (clientModel.getGameState() == GameState.CREATE_DECK) {
            Platform.runLater(() -> loadView("/gui/CardPhaseView.fxml", CARD_PHASE_CONTROLLER));
        }
    }

    @Override
    public void askNickname() {
        executeWithController(
                START_CONTROLLER,
                () -> startViewController.askNickname()
        );
    }

    @Override
    public void showError(String errorMessage) {
        switch (errorMessage) {
            case "Color already in use", "GameModel already started":
                if (mainMenuViewController != null) {
                    mainMenuViewController.showMessage(errorMessage, false);
                } else {
                    executeWithController(
                            MAIN_MENU_CONTROLLER,
                            () -> mainMenuViewController.showMessage(errorMessage, false)
                    );
                }
                break;
            default:
                if (startViewController != null) {
                    startViewController.showServerError(errorMessage);
                } else {
                    executeWithController(
                            START_CONTROLLER,
                            () -> startViewController.showServerError(errorMessage)
                    );
                }
        }
    }

    @Override
    public void showMessage(String message, MessageType type) {
        if (clientModel.getGameState() == null)
            return;

        switch (clientModel.getGameState()) {
            case BUILD_SHIPBOARD, CHECK_SHIPBOARD:
                if (buildAndCheckShipBoardController != null) {
                    buildAndCheckShipBoardController.showMessage(message.split("\n")[0], false);
                } else {
                    executeWithController(
                            BUILD_SHIPBOARD_CONTROLLER,
                            () -> buildAndCheckShipBoardController.showMessage(message.split("\n")[0], false)
                    );
                }
                break;
        }
    }

    @Override
    public void updateTimeLeft(int timeLeft, int flipsLeft) {
        if (buildAndCheckShipBoardController != null && buildAndCheckShipBoardController.getModelFxAdapter() != null) {
            buildAndCheckShipBoardController.getModelFxAdapter().refreshTimer(timeLeft, flipsLeft);
        }
    }

    /**
     * Load the start view
     */
    private void loadStartView() {
        Platform.runLater(() -> loadView("/gui/StartView.fxml", START_CONTROLLER));
    }

    // Getters and other methods remain the same
    public CompletableFuture<Void> getInitializationDoneFuture() {
        return initializationDone;
    }

    @Override
    public ClientController getClientController() {
        return clientController;
    }

    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    // All other interface methods remain unchanged
    @Override public void notifyHourglassRestarted(int flipsLeft) {}
    @Override public void setIsTestFlight(boolean isTestFlight) {}
    @Override public void showPickReservedComponentQuestion() {}
    @Override public Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents) { return null; }
    @Override public void checkShipBoardAfterAttackMenu() {}
    @Override public void showCurrentRanking() {}
    @Override public void showValidShipBoardMenu() {}
    @Override public void showChooseComponentToRemoveMenu() {}
    @Override public void showVisitLocationMenu() {}
    @Override public void showChooseEnginesMenu() {}
    @Override public void showAcceptTheRewardMenu() {}
    @Override public void showChooseCannonsMenu() {}
    @Override public void showSmallDanObjMenu() {}
    @Override public void showBigMeteoriteMenu() {}
    @Override public void showBigShotMenu() {}
    @Override public void showHandleRemoveCrewMembersMenu() {}
    @Override public void showHandleCubesRewardMenu() {}
    @Override public void showEpidemicMenu() {}
    @Override public void showStardustMenu() {}
    @Override public void showHandleCubesMalusMenu() {}
    @Override public PlayerColor intToPlayerColor(int colorChoice) { return ClientView.super.intToPlayerColor(colorChoice); }
    @Override public void showWaitingForPlayers() {}
    @Override public void initialize() {}
    @Override public String askForInput(String questionDescription, String interrogationPrompt) { return ""; }
    @Override public void cancelInputWaiting() {}
    @Override public int[] askCreateGame() { return new int[0]; }
    @Override public String[] askJoinGame(List<GameInfo> games) { return new String[0]; }
    @Override public int showGameMenu() { return 0; }
    @Override public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {}
    @Override public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {}
    @Override public void notifyGameCreated(String gameId) {}
    @Override public void notifyGameStarted(GameState gameState) {}
    @Override public void notifyGameEnded(String reason) {}
    @Override public String askPlayerColor(List<PlayerColor> availableColors) { return ""; }
    @Override public void showDangerousObj() {}
    @Override public void showNewCardState() { CardState currentCardState = clientModel.getCurrCardState(); ClientState mappedState = cardStateToClientState(currentCardState, clientModel); this.showCardStateMenu(mappedState); }
    @Override public void showCurrAdventureCard(boolean isFirstTime) {}
    @Override public void notifyNoMoreComponentAvailable() {}
    @Override public void showShipBoard(ShipBoardClient shipBoard, String shipBoardOwnerNickname) {}
    @Override public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {}
    @Override public void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {}
    @Override public void showComponentHitInfo(Coordinates coordinates) {}
    @Override public void showLittleDeck(int littleDeckChoice) {}
    @Override public void notifyTimerEnded(int flipsLeft) {}
    @Override public void notifyHourglassStarted(int flipsLeft, String nickname) {}
    public void showExitMenu() {}
}