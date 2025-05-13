package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.client.view.MessageType;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.GuiController;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.MainMenuViewController;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.StartViewController;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ClientGuiController extends Application implements ClientView {

    final ClientModel clientModel;
    final ClientController clientController;
    private static final CompletableFuture<Void> initializationDone = new CompletableFuture<>();
    private static ClientGuiController instance;
    private Stage primaryStage;

    StartViewController startViewController;
    MainMenuViewController mainMenuViewController;

    public static ClientGuiController getInstance() {
        return instance;
    }

    public ClientGuiController() throws RemoteException {
        clientModel = new ClientModel();
        clientController = new ClientController(clientModel);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.primaryStage = primaryStage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/StartView.fxml"));
        Scene scene = new Scene(loader.load());
        startViewController = loader.getController();
        startViewController.setClientModel(clientModel);
        GuiController.setClientController(clientController);
        primaryStage.setTitle("Galaxy Trucker");
        primaryStage.setScene(scene);
        primaryStage.show();
        initializationDone.complete(null);
    }

    public CompletableFuture<Void> getInitializationDoneFuture() {
        return initializationDone;
    }

    @Override
    public ClientController getClientController() {
        return clientController;
    }

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

    }

    @Override
    public void showError(String errorMessage) {
        startViewController.showServerError(errorMessage);
    }

    @Override
    public void askNickname() {
        startViewController.askNickname();
    }

    @Override
    public String askServerAddress() {
        return "";
    }

    @Override
    public void showAvailableGames(Iterable<GameInfo> games) {
        mainMenuViewController.showAvailableGames(games);
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainMenuView.fxml"));
            Parent root = loader.load();
            mainMenuViewController = loader.getController();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error while loading the main menu view.");
        }

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
    public String askPlayerColor() {
        return "";
    }

    @Override
    public String askPlayerColor(List<PlayerColor> availableColors) {
        return "";
    }

    @Override
    public void showNewGameState() {

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
    public BiFunction<CallableOnGameController, String, Boolean> showBuildShipBoardMenu() {
        return null;
    }

    @Override
    public void notifyNoMoreComponentAvailable() {

    }

    @Override
    public void showShipBoardsMenu() {

    }

    @Override
    public BiFunction<CallableOnGameController, String, Boolean> showPickedComponentAndMenu(Component component) {
        return null;
    }

    @Override
    public void showShipBoard(Component[][] shipBoard, String shipBoardOwnerNickname) {

    }

    @Override
    public BiFunction<CallableOnGameController, String, Component> showVisibleComponentAndMenu(Map<Integer, Component> visibleComponents) {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showVisitLocationMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showThrowDicesMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChoosePlanetMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChooseEnginesMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showAcceptTheRewardMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showChooseCannonsMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showSmallDanObjMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showBigMeteoriteMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showBigShotMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleRemoveCrewMembersMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleCubesRewardMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showEpidemicMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showStardustMenu() {
        return null;
    }

    @Override
    public BiConsumer<CallableOnGameController, String> showHandleCubesMalusMenu() {
        return null;
    }

    @Override
    public Boolean showLittleDeck(int littleDeckChoice) {
        return null;
    }

    @Override
    public void updateTimeLeft(int timeLeft) {

    }

    @Override
    public void notifyTimerEnded(int flipsLeft) {

    }

    @Override
    public void notifyHourglassStarted(int flipsLeft, String nickname) {

    }

}