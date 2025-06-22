package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
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

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.WAIT_PLAYER;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.STANDARD;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.ASK;
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
    private final List<Coordinates> selectedCabins = new ArrayList<>();
    private boolean hasChosenDoubleCannon = false;


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
//        ClientGuiController guiController = ClientGuiController.getInstance();
//        if (guiController != null) {
//            modelFxAdapter = guiController.getSharedModelFxAdapter();
//        } else {
//            // Fallback: crea un nuovo adapter
//            modelFxAdapter = new ModelFxAdapter(clientModel);
//        }

        modelFxAdapter = new ModelFxAdapter(clientModel, true);

        // Binding e setup
        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);
        this.bindCurrAdventureCard();

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

                            System.err.println("Updating curr adventure card...");

                            if (newVal != null) {
                                String imagePath = newVal.getImageName();
                                Image cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagePath)));
                                cardImageView.setImage(cardImage);
                            } else {
                                cardImageView.setImage(null);
                            }

                            System.err.println("Updated: " + cardImageView.getImage());

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
                        Platform.runLater(() -> bottomHBox.getChildren().clear());
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
        if (!(card instanceof ClientStarDust starDust)){
            showMessage("Error: Expected StarDust card", false);
            return;
        }

        initializeBeforeCard();
        String warningMessage = null;

        showMessage("Stardust has been detected in your flight path!", true);

        int exposedConnector = clientModel.getMyShipboard().countExposed();
        if (exposedConnector > 0){
            warningMessage = String.format(
                           """
                           Your shipboard wasn't well built.
                           You will lose %d flight days.
                           """,exposedConnector);
        }
        else {
            warningMessage = String.format(
                    """
                    GOOD JOB, your shipboard was built excently.
                    You won't lose any flight days""", starDust);
        }

        showInfoPopup(warningMessage, starDust);
        clientController.stardustEvent(clientController.getNickname());

    }

    //-------------------- ABANDONED SHIP ----------------------
    public void showAbandonedShipMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientAbandonedShip abandonedShip)) {
            showMessage("Error: Expected AbandonedShip card", false);
            return;
        }

        initializeBeforeCard();
        showMessage("You've found an abandoned ship!", true);

        int totalCrew = clientModel.getMyShipboard().getCrewMembers().size();
        if (totalCrew < abandonedShip.getCrewMalus()) {
            showMessage("WARNING: You only have" + totalCrew + "crew members, you cannot accept the reward", false);
            showCannotVisitLocationMenu();
        } else {
            showCanVisitLocationMenu();
        }
    }

    private void showCannotVisitLocationMenu(){
        showMessage("You cannot visit this location!", true);
        Button continueButton = new Button("Continue");
        continueButton.getStyleClass().add("action-button");
        continueButton.setOnAction(_ -> {
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
        });
        Platform.runLater(() -> bottomHBox.getChildren().add(continueButton));
    }

    public void showCanVisitLocationMenu(){
        showMessage("Do you want to visit the abandoned ship?", true);
        Button continueButton = new Button("Continue");
        Button skipButton = new Button("Skip");
        continueButton.getStyleClass().add("action-button");
        skipButton.getStyleClass().add("action-button");

        continueButton.setOnAction(_ -> {
            clientController.playerWantsToVisitLocation(clientController.getNickname(), true);
        });
        skipButton.setOnAction(_ -> {
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
        });

        Platform.runLater(() -> {
            bottomHBox.getChildren().add(continueButton);
            bottomHBox.getChildren().add(skipButton);
        });


    }

    public void showChooseCabinMenu() {
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();

        showMessage("Choose cabins to remove crew from!", true);

        Button confirmChoiceButton  = new Button("Confirm");
        confirmChoiceButton.getStyleClass().add("action-button");

        Platform.runLater(() -> {
            bottomHBox.getChildren().add(confirmChoiceButton);
        });

        confirmChoiceButton.setOnAction(_ -> {
            int malus = card.getCrewMalus();
            int selectedCount = selectedCabins.size();
            int totalCrew = clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size();

            if (selectedCount == 0) {
                showMessage("You must select at least one cabin", false);
                return;
            }

            if (selectedCount > malus) {
                showMessage("You have selected too many cabins. Select at most " + malus + " crew members.", false);
                return;
            }

            if(malus - selectedCount != 0){
                showMessage("You still need to remove " + (malus - selectedCount) + " crew member(s). Select other cabins.", false);
                return;
            }

            boolean success = clientController.playerChoseCabins(clientController.getNickname(), selectedCabins);
            if (success) {
                selectedCabins.clear();
            } else {
                selectedCabins.clear();
                showMessage("Invalid choices. Start again with your selection.", false);
            }
        });

        highlightCabin();
    }

    private void handleCabinSelection(Coordinates coordinates) {

        showMessage("Select cabin to remove crew!", true);

        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> cabinCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getCabin());

        if(!cabinCoordinates.contains(coordinates)) {
            showMessage("You did not select a cabin!", false);
            return;
        }

        Cabin cabin = (Cabin) shipboard.getComponentAt(coordinates);
        int timesAlreadySelected = Collections.frequency(cabinCoordinates, selectedCabins);
        if(cabin.getInhabitants().size() <= timesAlreadySelected) {
            showMessage("This cabin has no more crew member. Select another one.", false);
            return;
        }

        if(!cabin.hasInhabitants()){
            showMessage("You don't have any crew member. Select another one.", false);
        }

        selectedCabins.add(coordinates);
        boardsController.removeHighlightColor();

    }


    //-------------------- PIRATES ----------------------

    public void showPiratesMenu(){
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientPirates pirates)){
            showMessage("Error: Expected Pirates card", false);
            return;
        }

        initializeBeforeCard();

        showMessage("Pirates has been detected!", true);

        if (clientModel.getMyShipboard().getDoubleCannons().isEmpty())  {
            informationalPopUp("""
                    No double cannons available.
                    You can use only single engine.
                    """, pirates);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        if (clientModel.getMyShipboard().getBatteryBoxes().isEmpty()){
            informationalPopUp("""
                    No battery boxes available so you can't activate double cannon
                    You can use only single cannon
                    """, pirates);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            informationalPopUp("""
                    You ran out of batteries so you can't activate double cannons.
                    You can use only single cannon.
                    """, pirates);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        showChooseDoubleCannonsMenu();
    }

    private void showChooseDoubleCannonsMenu() {

        showMessage("You can activate double cannons, each double cannon will require a battery", true);

        Button skipButton = new Button("Skip");
        Button confirmChoiceButton  = new Button("Confirm");
        skipButton.getStyleClass().add("action-button");
        confirmChoiceButton.getStyleClass().add("action-button");

        Platform.runLater(() -> {
            bottomHBox.getChildren().add(confirmChoiceButton);
            bottomHBox.getChildren().add(skipButton);
        });

        highlightDoubleCannon();

        skipButton.setOnAction(_ ->
                clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>())
        );

        confirmChoiceButton.setOnAction(_ -> {
            if (selectedDoubleCannons.size() != selectedBatteryBoxes.size()) {
                showMessage("Please select a battery before confirming", false);
                return;
            }
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
        });

        hasChosenDoubleCannon = false;
    }

    private void handleDoubleCannonSelection(Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> doubleCannonCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getDoubleCannons());

        if (!doubleCannonCoordinates.contains(coordinates)) {
            showMessage("You did not select a double engine.", false);
            return;
        }

        if (selectedDoubleCannons.contains(coordinates)) {
            showMessage("Engine already selected, select another one", false);
            return;
        }

        selectedDoubleEngines.add(coordinates);
        boardsController.removeHighlightColor();
//        hasChosenDoubleEngine = true;
//        showChooseBatteryBoxMenu();
    }

    public void showRewardMenu(){
        showMessage("Well done, you beat them!", true);

        ClientCard card = clientModel.getCurrAdventureCard();
        // TODO finire


    }

    // -------------------- FREE SPACE ----------------------

    public void showFreeSpaceMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientFreeSpace freeSpace)) {
            showMessage("Error: Expected FreeSpace card", false);
            return;
        }

        initializeBeforeCard();

        boolean canActivateDoubleEngines = true;
        String warningMessage = null;

        if (clientModel.getShipboardOf(clientController.getNickname()).getDoubleEngines().isEmpty()) {
            canActivateDoubleEngines = false;
            warningMessage = """
                No double engines available.
                You can use only single engine.
                ⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!""";
        } else if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
            canActivateDoubleEngines = false;
            warningMessage = """
                No battery boxes available so you can't activate double engine.
                You can use only single engine.
                ⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!""";
        } else if (!isThereAvailableBattery()) {
            canActivateDoubleEngines = false;
            warningMessage = """
                You ran out of batteries so you can't activate double engine.
                You can use only single engine.
                ⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!""";
        }

        if (!canActivateDoubleEngines) {
            if (!clientModel.getOutPlayers().contains(clientController.getNickname())) {
                showInfoPopupWithCallback(warningMessage, freeSpace, () -> {
                    Button continueButton = new Button("Continue");
                    continueButton.getStyleClass().add("action-button");
                    continueButton.setOnAction(_ -> {
                        Platform.runLater(() -> bottomHBox.getChildren().clear());
                        clientController.playerChoseDoubleEngines(
                                clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
                    });
                    Platform.runLater(() -> bottomHBox.getChildren().add(continueButton));
                });
            }
            else
                showMessage("You don't have engine power, you’ve been eliminated!", true);
        } else {
            showChooseDoubleEngineMenu();
        }
    }

    public void showChooseDoubleEngineMenu() {
        updateSelectionMessage();
        setupControlButtons();
        highlightAvailableComponents();
    }

    private void setupControlButtons() {
        Platform.runLater(() -> {
            bottomHBox.getChildren().clear();

            Button skipButton = new Button("Skip All");
            Button confirmButton = new Button("Confirm Selection");
            Button resetButton = new Button("Reset Selection");

            skipButton.getStyleClass().add("action-button");
            confirmButton.getStyleClass().add("action-button");
            resetButton.getStyleClass().add("action-button");

            skipButton.setOnAction(_ -> {
                selectedDoubleEngines.clear();
                selectedBatteryBoxes.clear();
                clientController.playerChoseDoubleEngines(
                        clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());

                Platform.runLater(() -> bottomHBox.getChildren().clear());
                boardsController.removeHighlightColor();

                String currentPlayer = clientModel.getCurrentPlayer();
                if (currentPlayer != null && !currentPlayer.equals(clientModel.getMyNickname())) {
                    showMessage("It's " + currentPlayer + "'s turn. Please wait...", true);
                } else {
                    showMessage("Waiting for other players...", true);
                }
            });

            confirmButton.setOnAction(_ -> handleConfirmSelection()
            );

            resetButton.setOnAction(_ -> {
                selectedDoubleEngines.clear();
                selectedBatteryBoxes.clear();
                boardsController.removeHighlightColor();
                updateSelectionMessage();
                highlightAvailableComponents();
            });

            bottomHBox.getChildren().addAll(confirmButton, resetButton, skipButton);
        });
    }

    private void handleConfirmSelection() {
        if (selectedDoubleEngines.isEmpty()) {
            showMessage("No double engines selected. Use 'Skip All' if you don't want to activate any engines.", false);
            return;
        }

        if (selectedDoubleEngines.size() != selectedBatteryBoxes.size()) {
            showMessage("Each double engine needs exactly one battery. Selected: " +
                    selectedDoubleEngines.size() + " engines, " +
                    selectedBatteryBoxes.size() + " batteries.", false);
            return;
        }

        if (!areSelectedBatteriesValid()) {
            showMessage("Some selected batteries are no longer available. Please review your selection.", false);
            return;
        }

        clientController.playerChoseDoubleEngines(
                clientModel.getMyNickname(),
                new ArrayList<>(selectedDoubleEngines),
                new ArrayList<>(selectedBatteryBoxes));

        Platform.runLater(() -> bottomHBox.getChildren().clear());
        boardsController.removeHighlightColor();

        String currentPlayer = clientModel.getCurrentPlayer();
        if (currentPlayer != null && !currentPlayer.equals(clientModel.getMyNickname())) {
            showMessage("It's " + currentPlayer + "'s turn. Please wait...", true);
        } else {
            showMessage("Waiting for other players...", true);
        }
    }

    private boolean areSelectedBatteriesValid() {
        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (Coordinates coords : selectedBatteryBoxes) {
            BatteryBox batteryBox = (BatteryBox) shipboard.getComponentAt(coords);
            int timesSelected = Collections.frequency(selectedBatteryBoxes, coords);

            if (batteryBox.getRemainingBatteries() < timesSelected) {
                return false;
            }
        }

        return true;
    }

    private boolean isSelectingEngine() {
        return selectedDoubleEngines.size() == selectedBatteryBoxes.size();
    }

    private boolean isSelectingBattery() {
        return selectedDoubleEngines.size() > selectedBatteryBoxes.size();
    }

    private void updateSelectionMessage() {
        String message;
        if (isSelectingEngine()) {
            if (selectedDoubleEngines.isEmpty()) {
                message = "Select a double engine to activate (or skip all to use single engines only).";
            } else {
                message = "Selection complete! " + selectedDoubleEngines.size() + " engines paired with " +
                        selectedBatteryBoxes.size() + " batteries. Select another engine or confirm.";
            }
        } else if (isSelectingBattery()) {
            message = " Select a battery to activate the engine you just chose.";
        } else {
            message = "Select components to activate double engines.";
        }

        showMessage(message, true);
    }

    private void highlightAvailableComponents() {
        boardsController.removeHighlightColor();
        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (Coordinates coords : selectedDoubleEngines) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        for (Coordinates coords : selectedBatteryBoxes) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        if (isSelectingEngine()) {
            Set<Coordinates> doubleEngineCoords = shipboard.getCoordinatesOfComponents(shipboard.getDoubleEngines());
            for (Coordinates coords : doubleEngineCoords) {
                if (!selectedDoubleEngines.contains(coords)) {
                    boardsController.applyHighlightEffect(coords, Color.YELLOW);
                }
            }
        } else if (isSelectingBattery()) {
            Set<Coordinates> batteryBoxCoords = shipboard.getCoordinatesOfComponents(shipboard.getBatteryBoxes());
            for (Coordinates coords : batteryBoxCoords) {
                BatteryBox batteryBox = (BatteryBox) shipboard.getComponentAt(coords);
                int timesSelected = Collections.frequency(selectedBatteryBoxes, coords);

                if (batteryBox.getRemainingBatteries() > timesSelected) {
                    boardsController.applyHighlightEffect(coords, Color.GREEN);
                }
            }
        }
    }

    private void handleDoubleEngineSelection(Coordinates coordinates) {
        if (!isSelectingEngine()) {
            showMessage("You must first select a battery for the previous engine.", false);
            return;
        }

        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> doubleEngineCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getDoubleEngines());

        if (!doubleEngineCoordinates.contains(coordinates)) {
            showMessage("You did not select a double engine.", false);
            return;
        }

        if (selectedDoubleEngines.contains(coordinates)) {
            showMessage("This engine is already selected.", false);
            return;
        }

        selectedDoubleEngines.add(coordinates);

        showMessage("Double engine selected. Now select a battery to activate this engine.", true);
        updateSelectionMessage();
        highlightAvailableComponents();
    }

    private void handleBatteryBoxSelection(Coordinates coordinates) {
        if (!isSelectingBattery()) {
            showMessage("You must first select a double engine.", false);
            return;
        }

        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> batteryBoxesCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getBatteryBoxes());

        if (!batteryBoxesCoordinates.contains(coordinates)) {
            showMessage("You did not select a battery box.", false);
            return;
        }

        BatteryBox batteryBox = (BatteryBox) shipboard.getComponentAt(coordinates);
        int timesAlreadySelected = Collections.frequency(selectedBatteryBoxes, coordinates);

        if (batteryBox.getRemainingBatteries() <= timesAlreadySelected) {
            showMessage("This battery box has no more available batteries. Select another one.", false);
            return;
        }

        selectedBatteryBoxes.add(coordinates);

        showMessage("Battery selected! You can activate more engine or confirm your selection.", true);
        updateSelectionMessage();
        highlightAvailableComponents();
    }



    // ------------------------------------------

    private void initializeBeforeCard() {
        this.selectedDoubleEngines.clear();
        this.selectedBatteryBoxes.clear();
        this.selectedDoubleCannons.clear();
        Platform.runLater(() -> bottomHBox.getChildren().clear());
    }

    private void informationalPopUp(String message, ClientCard card) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle(card.getCardType());
//        alert.setContentText(message);
//
//        alert.show();
//
//        // close after 2 seconds
//        PauseTransition delay = new PauseTransition(Duration.seconds(2));
//        delay.setOnFinished(_ -> alert.close());
//        delay.play();
        return;
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

                if (isSelectingEngine()) {
                    handleDoubleEngineSelection(coordinates);
                } else if (isSelectingBattery()) {
                    handleBatteryBoxSelection(coordinates);
                }
            }
            case CardState.CHOOSE_CANNONS -> {
                // Mantieni la vecchia logica per i cannoni (da correggere in futuro)
                if (hasChosenDoubleCannon) {
                    handleDoubleCannonSelection(coordinates);
                } else {
                    handleBatteryBoxSelection(coordinates);
                }
            }
            case CardState.VISIT_LOCATION ->
                showCanVisitLocationMenu();

            case CardState.REMOVE_CREW_MEMBERS ->
                handleCabinSelection(coordinates);

            case CardState.STARDUST ->
                showStardustMenu();

            default -> System.err.println("Unknown card state: " + clientModel.getCurrCardState());
        }
    }

    private void showOverlayPopup(String title, String message, Runnable onClose) {
        // Overlay di sfondo
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        // Container del popup
        VBox popupContent = new VBox();
        popupContent.getStyleClass().add("popup-container");

        // Titolo
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("popup-title");
        titleLabel.setWrapText(true);

        // Messaggio
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-message");
        messageLabel.setWrapText(true);

        // Bottone OK
        Button okButton = new Button("OK");
        okButton.getStyleClass().add("popup-button");
        okButton.setOnAction(e -> {
            centerStackPane.getChildren().remove(overlay);
            if (onClose != null) {
                onClose.run();
            }
        });

        // Assembla il popup
        popupContent.getChildren().addAll(titleLabel, messageLabel, okButton);
        overlay.getChildren().add(popupContent);

        // Chiudi cliccando fuori dal popup
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                centerStackPane.getChildren().remove(overlay);
                if (onClose != null) {
                    onClose.run();
                }
            }
        });

        // Aggiungi l'overlay
        Platform.runLater(() -> {
            centerStackPane.getChildren().add(overlay);
            overlay.toFront();
            if (onClose == null) {
                PauseTransition delay = new PauseTransition(Duration.seconds(2));
                delay.setOnFinished(_ -> {
                    centerStackPane.getChildren().remove(overlay);
                });
                delay.play();
            }
        });
    }

    private void showInfoPopup(String message, ClientCard card) {
        showOverlayPopup(card.getCardType(), message, null);
    }

    private void showInfoPopupWithCallback(String message, ClientCard card, Runnable onClose) {
        showOverlayPopup(card.getCardType(), message, onClose);
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

    private void highlightCabin() {

        List<Cabin> CabinWithCrew = clientModel.getMyShipboard()
                .getCabin()
                .stream()
                .filter(cabin -> !cabin.getInhabitants().isEmpty())
                .collect(Collectors.toList());

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(CabinWithCrew)
                .stream()
                .filter(coords -> {
                    Cabin cabin = ((Cabin) clientModel.getMyShipboard().getComponentAt(coords));
                    return !cabin.getInhabitants().isEmpty();
                })
                .forEach(coordinates -> boardsController.applyHighlightEffect(coordinates, Color.RED));


    }

    private void highlightDoubleEngines() {
        List<DoubleEngine> availableDoubleEngines = clientModel.getMyShipboard().getDoubleEngines();

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(availableDoubleEngines)
                .stream()
                .filter(coords -> !selectedDoubleEngines.contains(coords))
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

    public void showThrowDicesMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card.getCardType().equals("Pirates") ||card.getCardType().equals("SlaveTraders")) {
            showMessage("\nThe enemies are firing at you!", false);
        } else if (card.getCardType().equals("MeteoriteStorm")) {
            showMessage("\nMeteors are heading your way!", false);
        }


        if (clientModel.isMyTurn()) {
            showMessage("Press Enter to throw dice and see where they hit...", true);
            Platform.runLater(this::showThrowDicePopUp);
        } else
            showMessage("The first player is throwing dices, wait...", true);

    }

    public void showThrowDicePopUp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Throw Dice");

        // Apply custom CSS class to the dialog pane
        dialog.getDialogPane().getStyleClass().add("popup-container");

        // Set content text
        Label contentLabel = new Label("Press to throw the dice, other player's future is in your hand");
        contentLabel.setWrapText(true);

        // Create custom styled button
        Button throwButton = new Button("Throw Dice");
        throwButton.getStyleClass().add("custom-popup-button");
        throwButton.setOnAction(_ -> {
            dialog.close();
            clientController.playerWantsToThrowDices(clientModel.getMyNickname());
        });

        VBox vbox = new VBox(15, contentLabel, throwButton);
        vbox.setPrefWidth(300);
        dialog.getDialogPane().setContent(vbox);

        // Remove default buttons
        dialog.getDialogPane().getButtonTypes().clear();

        dialog.showAndWait();
    }

}
