package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.LifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.ERROR;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.STANDARD;

public class BuildAndCheckShipBoardController extends GuiController implements BoardsEventHandler {

    @FXML
    public Button goBackButton;
    @FXML
    public VBox componentControlsPanel;
    @FXML
    public Button rotateComponentButton;
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
    public Label messageLabel;
    @FXML
    public Button endPhaseButton;
    @FXML
    public VBox componentsBoxV;
    @FXML
    public HBox bottomBox;
    @FXML
    public StackPane centerStackPane;
    @FXML
    public Button placePawnButton;
    @FXML
    public StackPane pawnButtonPane;
    @FXML
    public Button confirmCrewMemberButton;
    @FXML
    public Button pickRandomComponentButton;
    @FXML
    public Button visibleComponentButton;
    @FXML
    public Button prefabShipBoardButton;
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
    private HBox componentsBoxH;
    @FXML
    public ComboBox<Integer> littleDeckComboBox;
    @FXML
    public VBox prefabShipsMenu;
    @FXML
    public VBox prefabShipsContainer;
    @FXML
    public Button cancelPrefabSelectionButton;

    private int focusComponentRotation = 0;
    private ModelFxAdapter modelFxAdapter;
    private List<Set<Coordinates>> shipParts = new ArrayList<>();
    private BoardsController boardsController;
    private final int FIXED_COMPONENT_LENGTH = 70;
    private Optional<BiConsumer<Integer, Integer>> correctShipBoardAction = Optional.empty();
    private final Map<Coordinates, Set<ColorLifeSupport>> cabinsWithLifeSupport = new HashMap<>();
    private final Map<Coordinates, CrewMember> crewMemberChoice = new HashMap<>();
    private CrewMember currentCrewMemberChoice;


    @FXML
    private void handleExitGame() {
        try {
            clientController.leaveGame();

            Platform.runLater(() -> javafx.application.Platform.exit());

        } catch (Exception e) {
            showMessage("Error leaving the game: " + e.getMessage(), true);
        }
    }

    @Override
    public String getControllerType() {
        return "BuildAndCheckShipBoardController";
    }

    private void setupFocusedComponentBinding() {
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
    }

    private void setupVisibleComponentsBinding() {
        modelFxAdapter.getObservableVisibleComponents()
                .addListener((InvalidationListener) _ -> Platform.runLater(() -> {
                    componentsContainer.getChildren().clear();

                    componentsContainer.getStyleClass().clear();
                    componentsContainer.getStyleClass().add("components-container");

                    List<String> visibleComponents = new ArrayList<>(modelFxAdapter.getObservableVisibleComponents());

                    for (String string : visibleComponents) {
                        // image set up
                        ImageView imageView = new ImageView(new Image(
                                Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/component/" + string))));
                        imageView.setFitWidth(FIXED_COMPONENT_LENGTH);
                        imageView.setFitHeight(FIXED_COMPONENT_LENGTH);

                        imageView.getStyleClass().add("image-view");


                        // button setup
                        Button button = new Button();
                        button.setGraphic(imageView);

                        button.getStyleClass().clear();
                        button.getStyleClass().add("component-item");

                        // button.getStyleClass().add("cell-button");
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

    private void applyInitialStyling() {
        Platform.runLater(() -> {
            // CSS to the main components
            visibleComponentsPanel.getStyleClass().add("visible-components-panel");
            componentsScrollPane.getStyleClass().add("components-scroll-pane");
        });
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

    private void handleGridButtonBuilding(int row, int column) {

        ShipBoardClient shipboard = clientModel.getMyShipboard();

        if (row == 4 && (column == 8 || column == 9)) {

            // TODO
            if (shipboard.getBookedComponents().size() == 2 && shipboard.getFocusedComponent() != null) {
                showMessage("You already have two components booked", false);
                return;
            }

            //if there isn't a focus component
            if (shipboard.getFocusedComponent() == null) {
                //if in the reserved components list there is something, the component returns focus and it is removed from the reserved components list
                if (column == 8 && shipboard.getBookedComponents().get(0) != null) {
                    clientController.pickReservedComponent(1);
                } else if (column == 9 && shipboard.getBookedComponents().get(1) != null) {
                    clientController.pickReservedComponent(2);
                }
                showMessage("The booked component you picked can be placed or booked again, you can't release it", true);
            } else
                clientController.reserveFocusedComponent();

            return;
        }

        if (shipboard.getFocusedComponent() != null)
            clientController.placeFocusedComponent(row, column);
    }

    private void handleInvalidComponent(int row, int column) {

        if (row == 4 && (column == 8 || column == 9)) {
            showMessage("Remove one of the wrongly placed components", true);
            return;
        }

        if (clientModel.getMyShipboard().getIncorrectlyPositionedComponentsCoordinates().contains(new Coordinates(row, column))) {
            this.boardsController.removeHighlightColor();
            clientController.removeComponent(row, column);
        } else
            showMessage("Remove one of the wrongly placed components", true);
    }

    private void handleShipParts(int row, int column) {
        IntStream.range(0, shipParts.size())
                .filter(i -> shipParts.get(i).contains(new Coordinates(row, column)))
                .findFirst()
                .ifPresentOrElse(
                        i -> {
                            this.boardsController.removeHighlightColor();
                            clientController.handleShipPartSelection(i + 1);
                        },
                        () -> showMessage("Invalid selection, try again", false)
                );
    }

    public void handleLittleDeck() {

        long componentInShipBoard = clientModel.getMyShipboard().getNumberOfComponents();
        if (componentInShipBoard == 1) {
            showMessage("You cannot watch a little deck before having placed any component", false);
            return;
        }

        int index = littleDeckComboBox.getValue();
        List<String> imagesName = clientModel.getLittleVisibleDecks()
                .get(index - 1)
                .stream()
                .map(ClientCard::getImageName)
                .toList();

        Platform.runLater(() -> {
            try {
                goBackButton.setVisible(true);
                visibleCard1.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.getFirst()))));
                visibleCard2.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(1)))));
                visibleCard3.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagesName.get(2)))));
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
        if (clientModel.getMyShipboard().getFocusedComponent() == null)
            return;
        clientController.releaseFocusedComponent();
    }

    public void handleGoBackButton() {
        Platform.runLater(() -> {
            littleDeckFlowPane.setVisible(false);
            littleDeckFlowPane.setManaged(false);
            goBackButton.setVisible(false);
            littleDeckComboBox.setPromptText("Watch a little deck");
        });
    }

    public void handleRotateComponentButton() {

        Component focusedComponent = clientModel.getMyShipboard().getFocusedComponent();
        if (focusedComponent == null)
            return;

        focusedComponent.rotate();
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

    public void handleEndPhaseButton() {
        clientController.endBuildShipBoardPhase();

        Platform.runLater(this::disableBuildShipboardElements);
    }

    private void disableBuildShipboardElements() {
        visibleComponentsPanel.setVisible(false);
        componentsBoxV.setVisible(false);
        componentsBoxH.setVisible(false);
        pickRandomComponentButton.setVisible(false);
        pickRandomComponentButton.setManaged(false);
        endPhaseButton.setVisible(false);
        endPhaseButton.setManaged(false);
        littleDeckComboBox.setVisible(false);
        littleDeckComboBox.setManaged(false);
        timerLabel.setVisible(false);
        timerLabel.setManaged(false);
        flipHourglassButton.setVisible(false);
        flipHourglassButton.setManaged(false);
        flipsLeftLabel.setVisible(false);
        flipsLeftLabel.setManaged(false);
        prefabShipBoardButton.setVisible(false);
        prefabShipBoardButton.setManaged(false);
    }

    @Override
    public void showMessage(String message, boolean isPermanent) {

        if (isPermanent) {
            Platform.runLater(() -> showPermanentMessage(message, messageLabel));
            return;
        }

        // Stop any ongoing transitions before starting new fade
        messageLabel.getTransforms().clear();
        Platform.runLater(() -> showNonPermanentMessage(message, messageLabel));
    }

    public void showInvalidComponents() {

        showMessage("Time to correct the invalid shipboard", true);

        correctShipBoardAction = Optional.of(this::handleInvalidComponent);
        ShipBoardClient shipBoard = clientModel.getMyShipboard();
        shipBoard.getIncorrectlyPositionedComponentsCoordinates()
                .forEach(coordinate -> this.boardsController.applyHighlightEffect(coordinate, Color.RED));
    }

    public void showShipParts(List<Set<Coordinates>> shipParts) {

        List<Color> colors = List.of(Color.RED, Color.ORANGE, Color.BLUE, Color.YELLOW);
        Map<Color, Set<Coordinates>> shipPartsMap = new HashMap<>();
        this.shipParts = shipParts;

        for (int i = 0; i < shipParts.size(); i++) {
            shipPartsMap.put(colors.get(i), shipParts.get(i));
        }

        correctShipBoardAction = Optional.of(this::handleShipParts);

        shipPartsMap.forEach((color, coordinates) ->
                coordinates.forEach(coordinate ->
                        this.boardsController.applyHighlightEffect(coordinate, color)
                )
        );

    }

    @Override
    public void onGridButtonClick(int row, int column) {
        if (clientModel.getGameState() == GameState.BUILD_SHIPBOARD)
            handleGridButtonBuilding(row, column);
        else if (clientModel.getGameState() == GameState.CHECK_SHIPBOARD)
            correctShipBoardAction.ifPresentOrElse(
                    action -> {
                        action.accept(row, column);
                        //correctShipBoardAction = Optional.empty();
                    },
                    () -> showMessage("This action is not allowed in this phase", false));
        else if (clientModel.getGameState() == GameState.PLACE_CREW)
            handleCrewPlacement(row, column);
    }

    public void handlePlacePawnButton() {
        Platform.runLater(() -> {
            littleDeckFlowPane.setVisible(false);
            littleDeckFlowPane.setManaged(false);
            pawnButtonPane.setVisible(false);
            pawnButtonPane.setManaged(false);
            placePawnButton.setVisible(false);
            placePawnButton.setManaged(false);
        });
        clientController.placePawn();
    }

    public void showFirstToEnterButton() {
        Platform.runLater(() -> {
            this.disableBuildShipboardElements();
            pawnButtonPane.setVisible(true);
            pawnButtonPane.setManaged(true);
            placePawnButton.setVisible(true);
            placePawnButton.setManaged(true);
        });
    }

    public void showCrewPlacementMenu(boolean isPurpleSubmitted) {

        showMessage("Time to place your crew members!", true);

        Platform.runLater(() -> {
            confirmCrewMemberButton.setVisible(true);
            confirmCrewMemberButton.setManaged(true);
        });

        ShipBoardClient shipBoard = clientModel.getMyShipboard();
        this.cabinsWithLifeSupport.putAll(shipBoard.getCabinsWithLifeSupport());

        boolean hasPurple = cabinsWithLifeSupport.keySet()
                .stream()
                .map(cabinsWithLifeSupport::get)
                .anyMatch(colors -> colors.contains(ColorLifeSupport.PURPLE));

        boolean hasBrown = cabinsWithLifeSupport.keySet()
                .stream()
                .map(cabinsWithLifeSupport::get)
                .anyMatch(colors -> colors.contains(ColorLifeSupport.BROWN));

        if (hasPurple && !isPurpleSubmitted) {
            this.currentCrewMemberChoice = CrewMember.PURPLE_ALIEN;
            showMessage("Select the cabin you want to place the purple alien in then press CONFIRM...", true);
            cabinsWithLifeSupport.keySet()
                    .stream()
                    .filter(coords -> cabinsWithLifeSupport.get(coords).contains(ColorLifeSupport.PURPLE))
                    .filter(coords -> !crewMemberChoice.containsKey(coords))
                    .forEach(coords -> boardsController.applyHighlightEffect(coords, Color.PURPLE));
        } else if (hasBrown) {
            this.currentCrewMemberChoice = CrewMember.BROWN_ALIEN;
            showMessage("Select the cabin you want to place the brown alien in then press CONFIRM...", true);
            cabinsWithLifeSupport.keySet()
                    .stream()
                    .filter(coords -> cabinsWithLifeSupport.get(coords).contains(ColorLifeSupport.BROWN))
                    .filter(coords -> !crewMemberChoice.containsKey(coords))
                    .forEach(coords -> boardsController.applyHighlightEffect(coords, Color.BROWN));
        } else {
            this.currentCrewMemberChoice = CrewMember.BROWN_ALIEN;
            handleConfirmCrewMemberButton();
        }

    }

    public void showDisconnectMessage(String message) {
        showMessage(message, true);
        System.exit(0);
    }

    private void handleCrewPlacement(int row, int column) {

        if (!cabinsWithLifeSupport.containsKey(new Coordinates(row, column)))
            return;

        Coordinates selectedCoords = new Coordinates(row, column);

        if (this.currentCrewMemberChoice == CrewMember.PURPLE_ALIEN) {

            if (!cabinsWithLifeSupport.get(selectedCoords).contains(ColorLifeSupport.PURPLE)) {
                showMessage("The selected cabin cannot accept PURPLE", false);
                return;
            }

            // if the player has previously selected a purple alien and has changed its idea
            if (this.crewMemberChoice.containsValue(CrewMember.PURPLE_ALIEN)) {
                Coordinates removedCoords = crewMemberChoice.keySet()
                        .stream()
                        .filter(coords -> crewMemberChoice.get(coords) == CrewMember.PURPLE_ALIEN)
                        .findFirst()
                        .orElseThrow();
                this.boardsController.applyHighlightEffect(removedCoords, Color.PURPLE);
                this.crewMemberChoice.remove(removedCoords);
            }
            this.crewMemberChoice.put(selectedCoords, CrewMember.PURPLE_ALIEN);
            boardsController.applyHighlightEffect(selectedCoords, Color.GREEN);

        } else if (this.currentCrewMemberChoice == CrewMember.BROWN_ALIEN) {

            if (!cabinsWithLifeSupport.get(selectedCoords).contains(ColorLifeSupport.BROWN)) {
                showMessage("The selected cabin cannot accept BROWN", false);
                return;
            }

            // if the player has already selected this cabin for the purple alien
            if (this.crewMemberChoice.containsKey(selectedCoords)) {
                showMessage("This cabin was already selected for the PURPLE alien", false);
                return;
            }

            // if the player has previously selected a brown alien and has changed its idea
            if (this.crewMemberChoice.containsValue(CrewMember.BROWN_ALIEN)) {
                Coordinates removedCoords = crewMemberChoice.keySet()
                        .stream()
                        .filter(coords -> crewMemberChoice.get(coords) == CrewMember.BROWN_ALIEN)
                        .findFirst()
                        .orElseThrow();
                this.boardsController.applyHighlightEffect(removedCoords, Color.BROWN);
                this.crewMemberChoice.remove(removedCoords);
            }
            this.crewMemberChoice.put(selectedCoords, CrewMember.BROWN_ALIEN);
            boardsController.applyHighlightEffect(selectedCoords, Color.GREEN);

        }
    }

    public void handleConfirmCrewMemberButton() {
        if (this.currentCrewMemberChoice == CrewMember.PURPLE_ALIEN)
            showCrewPlacementMenu(true);
        else {
            Platform.runLater(() -> {
                boardsController.removeHighlightColor();
                confirmCrewMemberButton.setVisible(false);
                confirmCrewMemberButton.setManaged(false);
            });
            clientController.submitCrewChoices(crewMemberChoice);
        }
    }

    public void initialize() {
        // board setup
        try {
            if (clientController.getCurrentGameInfo().isTestFlight()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Level1Boards.fxml"));
                VBox mainBoardBox = loader.load();
                this.boardsController = loader.getController();

                Platform.runLater(() -> {
                    centerStackPane.getChildren().addFirst(mainBoardBox);
                    this.boardsController = loader.getController();
                    littleDeckComboBox.setVisible(false);
                    littleDeckComboBox.setManaged(false);
                    flipHourglassButton.setVisible(false);
                    flipHourglassButton.setManaged(false);
                });

            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Level2Boards.fxml"));
                VBox mainBoardBox = loader.load();
                this.boardsController = loader.getController();
                Platform.runLater(() -> centerStackPane.getChildren().addFirst(mainBoardBox));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading boards: " + e.getMessage());
        }

        modelFxAdapter = new ModelFxAdapter(clientModel, false, boardsController);

        // Binding e setup
        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);

        this.boardsController.createPaws();

        // other setup
        setupFocusedComponentBinding();
        setupTimerBinding();
        setupVisibleComponentsBinding();

        // initial refresh
        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
        clientModel.getPlayerClientData().keySet().forEach(nickname ->
                modelFxAdapter.refreshShipBoardOf(nickname));

        applyInitialStyling();
    }

    public void handleGenerateShipBoardButton() {
        clientController.requestPrefabShipsList();
    }

    public void showPrefabShipBoards(List<PrefabShipInfo> prefabShips) {
        Platform.runLater(() -> {
            // clean container before adding new elements
            prefabShipsContainer.getChildren().clear();

            for (PrefabShipInfo shipInfo : prefabShips) {

                if (!shipInfo.isForGui())
                    continue;

                // Creation of a VBox to contain the ship name, description and button
                VBox shipCard = new VBox();
                shipCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                shipCard.setSpacing(10.0);
                shipCard.getStyleClass().add("component-controls");
                shipCard.setPadding(new javafx.geometry.Insets(15, 15, 15, 15));

                // Ship name
                Label nameLabel = new Label(shipInfo.getName());
                nameLabel.getStyleClass().add("controls-title");
                shipCard.getChildren().add(nameLabel);

                // Ship description
                Label descLabel = new Label(shipInfo.getDescription());
                descLabel.getStyleClass().add("timer-label");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(500);
                shipCard.getChildren().add(descLabel);

                // Button to select the ship
                Button selectButton = new Button("Select Ship");
                selectButton.getStyleClass().add("action-button");
                selectButton.setOnAction(_ -> {
                    hidePrefabShipsMenu();
                    clientController.selectPrefabShip(shipInfo.getId());
                });
                shipCard.getChildren().add(selectButton);

                // Add card to the container
                prefabShipsContainer.getChildren().add(shipCard);
            }

            // Show menu
            Platform.runLater(() -> {
                prefabShipsMenu.setVisible(true);
                prefabShipsMenu.setManaged(true);
            });
        });
    }

    public void handleCancelPrefabSelection() {
        hidePrefabShipsMenu();
    }

    private void hidePrefabShipsMenu() {
        Platform.runLater(() -> {
            prefabShipsMenu.setVisible(false);
            prefabShipsMenu.setManaged(false);
            prefabShipsContainer.getChildren().clear();
        });
    }

    public void showNoMoreHiddenComponents() {
        showMessage("""
                Hidden components are no longer available, look among the visible ones...""", false);
    }

}