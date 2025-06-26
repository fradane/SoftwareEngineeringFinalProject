package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.ClientPingPongManager;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.card.*;
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
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
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
    private EndGameController endGameController;

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
    private static final String END_GAME_CONTROLLER = "EndGameController";

    public static ClientGuiController getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        // Initialize task queues
        initializeTaskQueues();

        // Icona per la taskbar/dock
        try {
            // Icona per barra del titolo
            primaryStage.getIcons().add(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/galaxy_trucker_icon.png"))
            ));

            if (Taskbar.isTaskbarSupported()) {
                java.awt.Image dockIcon = ImageIO.read(Objects.requireNonNull(getClass().getResource("/gui/graphics/galaxy_trucker_icon.png")));
                Taskbar.getTaskbar().setIconImage(dockIcon);
            }
        } catch (Exception e) {
            System.err.println("Failed to set Taskbar icon: not supported by current OS");
        }

        // Carica il container principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainContainer.fxml"));
        loader.setController(this);
        Parent root = loader.load();

        // CAMBIATO: Crea la scene senza dimensioni fisse
        Scene scene = new Scene(root);

        // Configura il model e il controller
        GuiController.setClientModel(clientModel);
        GuiController.setClientController(clientController);

        // NUOVO: Setup responsive
        setupResponsiveWindow(primaryStage, scene);

        // Carica la schermata iniziale
        loadStartView();

        initializationDone.complete(null);
    }

//    public ClientGuiController() throws RemoteException {
//        clientModel = new ClientModel();
//        clientController = new ClientController(clientModel, new ClientPingPongManager());
//    }

//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        instance = this;
//        this.primaryStage = primaryStage;
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/StartView.fxml"));
//        Scene scene = new Scene(loader.load());
//        startViewController = loader.getController();
//        GuiController.setClientModel(clientModel);
//        GuiController.setClientController(clientController);
//        primaryStage.setTitle("Galaxy Trucker");
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }

    @Override
    public void notifyHourglassRestarted(int flipsLeft) {

    }

    @Override
    public void setIsTestFlight(boolean isTestFlight) {

    }

    @Override
    public void showPickReservedComponentQuestion() {

    }

    @Override
    public Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents) {
        return null;
    }

    @Override
    public void checkShipBoardAfterAttackMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.checkShipBoardAfterAttackMenu()
        );
    }

    @Override
    public void showCurrentRanking() {
        //TODO
    }

    @Override
    public void showCrewMembersInfo() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showCrewMembersInfo()
        );
    }

    @Override
    public void showDisconnectMessage(String message) {
        // TODO generalizzare per gli stati
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showDisconnectMessage(message)
        );
    }

    @Override
    public void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors) {

    }

    @Override
    public void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        executeWithController(
                END_GAME_CONTROLLER,
                () -> endGameController.showEndGameInfoMenu(finalRanking, playersNicknamesWithPrettiestShip)
        );
    }

    @Override
    public void showPlayerEarlyLanded(String nickname) {
        if (nickname.equals(clientModel.getMyNickname())) {
            // Il giocatore corrente è atterrato anticipatamente
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showPlayerLanded()
            );
        } else {
            // Un altro giocatore è atterrato anticipatamente
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.notifyOtherPlayerEarlyLanded(nickname)
            );
        }
    }

    @Override
    public void showCubes(ShipBoardClient shipboardOf, String nickname) {

    }

    @Override
    public void showCubeRedistributionMenu() {
        //TODO
    }

    @Override
    public void showNoMoreHiddenComponents() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showNoMoreHiddenComponents()
        );
    }

    @Override
    public void showValidShipBoardMenu() {
        //TODO
    }

    @Override
    public void showChooseComponentToRemoveMenu() {
        //TODO
    }

    @Override
    public void showVisitLocationMenu() {
        ClientCard currentCard = clientModel.getCurrAdventureCard();
        System.out.println("Current card: " + currentCard.getClass().getSimpleName());
        if (clientModel.getCurrAdventureCard() instanceof ClientAbandonedShip) {
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                        () -> cardPhaseController.showAbandonedShipMenu());
        } else if (clientModel.getCurrAdventureCard() instanceof ClientAbandonedStation){
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showAbandonedStationMenu());
        }
        else
            System.err.println("Not an expective card: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());

    }

    @Override
    public void showChooseEnginesMenu() {
        ClientCard currentCard = clientModel.getCurrAdventureCard();
        System.out.println("Current card: " + currentCard.getClass().getSimpleName());
        if (
                currentCard instanceof ClientFreeSpace ||
                currentCard instanceof ClientWarField
        ) {
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showFreeSpaceMenu()
            );
        }
        else
            System.err.println("Not FreeSpace card: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
    }

    @Override
    public void showAcceptTheRewardMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();

        if (card.hasReward()) {
            if (
                    card instanceof ClientPirates ||
                    card instanceof ClientSmugglers ||
                    card instanceof ClientSlaveTraders
            ) {
                executeWithController(
                        CARD_PHASE_CONTROLLER,
                        () -> cardPhaseController.showRewardMenu()
                );
            } else
                System.err.println("Not an enemy card: " + card.getClass().getSimpleName());
        }
    }


    @Override
    public void showChooseCannonsMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();

        if (
                card instanceof ClientPirates ||
                card instanceof ClientSlaveTraders ||
                card instanceof ClientSmugglers ||
                card instanceof ClientWarField
        ) {
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showChooseCannonsMenu()
            );
        } else
            System.err.println("Not enemies: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
    }

    @Override
    public void showSmallDanObjMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleSmallDanObjMenu()
        );
    }

    @Override
    public void showBigMeteoriteMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showBigMeteoriteMenu()
        );
    }

    @Override
    public void showBigShotMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showBigShotMenu()
        );
    }

    @Override
    public void showHandleRemoveCrewMembersMenu() {
        if (
                clientModel.getCurrAdventureCard() instanceof ClientAbandonedShip ||
                clientModel.getCurrAdventureCard() instanceof ClientSlaveTraders ||
                clientModel.getCurrAdventureCard() instanceof ClientWarField
        ) {
            executeWithController(
                   CARD_PHASE_CONTROLLER,
                  () -> cardPhaseController.showChooseCabinMenu()
           );
        } else
           System.err.println("Not AbandonedShipCard or SlaveTraders or War field: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
   }

    @Override
    public void showHandleCubesRewardMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleCubesRewardMenu()
        );
    }

    @Override
    public void showEpidemicMenu() {
        ClientCard currentCard = clientModel.getCurrAdventureCard();
        System.out.println("Current card: " + currentCard.getClass().getSimpleName());

        if (clientModel.getCurrAdventureCard() instanceof ClientEpidemic) {
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showEpidemicMenu());
        }
        else
            System.err.println("Not EpidemicCard: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
    }

    @Override
    public void showStardustMenu() {

        ClientCard currentCard = clientModel.getCurrAdventureCard();
        System.out.println("Current card: " + currentCard.getClass().getSimpleName());

        if (clientModel.getCurrAdventureCard() instanceof ClientStarDust) {
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showStardustMenu()
            );
        } else
            System.err.println("Not Stardust card: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
    }

    @Override
    public void showHandleCubesMalusMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleCubesMalusMenu()
        );
    }

    @Override
    public PlayerColor intToPlayerColor(int colorChoice) {
        return ClientView.super.intToPlayerColor(colorChoice);
    }

    public CompletableFuture<Void> getInitializationDoneFuture() {
        return initializationDone;
    }

    @Override
    public void showWaitingForPlayers() {}

    @Override
    public void initialize() {}

    @Override
    public String askForInput(String questionDescription, String interrogationPrompt) {
        return "";
    }

    @Override
    public void cancelInputWaiting() {

    }

    @Override
    public int[] askCreateGame() {
        return new int[0];
    }

    @Override
    public String[] askJoinGame(List<GameInfo> games) {
        return new String[0];
    }

//    @Override
//    public void showMainMenu() {
//
//        javafx.application.Platform.runLater(() -> {
//            try {
//                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainMenuView.fxml"));
//                Parent root = loader.load();
//                mainMenuViewController = loader.getController();
//                mainMenuViewController.setAvailableGames();
//                primaryStage.setScene(new Scene(root));
//                primaryStage.show();
//            } catch (IOException e) {
//                System.out.println("Error while loading the main menu view.");
//                e.printStackTrace();
//            }
//        });
//
//    }

    @Override
    public int showGameMenu() {
        return 0;
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {

    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {

    }

    @Override
    public void notifyGameCreated(String gameId) {

    }

    @Override
    public void notifyGameStarted(GameState gameState) {

    }

    @Override
    public void notifyGameEnded(String reason) {

    }

    @Override
    public String askPlayerColor(List<PlayerColor> availableColors) {
        return "";
    }

//    @Override
//    public void showNewGameState() {
//        if (clientModel.getGameState() == GameState.CREATE_DECK) {
//            javafx.application.Platform.runLater(() -> {
//                try {
//                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CardPhaseView.fxml"));
//                    Parent root = loader.load();
//                    cardPhaseController = loader.getController();
//                    primaryStage.setScene(new Scene(root));
//                    primaryStage.setFullScreen(true);
//                    primaryStage.setMaximized(true);
//                    primaryStage.show();
//                } catch (IOException e) {
//                    System.out.println("Error while loading the new card phase view.");
//                    e.printStackTrace();
//                }
//            });
//        }
//    }

    @Override
    public void showDangerousObj() {

    }

    @Override
    public void showNewCardState() {
        CardState currentCardState = clientModel.getCurrCardState();
        ClientState mappedState = cardStateToClientState(currentCardState, clientModel);
        this.showCardStateMenu(mappedState);
    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {

    }

    // TODO
//    @Override
//    public void showBuildShipBoardMenu() {
//
//        if (buildAndCheckShipBoardController != null) return;
//
//        String fxmlPath = "/gui/BuildAndCheckShipBoardView.fxml";
//
//        javafx.application.Platform.runLater(() -> {
//            try {
//                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
//                Parent root = loader.load();
//                buildAndCheckShipBoardController = loader.getController();
//                GuiController.setClientModel(clientModel);
//                primaryStage.setScene(new Scene(root));
//                primaryStage.show();
//            } catch (IOException e) {
//                System.out.println("Error while loading the shipboard view.");
//                e.printStackTrace();
//            }
//        });
//    }

    @Override
    public void showBuildShipBoardMenu() {
        // Only load if not already loaded
        if (buildAndCheckShipBoardController == null) {
            Platform.runLater(() -> loadView("/gui/BuildAndCheckShipBoardView.fxml", BUILD_SHIPBOARD_CONTROLLER));
        }
    }

    @Override
    public void notifyNoMoreComponentAvailable() {

    }

    @Override
    public void showShipBoard(ShipBoardClient shipBoard, String shipBoardOwnerNickname) {

    }

    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {
        //TODO
    }

    @Override
    public void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {

    }

    @Override
    public void showComponentHitInfo(Coordinates coordinates){
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showComponentHitInfo(coordinates)
        );
    }

    @Override
    public void showLittleDeck(int littleDeckChoice) {

    }

    @Override
    public void updateTimeLeft(int timeLeft, int flipsLeft) {
        if (buildAndCheckShipBoardController != null && buildAndCheckShipBoardController.getModelFxAdapter() != null)
            buildAndCheckShipBoardController.getModelFxAdapter().refreshTimer(timeLeft, flipsLeft);
    }

    @Override
    public void notifyTimerEnded(int flipsLeft) {

    }

    @Override
    public void notifyHourglassStarted(int flipsLeft, String nickname) {

    }

    public void showExitMenu(){

    }

    private static final CompletableFuture<Void> initializationDone = new CompletableFuture<>();

    // Nuovo container principale
    @FXML
    private StackPane mainContainer;

    // Elementi attuali
    private Parent currentView;
    private GuiController currentController;

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
        pendingTasks.put(END_GAME_CONTROLLER, new ConcurrentLinkedQueue<>());
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

            // Check if the controller already exists and is the correct type
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
            case END_GAME_CONTROLLER:
                endGameController = (EndGameController) controller;
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
            case END_GAME_CONTROLLER -> (T) endGameController;
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
            case END_GAME_CONTROLLER -> fxmlPath = "/gui/EndGameView.fxml";
        }

        synchronized (loaderLock) {
            // Check if the controller is ready for immediate execution
            if (isControllerReadyForImmediateExecution(controllerType)) {
                // Controller exists and no pending operations, execute immediately
                if (Platform.isFxApplicationThread()) {
                    task.run();
                } else {
                    Platform.runLater(task);
                }
                return;
            }

            // Controller doesn't exist, OR we're processing pending tasks, queue the task
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

        if (clientModel.getCurrAdventureCard() == null)
            executeWithController(
                    BUILD_SHIPBOARD_CONTROLLER,
                    () -> buildAndCheckShipBoardController.showInvalidComponents()
            );
        else
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showInvalidComponents()
            );
    }

    @Override
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts) {
        if (clientModel.getCurrAdventureCard() == null)
            executeWithController(
                    BUILD_SHIPBOARD_CONTROLLER,
                    () -> buildAndCheckShipBoardController.showShipParts(shipParts)
            );
        else
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showShipParts(shipParts)
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
            case BUILD_SHIPBOARD, CHECK_SHIPBOARD, PLACE_CREW:
                if (buildAndCheckShipBoardController != null) {
                    buildAndCheckShipBoardController.showMessage(message.split("\n")[0], false);
                } else {
                    executeWithController(
                            BUILD_SHIPBOARD_CONTROLLER,
                            () -> buildAndCheckShipBoardController.showMessage(message.split("\n")[0], false)
                    );
                }
                break;

            default:
                executeWithController(
                        CARD_PHASE_CONTROLLER,
                        () -> cardPhaseController.showMessage(message.split("\n")[0], false)
                );
        }
    }

    /**
     * Load the start view
     */
    private void loadStartView() {
        Platform.runLater(() -> loadView("/gui/StartView.fxml", START_CONTROLLER));
    }

    @Override
    public ClientController getClientController() {
        return clientController;
    }

    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    @Override
    public void refreshGameInfos(List<GameInfo> gameInfos) {
        executeWithController(
                MAIN_MENU_CONTROLLER,
                () -> mainMenuViewController.refreshGameInfos(gameInfos)
        );
    }

    private void transitionToEndGame() {
        Platform.runLater(() -> {
            // Clear any existing views
            if (mainContainer != null && currentView != null) {
                mainContainer.getChildren().clear();
            }

            // Load the end game view
            loadView("/gui/EndGameView.fxml", END_GAME_CONTROLLER);
        });
    }

    public void handleGameExit() {
        Platform.runLater(() -> {
            try {
                // Cleanup any resources
                if (clientController != null) {
                    clientController.leaveGame();
                }

                // Exit the application
                Platform.exit();
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Error during game exit: " + e.getMessage());
                // Force exit even if there's an error
                System.exit(1);
            }
        });
    }

    /**
     * Configura la finestra per essere responsive
     */
    private void setupResponsiveWindow(Stage stage, Scene scene) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setMinWidth(800);
        stage.setMinHeight(600);

        double targetWidth = screenBounds.getWidth() * 0.8;
        double targetHeight = screenBounds.getHeight() * 0.8;

        double windowWidth = Math.max(targetWidth, 800);
        double windowHeight = Math.max(targetHeight, 600);

        windowWidth = Math.min(windowWidth, screenBounds.getWidth());
        windowHeight = Math.min(windowHeight, screenBounds.getHeight());

        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);

        stage.setX((screenBounds.getWidth() - windowWidth) / 2);
        stage.setY((screenBounds.getHeight() - windowHeight) / 2);

        stage.setResizable(true);

        stage.setScene(scene);
        stage.setTitle("Galaxy Trucker");

        stage.show();

        stage.setFullScreenExitHint("");

        // Usa Platform.runLater per impostare fullscreen dopo che tutto è stato caricato
        Platform.runLater(() -> {
            stage.setFullScreen(true);
        });

    }

}