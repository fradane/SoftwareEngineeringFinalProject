package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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

    @FXML
    private VBox gameCreatedScreen;

    @FXML
    private VBox joinOtherPlayersScreen;

    @FXML
    private Label errorLabel;

    public void setAvailableGames() {
        Platform.runLater(() -> gameListView.setItems(clientController.getObservableGames()));
    }

    @FXML
    public void initialize() {
        colorComboBox.getItems().setAll(PlayerColor.values());
        playerCountComboBox.getItems().setAll(2, 3, 4);

        gameListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(GameInfo gameInfo, boolean empty) {
                super.updateItem(gameInfo, empty);
                if (empty || gameInfo == null) {
                    setText(null);
                } else {
                    setText("Game " + gameInfo.getGameId() +
                            " | Players: " + gameInfo.getConnectedPlayersNicknames().size() + "/" + gameInfo.getMaxPlayers() +
                            " | Mode: " + (gameInfo.isTestFlight() ? "Test Flight" : "Full Mission"));
                }
            }
        });

        colorListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerColor color, boolean empty) {
                super.updateItem(color, empty);
                if (empty || color == null) {
                    setText(null);
                } else {
                    setText(color.toString());
                }
            }
        });
    }

    @FXML
    private void handleCreateGame() {
        hideErrorLabel();
        showForm(createGameForm);
    }

    @FXML
    private void handleSubmitCreateGame() {
        Integer numPlayers = playerCountComboBox.getValue();
        boolean isEasyMode = easyModeCheckBox.isSelected();
        PlayerColor chosenColor = colorComboBox.getValue();

        if (chosenColor == null || numPlayers == null) {
            showErrorMessage("Please fill all fields to create your mission.");
            return;
        }

        clientController.handleCreateGameMenu(numPlayers, isEasyMode, chosenColor);
        showForm(gameCreatedScreen);
    }

    @FXML
    private void handleExit() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainMenu);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> System.exit(0));
        fadeOut.play();
    }

    @FXML
    public void handleChooseGameForm() {
        hideErrorLabel();
        showForm(chooseGameForm);
    }

    @FXML
    public void handleChooseGame() {
        GameInfo gameInfo = gameListView.getSelectionModel().getSelectedItem();
        if (gameInfo == null) {
            showErrorMessage("Select a mission from the list to join the adventure.");
            return;
        }

        currGameId = gameInfo.getGameId();
        showForm(joinGameForm);

        colorListView.setItems(FXCollections.observableArrayList(
                colors.stream()
                        .filter(color -> !gameInfo.getConnectedPlayers().containsValue(color))
                        .collect(Collectors.toSet()))
        );
    }

    @FXML
    public void handleSubmitJoinGame() {
        PlayerColor chosenColor = colorListView.getSelectionModel().getSelectedItem();
        if (chosenColor == null) {
            showErrorMessage("Select your color to join the mission.");
            return;
        }

        clientController.joinGame(currGameId, chosenColor);
        showForm(joinOtherPlayersScreen);
    }

    @FXML
    private void backToMainMenu() {
        hideErrorLabel();
        showForm(mainMenu);
    }

    @FXML
    private void backToChooseGame() {
        hideErrorLabel();
        showForm(chooseGameForm);
    }


    private void showForm(VBox targetForm) {
        Platform.runLater(() -> {
            VBox[] allForms = {mainMenu, createGameForm, chooseGameForm,
                    joinGameForm, gameCreatedScreen, joinOtherPlayersScreen};

            for (VBox form : allForms) {
                form.setVisible(false);
                form.setManaged(false);
            }

            // Mostra il form target con animazione
            targetForm.setVisible(true);
            targetForm.setManaged(true);

            // Animazione di entrata
            targetForm.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), targetForm);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            if (errorLabel != null) {
                errorLabel.setText(message);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);

                errorLabel.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), errorLabel);
                fadeIn.setToValue(1.0);
                fadeIn.play();

                System.err.println("Error: " + message);
            }
        });
    }

    private void hideErrorLabel() {
        Platform.runLater(() -> {
            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
                errorLabel.setText("");
            }
        });
    }

    @Override
    public void showMessage(String errorMessage, boolean isPermanent) {
        Platform.runLater(() -> {
            if (errorMessage.equals("Color already in use")) {
                showForm(joinGameForm);
                showErrorMessage("Color already taken. Choose a different color.");
            } else if (errorMessage.equals("GameModel already started")) {
                showForm(mainMenu);
                showErrorMessage("This game has already started. Please choose another game.");
            } else {
                showErrorMessage(errorMessage);
            }

            if (!isPermanent) {
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        hideErrorLabel();
                    }
                }, 4000);
            }
        });
    }
}