package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class MainMenuViewController extends GuiController {

    private final Set<PlayerColor> colors = new HashSet<>(Arrays.asList(PlayerColor.values()));
    private String currGameId;

    @FXML
    public Button submitCreateGameButton;

    @FXML
    public Button chooseGameButton;

    @FXML
    public Button submitJoinGameButton;

    @FXML
    public VBox chooseGameForm;

    @FXML
    private VBox mainMenu;

    @FXML
    private VBox createGameForm;

    @FXML
    private VBox joinGameForm;

    @FXML
    private CheckBox easyModeCheckBox;

    @FXML
    private ComboBox<PlayerColor> colorComboBox;

    @FXML
    private ComboBox<Integer> playerCountComboBox;

    @FXML
    private ListView<PlayerColor> colorListView;

    @FXML
    private ListView<GameInfo> gameListView;

    public void setAvailableGames() {
        Platform.runLater(() -> gameListView.setItems(clientController.getObservableGames()));
    }

    @FXML
    public void initialize() {
        colorComboBox.getItems().setAll(PlayerColor.values());
        playerCountComboBox.getItems().setAll(2, 3, 4);
    }

    @FXML
    private void handleCreateGame() {
        mainMenu.setVisible(false);
        mainMenu.setManaged(false);
        createGameForm.setVisible(true);
        createGameForm.setManaged(true);
    }

    @FXML
    private void handleSubmitCreateGame() {
        Integer numPlayers = playerCountComboBox.getValue();
        boolean isEasyMode = easyModeCheckBox.isSelected();
        PlayerColor chosenColor = colorComboBox.getValue();

        if (chosenColor == null || numPlayers == null) {
            showInfo("Please fill all fields.");
            return;
        }

        clientController.handleCreateGameMenu(numPlayers, isEasyMode, chosenColor);
    }

    @FXML
    private void handleExit() {

        System.exit(0);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleChooseGameForm() {
        mainMenu.setVisible(false);
        mainMenu.setManaged(false);
        chooseGameForm.setVisible(true);
        chooseGameForm.setManaged(true);
    }

    @FXML
    public void handleChooseGame() {
        GameInfo gameInfo = gameListView.getSelectionModel().getSelectedItem();
        if (gameInfo == null) {
            showInfo("Select a game from the list to join.");
            return;
        }

        currGameId = gameInfo.getGameId();
        chooseGameForm.setVisible(false);
        chooseGameForm.setManaged(false);
        joinGameForm.setVisible(true);
        joinGameForm.setManaged(true);
        colorListView.setItems(FXCollections.observableArrayList(
                    colors.stream().filter(color -> !gameInfo.getConnectedPlayers().containsValue(color)).collect(Collectors.toSet()))
                );
    }

    @FXML
    public void handleSubmitJoinGame() {
        PlayerColor chosenColor = colorListView.getSelectionModel().getSelectedItem();
        if (chosenColor == null) {
            showInfo("Select a color from the list to join.");
            return;
        }
        clientController.joinGame(currGameId, chosenColor);
    }

    public void showError(String errorMessage) {

        Platform.runLater(() -> {
            joinGameForm.setVisible(false);
            joinGameForm.setManaged(false);

            if (errorMessage.equals("Color already in use")) {
                joinGameForm.setVisible(true);
                joinGameForm.setManaged(true);
            } else {
                mainMenu.setVisible(true);
                mainMenu.setManaged(true);
            }
            showInfo(errorMessage);
        });

    }

}