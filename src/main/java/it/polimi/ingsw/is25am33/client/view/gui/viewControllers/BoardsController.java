package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;

public abstract class BoardsController {

    @FXML public Button myShipButton, showFlyingBoardButton;
    @FXML public HBox shipNavigationBar;
    @FXML private StackPane flyingBoard;
    @FXML private StackPane mainPlayerShipBoard, player2ShipBoard, player3ShipBoard, player4ShipBoard;
    @FXML public GridPane myShipBoardGrid;
    @FXML private ImageView redPawn, greenPawn, bluePawn, yellowPawn;

    @FXML private ImageView p2_img04_08, p2_img04_09;
    @FXML private ImageView p3_img04_08, p3_img04_09;
    @FXML private ImageView p4_img04_08, p4_img04_09;

    protected ModelFxAdapter modelFxAdapter;
    protected BoardsEventHandler boardsEventHandler;
    protected ClientModel clientModel;

    private final Map<String, StackPane> otherPlayersShipBoards = new HashMap<>();
    private final Set<Button> navButtons = new HashSet<>();
    protected final Map<String, Button> buttonMap = new HashMap<>();
    private final Set<Button> shadowedButtons = new HashSet<>();

    abstract void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel);

    protected abstract void initializeButtonMap();

    protected abstract Map<Integer, Point2D> getFlyingBoardRelativePositions();

    public void removeHighlightColor() {
        shadowedButtons.forEach(button -> {
                shadowedButtons.remove(button);
                Platform.runLater(() -> {
                    button.setEffect(null);
                    button.getStyleClass().remove("no-hover");
                });
        });
    }

    public void applyHighlightEffect(Coordinates coordinates, Color color) {

        String buttonId = coordinates.getX() + "_" + coordinates.getY();
        Button button = buttonMap.get(buttonId);
        shadowedButtons.add(button);

        Platform.runLater(() -> {

            DropShadow shadow = new DropShadow();
            shadow.setColor(color);
            shadow.setRadius(10);
            shadow.setSpread(0.5);
            button.setEffect(shadow);

            if (!button.getStyleClass().contains("no-hover")) {
                button.getStyleClass().add("no-hover");
            }
        });
    }

    protected StackPane getNodeFromGridPane(GridPane gridPane, int row, int column) {
        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            // Default values are 0 if null
            if ((rowIndex == null ? 0 : rowIndex) == row && (colIndex == null ? 0 : colIndex) == column) {
                return (StackPane) node;
            }
        }
        return null; // Nessun nodo trovato in quella posizione
    }

    @FXML
    protected void handleShowFlyingBoardButton() {
        Platform.runLater(() -> {
            otherPlayersShipBoards.forEach((_, shipboardStackPane) -> shipboardStackPane.setVisible(false));
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().add("active");
            myShipButton.getStyleClass().remove("active");
            mainPlayerShipBoard.setVisible(false);
            flyingBoard.setVisible(true);
            mainPlayerShipBoard.setVisible(false);
        });
    }

    protected void setupFlyingBoardBinding() {

        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    modelFxAdapter.getObservableColorRanking().put(playerColor, new SimpleObjectProperty<>(position));
                });

        // Positioning of the spaceships on the flying board
        modelFxAdapter.getObservableColorRanking()
                .forEach((color, position) -> {
                    position.addListener((_, _, newVal) -> Platform.runLater(() -> {
                        double x = getFlyingBoardRelativePositions().get(newVal).getX();
                        double y = getFlyingBoardRelativePositions().get(newVal).getY();

                        switch (color) {
                            case RED:
                                redPawn.setLayoutX(x);
                                redPawn.setLayoutY(y);
                                redPawn.setVisible(true);
                                break;
                            case GREEN:
                                greenPawn.setLayoutX(x);
                                greenPawn.setLayoutY(y);
                                greenPawn.setVisible(true);
                                break;
                            case BLUE:
                                bluePawn.setLayoutX(x);
                                bluePawn.setLayoutY(y);
                                bluePawn.setVisible(true);
                                break;
                            case YELLOW:
                                yellowPawn.setLayoutX(x);
                                yellowPawn.setLayoutY(y);
                                yellowPawn.setVisible(true);
                                break;
                        }
                    }));
                });
    }

    protected void setupShipBoardNavigationBarAndBoards() {

        Map<StackPane, Pair<ImageView, ImageView>> shipBoardsAndReservedComponents = Map.of(
                player2ShipBoard, new Pair<>(p2_img04_08, p2_img04_09),
                player3ShipBoard, new Pair<>(p3_img04_08, p3_img04_09),
                player4ShipBoard, new Pair<>(p4_img04_08, p4_img04_09)
        );

        Iterator<StackPane> stackPaneIterator = shipBoardsAndReservedComponents.keySet().iterator();

        navButtons.add(myShipButton);

        clientModel.getPlayerClientData()
                .keySet()
                .forEach(nickname -> {
                    // my ship board is loaded by default
                    if (nickname.equals(clientModel.getMyNickname()))
                        return;

                    StackPane playerStackPane = stackPaneIterator.next();
                    otherPlayersShipBoards.put(nickname, playerStackPane);

                    // other players ship board binding
                    ObjectProperty<Component>[][] observableMatrix = modelFxAdapter.getObservableShipBoardOf(nickname);
                    for (int row = 4; row <= 8; row++) {
                        for (int column = 3; column <= 9; column++) {
                            int finalColumn = column;
                            int finalRow = row;
                            observableMatrix[row][column].addListener((_, _, newVal) ->
                                    Platform.runLater(() -> updateOtherShipBoardsAppearance(playerStackPane, newVal, finalRow, finalColumn)
                                    ));
                        }
                    }

                    if (this instanceof Level2BoardsController)
                        ((Level2BoardsController) this).setupOthersBookedComponentBinding(nickname, shipBoardsAndReservedComponents.get(playerStackPane));

                    // navigation button binding
                    setUpNavigationButton(nickname);
                });

    }

    private void setUpNavigationButton(String nickname) {
        Button shipBoardButton = new Button();
        navButtons.add(shipBoardButton);
        shipBoardButton.getStyleClass().add("ship-nav-button");
        shipBoardButton.setText(nickname);
        shipNavigationBar.getChildren().add(shipBoardButton);

        shipBoardButton.setOnAction(_ -> Platform.runLater(() -> {
            flyingBoard.setVisible(false);
            otherPlayersShipBoards.forEach((_, shipboardStackPane1) -> shipboardStackPane1.setVisible(false));
            otherPlayersShipBoards.get(nickname).setVisible(true);
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().remove("active");
            mainPlayerShipBoard.setVisible(false);
            enableMyShipBoardView(false);
            shipBoardButton.getStyleClass().add("active");
        }));
    }

    private void enableMyShipBoardView(boolean enable) {
        Platform.runLater(() -> {
            if (enable) {
                myShipBoardGrid.setManaged(true);
                myShipBoardGrid.setOpacity(1);
            } else {
                myShipBoardGrid.setManaged(false);
                myShipBoardGrid.setOpacity(0.1);
            }
        });
    }

    @FXML
    public void handleMyShipBoardButton() {
        Platform.runLater(() -> {
            flyingBoard.setVisible(false);
            otherPlayersShipBoards.forEach((_, shipboardStackPane) -> shipboardStackPane.setVisible(false));
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().remove("active");
            myShipButton.getStyleClass().add("active");
            mainPlayerShipBoard.setVisible(true);
            enableMyShipBoardView(true);
        });
    }

    @FXML
    public void handleGridButtonClick(ActionEvent actionEvent) {
        Button clickedButton = (Button) actionEvent.getSource();
        String id = clickedButton.getId();

        // Parsing corretto dell'ID del pulsante
        String[] parts = id.replace("button", "").split("_");
        int row = Integer.parseInt(parts[0]);
        int column = Integer.parseInt(parts[1]);

        boardsEventHandler.onGridButtonClick(row, column);
    }

    private void updateOtherShipBoardsAppearance(StackPane playerStackPane, Component newVal, int row, int column) {
        try {
            StackPane cellStackPane = getNodeFromGridPane(((GridPane) playerStackPane.getChildren().get(1)), row - 4, column - 3);

            if (cellStackPane != null && !cellStackPane.getChildren().isEmpty()) {
                // Ottieni la prima ImageView dallo StackPane (quella di default)
                ImageView imageView = (ImageView) cellStackPane.getChildren().getFirst();

                if (newVal != null) {
                    String componentFile = newVal.toString().split("\\n")[0];
                    Image image = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/component/" + componentFile)));
                    imageView.setImage(image);
                    imageView.setRotate(newVal.getRotation() * 90);

                    StackPane newStackPane = getUpdatedStackPaneWithImages(imageView, newVal);
                    GridPane gridPane = (GridPane) playerStackPane.getChildren().get(1);

                    // Rimuovi il nodo esistente, se presente
                    Node existingNode = getNodeFromGridPane(gridPane, row - 4, column - 3);
                    if (existingNode != null) {
                        gridPane.getChildren().remove(existingNode);
                    }

                    // Imposta posizione e aggiungi il nuovo StackPane
                    GridPane.setRowIndex(newStackPane, row - 4);
                    GridPane.setColumnIndex(newStackPane, column - 3);
                    gridPane.getChildren().add(newStackPane);
                } else {
                    imageView.setImage(null);
                    imageView.setRotate(0);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating component appearance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateButtonAppearance(Button button, Component component) {
        final int FIXED_COMPONENT_LENGTH = 70;

        if (component != null) {
            try {
                String fileName = component.getImageName();
                Image img = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/gui/graphics/component/" + fileName)));
                ImageView imgView = new ImageView(img);
                button.setVisible(true);
                button.setManaged(true);
                imgView.setFitWidth(FIXED_COMPONENT_LENGTH);
                imgView.setFitHeight(FIXED_COMPONENT_LENGTH);
                imgView.setPreserveRatio(true);
                imgView.setRotate(component.getRotation() * 90);
                StackPane stackPane = getUpdatedStackPaneWithImages(imgView, component);
                button.setGraphic(stackPane);
            } catch (Exception e) {
                System.err.println("Error updating button appearance: " + e.getMessage());
                // Fallback: mostra almeno un indicatore visivo
                button.setStyle("-fx-background-color: yellow;");
            }
        } else {
            button.setGraphic(null);
            button.setStyle("-fx-background-color: transparent;");
        }
    }

    protected void setupGridBindings(ObjectProperty<Component>[][] observableMatrix) {

        for (Map.Entry<String, Button> entry : buttonMap.entrySet()) {
            String[] coords = entry.getKey().split("_");
            int modelRow = Integer.parseInt(coords[0]);
            int modelCol = Integer.parseInt(coords[1]);
            Button button = entry.getValue();
            ObjectProperty<Component> cellProperty = observableMatrix[modelRow][modelCol];

            cellProperty.addListener((_, _, newVal) ->
                    Platform.runLater(() ->
                            updateButtonAppearance(button, newVal)
                    ));

            Component initialComponent = cellProperty.get();
            if (initialComponent != null)
                Platform.runLater(() -> updateButtonAppearance(button, initialComponent));
        }
    }

    private StackPane getUpdatedStackPaneWithImages(ImageView imgView, Component component) {

        ImageView featureImageView = new ImageView();
        Image featureImage;
        DropShadow dropShadow = new DropShadow();

        dropShadow.setColor(Color.BLACK);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setSpread(0.6);
        dropShadow.setRadius(15);


        featureImageView.setEffect(dropShadow);
        featureImageView.setFitWidth(40);
        featureImageView.setFitHeight(40);
        featureImageView.setPreserveRatio(true);

        switch (component.getLabel()) {
            case "CAB", "MCB":

                List<CrewMember> crewMembers = ((Cabin) component).getInhabitants();

                System.err.println("Size: " + crewMembers.size());

                if (crewMembers.isEmpty()) {
                    return new StackPane(imgView);
                } else if (crewMembers.contains(CrewMember.PURPLE_ALIEN)) {
                    featureImage = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/crewMembers/purple_alien.png")));
                } else if (crewMembers.contains(CrewMember.BROWN_ALIEN)) {
                    featureImage = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/crewMembers/brown_alien.png")));
                } else if (crewMembers.size() == 1) {
                    featureImage = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/crewMembers/human.png")));
                } else {

                    ImageView featureImageView1 = new ImageView();
                    Image featureImage1;

                    featureImageView1.setEffect(dropShadow);
                    featureImageView1.setFitWidth(40);
                    featureImageView1.setFitHeight(40);
                    featureImageView1.setPreserveRatio(true);

                    featureImage = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/crewMembers/human.png")));

                    featureImage1 = new Image(Objects.requireNonNull(getClass()
                            .getResourceAsStream("/gui/graphics/crewMembers/human.png")));

                    featureImageView.setImage(featureImage);
                    featureImageView1.setImage(featureImage1);

                    featureImageView.setTranslateX(-10);   // sinistra
                    featureImageView1.setTranslateX(10);    // destra

                    return new StackPane(imgView, featureImageView1, featureImageView);

                }
                break;

            default:
                return new StackPane(imgView);

        }

        featureImageView.setImage(featureImage);
        return new StackPane(imgView, featureImageView);
    }

}
