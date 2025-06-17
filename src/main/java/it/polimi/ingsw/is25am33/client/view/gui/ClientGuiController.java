package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.ClientPingPongManager;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.model.card.ClientFreeSpace;
import it.polimi.ingsw.is25am33.client.model.card.ClientStarDust;
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
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ClientGuiController extends Application implements ClientView {

    final ClientModel clientModel;
    final ClientController clientController;
    private static ClientGuiController instance;
    private Stage primaryStage;

    private StartViewController startViewController;
    private MainMenuViewController mainMenuViewController;
    private BuildAndCheckShipBoardController buildAndCheckShipBoardController;
    private CardPhaseController cardPhaseController;

    public static ClientGuiController getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.primaryStage = primaryStage;

        // Icona per barra del titolo
        primaryStage.getIcons().add(new javafx.scene.image.Image(
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
    public void showPickedComponentAndMenu() {
        buildAndCheckShipBoardController.showFocusComponent();
    }

    @Override
    public Component askComponentToRemove(ShipBoardClient shipBoard, List<Component> incorrectlyPositionedComponents) {
        return null;
    }

    @Override
    public void showFirstToEnter() {
        if (buildAndCheckShipBoardController != null)
            buildAndCheckShipBoardController.showFirstToEnterButton();
    }

    @Override
    public void checkShipBoardAfterAttackMenu() {

    }

    @Override
    public void showCurrentRanking() {
        //TODO
    }

    @Override
    public void showCrewPlacementMenu() {
        if (buildAndCheckShipBoardController != null)
            buildAndCheckShipBoardController.showCrewPlacementMenu(false);
    }

    @Override
    public void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips) {
        if (buildAndCheckShipBoardController != null)
            buildAndCheckShipBoardController.showPrefabShipBoards(prefabShips);
    }

    @Override
    public void showInvalidShipBoardMenu() {
        if (buildAndCheckShipBoardController != null)
            buildAndCheckShipBoardController.showInvalidComponents();
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
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts) {
        if (buildAndCheckShipBoardController != null)
            buildAndCheckShipBoardController.showShipParts(shipParts);
    }

    @Override
    public void showVisitLocationMenu() {
        //TODO
    }

    @Override
    public void showThrowDicesMenu() {
        //TODO
    }

    @Override
    public void showChoosePlanetMenu() {
        if (cardPhaseController != null)
            cardPhaseController.showChoosePlanetMenu();
    }

    @Override
    public void showChooseEnginesMenu() {
        // TODO controllare come viene gestito per altre carte
        if (cardPhaseController != null) {
            if (clientModel.getCurrAdventureCard() instanceof ClientFreeSpace)
                cardPhaseController.showFreeSpaceMenu();
            else
                System.err.println("Not FreeSpace card: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
        }
    }

    @Override
    public void showAcceptTheRewardMenu() {
        //TODO
    }

    @Override
    public void showChooseCannonsMenu() {
        //TODO
    }

    @Override
    public void showSmallDanObjMenu() {
        //TODO
    }

    @Override
    public void showBigMeteoriteMenu() {
        //TODO
    }

    @Override
    public void showBigShotMenu() {
        //TODO
    }

    @Override
    public void showHandleRemoveCrewMembersMenu() {
        //TODO
    }

    @Override
    public void showHandleCubesRewardMenu() {
        //TODO
    }

    @Override
    public void showEpidemicMenu() {
        //TODO
    }

    @Override
    public void showStardustMenu() {
        // TODO controllare poi le altre carte
        if (cardPhaseController != null) {
            if (clientModel.getCurrAdventureCard() instanceof ClientStarDust)
                cardPhaseController.showStardustMenu();
            else
                System.err.println("Not Stardust card: " + clientModel.getCurrAdventureCard().getClass().getSimpleName());
        }
    }

    @Override
    public void showHandleCubesMalusMenu() {
        //TODO
    }

    @Override
    public PlayerColor intToPlayerColor(int colorChoice) {
        return ClientView.super.intToPlayerColor(colorChoice);
    }

    public CompletableFuture<Void> getInitializationDoneFuture() {
        return initializationDone;
    }

    @Override
    public ClientController getClientController() {
        return clientController;
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
    public void showMessage(String message, MessageType type) {

        if (clientModel.getGameState() == null)
            return;

        switch (clientModel.getGameState()) {

            case BUILD_SHIPBOARD, CHECK_SHIPBOARD:
                if (buildAndCheckShipBoardController != null) {
                    buildAndCheckShipBoardController.showMessage(message.split("\n")[0], false);
                }

        }

    }

//    @Override
//    public void showError(String errorMessage) {
//        switch (errorMessage) {
//            case "Color already in use", "GameModel already started":
//                mainMenuViewController.showError(errorMessage);
//                break;
//            default:
//                startViewController.showServerError(errorMessage);
//        }
//    }

//    @Override
//    public void askNickname() {
//        startViewController.askNickname();
//    }

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

    @Override
    public ClientModel getClientModel() {
        return clientModel;
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
        // if not the first time return
        if (buildAndCheckShipBoardController != null) return;

        String fxmlPath = "/gui/BuildAndCheckShipBoardView.fxml";
        Platform.runLater(() ->
                buildAndCheckShipBoardController = loadView(fxmlPath)
        );
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
     * Carica una nuova vista nel container principale
     * @param fxmlPath il percorso del file FXML da caricare
     * @return il controller della vista caricata
     */
    private <T extends GuiController> T loadView(String fxmlPath) {
        try {
            // Carica la nuova vista
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newView = loader.load();
            T controller = loader.getController();

            // Sostituisci la vista corrente
            Platform.runLater(() -> {
                // Verifica che mainContainer non sia null
                if (mainContainer == null) {
                    System.err.println("Error: mainContainer is null. Cannot load view: " + fxmlPath);
                    return;
                }

                // Rimuovi la vista precedente
                if (currentView != null) {
                    mainContainer.getChildren().remove(currentView);
                }

                // Aggiungi la nuova vista
                mainContainer.getChildren().add(newView);
                currentView = newView;
                currentController = controller;
            });

            return controller;
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Carica la schermata iniziale
     */
    private void loadStartView() {
        startViewController = loadView("/gui/StartView.fxml");
    }

    // Metodi per caricare le diverse viste

    @Override
    public void showMainMenu() {
        Platform.runLater(() -> {
            mainMenuViewController = loadView("/gui/MainMenuView.fxml");
            if (mainMenuViewController != null) {
                mainMenuViewController.setAvailableGames();
            }
        });
    }

    @Override
    public void showNewGameState() {
        if (clientModel.getGameState() == GameState.CREATE_DECK) {
            // Prepara la transizione se necessario
            if (buildAndCheckShipBoardController != null) {
                buildAndCheckShipBoardController.prepareForPhaseTransition();
            }

            Platform.runLater(() -> {
                cardPhaseController = loadView("/gui/CardPhaseView.fxml");

                // Imposta fullscreen dopo aver caricato la vista
                primaryStage.setFullScreen(true);
                primaryStage.setMaximized(true);
            });
        }
    }

    @Override
    public void askNickname() {
        if (startViewController != null) {
            startViewController.askNickname();
        } else {
            loadStartView();
            startViewController.askNickname();
        }
    }

    // Un nuovo metodo per accedere al ModelFxAdapter tra i controller
    public ModelFxAdapter getSharedModelFxAdapter() {
        if (buildAndCheckShipBoardController != null && buildAndCheckShipBoardController.getModelFxAdapter() != null) {
            return buildAndCheckShipBoardController.getModelFxAdapter();
        } else if (cardPhaseController != null && cardPhaseController.getModelFxAdapter() != null) {
            return cardPhaseController.getModelFxAdapter();
        }

        // Se non esiste, creane uno nuovo
        return new ModelFxAdapter(clientModel);
    }

    @Override
    public void showError(String errorMessage) {
        switch (errorMessage) {
            case "Color already in use", "GameModel already started":
                if (mainMenuViewController != null) {
                    mainMenuViewController.showMessage(errorMessage, false);
                }
                break;
            default:
                if (startViewController != null) {
                    startViewController.showServerError(errorMessage);
                } else {
                    // Fallback: mostra un alert generico
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText(errorMessage);
                        alert.showAndWait();
                    });
                }
        }
    }

}