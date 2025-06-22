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
        // Inizializza ComboBox
        colorComboBox.getItems().setAll(PlayerColor.values());
        playerCountComboBox.getItems().setAll(2, 3, 4);

        // Personalizza la visualizzazione delle GameInfo nella ListView
        gameListView.setCellFactory(param -> new ListCell<>() {
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

        // Personalizza la visualizzazione dei colori nella ListView
        colorListView.setCellFactory(param -> new ListCell<>() {
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
        showForm(createGameForm);
    }

    @FXML
    private void handleSubmitCreateGame() {
        Integer numPlayers = playerCountComboBox.getValue();
        boolean isEasyMode = easyModeCheckBox.isSelected();
        PlayerColor chosenColor = colorComboBox.getValue();

        if (chosenColor == null || numPlayers == null) {
            showError("Please fill all fields to create your mission.");
            return;
        }

        clientController.handleCreateGameMenu(numPlayers, isEasyMode, chosenColor);
        showForm(gameCreatedScreen);
    }

    @FXML
    private void handleExit() {
        // Animazione di uscita opzionale
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainMenu);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> System.exit(0));
        fadeOut.play();
    }

    @FXML
    public void handleChooseGameForm() {
        showForm(chooseGameForm);
    }

    @FXML
    public void handleChooseGame() {
        GameInfo gameInfo = gameListView.getSelectionModel().getSelectedItem();
        if (gameInfo == null) {
            showError("Select a mission from the list to join the adventure.");
            return;
        }

        currGameId = gameInfo.getGameId();
        showForm(joinGameForm);

        // Filtra i colori disponibili
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
            showError("Select your pilot color to join the mission.");
            return;
        }

        clientController.joinGame(currGameId, chosenColor);
        showForm(joinOtherPlayersScreen);
    }

    // Metodi di navigazione per i bottoni BACK
    @FXML
    private void backToMainMenu() {
        showForm(mainMenu);
    }

    @FXML
    private void backToChooseGame() {
        showForm(chooseGameForm);
    }

    /**
     * Mostra un form specifico nascondendo tutti gli altri
     */
    private void showForm(VBox targetForm) {
        Platform.runLater(() -> {
            // Lista di tutti i form
            VBox[] allForms = {mainMenu, createGameForm, chooseGameForm,
                    joinGameForm, gameCreatedScreen, joinOtherPlayersScreen};

            // Nascondi tutti i form
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

    /**
     * Mostra un messaggio di errore senza popup (usando label o console)
     */
    private void showError(String message) {
        // Per ora stampa in console, potresti aggiungere una label di errore nell'FXML
        System.err.println("Error: " + message);

        // Opzionale: potresti aggiungere una label di errore nell'interfaccia
        // errorLabel.setText(message);
        // errorLabel.setVisible(true);
    }

    /**
     * Implementazione del metodo showMessage senza popup
     */
    @Override
    public void showMessage(String errorMessage, boolean isPermanent) {
        Platform.runLater(() -> {
            // Gestione degli errori specifici
            if (errorMessage.equals("Color already in use")) {
                showForm(joinGameForm);
                showError("Color already taken by another pilot. Choose a different color.");
            } else {
                showForm(mainMenu);
                showError(errorMessage);
            }
        });
    }
}