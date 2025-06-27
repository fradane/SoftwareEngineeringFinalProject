package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.component.Component;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Level2BoardsController extends BoardsController {

    @FXML public StackPane boardsPane;
    @FXML private Button button04_03, button04_04, button04_05, button04_07, button04_08, button04_09;
    @FXML private Button button05_04, button05_05, button05_06, button05_07, button05_08;
    @FXML private Button button06_03, button06_04, button06_05, button06_06, button06_07, button06_08, button06_09;
    @FXML private Button button07_03, button07_04, button07_05, button07_06, button07_07, button07_08, button07_09;
    @FXML private Button button08_03, button08_04, button08_05, button08_07, button08_08, button08_09;

    @FXML private GridPane player2Grid, player3Grid, player4Grid;

    private final int FIXED_BOOKED_COMPONENT_HEIGHT = 55;

    private static final Map<Integer, Pair<Integer, Integer>> flyingBoardRelativePositions = Map.ofEntries(
            Map.entry(Integer.MIN_VALUE, new Pair<>(8, 6)),
            Map.entry(0, new Pair<>(10, 12)),
            Map.entry(1, new Pair<>(8, 16)),
            Map.entry(2, new Pair<>(7, 21)),
            Map.entry(3, new Pair<>(6, 25)),
            Map.entry(4, new Pair<>(6, 30)),
            Map.entry(5, new Pair<>(7, 34)),
            Map.entry(6, new Pair<>(8, 39)),
            Map.entry(7, new Pair<>(10, 43)),
            Map.entry(8, new Pair<>(12, 47)),
            Map.entry(9, new Pair<>(16, 50)),
            Map.entry(10, new Pair<>(21, 49)),
            Map.entry(11, new Pair<>(25, 46)),
            Map.entry(12, new Pair<>(28, 42)),
            Map.entry(13, new Pair<>(30, 38)),
            Map.entry(14, new Pair<>(31, 34)),
            Map.entry(15, new Pair<>(31, 29)),
            Map.entry(16, new Pair<>(32, 25)),
            Map.entry(17, new Pair<>(32, 20)),
            Map.entry(18, new Pair<>(31, 16)),
            Map.entry(19, new Pair<>(29, 11)),
            Map.entry(20, new Pair<>(26, 7)),
            Map.entry(21, new Pair<>(21, 4)),
            Map.entry(22, new Pair<>(16, 5)),
            Map.entry(23, new Pair<>(12, 8))
    );

    /**
     * Gets the relative positions for the Level 2 flying board.
     *
     * @return a map of position indices to coordinate pairs for Level 2
     */
    @Override
    protected Map<Integer, Pair<Integer, Integer>> getFlyingBoardRelativePositions() {
        return flyingBoardRelativePositions;
    }

    /**
     * Binds the Level 2 boards controller to the model and event handlers.
     *
     * @param modelFxAdapter the adapter for reactive UI updates
     * @param boardsEventHandler the handler for board interaction events
     * @param clientModel the client-side game model
     */
    @Override
    public void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel) {
        this.modelFxAdapter = modelFxAdapter;
        this.boardsEventHandler = boardsEventHandler;
        this.clientModel = clientModel;
        initializeButtonMap();
        setupFlyingBoardBinding();
        setupShipBoardNavigationBarAndBoards();
        setupMyBookedComponentsBinding(boardsEventHandler);
        setupGridBindings(modelFxAdapter.getMyObservableMatrix());
        //setupChangedAttributesBinding();

        clientModel.getPlayerClientData().forEach((nickname, _) -> clientModel.refreshShipBoardOf(nickname));
    }

    /**
     * Initializes the Level 2 boards controller, called automatically by JavaFX.
     */
    public void initialize() {

    }

    /**
     * Initializes the mapping between coordinates and UI buttons for Level 2.
     * Maps Level 2 ship board positions to their corresponding FXML button references.
     */
    @Override
    protected void initializeButtonMap() {

        // Model row 4 -> UI grid row 0
        buttonMap.put("4_3", button04_03);   buttonMap.put("4_4", button04_04);   buttonMap.put("4_5", button04_05);
        buttonMap.put("4_7", button04_07);   buttonMap.put("4_8", button04_08);   buttonMap.put("4_9", button04_09);

        // Model row 5 -> UI grid row 1
        buttonMap.put("5_4", button05_04);   buttonMap.put("5_5", button05_05);
        buttonMap.put("5_6", button05_06);   buttonMap.put("5_7", button05_07);   buttonMap.put("5_8", button05_08);

        // Model row 6 -> UI grid row 2 (CENTER!)
        buttonMap.put("6_3", button06_03);   buttonMap.put("6_4", button06_04);   buttonMap.put("6_5", button06_05);
        buttonMap.put("6_6", button06_06);   buttonMap.put("6_7", button06_07);   buttonMap.put("6_8", button06_08);   buttonMap.put("6_9", button06_09);

        // Model row 7 -> UI grid row 3
        buttonMap.put("7_3", button07_03);   buttonMap.put("7_4", button07_04);   buttonMap.put("7_5", button07_05);
        buttonMap.put("7_6", button07_06);   buttonMap.put("7_7", button07_07);   buttonMap.put("7_8", button07_08);   buttonMap.put("7_9", button07_09);

        // Model row 8 -> UI grid row 4
        buttonMap.put("8_3", button08_03);   buttonMap.put("8_4", button08_04);   buttonMap.put("8_5", button08_05);
        buttonMap.put("8_7", button08_07);   buttonMap.put("8_8", button08_08);   buttonMap.put("8_9", button08_09);

    }

    private void updateOthersBookedComponents(ImageView imageView, Component newVal) {
        if (newVal != null) {
            String componentFile = newVal.toString().split("\\n")[0];
            Image image = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/component/" + componentFile)));
            imageView.setFitWidth(FIXED_BOOKED_COMPONENT_HEIGHT);
            imageView.setFitHeight(FIXED_BOOKED_COMPONENT_HEIGHT);
            imageView.setRotate(newVal.getRotation() * 90);
            imageView.setImage(image);
            if (imageView.getParent() != null) {
                GridPane.setMargin(imageView.getParent(), new Insets(0, 0, 0, 10));
            }
        } else {
            imageView.setImage(null);
            imageView.setRotate(0);
        }
    }

    private void updateMyBookedComponents(Button button, Component newVal) {

        if(newVal != null) {
            String bookedComponentFile = newVal.toString().split("\\n")[0];
            Image image = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/component/" + bookedComponentFile)));
            ImageView imageview = new ImageView(image);
            imageview.setFitWidth(FIXED_BOOKED_COMPONENT_HEIGHT);
            imageview.setFitHeight(FIXED_BOOKED_COMPONENT_HEIGHT);
            button.setAlignment(Pos.CENTER_RIGHT);
            button.setGraphic(imageview);
        } else {
            button.setGraphic(null);
            button.setStyle("-fx-background-color: transparent;");
        }

    }

    private void setupMyBookedComponentsBinding(BoardsEventHandler boardsEventHandler) {

        if (boardsEventHandler instanceof BuildAndCheckShipBoardController) {
            Pair<ObjectProperty<Component>, ObjectProperty<Component>> reservedComponents = modelFxAdapter.getObservableBookedComponentsOf(clientModel.getMyNickname());

            reservedComponents.getKey()
                    .addListener((_, _, newVal) ->
                            Platform.runLater(() -> updateMyBookedComponents(button04_08, newVal))
                    );

            reservedComponents.getValue()
                    .addListener((_, _, newVal) ->
                            Platform.runLater(() -> updateMyBookedComponents(button04_09, newVal))
                    );
        } else if (boardsEventHandler instanceof CardPhaseController) {
            modelFxAdapter.getObservableLostComponents()
                    .get(clientModel.getMyNickname()).addListener(
                            (_, _, newVal) -> updateMyNotActiveComponents(newVal, button04_09)
                    );
        } else {
            System.err.println("Boards event handler is " + boardsEventHandler.getClass().getSimpleName());
        }

    }

    /**
     * Sets up binding for other players' booked components display.
     *
     * @param nickname the nickname of the player
     * @param imageViewImageViewPair the pair of image views for displaying booked components
     */
    protected void setupOthersBookedComponentBinding(String nickname, Pair<ImageView, ImageView> imageViewImageViewPair) {
        // reserved components binding
        Pair<ObjectProperty<Component>, ObjectProperty<Component>> reservedComponents = modelFxAdapter.getObservableBookedComponentsOf(nickname);
        reservedComponents.getKey()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() ->
                                updateOthersBookedComponents(imageViewImageViewPair.getKey(), newVal)));

        reservedComponents.getValue()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() ->
                                updateOthersBookedComponents(imageViewImageViewPair.getValue(), newVal)));
    }
}
