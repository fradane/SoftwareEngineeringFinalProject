package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class ShipBoardViewController extends GuiController {

    @FXML
    public Button goBackButton;
    @FXML
    public VBox componentControlsPanel;
    @FXML
    public Button rotateComponentButton;
    @FXML
    public HBox shipNavigationBar;
    @FXML
    public Button myShipButton;
    @FXML
    public Button releaseComponentButton;
    @FXML
    public Label timerLabel;
    @FXML
    public Button flipHourglassButton;
    @FXML
    public Label flipsLeftLabel;
    @FXML
    public ScrollPane componentsScrollPane;
    @FXML
    public VBox visibleComponentsPanel;
    @FXML
    public VBox componentsContainer;
    @FXML
    public StackPane player2ShipBoard, player3ShipBoard,  player4ShipBoard;
    @FXML
    public Label messageLabel;
    @FXML
    public StackPane mainPlayerShipBoard;
    @FXML
    public Pane pawnsPane;
    @FXML
    public StackPane flyingBoard;
    @FXML
    public Button showFlyingBoardButton;
    @FXML
    public Button endPhaseButton;
    @FXML
    public VBox hourglassBox;
    @FXML
    public VBox componentsBoxV;
    @FXML
    public HBox bottomBox;
    @FXML
    private ImageView visibleCard1 = new ImageView();
    @FXML
    private ImageView visibleCard2 = new ImageView();
    @FXML
    private ImageView visibleCard3 = new ImageView();
    @FXML
    private ImageView focusComponentImage = new ImageView();
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
    private HBox componentsBoxH;

    @FXML public ImageView redPawn, greenPawn, bluePawn, yellowPawn;

    @FXML private Button button04_03, button04_04, button04_05, button04_06, button04_07, button04_08, button04_09;
    @FXML private Button button05_03, button05_04, button05_05, button05_06, button05_07, button05_08, button05_09;
    @FXML private Button button06_03, button06_04, button06_05, button06_06, button06_07, button06_08, button06_09;
    @FXML private Button button07_03, button07_04, button07_05, button07_06, button07_07, button07_08, button07_09;
    @FXML private Button button08_03, button08_04, button08_05, button08_06, button08_07, button08_08, button08_09;

    @FXML private Button pickRandomComponentButton;
    @FXML private Button visibleComponentButton;
    @FXML public ComboBox<Integer> littleDeckComboBox;

    private final int FIXED_COMPONENT_LENGTH = 70;
    private final int FIXED_BOOKED_COMPONENT_HEIGHT = 55;

    private int focusComponentRotation = 0;
    private ModelFxAdapter modelFxAdapter;
    private final Map<String, StackPane> otherPlayersShipBoards = new HashMap<>();
    private final Set<Button> navButtons = new HashSet<>();
    private List<Set<Coordinates>> shipParts = new ArrayList<>();

    private final Map<String, Button> buttonMap = new HashMap<>();
    private final Map<Button, DropShadow> shadowedButtons = new ConcurrentHashMap<>();

    private BiConsumer<Integer, Integer> correctShipBoardAction;

    private static final Map<Integer, Point2D> boardPositions = Map.ofEntries(
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

    public void initialize() {
        borderPane.setVisible(true);
        grid.setVisible(true);
        modelFxAdapter = new ModelFxAdapter(clientModel);

        // initialize buttonMap
        initializeButtonMap();

        ObjectProperty<Component>[][] observableMatrix = modelFxAdapter.getMineObservableMatrix();

        // set listener for the grid matrix
        modelFxAdapter.getObservableFocusedComponent()
                .addListener((_, _, newVal) -> Platform.runLater(() -> {
                    if(newVal != null) {
                        String focusedComponentFile = newVal.toString().split("\\n")[0];
                        Image image = new Image(Objects.requireNonNull(getClass()
                                .getResourceAsStream("/gui/graphics/component/" + focusedComponentFile)));
                        focusComponentImage.rotateProperty().set(360 - focusComponentRotation);
                        focusComponentRotation = 0;
                        focusComponentImage.setImage(image);
                        System.out.println("Focused component rot: " + newVal.getRotation());
                        showFocusComponent();
                    } else {
                        focusComponentImage.setImage(null);
                        componentsBoxH.setVisible(false);
                        componentsBoxH.setManaged(false);
                    }
                }));

        // setting up dynamic elements of the scene
        setupGridBindings(observableMatrix);
        setupBookedComponentsBindings();
        setupTimerBinding();
        setupVisibleComponentsBinding();
        setupShipBoardNavigationBar();
        setupFlyingBoardBinding();

        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
        clientModel.getPlayerClientData().keySet().forEach(nickname -> modelFxAdapter.refreshShipBoardOf(nickname));
    }

    private void setupFlyingBoardBinding() {

        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    modelFxAdapter.getObservableColorRanking().put(playerColor, new SimpleObjectProperty<>(position));
                });

        modelFxAdapter.getObservableColorRanking()
            .forEach((color, position) -> {
                position.addListener((_, _, newVal) -> Platform.runLater(() -> {
                    double x = boardPositions.get(newVal).getX();
                    double y = boardPositions.get(newVal).getY();

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

    /**
     * Configures a binding to dynamically display and update visible components in the user interface.
     * This method listens for changes in the list of visible components and updates the graphical
     * representation accordingly. It associates each component with a button that allows interaction.
     *<p>
     * - When changes occur in the observable list of visible components, the method executes a UI update
     *   inside a JavaFX application thread.
     * - It clears the current component display, creates graphical elements (such as images and buttons)
     *   for the updated components, and appends them to the container.
     * - Each button is set up to trigger an action that interacts with the current focused component
     *   or assigns the selected component to the client controller for further processing.
     *<p>
     * Note:
     * - The method relies on `modelFxAdapter.getObservableVisibleComponents()` for the observable list of
     *   visible components.
     * - It leverages JavaFX's `Platform.runLater()` to ensure UI updates are performed on the JavaFX
     *   application thread.
     * - Graphics representing components are loaded as images with a fixed size from a specific resource path.
     */
    private void setupVisibleComponentsBinding() {
        modelFxAdapter.getObservableVisibleComponents()
            .addListener((InvalidationListener) _ -> Platform.runLater(() -> {
                componentsContainer.getChildren().clear();

                for (String string : modelFxAdapter.getObservableVisibleComponents()) {
                    // image set up
                    ImageView imageView = new ImageView(new Image(
                            Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/component/" + string))));
                    imageView.setFitWidth(FIXED_COMPONENT_LENGTH);
                    imageView.setFitHeight(FIXED_COMPONENT_LENGTH);

                    // button set up
                    Button button = new Button();
                    button.setGraphic(imageView);
                    button.getStyleClass().add("cell-button");
                    componentsContainer.getChildren().add(button);

                    // set up the button action
                    button.setOnAction(_ -> {
                        if (clientModel.getMyShipboard().getFocusedComponent() == null) {
                            clientModel.getVisibleComponents().forEach((index, component) -> {
                                if (component.toString().split("\\n")[0].equals(string))
                                    clientController.pickVisibleComponent(index);
                            });
                        }
                    });
                }

                componentsScrollPane.setContent(componentsContainer);
            }));
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

            cellProperty.addListener((_, _, newVal) -> Platform.runLater(() -> updateButtonAppearance(button, newVal)));

            Component initialComponent = cellProperty.get();
            if (initialComponent != null) {
                updateButtonAppearance(button, initialComponent);
            }
        }
    }

    private void setupTimerBinding() {
        modelFxAdapter.getObservableTimer()
                .addListener((_, _, newVal) -> Platform.runLater(() -> {
                    if (newVal != null) {
                        timerLabel.setText(":" + newVal);
                    } else {
                        timerLabel.setText("ERROR");
                    }
                }));

        modelFxAdapter.getObservableFlipsLeft()
                .addListener((_, _, newVal) -> Platform.runLater(() -> {
                    if (newVal != null)
                        flipsLeftLabel.setText( newVal + " flips left.");
                }));
    }

    private void updateButtonAppearance(Button button, Component component) {
        if (component != null) {
            try {
                String componentName = component.toString().split("\\n")[0];
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

    public void handleGridButtonClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String id = clickedButton.getId();

        // Parsing corretto dell'ID del pulsante
        String[] parts = id.replace("button", "").split("_");
        int row = Integer.parseInt(parts[0]);
        int column = Integer.parseInt(parts[1]);

        // TODO DEBUG: da togliere
        //System.out.println("Button clicked: " + id + " -> coordinates [" + row + "][" + column + "]\nRotation: " + clientModel.getMyShipboard().getFocusedComponent().getRotation());

        if (clientModel.getGameState() == GameState.BUILD_SHIPBOARD)
            handleGridButtonBuilding(row, column);
        else if (clientModel.getGameState() == GameState.CHECK_SHIPBOARD)
            correctShipBoardAction.accept(row, column);
    }

    private void handleGridButtonBuilding(int row, int column) {

        ShipBoardClient shipboard = clientModel.getMyShipboard();

        if (row == 4 && (column == 8 || column == 9)) {

            // TODO
            if (shipboard.getBookedComponents().size() == 2 && shipboard.getFocusedComponent() != null) {
                showMessage("You already have two components booked");
                return;
            }

            //se non ho nessun componente in focus
            if (shipboard.getFocusedComponent() == null) {
                //se nella lista dei componenti riservati c'è qualcosa, il componente torna in focus e viene tolto dai componenti riservati
                if (column == 8 && shipboard.getBookedComponents().get(0) != null) {
                    clientController.pickReservedComponent(1);
                } else if (column == 9 && shipboard.getBookedComponents().get(1) != null) {
                    clientController.pickReservedComponent(2);
                }
            } else
                clientController.reserveFocusedComponent();

            return;
        }

        if (shipboard.getFocusedComponent() != null)
            clientController.placeFocusedComponent(row, column);
    }

    private void handleInvalidComponent(int row, int column) {

        if (row == 4 && (column == 8 || column == 9)) {
            showMessage("Remove one of the wrongly placed components");
            return;
        }

        if (clientModel.getMyShipboard().getIncorrectlyPositionedComponentsCoordinates().contains(new Coordinates(row, column))) {
            shadowedButtons.keySet().forEach(this::removeHighlightColor);
            clientController.removeComponent(row, column);
        } else
            showMessage("Remove one of the wrongly placed components");
    }

    private void handleShipParts(int row, int column) {
        IntStream.range(0, shipParts.size() - 1)
                        .filter(i -> shipParts.get(i).contains(new Coordinates(row, column)))
                        .findFirst()
                        .ifPresentOrElse(
                                i -> {
                                    shadowedButtons.keySet().forEach(ShipBoardViewController.this::removeHighlightColor);
                                    clientController.handleShipPartSelection(i + 1);
                                    },
                                () -> showMessage("Invalid selection, try again")
                        );
    }

    public void handleLittleDeck() {
        int index = littleDeckComboBox.getValue();
        List<String> imagesName = clientModel.getLittleVisibleDecks()
                .get(index - 1)
                .stream()
                .map(card -> card.split("\\n")[0])
                .toList();

        Platform.runLater(() -> {
            try {
                visibleCard1.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.getFirst()))));
                visibleCard2.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(1)))));
                visibleCard3.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(2)))));
                grid.setManaged(false);
                grid.setOpacity(0.1);
                littleDeckFlowPane.setVisible(true);
                littleDeckFlowPane.setManaged(true);
            } catch (Exception e){
                System.err.println("Error setting visible cards: " + e.getMessage());
            }
        });
    }

    public void showFocusComponent(){
        Platform.runLater(() -> {
            componentsBoxH.setVisible(true);
            componentsBoxH.setManaged(true);
            componentControlsPanel.setVisible(true);
            componentControlsPanel.setManaged(true);
        });
    }

    public void handlePickRandomComponentButton() {
        if (clientModel.getMyShipboard().getFocusedComponent() == null)
            clientController.pickRandomComponent();
    }

    public void handleReleaseComponentButton(){
        clientController.releaseFocusedComponent();
    }

    public void handleGoBackButton() {
        Platform.runLater(() -> {
            grid.setOpacity(1);
            grid.setManaged(true);
            littleDeckFlowPane.setVisible(false);
            littleDeckFlowPane.setManaged(false);
        });
    }

    public void handleRotateComponentButton() {
        clientModel.getMyShipboard().getFocusedComponent().rotate();
        ImageView imgView = focusComponentImage;
        Image oldImage = imgView.getImage();
        if (oldImage != null) {
            Platform.runLater(() -> {
                RotateTransition rotateTransition = new RotateTransition(Duration.millis(200), imgView);
                rotateTransition.setByAngle(90);
                rotateTransition.play();
            });
        }
    }

    public void handleFlipHourglassButton() {
        clientController.restartHourglass();
    }

    public ModelFxAdapter getModelFxAdapter() {
        return modelFxAdapter;
    }

    public void handleVisibleComponentButton() {
        if(clientModel.getMyShipboard().getFocusedComponent() == null) {
            Platform.runLater(() -> {
                visibleComponentsPanel.setVisible(true);
                visibleComponentsPanel.setManaged(true);
            });
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

    private void enableMyShipBoardView(boolean enable) {
        Platform.runLater(() -> {
            if (enable) {
                grid.setManaged(true);
                grid.setOpacity(1);
            } else {
                grid.setManaged(false);
                grid.setOpacity(0.1);
            }
        });
    }

    public void handleEndPhaseButton() {
        clientController.endBuildShipBoardPhase();

        Platform.runLater(() -> {
            hourglassBox.setVisible(false);
            visibleComponentsPanel.setVisible(false);
            componentsBoxV.setVisible(false);
            componentsBoxH.setVisible(false);
            bottomBox.setVisible(false);
        });
    }

    public void showMessage(String message) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            messageLabel.setOpacity(0.0);

            // Fade in
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.5), messageLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // Pause before fading out
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.0));

            // Fade out
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.5), messageLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(_ -> messageLabel.setText(""));

            // Play sequence
            javafx.animation.SequentialTransition sequence = new javafx.animation.SequentialTransition(fadeIn, pause, fadeOut);
            sequence.play();
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

    public void showInvalidComponents() {

        ShipBoardClient shipBoard = clientModel.getMyShipboard();

        correctShipBoardAction = this::handleInvalidComponent;

        shipBoard.getIncorrectlyPositionedComponentsCoordinates()
                .forEach(coordinate -> {
                    String buttonId = coordinate.getX() + "_" + coordinate.getY();
                    Button button = buttonMap.get(buttonId);
                    applyHighlightEffect(button, Color.RED);
                });

    }

    private void applyHighlightEffect(Button button, Color color) {
        Platform.runLater(() -> {
            DropShadow shadow = new DropShadow();
            shadowedButtons.put(button, shadow);
            shadow.setColor(color);
            shadow.setRadius(10);
            shadow.setSpread(0.5);
            button.setEffect(shadow);

            if (!button.getStyleClass().contains("no-hover")) {
                button.getStyleClass().add("no-hover");
            }
        });
    }

    private void removeHighlightColor(Button button) {
        Platform.runLater(() -> {
            shadowedButtons.remove(button);
            button.setEffect(null);
            button.getStyleClass().remove("no-hover");
        });
    }

    public void showShipParts(List<Set<Coordinates>> shipParts) {

        List<Color> colors = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
        Map<Color, Set<Coordinates>> shipPartsMap = new HashMap<>();
        this.shipParts = shipParts;

        for (int i = 0; i < shipParts.size(); i++) {
            shipPartsMap.put(colors.get(i), shipParts.get(i));
        }

        correctShipBoardAction = this::handleShipParts;

        shipPartsMap.forEach((color, coordinates) -> {
            coordinates.forEach(coordinate -> {
                String buttonId = coordinate.getX() + "_" + coordinate.getY();
                Button button = buttonMap.get(buttonId);
                applyHighlightEffect(button, color);
            });
        });

    }
}