package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.ClientPingPongManager;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.*;
import it.polimi.ingsw.is25am33.client.view.tui.MessageType;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ClientGuiController extends Application implements ClientView {

    final ClientModel clientModel;
    final ClientController clientController;
    private static final CompletableFuture<Void> initializationDone = new CompletableFuture<>();
    private static ClientGuiController instance;
    private Stage primaryStage;
    private boolean isTestFlight;

    StartViewController startViewController;
    MainMenuViewController mainMenuViewController;
    BuildAndCheckShipBoardController buildAndCheckShipBoardController;
    CardPhaseController cardPhaseController;

    public static ClientGuiController getInstance() {
        return instance;
    }

    public ClientGuiController() throws RemoteException {
        clientModel = new ClientModel();
        clientController = new ClientController(clientModel, new ClientPingPongManager());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.primaryStage = primaryStage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/StartView.fxml"));
        Scene scene = new Scene(loader.load());
        startViewController = loader.getController();
        GuiController.setClientModel(clientModel);
        GuiController.setClientController(clientController);
        primaryStage.setTitle("Galaxy Trucker");
        primaryStage.setScene(scene);
        primaryStage.show();
        initializationDone.complete(null);
    }

    @Override
    public void notifyHourglassRestarted(int flipsLeft) {

    }

    @Override
    public void setIsTestFlight(boolean isTestFlight) {
        this.isTestFlight = isTestFlight;
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
    public void showCurrentRanking() {
        //TODO
    }

    @Override
    public void showCrewPlacementMenu() {
        //TODO
    }

    @Override
    public void showPrefabShipsMenu(List<PrefabShipInfo> prefabShips) {
        //TODO
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
        //TODO
    }

    @Override
    public void showChooseEnginesMenu() {
        //TODO
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
        //TODO
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
                    buildAndCheckShipBoardController.showMessage(message.split("\n")[0]);
                }

        }

    }

    @Override
    public void showError(String errorMessage) {
        switch (errorMessage) {
            case "Color already in use", "GameModel already started":
                mainMenuViewController.showError(errorMessage);
                break;
            default:
                startViewController.showServerError(errorMessage);
        }
    }

    @Override
    public void askNickname() {
        startViewController.askNickname();
    }

    @Override
    public int[] askCreateGame() {
        return new int[0];
    }

    @Override
    public String[] askJoinGame(List<GameInfo> games) {
        return new String[0];
    }

    @Override
    public void showMainMenu() {

        javafx.application.Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainMenuView.fxml"));
                Parent root = loader.load();
                mainMenuViewController = loader.getController();
                mainMenuViewController.setAvailableGames();
                primaryStage.setScene(new Scene(root));
                primaryStage.show();
            } catch (IOException e) {
                System.out.println("Error while loading the main menu view.");
                e.printStackTrace();
            }
        });

    }

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

    @Override
    public void showNewGameState() {
        if (clientModel.getGameState() == GameState.CREATE_DECK) {
            javafx.application.Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CardPhaseView.fxml"));
                    Parent root = loader.load();
                    cardPhaseController = loader.getController();
                    primaryStage.setScene(new Scene(root));
                    primaryStage.setFullScreen(true);
                    primaryStage.setMaximized(true);
                    primaryStage.show();
                } catch (IOException e) {
                    System.out.println("Error while loading the new card phase view.");
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void showDangerousObj() {

    }

    @Override
    public void showNewCardState() {

    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {

    }

    @Override
    public ClientModel getClientModel() {
        return clientModel;
    }

    @Override
    public void showBuildShipBoardMenu() {

        if (buildAndCheckShipBoardController != null) return;

        boolean isTestFlight = clientController.getCurrentGameInfo().isTestFlight();

        String fxmlPath =  isTestFlight ? "/gui/Shipboard_1.fxml" : "/gui/BuildAndCheckShipBoardView.fxml";

        javafx.application.Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                buildAndCheckShipBoardController = loader.getController();
                GuiController.setClientModel(clientModel);
                primaryStage.setScene(new Scene(root));
                primaryStage.show();
            } catch (IOException e) {
                System.out.println("Error while loading the shipboard view.");
                e.printStackTrace();
            }
        });
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

//    @Override
//    public BiConsumer<CallableOnGameController, String> showVisitLocationMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showThrowDicesMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChoosePlanetMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChooseEnginesMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showChooseCannonsMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showSmallDanObjMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showBigShotMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showEpidemicMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showStardustMenu() {
//        return null;
//    }
//
//    @Override
//    public BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu() {
//        return null;
//    }

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

}