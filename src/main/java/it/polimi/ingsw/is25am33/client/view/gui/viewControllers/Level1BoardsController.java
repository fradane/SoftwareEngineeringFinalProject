package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.*;

public class Level1BoardsController implements BoardsController {

    @FXML public Pane pawnsPane;
    @FXML public StackPane boardsPane;
    @FXML public Button myShipButton, showFlyingBoardButton;
    @FXML public HBox shipNavigationBar;
    @FXML public GridPane myShipBoardGrid;
    @FXML private Button button04_03, button04_04, button04_05, button04_06, button04_07, button04_08, button04_09;
    @FXML private Button button05_03, button05_04, button05_05, button05_06, button05_07, button05_08, button05_09;
    @FXML private Button button06_03, button06_04, button06_05, button06_06, button06_07, button06_08, button06_09;
    @FXML private Button button07_03, button07_04, button07_05, button07_06, button07_07, button07_08, button07_09;
    @FXML private Button button08_03, button08_04, button08_05, button08_06, button08_07, button08_08, button08_09;

    @FXML private StackPane mainPlayerShipBoard, player2ShipBoard, player3ShipBoard, player4ShipBoard;
    @FXML private StackPane flyingBoard;

    @FXML private ImageView redPawn, greenPawn, bluePawn, yellowPawn;

    private ModelFxAdapter modelFxAdapter;
    private BoardsEventHandler boardsEventHandler;
    private ClientModel clientModel;

    private final Map<String, Button> buttonMap = new HashMap<>();
    private final Map<String, StackPane> otherPlayersShipBoards = new HashMap<>();
    private final Set<Button> navButtons = new HashSet<>();
    private final Set<Button> shadowedButtons = new HashSet<>();

    private final int FIXED_COMPONENT_LENGTH = 70;
    private final int FIXED_BOOKED_COMPONENT_HEIGHT = 55;

    private static final Map<Integer, Point2D> flyingBoardPositions = Map.ofEntries(
            Map.entry(0, new Point2D(149.0, 189.0)),
            Map.entry(1, new Point2D(192.0, 171.0)),
            Map.entry(2, new Point2D(238.0, 160.0)),
            Map.entry(3, new Point2D(282.0, 154.0)),
            Map.entry(4, new Point2D(327.0, 155.0)),
            Map.entry(5, new Point2D(371.0, 160.0)),
            Map.entry(6, new Point2D(415.0, 173.0)),
            Map.entry(7, new Point2D(458.0, 191.0)),
            Map.entry(8, new Point2D(497.0, 219.0)),
            Map.entry(9, new Point2D(524.0, 261.0)),
            Map.entry(10, new Point2D(517.0, 313.0)),
            Map.entry(11, new Point2D(493.0, 352.0)),
            Map.entry(12, new Point2D(452.0, 377.0)),
            Map.entry(13, new Point2D(409.0, 393.0))
            // TODO finire le posizioni
    );

    @Override
    public void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel) {
        this.modelFxAdapter = modelFxAdapter;
        this.boardsEventHandler = boardsEventHandler;
        this.clientModel = clientModel;
        initializeButtonMap();
        setupFlyingBoardBinding();
        setupShipBoardNavigationBar();
        setupBookedComponentsBindings();
        setupGridBindings(modelFxAdapter.getMineObservableMatrix());
    }

    public void initialize() {

    }

    public void handleGridButtonClick(ActionEvent actionEvent) {
        Button clickedButton = (Button) actionEvent.getSource();
        String id = clickedButton.getId();

        // Correct parsing of the button ID
        String[] parts = id.replace("button", "").split("_");
        int row = Integer.parseInt(parts[0]);
        int column = Integer.parseInt(parts[1]);

        boardsEventHandler.onGridButtonClick(row, column);
    }

    private void setupFlyingBoardBinding() {

        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    modelFxAdapter.getObservableColorRanking().put(playerColor, new SimpleObjectProperty<>(position));
                });

        // Positioning of the spaceships on the flying board
        modelFxAdapter.getObservableColorRanking()
                .forEach((color, position) -> {
                    position.addListener((_, _, newVal) -> Platform.runLater(() -> {
                        double x = flyingBoardPositions.get(newVal).getX();
                        double y = flyingBoardPositions.get(newVal).getY();

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

    private void setupShipBoardNavigationBar() {

        List<StackPane> othersStackPanes = List.of(player2ShipBoard, player3ShipBoard, player4ShipBoard);
        Iterator<StackPane> stackPaneIterator = othersStackPanes.iterator();

        navButtons.add(myShipButton);

        clientModel.getPlayerClientData()
                .forEach((nickname, playerClientData) -> {
                    // my ship board is loaded by default
                    if (nickname.equals(clientModel.getMyNickname()))
                        return;

                    StackPane playerStackPane = stackPaneIterator.next();
                    otherPlayersShipBoards.put(nickname, playerStackPane);

                    ObjectProperty<Component>[][] observableMatrix = modelFxAdapter.getObservableShipBoardOf(nickname);
                    for (int row = 4; row < 8; row++) {
                        for (int column = 3; column < 9; column++) {
                            int finalRow = row;
                            int finalColumn = column;
                            observableMatrix[row][column].addListener((_, _, newVal) -> Platform.runLater(() -> {
                                try {
                                    if (newVal != null) {
                                        String componentFile = newVal.toString().split("\\n")[0];
                                        Image image = new Image(Objects.requireNonNull(getClass()
                                                .getResourceAsStream("/gui/graphics/component/" + componentFile)));
                                        Objects.requireNonNull(getNodeFromGridPane(((GridPane) playerStackPane.getChildren().get(1)), finalRow - 4, finalColumn - 3)).setImage(image);
                                    } else {
                                        Objects.requireNonNull(getNodeFromGridPane(((GridPane) playerStackPane.getChildren().get(1)), finalRow - 4, finalColumn - 3)).setImage(null);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error updating component appearance: " + e.getMessage());
                                }
                            }));
                        }
                    }

                    // Navigation button setup
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

                });


    }

    // Returns the ImageView at the given row and column in the GridPane, or null if not found.
    private ImageView getNodeFromGridPane(GridPane gridPane, int row, int column) {
        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            // Default values are 0 if null
            if ((rowIndex == null ? 0 : rowIndex) == row && (colIndex == null ? 0 : colIndex) == column) {
                return (ImageView) node;
            }
        }
        return null; // Nessun nodo trovato in quella posizione
    }

    private void initializeButtonMap() {

        // Riga 4 del modello -> Riga 0 della griglia UI
        buttonMap.put("4_3", button04_03);   buttonMap.put("4_4", button04_04);   buttonMap.put("4_5", button04_05);
        buttonMap.put("4_6", button04_06);   buttonMap.put("4_7", button04_07);   buttonMap.put("4_8", button04_08);   buttonMap.put("4_9", button04_09);

        // Riga 5 del modello -> Riga 1 della griglia UI
        buttonMap.put("5_3", button05_03);   buttonMap.put("5_4", button05_04);   buttonMap.put("5_5", button05_05);
        buttonMap.put("5_6", button05_06);   buttonMap.put("5_7", button05_07);   buttonMap.put("5_8", button05_08);   buttonMap.put("5_9", button05_09);

        // Riga 6 del modello -> Riga 2 della griglia UI (CENTRO!)
        buttonMap.put("6_3", button06_03);   buttonMap.put("6_4", button06_04);   buttonMap.put("6_5", button06_05);
        buttonMap.put("6_6", button06_06);   buttonMap.put("6_7", button06_07);   buttonMap.put("6_8", button06_08);   buttonMap.put("6_9", button06_09);

        // Riga 7 del modello -> Riga 3 della griglia UI
        buttonMap.put("7_3", button07_03);   buttonMap.put("7_4", button07_04);   buttonMap.put("7_5", button07_05);
        buttonMap.put("7_6", button07_06);   buttonMap.put("7_7", button07_07);   buttonMap.put("7_8", button07_08);   buttonMap.put("7_9", button07_09);

        // Riga 8 del modello -> Riga 4 della griglia UI
        buttonMap.put("8_3", button08_03);   buttonMap.put("8_4", button08_04);   buttonMap.put("8_5", button08_05);
        buttonMap.put("8_6", button08_06);   buttonMap.put("8_7", button08_07);   buttonMap.put("8_8", button08_08);   buttonMap.put("8_9", button08_09);

    }

    private void setupBookedComponentsBindings() {

        modelFxAdapter.getObservableReservedComponent1()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() -> {
                            if(newVal != null) {
                                String bookedComponentFile = newVal.toString().split("\\n")[0];
                                Image image = new Image(Objects.requireNonNull(getClass()
                                        .getResourceAsStream("/gui/graphics/component/" + bookedComponentFile)));
                                ImageView imageview = new ImageView(image);
                                imageview.setFitWidth(FIXED_BOOKED_COMPONENT_HEIGHT);
                                imageview.setFitHeight(FIXED_BOOKED_COMPONENT_HEIGHT);
                                button04_08.setAlignment(Pos.CENTER_RIGHT);
                                button04_08.setGraphic(imageview);
                            } else {
                                button04_08.setGraphic(null);
                                button04_08.setStyle("-fx-background-color: transparent;");
                            }
                        })
                );

        modelFxAdapter.getObservableReservedComponent2()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() -> {
                            if(newVal != null) {
                                String bookedComponentFile = newVal.toString().split("\\n")[0];
                                Image image = new Image(Objects.requireNonNull(getClass()
                                        .getResourceAsStream("/gui/graphics/component/" + bookedComponentFile)));
                                ImageView imageview = new ImageView(image);
                                imageview.setFitWidth(FIXED_BOOKED_COMPONENT_HEIGHT);
                                imageview.setFitHeight(FIXED_BOOKED_COMPONENT_HEIGHT);
                                button04_09.setAlignment(Pos.CENTER_RIGHT);
                                button04_09.setGraphic(imageview);
                            } else {
                                button04_09.setGraphic(null);
                                button04_09.setStyle("-fx-background-color: transparent;");
                            }
                        })
                );
    }

    private void setupGridBindings(ObjectProperty<Component>[][] observableMatrix) {

        for (Map.Entry<String, Button> entry : buttonMap.entrySet()) {
            String[] coords = entry.getKey().split("_");
            int modelRow = Integer.parseInt(coords[0]);
            int modelCol = Integer.parseInt(coords[1]);
            Button button = entry.getValue();
            ObjectProperty<Component> cellProperty = observableMatrix[modelRow][modelCol];

            cellProperty.addListener((_, _, newVal) ->
                    Platform.runLater(() -> updateButtonAppearance(button, newVal)));

            Component initialComponent = cellProperty.get();
            if (initialComponent != null) {
                Platform.runLater(() -> updateButtonAppearance(button, initialComponent));
            }
        }
    }

    private void updateButtonAppearance(Button button, Component component) {
        if (component != null) {
            try {
                String componentName = component.getImageName();
                Image img = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/gui/graphics/component/" + componentName)));
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(FIXED_COMPONENT_LENGTH);
                imgView.setFitHeight(FIXED_COMPONENT_LENGTH);
                imgView.setPreserveRatio(true);
                imgView.setRotate(component.getRotation() * 90);
                button.setGraphic(imgView);
                System.out.println("Updated button appearance with component: " + componentName + "\nRotation: " + component.getRotation());
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

    public void handleShowFlyingBoardButton() {
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

    @Override
    public void applyHighlightEffect(Coordinates coordinates, Color color) {

        String buttonId = coordinates.getX() + "_" + coordinates.getY();
        Button button = buttonMap.get(buttonId);

        Platform.runLater(() -> {
            DropShadow shadow = new DropShadow();
            shadowedButtons.add(button);
            shadow.setColor(color);
            shadow.setRadius(10);
            shadow.setSpread(0.5);
            button.setEffect(shadow);

            if (!button.getStyleClass().contains("no-hover")) {
                button.getStyleClass().add("no-hover");
            }
        });
    }

    @Override
    public void removeHighlightColor() {

        shadowedButtons.forEach(button ->
                Platform.runLater(() -> {
                    shadowedButtons.remove(button);
                    button.setEffect(null);
                    button.getStyleClass().remove("no-hover");
                })
        );

    }

}
