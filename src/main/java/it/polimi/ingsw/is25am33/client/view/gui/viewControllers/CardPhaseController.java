package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientFreeSpace;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.client.model.card.ClientStarDust;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.STANDARD;

public class CardPhaseController extends GuiController implements BoardsEventHandler {

    @FXML
    public StackPane centerStackPane;
    @FXML
    public Label messageLabel;
    @FXML
    public ImageView cardImageView;
    @FXML
    public HBox bottomHBox;

    private Level2BoardsController boardsController;
    private ModelFxAdapter modelFxAdapter;
    private final List<Coordinates> selectedDoubleEngines = new ArrayList<>();
    private final List<Coordinates> selectedDoubleCannons = new ArrayList<>();
    private final List<Coordinates> selectedBatteryBoxes = new ArrayList<>();
    private boolean hasChosenDoubleEngine = false;


    @Override
    void showMessage(String message, boolean isPermanent) {
        if (isPermanent) {
            Platform.runLater(() -> showPermanentMessage(message, messageLabel));
            return;
        }

        // Stop any ongoing transitions before starting new fade
        messageLabel.getTransforms().clear();
        Platform.runLater(() -> showNonPermanentMessage(message, messageLabel));
    }

    public ModelFxAdapter getModelFxAdapter() {
        return modelFxAdapter;
    }

    public void initialize() {
        // Caricamento delle board
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Level2Boards.fxml"));
            VBox mainBoardBox = loader.load();
            centerStackPane.getChildren().addFirst(mainBoardBox);
            this.boardsController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading boards: " + e.getMessage());
        }

        // Prova a utilizzare un ModelFxAdapter condiviso
        ClientGuiController guiController = ClientGuiController.getInstance();
        if (guiController != null) {
            modelFxAdapter = guiController.getSharedModelFxAdapter();
        } else {
            // Fallback: crea un nuovo adapter
            modelFxAdapter = new ModelFxAdapter(clientModel);
        }

        // Binding e setup
        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);
        bindCurrAdventureCard();

        // Refresh iniziale
        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
        clientModel.getPlayerClientData().keySet().forEach(nickname ->
                modelFxAdapter.refreshShipBoardOf(nickname));
        modelFxAdapter.refreshRanking();
    }

    private void bindCurrAdventureCard() {

        modelFxAdapter.getObservableCurrAdventureCard()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() -> {

                            if (newVal != null) {
                                String imagePath = newVal.getImageName();
                                Image cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagePath)));
                                cardImageView.setImage(cardImage);
                            } else {
                                cardImageView.setImage(null);
                            }

                        }));

    }

    // ------------------ CARD PHASE MENU ------------------

    //-------------------- PLANET ----------------------
    public void showChoosePlanetMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientPlanets planets)) {
            showMessage("Error: Expected Planets card", false);
            return;
        }

        initializeBeforeCard();

        // Add button for each available planet
        for (int i = 0; i < planets.getAvailablePlanets().size(); i++) {
            Planet planet = planets.getAvailablePlanets().get(i);
            if (!planet.isBusy()) {
                Button planetButton = new Button();
                planetButton.setText("Planet " + (i + 1));
                planetButton.getStyleClass().add("action-button");

                // Set button action
                final int planetIndex = i + 1; // Store planet index (1-based)
                planetButton.setOnAction(_ -> {
                    try {
                        clientController.playerWantsToVisitPlanet(clientController.getNickname(), planetIndex);
                    } catch (Exception e) {
                        showMessage("Error selecting planet: " + e.getMessage(), false);
                    }
                });

                bottomHBox.getChildren().add(planetButton);
            }
        }

        // Add skip button
        Button skipButton = new Button("Skip (don't land on any planet)");
        skipButton.getStyleClass().add("action-button");
        skipButton.setOnAction(_ -> {
            clientController.playerWantsToVisitPlanet(clientController.getNickname(), 0);
        });

        Platform.runLater(() -> bottomHBox.getChildren().add(skipButton));
    }

    //-------------------- STARDUST ----------------------

    public void showStardustMenu(){
        ClientCard card = clientModel.getCurrAdventureCard();
        if(!(card instanceof ClientStarDust starDust)){
            showMessage("Error: Expected StarDust card", false);
            return;
        }

        initializeBeforeCard();

        showMessage("Stardust has been detected in your flight path!", true);
        int exposedConnector = clientModel.getMyShipboard().countExposed();
        if (exposedConnector > 0){
            informationalPopUp(String.format("""
                           Your shipboard wasn't well built.
                           You will lose %d flight days.
                           """,exposedConnector ), starDust);
        }
        else {
            informationalPopUp("""
                    GOOD JOB, your shipboard was built excently.
                    You won't lose any flight days""", starDust);
        }
    }

    //-------------------- METEORITE STORM ----------------------



    //-------------------- FREE SPACE ----------------------

    public void showFreeSpaceMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientFreeSpace freeSpace)) {
            showMessage("Error: Expected Planets card", false);
            return;
        }

        initializeBeforeCard();

        if (clientModel.getShipboardOf(clientController.getNickname()).getDoubleEngines().isEmpty()) {
            informationalPopUp("""
                    No double engines available.
                    You can use only single engine.
                    ATTENTION! If your ship doesn't have engine power, you will be eliminated!""", freeSpace);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
            informationalPopUp("""
                    No battery boxes available so you can't activate double engine.
                    You can use only single engine.
                    ATTENTION! If your ship doesn't have engine power, you will be eliminated!""",  freeSpace);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        if (!isThereAvailableBattery()) {
            informationalPopUp("""
                    You ran out of batteries so you can't activate double engine.
                    You can use only single engine.
                    ATTENTION! If your ship doesn't have engine power, you will be eliminated!""",  freeSpace);
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        showChooseDoubleEngineMenu();
    }

    public void showChooseDoubleEngineMenu() {

        showMessage("You can activate double engines, each double engine will require a battery", true);

        Button skipButton = new Button("Skip");
        Button confirmChoiceButton  = new Button("Confirm");
        skipButton.getStyleClass().add("action-button");
        confirmChoiceButton.getStyleClass().add("action-button");

        Platform.runLater(() -> {
            bottomHBox.getChildren().add(confirmChoiceButton);
            bottomHBox.getChildren().add(skipButton);
        });

        highlightDoubleEngines();

        skipButton.setOnAction(_ ->
                clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>())
        );

        confirmChoiceButton.setOnAction(_ -> {
            if (selectedDoubleEngines.size() != selectedBatteryBoxes.size()) {
                showMessage("Please select a battery before confirming", false);
                return;
            }
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedDoubleEngines, selectedBatteryBoxes);
        });

        hasChosenDoubleEngine = false;

    }

    private void showChooseBatteryBoxMenu() {
        showMessage("You need to select a battery to activate your double engine.", true);
        highlightBatteryBoxes();
    }

    private void highlightDoubleEngines() {
        List<DoubleEngine> availableDoubleEngines = clientModel.getMyShipboard().getDoubleEngines();

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(availableDoubleEngines)
                .stream()
                .filter(coords -> !selectedDoubleEngines.contains(coords))
                .forEach(coordinates -> boardsController.applyHighlightEffect(coordinates, Color.GREEN));
    }

    private void highlightBatteryBoxes() {
        List<BatteryBox> availableBatteryBoxes = clientModel.getMyShipboard()
                .getBatteryBoxes()
                .stream()
                .filter(batteryBox -> batteryBox.getRemainingBatteries() > 0)
                .collect(Collectors.toList());

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(availableBatteryBoxes)
                .stream()
                .filter(coords -> {
                    BatteryBox currBatteryBox = ((BatteryBox) clientModel.getMyShipboard().getComponentAt(coords));
                    return currBatteryBox.getRemainingBatteries() > Collections.frequency(selectedBatteryBoxes, coords);
                })
                .forEach(coordinates -> boardsController.applyHighlightEffect(coordinates, Color.GREEN));
    }

    private void highlightDoubleCannon() {
        List<DoubleCannon> availableDoubleCannons = clientModel.getMyShipboard().getDoubleCannons();

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(availableDoubleCannons)
                .stream()
                .filter(coords -> !selectedDoubleCannons.contains(coords))
                .forEach(coordinates -> boardsController.applyHighlightEffect(coordinates, Color.GREEN));
    }

    private void handleDoubleEngineSelection(Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> doubleEngineCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getDoubleEngines());

        if (!doubleEngineCoordinates.contains(coordinates)) {
            showMessage("You did not select a double engine.", false);
            return;
        }

        if (selectedDoubleEngines.contains(coordinates)) {
            showMessage("Engine already selected, select another one", false);
            return;
        }

        selectedDoubleEngines.add(coordinates);
        boardsController.removeHighlightColor();
        hasChosenDoubleEngine = true;
        showChooseBatteryBoxMenu();
    }

    private void handleBatteryBoxSelection(Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> batteryBoxesCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getBatteryBoxes());

        // se a quelle coordinate non c'è un batterybox
        if (!batteryBoxesCoordinates.contains(coordinates)) {
            showMessage("You did not select a battery box.", false);
            return;
        }

        BatteryBox batteryBox = (BatteryBox) shipboard.getComponentAt(coordinates);

        // se il batterybox selezionato non ha più batterie disponibili
        if (batteryBox.getRemainingBatteries() <= 0)
            showMessage("This batteryBox is empty, select another one.", false);

        // se il batterybox selezionato è gia stato selezionato altre volte e non ha più batterie disponibili
        int frequency = Collections.frequency(batteryBoxesCoordinates, coordinates);

        if (batteryBox.getRemainingBatteries() == frequency)
            showMessage("This battery box is empty, select another one.", false);

        selectedBatteryBoxes.add(coordinates);
        boardsController.removeHighlightColor();
        hasChosenDoubleEngine = false;
        showChooseDoubleEngineMenu();
    }


    private void initializeBeforeCard() {
        this.selectedDoubleEngines.clear();
        this.selectedBatteryBoxes.clear();
        this.selectedDoubleCannons.clear();
        Platform.runLater(() -> bottomHBox.getChildren().clear());
    }

    private void informationalPopUp(String message, ClientCard card) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(card.getCardType());
        alert.setContentText(message);

        alert.show();

        // close after 2 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(_ -> alert.close());
        delay.play();
    }

    private boolean isThereAvailableBattery() {
        List<BatteryBox> batteryBoxes = clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes();
        for (BatteryBox batteryBox : batteryBoxes) {
            if (batteryBox.getRemainingBatteries() >= 1) {
                return true;
            }
        }
        return false ;
    }

    @Override
    public void onGridButtonClick(int row, int column) {
        Coordinates coordinates = new Coordinates(row, column);
        switch (clientModel.getCurrCardState()) {
            case CardState.CHOOSE_ENGINES -> {
                if (hasChosenDoubleEngine)
                    handleDoubleEngineSelection(coordinates);
                else
                    handleBatteryBoxSelection(coordinates);
            }

            // TODO aggiungere stati

            default -> System.err.println("Unknown card state: " + clientModel.getCurrCardState());
        }

    }


}
