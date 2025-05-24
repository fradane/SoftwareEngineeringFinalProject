package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.component.Component;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private HBox componentBox;

    @FXML private Button button04_03, button04_04, button04_05, button04_06, button04_07, button04_08, button04_09;
    @FXML private Button button05_03, button05_04, button05_05, button05_06, button05_07, button05_08, button05_09;
    @FXML private Button button06_03, button06_04, button06_05, button06_06, button06_07, button06_08, button06_09;
    @FXML private Button button07_03, button07_04, button07_05, button07_06, button07_07, button07_08, button07_09;
    @FXML private Button button08_03, button08_04, button08_05, button08_06, button08_07, button08_08, button08_09;

    @FXML private Button pickRandomComponentButton;
    @FXML private Button visibleComponentButton;
    @FXML public ComboBox<Integer> littleDeckComboBox;

    private final int FIXED_COMPONENT_LENGTH = 70;
    private int focusComponentRotation = 0;
    private ModelFxAdapter modelFxAdapter;

    private final Map<String, Button> buttonMap = new HashMap<>();

    public void initialize() {
        borderPane.setVisible(true);
        stackPane.setVisible(true);
        grid.setVisible(true);
        modelFxAdapter = new ModelFxAdapter(clientModel);

        // initialize buttonMap
        initializeButtonMap();

        ShipBoardClient myShipBoard = clientModel.getPlayerClientData().get(clientModel.getMyNickname()).getShipBoard();
        ObjectProperty<Component>[][] observableMatrix = modelFxAdapter.getObservableMatrix();

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
                        componentBox.setVisible(false);
                        componentBox.setManaged(false);
                    }
                }));

        // Setup bindings SOLO per le coordinate che hanno pulsanti corrispondenti
        setupGridBindings(observableMatrix);

        setUpBookedComponentsBindings(myShipBoard);

        setUpTimerBinding();

        setUpVisibleComponentsBinding();

        // Refresh finale
        modelFxAdapter.refreshShipBoard();
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
    private void setUpVisibleComponentsBinding() {
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

    private void setUpBookedComponentsBindings(ShipBoardClient myShipBoard) {

        modelFxAdapter.getObservableReservedComponent1()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() -> {
                            if(newVal != null) {
                                String bookedComponentFile = newVal.toString().split("\\n")[0];
                                Image image = new Image(Objects.requireNonNull(getClass()
                                        .getResourceAsStream("/gui/graphics/component/" + bookedComponentFile)));
                                ImageView imageview = new ImageView(image);
                                imageview.setFitWidth(FIXED_COMPONENT_LENGTH);
                                imageview.setFitHeight(FIXED_COMPONENT_LENGTH);
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
                                imageview.setFitWidth(FIXED_COMPONENT_LENGTH);
                                imageview.setFitHeight(FIXED_COMPONENT_LENGTH);
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

    private void setUpTimerBinding() {
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

        if ((row == 4 && column == 8) || (row == 4 && column == 9)) {
            clientController.reserveFocusedComponent();
            return;
        }

        System.out.println("Button clicked: " + id + " -> coordinates [" + row + "][" + column + "]\nRotation: " + clientModel.getMyShipboard().getFocusedComponent().getRotation());

        clientController.placeFocusedComponent(row, column);
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
            componentBox.setVisible(true);
            componentBox.setManaged(true);
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

    public void handleCloseVisiblePanelButton(ActionEvent actionEvent) {
    }
}