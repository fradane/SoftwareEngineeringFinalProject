package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class MainMenuViewController extends GuiController {

    private final Map<String, GameInfo> games = new HashMap<>();
    private final Set<PlayerColor> colors = new HashSet<>(Arrays.asList(PlayerColor.values()));
    private String currGameId;

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
    private ComboBox<PlayerColor> joinColorComboBox;

    @FXML
    private ComboBox<String> joinGameComboBox;

    @FXML
    private Button submitJoinGameButton;

    @FXML
    private Button joinGameButton;

    @FXML
    private Button exitButton;

    @FXML
    private ListView<String> gameListView;

    public void updateGameInfo() {
        clientController.getGames().addListener((ListChangeListener<GameInfo>) _ -> {
            List<String> gameIds = clientController.getGames().stream()
                    .map(GameInfo::getGameId)
                    .toList();
            joinGameComboBox.getItems().setAll(gameIds);
        });
    }

    @FXML
    public void initialize() {
        colorComboBox.getItems().setAll(PlayerColor.values());
        playerCountComboBox.getItems().setAll(2, 3, 4);
        joinGameComboBox.getItems().setAll(games.keySet());
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
        showInfo("Game created successfully.");
    }

    @FXML
    private void handleHoverJoinGameComboBox() {
        List<String> gameNames = new ArrayList<>(games.keySet());
        joinGameComboBox.getItems().setAll(gameNames);
        joinGameComboBox.setVisible(true);
        joinGameComboBox.setManaged(true);
        joinGameComboBox.show();
    }

    @FXML
    private void handleExitHoverJoinGameComboBox() {
        joinGameComboBox.setVisible(false);
        joinGameComboBox.setManaged(false);
    }

    @FXML
    private void handleJoinGameComboBoxSelect() {
        String selected = joinGameComboBox.getValue();
        if (selected == null){
            showInfo("Select a game from the list to join.");
        }
        if (!games.containsKey(selected)) {
            showInfo("The selected game is not available.");
            return;
        }

        this.currGameId = games.get(selected).getGameId();

        mainMenu.setVisible(false);
        mainMenu.setManaged(false);
        joinGameForm.setVisible(true);
        joinGameForm.setManaged(true);

        Set<PlayerColor> availableColors = colors.stream()
                .filter(color -> !games.get(selected).getConnectedPlayers().containsValue(color))
                .collect(Collectors.toSet());

        joinColorComboBox.getItems().setAll(availableColors);

        joinGameComboBox.setVisible(false);
        joinGameComboBox.setManaged(false);
    }

    @FXML
    private void handleSubmitJoinGame() {
        PlayerColor chosenColor = joinColorComboBox.getValue();
        if (chosenColor == null) {
            showInfo("Select a color from the list to join.");
            return;
        }

        clientController.joinGame(currGameId, chosenColor);
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
}