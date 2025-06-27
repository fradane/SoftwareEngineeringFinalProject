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

    private ControllerState currentControllerState = ControllerState.START_CONTROLLER;

    /**
     * Returns the singleton instance of ClientGuiController.
     *
     * @return the current instance of ClientGuiController
     */
    public static ClientGuiController getInstance() {
        return instance;
    }

    private String getCurrentControllerType() {
        if (currentController == startViewController) {
            return START_CONTROLLER;
        } else if (currentController == mainMenuViewController) {
            return MAIN_MENU_CONTROLLER;
        } else if (currentController == buildAndCheckShipBoardController) {
            return BUILD_SHIPBOARD_CONTROLLER;
        } else if (currentController == cardPhaseController) {
            return CARD_PHASE_CONTROLLER;
        } else if (currentController == endGameController) {
            return END_GAME_CONTROLLER;
        }
        return null;
    }

    /**
     * Starts the JavaFX application and initializes the main GUI.
     *
     * @param primaryStage the primary stage for this application
     * @throws Exception if an error occurs during application startup
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        // Initialize task queues
        initializeTaskQueues();

        // icon for taskbar/dock
        try {
            // icon for title
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

        // loading of main container
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainContainer.fxml"));
        loader.setController(this);
        Parent root = loader.load();

        // creation of the scene
        Scene scene = new Scene(root);

        // configuration of model e controller
        GuiController.setClientModel(clientModel);
        GuiController.setClientController(clientController);

        // responsive setup
        setupResponsiveWindow(primaryStage, scene);

        // loading of main scene
        loadStartView();

        initializationDone.complete(null);
    }

    /**
     * Sets whether this is a test flight.
     *
     * @param isTestFlight true if this is a test flight, false otherwise
     */
    @Override
    public void setIsTestFlight(boolean isTestFlight) {

    }

    /**
     * Shows the question dialog for picking a reserved component.
     */
    @Override
    public void showPickReservedComponentQuestion() {

    }

    /**
     * Shows the ship board check menu after an attack.
     */
    @Override
    public void checkShipBoardAfterAttackMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.checkShipBoardAfterAttackMenu()
        );
    }

    /**
     * Shows the current ranking of players.
     */
    @Override
    public void showCurrentRanking() {
        //TODO
    }

    /**
     * Shows information about crew members.
     */
    @Override
    public void showCrewMembersInfo() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showCrewMembersInfo()
        );
    }

    /**
     * Shows a disconnect message to the user.
     *
     * @param message the disconnect message to display
     */
    @Override
    public void showDisconnectMessage(String message) {
        executeWithController(
                currentController.getControllerType(),
                () -> currentController.showDisconnectMessage(message)
        );
    }

    /**
     * Shows information about infected crew members that were removed.
     *
     * @param cabinWithNeighbors coordinates of cabins with their neighbors where crew members were removed
     */
    @Override
    public void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors) {

    }

    /**
     * Shows the end game information including final ranking and prettiest ship winners.
     *
     * @param finalRanking list of player final data sorted by ranking
     * @param playersNicknamesWithPrettiestShip list of player nicknames who have the prettiest ship
     */
    @Override
    public void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        executeWithController(
                END_GAME_CONTROLLER,
                () -> endGameController.showEndGameInfoMenu(finalRanking, playersNicknamesWithPrettiestShip)
        );
    }

    /**
     * Shows notification that a visible component was stolen.
     */
    @Override
    public void showStolenVisibleComponent() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showStolenVisibleComponent()
        );
    }

    /**
     * Shows notification that a player has landed early.
     *
     * @param nickname the nickname of the player who landed early
     */
    @Override
    public void showPlayerEarlyLanded(String nickname) {
        if (nickname.equals(clientModel.getMyNickname())) {
            // current player landed early
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.showPlayerLanded()
            );
        } else {
            // other player landed early
            executeWithController(
                    CARD_PHASE_CONTROLLER,
                    () -> cardPhaseController.notifyOtherPlayerEarlyLanded(nickname)
            );
        }
    }

    /**
     * Shows the cubes on a player's ship board.
     *
     * @param shipboardOf the ship board to display cubes for
     * @param nickname the nickname of the ship board owner
     */
    @Override
    public void showCubes(ShipBoardClient shipboardOf, String nickname) {

    }

    /**
     * Shows the cube redistribution menu.
     */
    @Override
    public void showCubeRedistributionMenu() {
    }

    /**
     * Shows notification that there are no more hidden components available.
     */
    @Override
    public void showNoMoreHiddenComponents() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showNoMoreHiddenComponents()
        );
    }

    /**
     * Shows the valid ship board menu.
     */
    @Override
    public void showValidShipBoardMenu() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showMessage("Your ship is correct, other player's ship is not, wait for them...", true)
        );
    }

    /**
     * Shows the menu for choosing which component to remove.
     */
    @Override
    public void showChooseComponentToRemoveMenu() {
    }

    /**
     * Shows the visit location menu based on the current adventure card.
     */
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

    /**
     * Shows the choose engines menu for space navigation.
     */
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

    /**
     * Shows the menu for accepting rewards from enemy encounters.
     */
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


    /**
     * Shows the menu for choosing cannons for combat.
     */
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

    /**
     * Shows the menu for handling small dangerous objects.
     */
    @Override
    public void showSmallDanObjMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleSmallDanObjMenu()
        );
    }

    /**
     * Shows the menu for handling big meteorite encounters.
     */
    @Override
    public void showBigMeteoriteMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showBigMeteoriteMenu()
        );
    }

    /**
     * Shows the menu for handling big shot encounters.
     */
    @Override
    public void showBigShotMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showBigShotMenu()
        );
    }

    /**
     * Shows the menu for handling removal of crew members.
     */
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

    /**
     * Shows the menu for handling cube rewards.
     */
    @Override
    public void showHandleCubesRewardMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleCubesRewardMenu()
        );
    }

    /**
     * Shows the epidemic handling menu.
     */
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

    /**
     * Shows the stardust encounter menu.
     */
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

    /**
     * Shows the menu for handling cube malus effects.
     */
    @Override
    public void showHandleCubesMalusMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showHandleCubesMalusMenu()
        );
    }

    /**
     * Converts an integer choice to a PlayerColor.
     *
     * @param colorChoice the integer representing the color choice
     * @return the corresponding PlayerColor
     */
    @Override
    public PlayerColor intToPlayerColor(int colorChoice) {
        return ClientView.super.intToPlayerColor(colorChoice);
    }

    /**
     * Returns a future that completes when initialization is done.
     *
     * @return a CompletableFuture that completes when initialization is finished
     */
    public CompletableFuture<Void> getInitializationDoneFuture() {
        return initializationDone;
    }

    /**
     * Shows the waiting for players screen.
     */
    @Override
    public void showWaitingForPlayers() {}

    /**
     * Initializes the GUI controller.
     */
    @Override
    public void initialize() {}

    /**
     * Asks for user input with a question and prompt.
     *
     * @param questionDescription description of the question being asked
     * @param interrogationPrompt the prompt to show to the user
     * @return the user's input as a string
     */
    @Override
    public String askForInput(String questionDescription, String interrogationPrompt) {
        return "";
    }

    /**
     * Notifies that a player has joined the game.
     *
     * @param nickname the nickname of the player who joined
     * @param gameInfo information about the game that was joined
     */
    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {

    }

    /**
     * Notifies that a player has disconnected from the game.
     *
     * @param disconnectedPlayerNickname the nickname of the disconnected player
     */
    @Override
    public void notifyPlayerDisconnected(String disconnectedPlayerNickname) {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.notifyPlayerDisconnected(disconnectedPlayerNickname)
        );
    }

    /**
     * Notifies that a new game has been created.
     *
     * @param gameId the ID of the created game
     */
    @Override
    public void notifyGameCreated(String gameId) {
    }

    /**
     * Notifies that a game has started.
     *
     * @param gameState the initial state of the started game
     */
    @Override
    public void notifyGameStarted(GameState gameState) {

    }

    /**
     * Shows the new card state when it changes.
     */
    @Override
    public void showNewCardState() {
        CardState currentCardState = clientModel.getCurrCardState();
        ClientState mappedState = cardStateToClientState(currentCardState, clientModel);
        this.showCardStateMenu(mappedState);
    }

    /**
     * Shows the current adventure card.
     *
     * @param isFirstTime true if this is the first time showing this card, false otherwise
     */
    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {
    }

    /**
     * Shows the build ship board menu for ship construction.
     */
    @Override
    public void showBuildShipBoardMenu() {
        // Only load if not already loaded
        if (buildAndCheckShipBoardController == null) {
            Platform.runLater(() -> loadView("/gui/BuildAndCheckShipBoardView.fxml", BUILD_SHIPBOARD_CONTROLLER));
        }
    }

    /**
     * Shows a ship board for a specific player.
     *
     * @param shipBoard the ship board to display
     * @param shipBoardOwnerNickname the nickname of the ship board owner
     */
    @Override
    public void showShipBoard(ShipBoardClient shipBoard, String shipBoardOwnerNickname) {

    }

    /**
     * Shows a ship board with color mapping for components.
     *
     * @param shipBoardClient the ship board to display
     * @param shipBoardOwnerNickname the nickname of the ship board owner
     * @param colorMap mapping of colors to coordinate sets for visual representation
     */
    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {
    }

    /**
     * Shows visible components and their associated menu.
     *
     * @param visibleComponents map of component IDs to visible components
     */
    @Override
    public void showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {

    }

    /**
     * Shows information about a component that was hit.
     *
     * @param coordinates the coordinates of the hit component
     */
    @Override
    public void showComponentHitInfo(Coordinates coordinates){
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showComponentHitInfo(coordinates)
        );
    }

    /**
     * Shows the little deck selection.
     *
     * @param littleDeckChoice the choice number for the little deck
     */
    @Override
    public void showLittleDeck(int littleDeckChoice) {

    }

    /**
     * Updates the time left display.
     *
     * @param timeLeft the time remaining in seconds
     * @param flipsLeft the number of hourglass flips remaining
     */
    @Override
    public void updateTimeLeft(int timeLeft, int flipsLeft) {
        if (buildAndCheckShipBoardController != null && buildAndCheckShipBoardController.getModelFxAdapter() != null)
            buildAndCheckShipBoardController.getModelFxAdapter().refreshTimer(timeLeft, flipsLeft);
    }

    /**
     * Notifies that the timer has ended.
     *
     * @param flipsLeft the number of hourglass flips remaining
     */
    @Override
    public void notifyTimerEnded(int flipsLeft) {

    }

    /**
     * Notifies that the hourglass has started.
     *
     * @param flipsLeft the number of hourglass flips remaining
     * @param nickname the nickname of the player who started the hourglass
     */
    @Override
    public void notifyHourglassStarted(int flipsLeft, String nickname) {

    }

    /**
     * Shows the exit menu for leaving the game.
     */
    public void showExitMenu(){

    }

    private static final CompletableFuture<Void> initializationDone = new CompletableFuture<>();

    // Main container
    @FXML
    private StackPane mainContainer;

    // Current elements
    private Parent currentView;
    private GuiController currentController;

    /**
     * Creates a new ClientGuiController instance.
     *
     * @throws RemoteException if there's an error with remote communication setup
     */
    public ClientGuiController() throws RemoteException {
        clientModel = new ClientModel();
        clientController = new ClientController(clientModel, new ClientPingPongManager());
    }

    /**
     * Initialize task queues for all controller types.
     */
    private void initializeTaskQueues() {
        pendingTasks.put(START_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(MAIN_MENU_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(BUILD_SHIPBOARD_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(CARD_PHASE_CONTROLLER, new ConcurrentLinkedQueue<>());
        pendingTasks.put(END_GAME_CONTROLLER, new ConcurrentLinkedQueue<>());
    }

    /**
     * Loads a view and handles task execution.
     *
     * @param fxmlPath the path to the FXML file
     * @param controllerType the type of controller to load
     * @param <T> the type of GuiController
     */
    private <T extends GuiController> void loadView(String fxmlPath, String controllerType) {
        synchronized (loaderLock) {
            // Check if already loading this controller
            if (loadingControllers.contains(controllerType)) {
                return;
            }

            // Check if the controller already exists and is the correct type
            T existingController = getCurrentControllerByType(controllerType);
            if (existingController != null) {
                return;
            }

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

                if (currentView != null) {
                    mainContainer.getChildren().remove(currentView);
                }

                mainContainer.getChildren().add(newView);
                currentView = newView;
                currentController = controller;

                updateControllerReference(controller, controllerType);

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
     * Updates the specific controller reference based on type.
     *
     * @param controller the controller instance to update
     * @param controllerType the type of controller
     * @param <T> the type of GuiController
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
     * Gets current controller by type.
     *
     * @param controllerType the type of controller to retrieve
     * @param <T> the type of GuiController
     * @return the controller instance or null if not found
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
     * Processes all pending tasks for a specific controller type.
     *
     * @param controllerType the type of controller whose tasks to process
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
     * Checks if a controller is ready and not processing pending tasks.
     *
     * @param controllerType the type of controller to check
     * @return true if the controller is ready for immediate execution, false otherwise
     */
    private boolean isControllerReadyForImmediateExecution(String controllerType) {
        return getCurrentControllerByType(controllerType) != null &&
                !processingTasks.contains(controllerType) &&
                !loadingControllers.contains(controllerType);
    }

    /**
     * Executes a task with a specific controller, queueing if necessary.
     *
     * @param controllerType the type of controller to execute the task with
     * @param task the task to execute
     */
    private void executeWithController(String controllerType, Runnable task) {
        ControllerState state = ControllerState.fromString(controllerType);

        if(state.getOrder() < currentControllerState.getOrder()){
            System.err.println("Error: Trying to execute task with controller in wrong order: " + controllerType);
            return;
        }

        currentControllerState = state;

        String fxmlPath = state.getFxmlPath();

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

    /**
     * Shows the crew placement menu for positioning crew members.
     */
    @Override
    public void showCrewPlacementMenu() {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showCrewPlacementMenu(false)
        );
    }

    /**
     * Shows the prefab ships menu for ship selection.
     *
     * @param prefabShips list of available prefab ship configurations
     */
    @Override
    public void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips) {
        executeWithController(
                BUILD_SHIPBOARD_CONTROLLER,
                () -> buildAndCheckShipBoardController.showPrefabShipBoards(prefabShips)
        );
    }

    /**
     * Shows the invalid ship board menu when ship configuration is invalid.
     */
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

    /**
     * Shows the menu for choosing ship parts.
     *
     * @param shipParts list of ship part coordinate sets to choose from
     */
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

    /**
     * Shows the throw dices menu for dice-based actions.
     */
    @Override
    public void showThrowDicesMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showThrowDicesMenu()
        );
    }

    /**
     * Shows the choose planet menu for planetary encounters.
     */
    @Override
    public void showChoosePlanetMenu() {
        executeWithController(
                CARD_PHASE_CONTROLLER,
                () -> cardPhaseController.showChoosePlanetMenu()
        );
    }

    /**
     * Shows the picked component and its associated menu options.
     */
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

    /**
     * Shows the first to enter notification and button.
     */
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

    /**
     * Shows the main menu with available games.
     */
    @Override
    public void showMainMenu() {
        executeWithController(
                MAIN_MENU_CONTROLLER,
                () -> mainMenuViewController.setAvailableGames()
        );
    }

    /**
     * Shows the new game state when it changes.
     */
    @Override
    public void showNewGameState() {
        if (clientModel.getGameState() == GameState.CREATE_DECK) {
            Platform.runLater(() -> loadView("/gui/CardPhaseView.fxml", CARD_PHASE_CONTROLLER));
        }
    }

    /**
     * Asks the user to enter their nickname.
     */
    @Override
    public void askNickname() {
        executeWithController(
                START_CONTROLLER,
                () -> startViewController.askNickname()
        );
    }

    /**
     * Shows an error message to the user.
     *
     * @param errorMessage the error message to display
     */
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

    /**
     * Shows a message to the user.
     *
     * @param message the message to display
     * @param type the type of message
     */
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
     * Load the start view.
     */
    private void loadStartView() {
        Platform.runLater(() -> loadView("/gui/StartView.fxml", START_CONTROLLER));
    }

    /**
     * Gets the client controller instance.
     *
     * @return the client controller instance
     */
    @Override
    public ClientController getClientController() {
        return clientController;
    }

    /**
     * Gets the client model instance.
     *
     * @return the client model instance
     */
    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    /**
     * Refreshes the game information display.
     *
     * @param gameInfos list of updated game information
     */
    @Override
    public void refreshGameInfos(List<GameInfo> gameInfos) {
        executeWithController(
                MAIN_MENU_CONTROLLER,
                () -> mainMenuViewController.refreshGameInfos(gameInfos)
        );
    }

    /**
     * Transitions to the end game view.
     */
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

    /**
     * Handles the game exit process, cleaning up resources and terminating the application.
     */
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

        // set fullscreen
        Platform.runLater(() -> {
            stage.setFullScreen(true);
        });

    }

}