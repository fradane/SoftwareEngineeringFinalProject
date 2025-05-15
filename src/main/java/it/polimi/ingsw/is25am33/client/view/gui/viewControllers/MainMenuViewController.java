package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class MainMenuViewController extends GuiController {

    private ClientModel clientModel;
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
    private Button submitJoinGameButton;

    @FXML
    private Button joinGameButton;

    @FXML
    private Button exitButton;

    @FXML
    private ListView<String> gameListView;


    @FXML
    private void handleCreateGame() {
        mainMenu.setVisible(false);
        mainMenu.setManaged(false);
        //createGameForm.setAlignment(Pos.CENTER);
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
    private void handleJoinGame() {
        String selected = gameListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a game from the list to join it.");
        } else {
            this.currGameId = games.get(selected).getGameId();
            Set<PlayerColor> availableColors = colors.stream()
                    .filter(color -> !games.get(selected)
                            .getConnectedPlayers()
                            .containsValue(color))
                    .collect(Collectors.toSet());
            joinColorComboBox.getItems().setAll(availableColors);

            joinGameForm.setVisible(true);
        }
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

    public void showAvailableGames(Iterable<GameInfo> games) {

        games.forEach(game -> this.games.put("ID: " + game.getGameId() +
                " | Players: " + game.getConnectedPlayersNicknames().size() + "/" + game.getMaxPlayers() +
                " | Test Flight: " + (game.isTestFlight() ? "Yes" : "No"), game));

        List<String> gameInfo = new ArrayList<>(this.games.keySet());
        gameListView.getItems().setAll(gameInfo);
        gameListView.setVisible(true);


    }

    @FXML
    public void handleSubmitJoinGame() {
        PlayerColor chosenColor = (PlayerColor) joinColorComboBox.getValue();

        if (chosenColor == null)
            showInfo("Select a color to join the game.");
        else
            clientController.joinGame(currGameId, chosenColor);
    }
}