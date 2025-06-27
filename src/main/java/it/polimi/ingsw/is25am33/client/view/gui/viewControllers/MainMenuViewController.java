package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.ERROR;

public class MainMenuViewController extends GuiController {

    private final Set<PlayerColor> colors = new HashSet<>(Arrays.asList(PlayerColor.values()));
    private String currGameId;
    private final ObservableList<GameInfo> gameInfoList = FXCollections.observableArrayList();

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

    /**
     * Updates the list of available games in the UI.
     * Synchronizes access to the game list and updates the ListView.
     */
    public void setAvailableGames() {
        Platform.runLater(() -> {
            synchronized (gameInfoList) {
                gameListView.setItems(gameInfoList);
            }
        });
    }

    /**
     * Initializes the main menu view controller, called automatically by JavaFX.
     * Sets up combo boxes and list view cell factories.
     */
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

    /**
     * Handles the UI event when user wants to create a new game.
     * Shows the create game form.
     */
    @FXML
    private void handleCreateGame() {
        hideErrorLabel();
        showForm(createGameForm);
    }

    /**
     * Handles the UI event when user submits game creation form.
     * Validates form data and creates the game on the server.
     */
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

    /**
     * Handles the UI event when user wants to exit the application.
     * Plays a fade-out animation before closing.
     */
    @FXML
    private void handleExit() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainMenu);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> System.exit(0));
        fadeOut.play();
    }

    /**
     * Handles the UI event when user wants to join an existing game.
     * Shows the choose game form.
     */
    @FXML
    public void handleChooseGameForm() {
        hideErrorLabel();
        showForm(chooseGameForm);
    }

    /**
     * Handles the selection of a game from the available games list.
     * Validates the selection and proceeds to color selection.
     */
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

    /**
     * Handles the UI event when user submits their game join request.
     * Validates color selection and sends join request to server.
     */
    @FXML
    public void handleSubmitJoinGame() {
        PlayerColor chosenColor = colorListView.getSelectionModel().getSelectedItem();
        if (chosenColor == null) {
            showErrorMessage("Select your color to join the mission.");
            return;
        }

        try {
            if (clientController.joinGame(currGameId, chosenColor))
                showForm(joinOtherPlayersScreen);
            else
                showForm(mainMenu);
        } catch (Exception e) {
            showForm(mainMenu);
            showErrorMessage(e.getMessage());
        }
    }

    /**
     * Handles the UI event when user wants to return to the main menu.
     * Hides error messages and shows the main menu form.
     */
    @FXML
    private void backToMainMenu() {
        hideErrorLabel();
        showForm(mainMenu);
    }

    /**
     * Handles the UI event when user wants to go back to game selection.
     * Hides error messages and shows the choose game form.
     */
    @FXML
    private void backToChooseGame() {
        hideErrorLabel();
        showForm(chooseGameForm);
    }

    @Override
    public String getControllerType() {
        return "MainMenuViewController";
    }

    public void showDisconnectMessage(String message) {
        showMessage(message, true);
        System.exit(0);
    }


    /**
     * Shows a specific form while hiding all others with fade animation.
     *
     * @param targetForm the form to display
     */
    private void showForm(VBox targetForm) {
        Platform.runLater(() -> {
            VBox[] allForms = {mainMenu, createGameForm, chooseGameForm,
                    joinGameForm, gameCreatedScreen, joinOtherPlayersScreen};

            for (VBox form : allForms) {
                form.setVisible(false);
                form.setManaged(false);
            }

            // Show target form with animation
            targetForm.setVisible(true);
            targetForm.setManaged(true);

            // Entry animation
            targetForm.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), targetForm);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

    /**
     * Displays an error message to the user.
     *
     * @param message the error message to display
     */
    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            if (errorLabel != null) {
                errorLabel.setText(message);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);

                errorLabel.setOpacity(1.0);

                System.err.println("Error: " + message);
            }
        });
    }

    /**
     * Hides the error label and clears its text.
     */
    private void hideErrorLabel() {
        Platform.runLater(() -> {
            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
                errorLabel.setText("");
            }
        });
    }

    /**
     * Shows a message to the user in the main menu view.
     *
     * @param errorMessage the message to display
     * @param isPermanent whether the message should persist or fade out
     */
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

    /**
     * Refreshes the list of available games with new data from the server.
     *
     * @param gameInfos the updated list of game information
     */
    public void refreshGameInfos(List<GameInfo> gameInfos) {
        synchronized (gameInfoList) {
            gameInfoList.clear();
            gameInfoList.addAll(gameInfos);
        }
    }
}