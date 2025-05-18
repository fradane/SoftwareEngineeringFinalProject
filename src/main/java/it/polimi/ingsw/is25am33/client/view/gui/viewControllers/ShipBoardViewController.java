package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShipBoardViewController extends GuiController {

    private final List<Coordinates> selectedCoordinates = new ArrayList<>();
    private boolean waitingForCoords = false;

    @FXML
    public Button goBackButton;

    @FXML
    private ImageView visibleCard1 = new ImageView();
    @FXML
    private ImageView visibleCard2 = new ImageView();
    @FXML
    private ImageView visibleCard3 = new ImageView();
    @FXML
    private ImageView component1 = new ImageView();


    @FXML
    public FlowPane littleDeckFlowPane;
    @FXML
    public ImageView shipboard;
    @FXML
    public StackPane stackPane;
    @FXML
    public BorderPane borderPane;
    @FXML
    private GridPane grid;
    @FXML
    private HBox componentBox;

    @FXML
    private Button button04_03;
    @FXML
    private Button button04_04;
    @FXML
    private Button button04_05;
    @FXML
    private Button button04_06;
    @FXML
    private Button button04_07;
    @FXML
    private Button button04_08;
    @FXML
    private Button button04_09;

    @FXML
    private Button button05_03;
    @FXML
    private Button button05_04;
    @FXML
    private Button button05_05;
    @FXML
    private Button button05_06;
    @FXML
    private Button button05_07;
    @FXML
    private Button button05_08;
    @FXML
    private Button button05_09;

    @FXML
    private Button button06_03;
    @FXML
    private Button button06_04;
    @FXML
    private Button button06_05;
    @FXML
    private Button button06_06;
    @FXML
    private Button button06_07;
    @FXML
    private Button button06_08;
    @FXML
    private Button button06_09;

    @FXML
    private Button button07_03;
    @FXML
    private Button button07_04;
    @FXML
    private Button button07_05;
    @FXML
    private Button button07_06;
    @FXML
    private Button button07_07;
    @FXML
    private Button button07_08;
    @FXML
    private Button button07_09;

    @FXML
    private Button button08_03;
    @FXML
    private Button button08_04;
    @FXML
    private Button button08_05;
    @FXML
    private Button button08_06;
    @FXML
    private Button button08_07;
    @FXML
    private Button button08_08;
    @FXML
    private Button button08_09;

    @FXML
    private Button randomComponentButton;
    @FXML
    private Button visibleComponentButton;
    @FXML
    public ComboBox<Integer> littleDeckComboBox;
    @FXML
    private ComboBox<String> viewOtherShipboardButton;

    public void initialize() {
        borderPane.setVisible(true);
        stackPane.setVisible(true);
        grid.setVisible(true);

        viewOtherShipboardButton.setItems(
                FXCollections.observableArrayList(
                        clientModel.getPlayerClientData().keySet()
                                .stream()
                                .filter(nick -> !nick.equals(clientModel.getMyNickname()))
                                .toList()
                )
        );
    }

    public void handleGridButtonClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String id = clickedButton.getId();

        int row = Integer.parseInt(id.split("0")[1].replace("_", ""));
        int column = Integer.parseInt(id.split("0")[2]);

        if (waitingForCoords)
            selectedCoordinates.add(new Coordinates(row, column));
    }


    public void handleLittleDeck() {

        int index = littleDeckComboBox.getValue();
        List<String> imagesName = clientModel.getLittleVisibleDecks().get(index - 1).stream().map(
                card -> card.split("\\n")[0]
        ).toList();

        Platform.runLater(() -> {
            visibleCard1.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.getFirst()))));
            visibleCard2.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(1)))));
            visibleCard3.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(2)))));
            grid.setManaged(false);
            grid.setOpacity(0.5);
            littleDeckFlowPane.setVisible(true);
        });

    }

    public void handleViewOthersShipboard() {
        String selectedNickname = viewOtherShipboardButton.getValue();
        // TODO
    }

    public void showFocusComponent(){
        componentBox.setVisible(true);
        componentBox.setManaged(true);
    }

    public void handleRandomComponentButton() {
        clientController.pickRandomComponent();



    }

    public void handleGoBackButton(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            grid.setOpacity(1);
            grid.setManaged(true);
            littleDeckFlowPane.setVisible(false);
        });
    }
}

