package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.StorageSelectionManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.card.SlaveTraders;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CardPhaseController extends GuiController implements BoardsEventHandler {
    @FXML
    private Button exitGameButton;
    @FXML
    private Button landButton;
    @FXML
    public StackPane centerStackPane;
    @FXML
    public Label messageLabel;
    @FXML
    public ImageView cardImageView;
    @FXML
    public HBox bottomHBox;
    @FXML
    public Label cosmicCreditsLabel;

    // TODO fare per il livello 1
    private BoardsController boardsController;
    private ModelFxAdapter modelFxAdapter;
    private final List<Coordinates> selectedDoubleEngines = new ArrayList<>();
    private final List<Coordinates> selectedDoubleCannons = new ArrayList<>();
    private final List<Coordinates> selectedBatteryBoxes = new ArrayList<>();
    private final List<Coordinates> selectedShield = new ArrayList<>();
    private Coordinates hitComponent = null;
    private StorageSelectionManager storageManager;
    private int planetChoice;
    private boolean hasChosenDoubleEngine = false;
    private final Map<Coordinates, Integer> selectedCrewPerCabin = new HashMap<>();
    private boolean hasChosenDoubleCannon = false;
    private boolean hasChosenShield = false;
    private final List<Set<Coordinates>> shipParts = new ArrayList<>();
    private final List<Coordinates> selectedStorage = new ArrayList<>();
    private final List<Coordinates> mostPreciousCube = new ArrayList<>();
    private boolean isRemovingBatteries = false;
    private int remainingCubesToRemove = 0;
    private final List<String> alreadyEliminated = new ArrayList<>();

    @FXML
    private void handleExitGame() {
        String confirmMessage = """
                Are you sure you want to leave the game?
                
                Exiting the game will cause 
                all your progress to be lost.
                """;

        showOverlayPopup("confirm Exit", confirmMessage,
                () -> {
                    clientController.leaveGame();
                    javafx.application.Platform.exit();
                });
    }

    @FXML
    public void handleLand() {
//        Platform.runLater(() -> landButton.setVisible(false));
//        createEarlyLandingDisplay();
        showPlayerLanded();
    }

    public void notifyOtherPlayerEarlyLanded(String nickname) {
        if (!alreadyEliminated.contains(nickname)) {
            createOtherPlayersDisplay(nickname);
            alreadyEliminated.add(nickname);
        }
    }

    @FXML
    private void handleSkipToLastCart() {
        clientController.skipToLastCard();
    }

    @Override
    public String getControllerType() {
        return "cardPhaseController";
    }

    /**
     * Helper method to create styled buttons and add them to bottomHBox
     *
     * @param buttonText The text to display on the button
     * @param action     The action to perform when button is clicked
     */
    private void createAndAddButton(String buttonText, Runnable action) {
        Button button = new Button(buttonText);
        button.getStyleClass().add("action-button");
        button.setOnAction(_ -> action.run());
        Platform.runLater(() -> bottomHBox.getChildren().add(button));
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

    public ModelFxAdapter getModelFxAdapter() {
        return modelFxAdapter;
    }

    public void initialize() {
        // Board loading
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    clientController.getCurrentGameInfo().isTestFlight() ?
                            "/gui/Level1Boards.fxml" :
                            "/gui/Level2Boards.fxml")
            );
            VBox mainBoardBox = loader.load();
            this.boardsController = loader.getController();
            Platform.runLater(() -> centerStackPane.getChildren().addFirst(mainBoardBox));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading boards: " + e.getMessage());
        }

        modelFxAdapter = new ModelFxAdapter(clientModel, true, boardsController);

        // Binding e setup
        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);
        this.bindCurrAdventureCard();
        this.initializeCosmicCredits();
        this.boardsController.createPaws();

        // initial refresh
        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
        clientModel.getPlayerClientData().keySet().forEach(nickname ->
                modelFxAdapter.refreshShipBoardOf(nickname));
        modelFxAdapter.refreshRanking();
        modelFxAdapter.refreshCosmicCredits();

    }

    public void updateCosmicCredits(int credits) {
        Platform.runLater(() -> {
            cosmicCreditsLabel.setText(String.valueOf(credits));

            cosmicCreditsLabel.getParent().getStyleClass().add("cosmic-credits-updated");

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e ->
                    cosmicCreditsLabel.getParent().getStyleClass().remove("cosmic-credits-updated"));
            delay.play();
        });
    }

    private void bindCurrAdventureCard() {

        modelFxAdapter.getObservableCurrAdventureCard()
                .addListener((_, _, newVal) ->
                        Platform.runLater(() -> {

                            System.err.println("Updating curr adventure card...");

                            if (newVal != null) {
                                String imagePath = newVal.getImageName();
                                Image cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gui/graphics/cards/" + imagePath)));

                                animateCardFall(cardImage);

                            } else {
                                cardImageView.setImage(null);
                            }

                            System.err.println("Updated: " + cardImageView.getImage());

                        }));
    }

    private void animateCardFall(Image cardImage) {
        Timeline cardFallAnimation = new Timeline();

        cardImageView.setScaleX(0.3);
        cardImageView.setScaleY(0.3);
        cardImageView.setOpacity(0.7);
        cardImageView.setRotate(-10);

        cardImageView.setImage(cardImage);

        KeyFrame scaleUp = new KeyFrame(Duration.millis(400),
                new KeyValue(cardImageView.scaleXProperty(), 1.05, Interpolator.EASE_OUT),
                new KeyValue(cardImageView.scaleYProperty(), 1.05, Interpolator.EASE_OUT),
                new KeyValue(cardImageView.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(cardImageView.rotateProperty(), 5, Interpolator.EASE_OUT)
        );

        KeyFrame bounce = new KeyFrame(Duration.millis(600),
                new KeyValue(cardImageView.scaleXProperty(), 0.95, Interpolator.EASE_IN),
                new KeyValue(cardImageView.scaleYProperty(), 0.95, Interpolator.EASE_IN),
                new KeyValue(cardImageView.rotateProperty(), -2, Interpolator.EASE_IN)
        );

        KeyFrame settle = new KeyFrame(Duration.millis(800),
                new KeyValue(cardImageView.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(cardImageView.scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(cardImageView.rotateProperty(), 0, Interpolator.EASE_OUT)
        );

        cardFallAnimation.getKeyFrames().addAll(scaleUp, bounce, settle);

        cardFallAnimation.play();
    }

    private void bindCosmicCredits() {
        modelFxAdapter.getObservableCosmicCredits()
                .addListener((_, _, newCredits) -> {
                    if (newCredits != null) {
                        updateCosmicCredits(newCredits.intValue());
                    }
                });
    }

    private void initializeCosmicCredits() {
        modelFxAdapter.refreshCosmicCredits();
        bindCosmicCredits();
    }

    // ------------------ CARD PHASE MENU ------------------

    //-------------------- PLANET ----------------------

    public void showChoosePlanetMenu() {

        ClientCard modelCard = clientModel.getCurrAdventureCard();
        if (!(modelCard instanceof ClientPlanets planets)) {
            showMessage("Error: Expected Planets card", false);
            return;
        }

        initializeBeforeCard();

        // Add button for each available planet
        for (int i = 0; i < planets.getAvailablePlanets().size(); i++) {

            Planet planet = planets.getAvailablePlanets().get(i);

            System.err.println("Setting button for planet: " + i);
            if (!planet.isBusy()) {
                final int planetIndex = i + 1; // Store planet index (1-based)
                createAndAddButton("Planet " + planetIndex, () -> {
                    planetChoice = planetIndex;
                    try {
                        Platform.runLater(() -> {
                            showMessage("Planet " + planetIndex + " selected", false);
                            bottomHBox.getChildren().clear();
                        });
                        clientController.playerWantsToVisitPlanet(clientController.getNickname(), planetIndex);
                    } catch (Exception e) {
                        showMessage("Error selecting planet: " + e.getMessage(), false);
                    }
                });
            }
        }

        // Add skip button
        createAndAddButton("Skip (don't land on any planet)", () -> {
            Platform.runLater(() -> {
                showMessage("You won't land on any planet", false);
                bottomHBox.getChildren().clear();
            });
            clientController.playerWantsToVisitPlanet(clientController.getNickname(), 0);
        });
    }

    //-------------------- STARDUST ----------------------

    public void showStardustMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientStarDust starDust)) {
            showMessage("Error: Expected StarDust card", false);
            return;
        }

        if (!clientModel.isMyTurn())
            return;

        initializeBeforeCard();
        String warningMessage;

        showMessage("Stardust has been detected in your flight path!", true);

        int exposedConnector = clientModel.getMyShipboard().countExposed();
        if (exposedConnector > 0) {
            warningMessage = String.format(
                    """
                            Your shipboard wasn't well built.
                            You will lose %d flight days.
                            """, exposedConnector);
        } else {
            warningMessage = String.format(
                    """
                            GOOD JOB, your shipboard was built excently.
                            You won't lose any flight days""");
        }

        showInfoPopupWithCallback(warningMessage, starDust, () -> {
                Platform.runLater(() -> bottomHBox.getChildren().clear());
                clientController.stardustEvent(clientModel.getMyNickname());
            });

    }

    //-------------------- ABANDONED SHIP ----------------------

    public void showAbandonedShipMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientAbandonedShip abandonedShip)) {
            showMessage("Error: Expected AbandonedShip card", false);
            return;
        }

        initializeBeforeCard();

        int totalCrew = clientModel.getMyShipboard().getCrewMembers().size();
        int requiredCrew = abandonedShip.getCrewMalus();

        if (totalCrew < requiredCrew) {
            showMessage("WARNING: You only have " + totalCrew + " crew members, but you need at least " +
                    requiredCrew + " to visit this ship. You cannot accept the reward.", true);
            showCannotVisitLocationMenu();
        } else {
            showCanVisitLocationMenu();
        }
    }

    private void showCannotVisitLocationMenu() {
        showMessage("You cannot visit this location!", true);

        createAndAddButton("Continue", () -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
            showWaitingMessage();
        });
    }

    private void showCanVisitLocationMenu() {
        showMessage("Do you want to visit the location?", true);

        createAndAddButton("Visit Ship", () -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), true);
        });

        createAndAddButton("Skip", () -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
            showWaitingMessage();
        });
    }

    public void showChooseCabinMenu() {
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        int crewToRemove = card.getCrewMalus();

        if (card.getCrewMalus() >= clientModel.getShipboardOf(clientModel.getMyNickname()).getCrewMembers().size()) {

            ShipBoardClient shipBoardClient = clientModel.getMyShipboard();
            List<Cabin> cabinsWithCrew = new ArrayList<>(shipBoardClient.getCabin()
                    .stream()
                    .filter(Cabin::hasInhabitants)
                    .toList());

            MainCabin mainCabin = shipBoardClient.getMainCabin();
            if (mainCabin != null && mainCabin.hasInhabitants())
                cabinsWithCrew.add(mainCabin);

            List<Coordinates> selectedCabinsCoords = new ArrayList<>();

            for (Cabin cabin : cabinsWithCrew) {
                Set<Coordinates> cabinCoords = shipBoardClient.getCoordinatesOfComponents(new ArrayList<>(List.of(cabin)));
                Coordinates coords = cabinCoords.iterator().next();

                for (int i = 0; i < cabin.getInhabitants().size(); i++) {
                    selectedCabinsCoords.add(coords);
                }
            }

            showInfoPopupWithCallback("""
                            You have not enough crew members. You must sacrifice all of them.
                            ATTENTION! you will be eliminated""",
                    (ClientCard) card,
                    () -> {
                        boolean success = clientController.checkCabinSelection(clientModel.getMyNickname(), selectedCabinsCoords);

                        if (success)
                            clientController.playerChoseCabins(clientModel.getMyNickname(), selectedCabinsCoords);
                        else
                            showChooseCabinMenu();
                    });

            return;
        }

        selectedCrewPerCabin.clear();

        showMessage("You need to remove " + crewToRemove + " crew member(s). Warning: if you run out of humans, you'll be eliminated.", true);

        Map<Coordinates, Cabin> cabinsWithCrew = clientModel.getMyShipboard().getCoordinatesAndCabinsWithCrew();

        // This should never be reached
        if (cabinsWithCrew.isEmpty()) {
            showMessage("ILLEGAL STATE: You have no occupied cabins. You cannot sacrifice crew members.", false);
            return;
        }

        createAndAddButton("Confirm Selection", () -> handleCabinConfirmation(crewToRemove));

        createAndAddButton("Reset All", () -> {
            selectedCrewPerCabin.clear();
            updateCabinSelectionMessage(crewToRemove);
            highlightCabinsWithSelection();
            showMessage("Selection reset. Start selecting crew members again.", false);
        });

        updateCabinSelectionMessage(crewToRemove);
        boardsController.removeHighlightColor();
        highlightCabinsWithSelection();
    }

    private void handleCabinSelection(Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Map<Coordinates, Cabin> cabinsWithCrew = shipboard.getCoordinatesAndCabinsWithCrew();

        if (!cabinsWithCrew.containsKey(coordinates)) {
            showMessage("You did not select a cabin with crew members!", false);
            return;
        }

        Cabin cabin = cabinsWithCrew.get(coordinates);
        int maxCrewInCabin = cabin.getInhabitants().size();
        int currentlySelected = selectedCrewPerCabin.getOrDefault(coordinates, 0);

        int newSelection;
        if (currentlySelected >= maxCrewInCabin) {
            newSelection = 0; // Reset to 0 when at maximum
            selectedCrewPerCabin.remove(coordinates);
        } else {
            newSelection = currentlySelected + 1;
            selectedCrewPerCabin.put(coordinates, newSelection);
        }

        if (newSelection == 0) {
            showMessage("Cabin deselected. No crew members selected from this cabin.", false);
        } else {
            showMessage(String.format("Selected %d/%d crew members from this cabin.",
                    newSelection, maxCrewInCabin), false);
        }

        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        updateCabinSelectionMessage(card.getCrewMalus());
        highlightCabinsWithSelection();
    }

    private void handleCabinConfirmation(int requiredCrewToRemove) {
        int totalSelectedCrew = selectedCrewPerCabin.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalSelectedCrew == 0) {
            showMessage("You must select at least one crew member.", false);
            return;
        }

        if (totalSelectedCrew != requiredCrewToRemove) {
            if (totalSelectedCrew < requiredCrewToRemove) {
                showMessage(String.format("You need to select exactly %d crew members. Currently selected: %d.",
                        requiredCrewToRemove, totalSelectedCrew), false);
            } else {
                showMessage(String.format("You selected too many crew members. Need exactly %d, selected %d.",
                        requiredCrewToRemove, totalSelectedCrew), false);
            }
            return;
        }

        List<Coordinates> cabinSelectionForServer = convertSelectionForServer();

        boolean isCorrect = clientController.checkCabinSelection(
                clientController.getNickname(),
                cabinSelectionForServer
        );

        if (isCorrect) {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            boardsController.removeHighlightColor();
            selectedCrewPerCabin.clear();
            showMessage("Crew members removed successfully!", true);
            showWaitingMessage();
            if (clientModel.getCurrAdventureCard() instanceof ClientAbandonedShip)
                showAbandonedShipReward();
            clientController.playerChoseCabins(clientModel.getMyNickname(), cabinSelectionForServer);
        } else {
            selectedCrewPerCabin.clear();
            boardsController.removeHighlightColor();
            showMessage("Invalid selection. Please try again.", false);
            updateCabinSelectionMessage(requiredCrewToRemove);
            highlightCabinsWithSelection();
        }
    }

    private List<Coordinates> convertSelectionForServer() {
        List<Coordinates> serverFormat = new ArrayList<>();

        for (Map.Entry<Coordinates, Integer> entry : selectedCrewPerCabin.entrySet()) {
            Coordinates cabinCoords = entry.getKey();
            int selectedCrewCount = entry.getValue();

            for (int i = 0; i < selectedCrewCount; i++) {
                serverFormat.add(cabinCoords);
            }
        }

        return serverFormat;
    }

    private void updateCabinSelectionMessage(int requiredCrewToRemove) {
        int totalSelectedCrew = selectedCrewPerCabin.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        String message;
        if (totalSelectedCrew == 0) {
            message = String.format("Select %d crew members to remove. Click on cabins to select individual crew members.",
                    requiredCrewToRemove);
        } else if (totalSelectedCrew < requiredCrewToRemove) {
            int stillNeeded = requiredCrewToRemove - totalSelectedCrew;
            message = String.format("Selected %d/%d crew members. Need %d more crew members.",
                    totalSelectedCrew, requiredCrewToRemove, stillNeeded);
        } else if (totalSelectedCrew == requiredCrewToRemove) {
            message = String.format("Perfect! Selected exactly %d crew members. Ready to confirm.",
                    totalSelectedCrew);
        } else {
            int excess = totalSelectedCrew - requiredCrewToRemove;
            message = String.format("Selected %d crew members (%d too many). Deselect %d crew members.",
                    totalSelectedCrew, excess, excess);
        }

        showMessage(message, true);
    }

    private void highlightCabinsWithSelection() {
        boardsController.removeHighlightColor();
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Map<Coordinates, Cabin> cabinsWithCrew = shipboard.getCoordinatesAndCabinsWithCrew();

        for (Map.Entry<Coordinates, Cabin> entry : cabinsWithCrew.entrySet()) {
            Coordinates coords = entry.getKey();
            Cabin cabin = entry.getValue();
            int maxCrew = cabin.getInhabitants().size();
            int selectedCrew = selectedCrewPerCabin.getOrDefault(coords, 0);

            Color highlightColor;
            if (selectedCrew == 0) {
                highlightColor = Color.YELLOW;
            } else if (selectedCrew == maxCrew) {
                highlightColor = Color.RED;
            } else {
                highlightColor = Color.GREEN;
            }

            boardsController.applyHighlightEffect(coords, highlightColor);
        }
    }

    private void showAbandonedShipReward() {
        ClientAbandonedShip abandonedShip = (ClientAbandonedShip) clientModel.getCurrAdventureCard();

        String message = "Great! You've successfully explored the abandoned ship and received " +
                abandonedShip.getReward() + " credits!";
        showMessage(message, false);

    }

    //-------------------- ABANDONED STATION ----------------------

    public void showAbandonedStationMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientAbandonedStation abandonedStation)) {
            showMessage("Error: Expected AbandonedStation card", false);
            return;
        }

        initializeBeforeCard();

        showMessage("You've found an abandoned station.", true);

        int totalCrew = clientModel.getMyShipboard().getCrewMembers().size();
        int requiredCrew = abandonedStation.getCrewMalus();

        if (totalCrew < requiredCrew) {
            showMessage("WARNING: You only have " + totalCrew + " crew members, but you need at least " +
                    requiredCrew + " to visit this cargo. You cannot accept the reward.", true);
            showCannotVisitLocationMenu();
        } else {
            showCanVisitLocationMenu();
        }
    }


    //-------------------- PIRATES ----------------------

    public void showChooseCannonsMenu(){
        ClientCard card = clientModel.getCurrAdventureCard();
        if (
                !(card instanceof ClientPirates ||
                card instanceof ClientSmugglers ||
                card instanceof ClientSlaveTraders ||
                card instanceof ClientWarField)
        ) {
            System.err.println("Error: Expected enemies card");
            return;
        }

        initializeBeforeCard();

        showMessage(card.getCardType() + " have been detected!", true);

        if (clientModel.getMyShipboard().getDoubleCannons().isEmpty()) {
            showInfoPopupWithCallback("""
                    No double cannons available.
                    You can use only single cannons.
                    """,
                    card,
                    () -> clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes)
            );
            return;
        }

        if (clientModel.getMyShipboard().getBatteryBoxes().isEmpty()){
            showInfoPopupWithCallback("""
                    No battery boxes available so you can't activate double cannons.
                    You can use only single cannons.
                    """, card,
                    () -> clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes));
            return;
        }

        if (!isThereAvailableBattery()) {
            showInfoPopupWithCallback("""
                    You ran out of batteries so you can't activate double cannons.
                    You can use only single cannons.
                    """, card,
                    () -> clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes));
            return;
        }

        showChooseDoubleCannonsMenu();
    }

    private void showChooseDoubleCannonsMenu() {

        showMessage("You can activate double cannons, each double cannon will require a battery", true);

        createAndAddButton("Confirm", () -> {
            if (selectedDoubleCannons.size() != selectedBatteryBoxes.size()) {
                showMessage("Please select a battery before confirming", false);
                return;
            }
            boardsController.removeHighlightColor();
            bottomHBox.getChildren().clear();
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
        });

        createAndAddButton("Skip", () -> {
            boardsController.removeHighlightColor();
            bottomHBox.getChildren().clear();
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
        });

        highlightDoubleCannon();

        hasChosenDoubleCannon = false;
    }

    public void showThrowDicesMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        switch (card) {
            case ClientPirates clientPirates -> showMessage("\nThe enemies are firing at you!", true);
            case ClientMeteoriteStorm clientMeteoriteStorm -> showMessage("\nMeteors are heading your way!", true);
            case ClientWarField clientWarField -> showMessage("\nShots are heading your way!", true);
            case null, default -> {
                System.err.println("Invalid card type!");
                return;
            }
        }

        if (clientModel.isMyTurn()) {
            showInfoPopupWithCallback("""
                            Throw the dice to see where the meteorite will hit""",
                    card,
                    () -> clientController.playerWantsToThrowDices(clientModel.getMyNickname()));

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

    private void handleDoubleCannonSelection(Coordinates coordinates) {
        if (!isSelectingDoubleCannon()) {
            showMessage("You must first select a battery for the previous engine.", false);
            return;
        }

        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Set<Coordinates> doubleCannonsCoordinates = shipboard.getCoordinatesOfComponents(shipboard.getDoubleCannons());

        if (!doubleCannonsCoordinates.contains(coordinates)) {
            showMessage("You did not select a double cannon.", false);
            return;
        }

        if (selectedDoubleCannons.contains(coordinates)) {
            showMessage("This cannon is already selected.", false);
            return;
        }

        selectedDoubleCannons.add(coordinates);

        showMessage("Double cannon selected. Now select a battery to activate this cannon.", true);
        updateSelectionMessage();
        highlightAvailableDoubleCannon();
    }

    public void showRewardMenu() {
        showMessage("Well done, you beat them!", true);

        ClientCard card = clientModel.getCurrAdventureCard();

        int stepsBack = 0;
        int reward = 0;

        // Extract reward and steps information based on card type
        if (card.hasReward()) {
            switch (card) {
                case ClientPirates piratesCard -> {
                    reward = piratesCard.getReward();
                    stepsBack = piratesCard.getStepsBack();
                }
                case ClientSlaveTraders slaveTraders -> {
                    reward = slaveTraders.getReward();
                    stepsBack = slaveTraders.getStepsBack();
                }
                case ClientSmugglers smugglers ->
                    stepsBack = smugglers.getStepsBack();
                default -> {
                }
            }
        }

        if (card instanceof ClientSmugglers) {
            showMessage("You can get cargo cube reward but will lose " + (stepsBack * -1) + " flight days. Do you accept?", true);
        } else if (stepsBack != 0 && reward != 0) {
            showMessage("You can get " + reward + " credits but will lose " + (stepsBack * -1) + " flight days. Do you accept?", true);
        } else {
            showMessage("You can get the reward, but if you do, you will lose flight days. Do you accept?", true);
        }

        createAndAddButton("Accept", () -> {
            bottomHBox.getChildren().clear();
            clientController.playerWantsToAcceptTheReward(clientModel.getMyNickname(), true);
        });

        createAndAddButton("Reject", () -> {
            bottomHBox.getChildren().clear();
            clientController.playerWantsToAcceptTheReward(clientModel.getMyNickname(), false);
        });

    }

    // -------------------- FREE SPACE ----------------------

    public void showFreeSpaceMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (
                !(card instanceof ClientFreeSpace ||
                card instanceof ClientWarField)
        ) {
            showMessage("Error: Expected FreeSpace or Warfield card", false);
            return;
        }

        initializeBeforeCard();


        boolean canActivateDoubleEngines = true;
        StringBuilder warningMessage = new StringBuilder();

        if (clientModel.getShipboardOf(clientController.getNickname()).getDoubleEngines().isEmpty()) {
            canActivateDoubleEngines = false;
            warningMessage.append("""
                No double engines available.
                You can use only single engine.""");
            if (card instanceof ClientFreeSpace) warningMessage.append("⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!");
        } else if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
            canActivateDoubleEngines = false;
            warningMessage.append("""
                No battery boxes available so you can't activate double engine.
                You can use only single engine.""");
            if (card instanceof ClientFreeSpace) warningMessage.append("⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!");
        } else if (!isThereAvailableBattery()) {
            canActivateDoubleEngines = false;
            warningMessage.append("""
                You ran out of batteries so you can't activate double engine.
                You can use only single engine.""");
            if (card instanceof ClientFreeSpace) warningMessage.append("⚠️ ATTENTION! If your ship doesn't have engine power, you will be eliminated!");
        }

        if (!canActivateDoubleEngines) {
            if (!clientModel.getOutPlayers().contains(clientController.getNickname())) {
                showInfoPopupWithCallback(warningMessage.toString(), card, () -> {
                    createAndAddButton("Continue", () -> {
                        Platform.runLater(() -> bottomHBox.getChildren().clear());
                        clientController.playerChoseDoubleEngines(
                                clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
                    });
                });
            } else
                showMessage("You don't have engine power, you've been eliminated!", true);
        } else {
            showChooseDoubleEngineMenu();
        }
    }

    public void showChooseDoubleEngineMenu() {
        updateSelectionMessage();
        setupControlButtons();
        highlightAvailableDoubleEngine();
    }

    private void setupControlButtons() {
        Platform.runLater(() -> {
            bottomHBox.getChildren().clear();

            createAndAddButton("Confirm Selection", this::handleConfirmSelection);

            createAndAddButton("Reset Selection", () -> {
                selectedDoubleEngines.clear();
                selectedBatteryBoxes.clear();
                boardsController.removeHighlightColor();
                updateSelectionMessage();
                highlightAvailableDoubleEngine();
            });

            createAndAddButton("Skip All", () -> {
                selectedDoubleEngines.clear();
                selectedBatteryBoxes.clear();
                clientController.playerChoseDoubleEngines(
                        clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());

                Platform.runLater(() -> bottomHBox.getChildren().clear());
                boardsController.removeHighlightColor();

                showWaitingMessage();
            });
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

    private boolean isSelectingDoubleEngine() {
        return selectedDoubleEngines.size() == selectedBatteryBoxes.size();
    }

    private boolean isSelectingBattery() {
        return selectedDoubleEngines.size() > selectedBatteryBoxes.size() || selectedDoubleCannons.size() > selectedBatteryBoxes.size();
    }

    private boolean isSelectingDoubleCannon() {
        return selectedBatteryBoxes.size() == selectedDoubleCannons.size();
    }

    private void updateSelectionMessage() {
        String message;
        if (isSelectingDoubleEngine()) {
            if (selectedDoubleEngines.isEmpty()) {
                message = "Select a double engine to activate (or skip all to use single engines only).";
            } else {
                message = "Selection complete! " + selectedDoubleEngines.size() + " engines paired with " +
                        selectedBatteryBoxes.size() + " batteries. Select another engine or confirm.";
            }
        } else if (isSelectingDoubleCannon()) {
            if (selectedDoubleCannons.isEmpty()) {
                message = "Select a double cannon to activate (or skip all to use single cannons only).";
            } else {
                message = "Selection complete! " + selectedDoubleCannons.size() + " cannons paired with " +
                        selectedBatteryBoxes.size() + " batteries. Select another cannon or confirm.";
            }
        } else if (isSelectingBattery()) {
            message = "Select a battery to activate the ship component you just chose.";
        } else {
            message = "Select components to activate double engines.";
        }

        showMessage(message, true);
    }

    private void highlightAvailableDoubleEngine() {
        boardsController.removeHighlightColor();
        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (Coordinates coords : selectedDoubleEngines) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        for (Coordinates coords : selectedBatteryBoxes) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        if (isSelectingDoubleEngine()) {
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

    private void highlightAvailableDoubleCannon() {
        boardsController.removeHighlightColor();
        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (Coordinates coords : selectedDoubleCannons) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        for (Coordinates coords : selectedBatteryBoxes) {
            boardsController.applyHighlightEffect(coords, Color.BLUE);
        }

        if (isSelectingDoubleCannon()) {
            Set<Coordinates> doubleCannonCoords = shipboard.getCoordinatesOfComponents(shipboard.getDoubleCannons());
            for (Coordinates coords : doubleCannonCoords) {
                if (!selectedDoubleCannons.contains(coords)) {
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

    private void highlightShields() {
        List<Shield> availableShields = clientModel.getMyShipboard().getShields();

        clientModel.getMyShipboard()
                .getCoordinatesOfComponents(availableShields)
                .stream()
                .filter(coords -> !selectedShield.contains(coords))
                .forEach(coordinates -> boardsController.applyHighlightEffect(coordinates, Color.GREEN));
    }

    private void highlightAvailableStorages() {

        Set<Coordinates> selectableStoragesCoords = storageManager.getSelectableCoordinates();

        selectableStoragesCoords.forEach(coords -> boardsController.applyHighlightEffect(coords, Color.GREEN));
    }

    private void handleDoubleEngineSelection(Coordinates coordinates) {
        if (!isSelectingDoubleEngine()) {
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
        highlightAvailableDoubleEngine();
    }

    private void handleBatteryBoxSelection(Coordinates coordinates) {

        ClientCard card = clientModel.getCurrAdventureCard();
        String componentToActivate = switch (card) {
            case ClientPirates clientPirates -> "cannon";
            case ClientFreeSpace clientFreeSpace -> "engine";
            case ClientSlaveTraders clientSlaveTraders -> "cannon";
            case null, default -> "incorrect component";
        };

        if (!isSelectingBattery()) {
            showMessage("You must first select a double " + componentToActivate + ".", false);
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

        showMessage("Battery selected! You can activate more " + componentToActivate + " or confirm your selection.", true);
        updateSelectionMessage();

        if (componentToActivate.equals("cannon"))
            highlightAvailableDoubleCannon();
        else if (componentToActivate.equals("engine"))
            highlightAvailableDoubleEngine();
        else
            System.err.println("Do not know what to activate " + componentToActivate);
    }

    // -------------------- EPIDEMIC ----------------------
    public void showEpidemicMenu() {
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientEpidemic epidemic)) {
            showMessage("Error: Expected Epidemic card", false);
            return;
        }

        if (!clientModel.isMyTurn())
            return;

        showMessage("An epidemic is spreading throughout the fleet!", true);
        showInfoPopupWithCallback("Each occupied cabin connected to another occupied cabin will lose one crew member. " +
                "Press confirm to see how epidemic is going to spread",
                epidemic,
                () -> clientController.spreadEpidemic(clientModel.getMyNickname())
        );

    }

    // ------------------------------------------

    private void initializeBeforeCard() {
        this.selectedDoubleEngines.clear();
        this.selectedBatteryBoxes.clear();
        this.selectedDoubleCannons.clear();
        this.selectedShield.clear();
        this.selectedCrewPerCabin.clear();
        hasChosenDoubleCannon = false;
        hasChosenDoubleEngine = false;
        hasChosenShield = false;
        if (boardsController != null) {
            boardsController.removeHighlightColor();
        }
        Platform.runLater(() -> bottomHBox.getChildren().clear());
    }

    private boolean isThereAvailableBattery() {
        List<BatteryBox> batteryBoxes = clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes();
        for (BatteryBox batteryBox : batteryBoxes) {
            if (batteryBox.getRemainingBatteries() >= 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onGridButtonClick(int row, int column) {
        if (!isPlayerTurnActive()) {
            showTurnMessage();
            return;
        }

        Coordinates coordinates = new Coordinates(row, column);
        switch (clientModel.getCurrCardState()) {
            case CardState.CHOOSE_ENGINES -> {
                if (isSelectingDoubleEngine()) {
                    handleDoubleEngineSelection(coordinates);
                } else if (isSelectingBattery()) {
                    handleBatteryBoxSelection(coordinates);
                }
            }
            case CardState.CHOOSE_CANNONS -> {
                if (isSelectingDoubleCannon()) {
                    handleDoubleCannonSelection(coordinates);
                } else {
                    handleBatteryBoxSelection(coordinates);
                }
            }
            case CardState.HANDLE_CUBES_REWARD ->
                handleChooseStorageSelection(coordinates);

            case CardState.HANDLE_CUBES_MALUS ->
                handleCubeMalusSelection(coordinates);

            case CardState.DANGEROUS_ATTACK -> {
                ClientDangerousObject dangerousObj = clientModel.getCurrDangerousObj();
                if (dangerousObj != null) {
                    String type = dangerousObj.getType();
                    if (hasChosenDoubleCannon || hasChosenShield)
                        handleSingleBatteryBox(coordinates);
                    else if (type.contains("small"))
                        handleSmallDanObj(coordinates);
                    else if (type.contains("bigMeteorite"))
                        handleBigMeteorite(coordinates);
                    else if (type.contains("bigShot"))
                        handleBigShot(coordinates);
                }
            }
            case CardState.CHECK_SHIPBOARD_AFTER_ATTACK -> {
                if (shipParts.isEmpty())
                    handeRepairShipboard(coordinates);
                else
                    handleShipParts(coordinates);
            }
            case CardState.VISIT_LOCATION -> showCanVisitLocationMenu();

            case CardState.REMOVE_CREW_MEMBERS -> handleCabinSelection(coordinates);

            case CardState.STARDUST -> showStardustMenu();

            case CardState.EPIDEMIC -> showEpidemicMenu();

            case CardState.ACCEPT_THE_REWARD -> showAcceptTheReward();

            default -> System.err.println("Unknown card state: " + clientModel.getCurrCardState());
        }
    }

    private void showAcceptTheReward() {

        ClientCard card = clientModel.getCurrAdventureCard();

        if (
                !(card instanceof ClientSlaveTraders ||
                card instanceof ClientPirates)
        ) {
            showMessage("Error: Expected StarDust card", false);
            return;
        }

        showMessage("Do you want to visit the location?", true);

        createAndAddButton("Accept the reward", () -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToAcceptTheReward(clientController.getNickname(), true);
        });

        createAndAddButton("Reject the reward", () -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToAcceptTheReward(clientController.getNickname(), false);
            showWaitingMessage();
        });

    }

    /**
     * Verifica se è il turno attivo del giocatore corrente basandosi sui controlli
     * implementati nel ClientController per mantenere coerenza logica.
     *
     * @return true se il giocatore può interagire, false altrimenti
     */
    private boolean isPlayerTurnActive() {
        if (!clientModel.isMyTurn()) {
            return false;
        }

        CardState currentState = clientModel.getCurrCardState();

        if (clientController.isStateRegardingCurrentPlayerOnly(currentState)) {
            return true;
        }

        // For states that may allow simultaneous actions or have special logic
            switch (currentState) {
            case CardState.START_CARD:
            case CardState.END_OF_CARD:
                return false;

            default:
                return true;
        }
    }

    /**
     * Mostra un messaggio appropriato quando non è il turno del giocatore,
     * utilizzando la stessa logica di messaggistica del ClientController
     */
    private void showTurnMessage() {
        CardState currentState = clientModel.getCurrCardState();
        String currentPlayer = clientModel.getCurrentPlayer();

        if (clientController.isStateRegardingCurrentPlayerOnly(currentState)) {
            if (currentPlayer != null && !currentPlayer.equals(clientModel.getMyNickname())) {
                showMessage(currentPlayer + " is currently playing. Soon will be your turn", true);
            } else {
                showMessage("Wait for " + currentPlayer + " to make his choice", true);
            }
        } else {
            if (currentPlayer != null && !currentPlayer.equals(clientModel.getMyNickname())) {
                showMessage("It's " + currentPlayer + "'s turn.", true);
            } else {
                showMessage("It's not your turn, wait before taking actions", true);
            }
        }
    }

    private void showWaitingMessage() {
        String currentPlayer = clientModel.getCurrentPlayer();
        if (currentPlayer != null && !currentPlayer.equals(clientModel.getMyNickname())) {
            showMessage("It's " + currentPlayer + "'s turn. Please wait...", true);
        } else {
            showMessage("Waiting for other players...", true);
        }
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

    private void handleShipParts(Coordinates coordinates) {
        IntStream.range(0, shipParts.size())
                .filter(i -> shipParts.get(i).contains(coordinates))
                .findFirst()
                .ifPresentOrElse(
                        i -> {
                            shipParts.clear();
                            this.boardsController.removeHighlightColor();
                            clientController.handleShipPartSelection(i + 1);
                        },
                        () -> showMessage("Invalid selection, try again", false)
                );
    }

    private void handeRepairShipboard(Coordinates coordinates) {
        if (clientModel.getMyShipboard().getIncorrectlyPositionedComponentsCoordinates().contains(coordinates)) {
            this.boardsController.removeHighlightColor();
            clientController.removeComponent(coordinates.getX(), coordinates.getY());
        } else
            showMessage("Remove one of the wrongly placed components", true);
    }

    private void handleSingleBatteryBox(Coordinates coordinates) {

        ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
        Component component = shipBoard.getComponentAt(coordinates);

        System.out.println(coordinates);

        if (component == null || !shipBoard.getBatteryBoxes().contains(component)) {
            showMessage("You did not select a battery box", false);
            return;
        }

        BatteryBox batteryBox = (BatteryBox) component;
        if (batteryBox.getRemainingBatteries() == 0) {
            showMessage("The selected battery box does not have enough batteries", false);
            return;
        }

        if (!selectedBatteryBoxes.isEmpty()) {
            showMessage("Battery box already selected", false);
            return;
        }

        selectedBatteryBoxes.add(coordinates);
        boardsController.removeHighlightColor();

        if (hasChosenShield) {
            showMessage("Defend method sent to server", false);
            clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes);
        } else if (hasChosenDoubleCannon)
            clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
    }

    private void handleBigShot(Coordinates coordinates) {
        // TODO
    }

    private void handleBigMeteorite(Coordinates coordinates) {
        ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
        Component component = shipBoard.getComponentAt(coordinates);

        if (component == null || !shipBoard.getDoubleCannons().contains(component)) {
            showMessage("You did not select a double cannon", false);
            return;
        }

        hasChosenDoubleCannon = true;
        boardsController.removeHighlightColor();
        highlightBatteryBoxes();
        Platform.runLater(() -> bottomHBox.getChildren().clear());
        selectedDoubleCannons.add(coordinates);
        showMessage("Now select a battery box for the double cannon you selected", true);
    }

    private void handleSmallDanObj(Coordinates coordinates) {
        ShipBoardClient shipBoard = clientModel.getShipboardOf(clientModel.getMyNickname());
        Component component = shipBoard.getComponentAt(coordinates);

        if (component == null || !shipBoard.getShields().contains(component)) {
            showMessage("You did not select a shield", false);
            return;
        }

        hasChosenShield = true;
        boardsController.removeHighlightColor();
        highlightBatteryBoxes();
        Platform.runLater(() -> bottomHBox.getChildren().clear());
        selectedShield.add(coordinates);
        showMessage("Now select a battery box for the shield you selected", true);
    }

    private void handleChooseStorageSelection(Coordinates coordinates) {

        Set<Coordinates> selectableStoragesCoords = storageManager.getSelectableCoordinates();

        if (!selectableStoragesCoords.contains(coordinates)) {
            showMessage("Your selection is not correct, select another one or skip the current reward.", false);
            return;
        }

        storageManager.addStorageSelectionWithCopy(coordinates);
        clientModel.refreshShipBoardOf(clientController.getNickname());
        boardsController.removeHighlightColor();
        Platform.runLater(() -> bottomHBox.getChildren().clear());

        showMessage("", true);

        storageSelectionPhase();
    }

    public void showHandleCubesRewardMenu() {

        List<CargoCube> cubesReward = null;

        try {
            cubesReward = clientModel.extractCubeRewardsFromCurrentCard();
        } catch (IllegalStateException e) {
            showMessage(e.getMessage(), false);
            // TODO che si fa?
        }

        // Initialize the storage manager with the cube rewards
        storageManager = new StorageSelectionManager(cubesReward, 0, clientModel.getMyShipboard());

        // Check if the player has any storage available
        if (!storageManager.hasAnyStorage()) {
//            showMessage("No available storages on your ship. You cannot accept any reward.", false);
            showInfoPopupWithCallback("No available storages on your ship. You cannot accept any reward.", clientModel.getCurrAdventureCard(),
                    () -> createAndAddButton("Continue", () -> {
                        Platform.runLater(() -> bottomHBox.getChildren().clear());
                        List<Coordinates> emptyList = new ArrayList<>();
                        clientController.playerChoseStorage(clientController.getNickname(), emptyList);
                    }));
            return;
        }

        // display reward info
        ClientCard currCard = clientModel.getCurrAdventureCard();
        if (currCard instanceof ClientPlanets)
            showMessage("You have chosen planet " + planetChoice + " look at your rewards!!!", true);

        else if (currCard instanceof ClientAbandonedStation)
            showMessage("You’ve landed on the cargo. Enjoy your rewards!!!", true);
        else
            showMessage("You have accepted the reward, look at it!!!", true);

        storageSelectionPhase();

    }

    private void storageSelectionPhase() {

        CargoCube currentCube = storageManager.getCurrentCube();

        // if every cube has been handled
        if (currentCube == null) {
            showMessage("You have placed every cube, enjoy your richness.", false);
            clientController.playerChoseStorage(clientModel.getMyNickname(), storageManager.getSelectedStorageCoordinates());
            return;
        }

        // if you cannot accept the red cube
        if (!storageManager.canAcceptCurrentCube()) {
            showMessage("Skipping storage selection for " + currentCube + " cube, you are not provided with advanced technology yet", false);
            if (!storageManager.skipCurrentCube())
                clientController.playerChoseStorage(clientModel.getMyNickname(), storageManager.getSelectedStorageCoordinates());
            else
                storageSelectionPhase();
            return;
        }

        createAndAddButton("Skip " + currentCube + " cube", () -> {
            bottomHBox.getChildren().clear();
            boardsController.removeHighlightColor();
            if (!storageManager.skipCurrentCube())
                clientController.playerChoseStorage(clientModel.getMyNickname(), storageManager.getSelectedStorageCoordinates());
            else
                storageSelectionPhase();
        });

        highlightAvailableStorages();

        showMessage("Now selecting a storage for the " + currentCube + " cube. " +
                "If a storage is full, the least valuable cube will be removed", true);

    }

    public void showHandleSmallDanObjMenu() {

        initializeBeforeCard();

        ClientDangerousObject smallObject = clientModel.getCurrDangerousObj();
        showInfoPopupWithCallback("SMALL OBJECT INCOMING FROM " + smallObject.getDirection() + " AT " + (smallObject.getCoordinate() + 1),
                clientModel.getCurrAdventureCard(),
                () -> {
                    if (clientModel.getShipboardOf(clientController.getNickname()).getShields().isEmpty() ) {
                        showInfoPopupWithCallback("You have no available shields on your ship, you cannot defend",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes));
                        return;
                    }

                    if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
                        showInfoPopupWithCallback("No batteries available, you will only defend your ship with single ones...",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes));
                        return;
                    }

                    //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
                    if (!isThereAvailableBattery()) {
                        showInfoPopupWithCallback("No batteries available, you will only defend your ship with single ones...",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes));
                        return;
                    }

                    showMessage("You can activate a shield or let the object hit your ship (reminder " + smallObject.getDirection() + " " + (smallObject.getCoordinate() + 1) + ")", true);
                    highlightShields();
                    createAndAddButton("Skip shield selection", () -> {
                        boardsController.removeHighlightColor();
                        bottomHBox.getChildren().clear();
                        clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes);
                    });
                });

    }

    public void showBigMeteoriteMenu() {

        initializeBeforeCard();

        ClientDangerousObject bigMeteorite = clientModel.getCurrDangerousObj();
        showInfoPopupWithCallback("BIG METEORITE INCOMING FROM " + bigMeteorite.getDirection() + " AT " + (bigMeteorite.getCoordinate() + 1),
                clientModel.getCurrAdventureCard(),
                () -> {
                    if (clientModel.getShipboardOf(clientController.getNickname()).getDoubleCannons().isEmpty()) {
                        showInfoPopupWithCallback("No double cannons available, you will only defend your ship with single ones...",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes));
                        return;
                    }

                    if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
                        showInfoPopupWithCallback("No batteries available, you will only defend your ship with single ones...",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes));

                        return;
                    }

                    if (!isThereAvailableBattery()) {
                        showInfoPopupWithCallback("No batteries available, you will only defend your ship with single ones...",
                                clientModel.getCurrAdventureCard(),
                                () -> clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes));
                        return;
                    }

                    showMessage("You can use a double or single cannon to destroy it or let it hit your ship (reminder " + bigMeteorite.getDirection() + " " + (bigMeteorite.getCoordinate() + 1) + ")", true);

                    highlightDoubleCannon();
                    createAndAddButton("Skip double engine selection",
                            () -> {
                        boardsController.removeHighlightColor();
                        bottomHBox.getChildren().clear();
                        clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
                    });
                });
    }

    private void showOverlayPopup(String title, String message, @NotNull Runnable onClose) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        // popup container
        VBox popupContent = new VBox();
        popupContent.getStyleClass().add("popup-container");

        // title
        if (title != null && !title.trim().isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("popup-title");
            titleLabel.setWrapText(true);
            popupContent.getChildren().add(titleLabel);
        }

        // message
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-message");
        messageLabel.setWrapText(true);
        popupContent.getChildren().add(messageLabel);

        // button container
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);
        buttonContainer.setSpacing(15);

        // confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().addAll("popup-button", "confirm-button");
        confirmButton.setOnAction(e -> {
            centerStackPane.getChildren().remove(overlay);
            onClose.run();
        });

        buttonContainer.getChildren().add(confirmButton);
        popupContent.getChildren().add(buttonContainer);

        overlay.getChildren().add(popupContent);

        // Close
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                centerStackPane.getChildren().remove(overlay);
                showMessage("Operation cancelled.", false); // Feedback opzionale
            }
        });

        Platform.runLater(() -> {
            centerStackPane.getChildren().add(overlay);
            overlay.toFront();
        });
    }

    private void showInfoPopupWithCallback(String message, ClientCard card, Runnable onClose) {
        String title = null;
        if (card != null) {
            title = card.getCardType();
        }
        showOverlayPopup(title, message, onClose);
    }

    public void checkShipBoardAfterAttackMenu() {
        if (hitComponent != null) {
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
            hitComponent = null;
        } else {
            showInfoPopupWithCallback("""
                    Your ship was not hit, now wait for the others to repair their ship""",
                    clientModel.getCurrAdventureCard(),
                    () -> clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent)
            );
        }
    }

    public void showComponentHitInfo(Coordinates coordinates) {
        this.hitComponent = coordinates;
        boardsController.removeHighlightColor();
        showInfoPopupWithCallback("""
                YOUR SHIP WAS HIT!!!
                REPAIR IT SO YOU CAN CONTINUE YOUR FLIGHT.""",
                clientModel.getCurrAdventureCard(),
                () -> {});
    }

    public void showInvalidComponents() {
        ShipBoardClient shipBoard = clientModel.getMyShipboard();
        shipBoard.getIncorrectlyPositionedComponentsCoordinates()
                .forEach(coordinate -> this.boardsController.applyHighlightEffect(coordinate, Color.RED));
    }

    public void showShipParts(List<Set<Coordinates>> shipParts) {
        List<Color> colors = List.of(Color.RED, Color.ORANGE, Color.BLUE, Color.YELLOW);
        Map<Color, Set<Coordinates>> shipPartsMap = new HashMap<>();
        this.shipParts.addAll(shipParts);

        for (int i = 0; i < shipParts.size(); i++) {
            shipPartsMap.put(colors.get(i), shipParts.get(i));
        }

        shipPartsMap.forEach((color, coordinates) ->
                coordinates.forEach(coordinate ->
                        this.boardsController.applyHighlightEffect(coordinate, color)
                )
        );
    }

    public void showBigShotMenu() {
        ClientDangerousObject dangerousObj = clientModel.getCurrDangerousObj();
        showInfoPopupWithCallback(String.format("""
                        Big Shot incoming! Nothing can stop this...
                        Ballistic info: %s %d""", dangerousObj.getDirection(), dangerousObj.getCoordinate() + 1),
                clientModel.getCurrAdventureCard(),
                () -> clientController.playerHandleBigShot(clientModel.getMyNickname()));
    }

    public void showHandleCubesMalusMenu() {

        if (!clientModel.isMyTurn())
            return;

        initializeBeforeCard();

        ClientCard currCard = clientModel.getCurrAdventureCard();
        int cubeMalus;

        // Extract the malus from the current card
        if (currCard.getCardName().equals("WarField")) {
            ClientWarField card = (ClientWarField) currCard;
            cubeMalus = card.getCubeMalus();
        } else if (currCard.getCardName().equals("Smugglers")) {
            ClientSmugglers card = (ClientSmugglers) currCard;
            cubeMalus = card.getCubeMalus();
        } else {
            System.err.println("Unknown card type for cube malus");
            return;
        }

        storageManager = new StorageSelectionManager(new ArrayList<>(), cubeMalus, clientModel.getMyShipboard());

        ShipBoardClient shipBoard = clientModel.getMyShipboard();

        List<CargoCube> allCubes = shipBoard.getCargoCubes();
        remainingCubesToRemove = cubeMalus;

        if (allCubes.size() == cubeMalus) {
            automaticCubeRemove(storageManager.mostPreciousCube(), 0);
        } else if (allCubes.size() > cubeMalus) {
            mostPreciousCube.addAll(storageManager.mostPreciousCube());
            showMessage("You must remove " + cubeMalus + " cargo cubes! Removing the most precious ones first.", true);
            highlightMostPreciousCubes();
        } else {
            automaticCubeRemove(storageManager.mostPreciousCube(), cubeMalus - allCubes.size());
        }
    }

    private void automaticCubeRemove(List<Coordinates> coordinates, int batteriesToRemove) {

        int totalBatteries = clientModel.getMyShipboard()
                .getBatteryBoxes()
                .stream()
                .mapToInt(BatteryBox::getRemainingBatteries)
                .sum();

        if (batteriesToRemove == 0)
            showInfoPopupWithCallback("""
                The number of cube you need to remove equals the number of cube you have.
                You will give back every cube you own automatically""",
                clientModel.getCurrAdventureCard(),
                () -> clientController.playerChoseStorageAndBattery(clientController.getNickname(), coordinates, selectedBatteryBoxes));
        else if (totalBatteries > batteriesToRemove)
            showInfoPopupWithCallback("""
                The number of cube you need to remove is less than the cubes you own.
                You will give back every cube you have and you will choose the batteries to remove.""",
                clientModel.getCurrAdventureCard(),
                () -> {
                    highlightBatteryBoxes();
                    selectedStorage.addAll(coordinates);
                    isRemovingBatteries = true;
                });
        else
            showInfoPopupWithCallback("""
                The number of cube and batteries you need to remove is less than the cubes and batteries you own.
                You will lose every cube or battery you own.""",
                clientModel.getCurrAdventureCard(),
                () -> clientController.playerChoseStorageAndBattery(clientController.getNickname(), coordinates, getAllAvailableBatteriesCoordinates()));
    }

    private void handleCubeMalusSelection(Coordinates coordinates) {
        if (isRemovingBatteries) {
            handleBatteryRemovalSelection(coordinates);
        } else {
            handleCubeRemovalSelection(coordinates);
        }
    }

    private void handleCubeRemovalSelection(Coordinates coordinates) {
        // Check if the coordinates are in the list of the most valuable cubes
        if (!mostPreciousCube.contains(coordinates)) {
            showMessage("You must select one of the highlighted storages containing the most precious cubes.", false);
            return;
        }

        selectedStorage.add(coordinates);
        mostPreciousCube.remove(coordinates);
        remainingCubesToRemove--;

        showMessage("Cube removed. Remaining cubes to remove: " + remainingCubesToRemove, false);

        // Update highlighting
        boardsController.removeHighlightColor();

        if (remainingCubesToRemove > 0) {
            highlightMostPreciousCubes();
        } else {
            handleCubeMalusConfirmation();
        }
    }

    private void handleBatteryRemovalSelection(Coordinates coordinates) {
        ShipBoardClient shipboard = clientModel.getMyShipboard();
        Component component = shipboard.getComponentAt(coordinates);

        if (!(component instanceof BatteryBox batteryBox)) {
            showMessage("You must select a battery box.", false);
            return;
        }

        if (batteryBox.getRemainingBatteries() <= 0) {
            showMessage("This battery box has no batteries left.", false);
            return;
        }

        if (Collections.frequency(selectedBatteryBoxes, coordinates) == batteryBox.getRemainingBatteries()) {
            showMessage("This battery box has no batteries left.", false);
            return;
        }

        selectedBatteryBoxes.add(coordinates);
        remainingCubesToRemove--;

        showMessage("Battery removed. Remaining batteries to remove: " + remainingCubesToRemove, false);

        // Update highlighting
        boardsController.removeHighlightColor();

        if (remainingCubesToRemove > 0) {
            highlightBatteryBoxes();
        } else {
            boardsController.removeHighlightColor();
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            isRemovingBatteries = false;
            clientController.playerChoseStorageAndBattery(clientModel.getMyNickname(), selectedStorage, selectedBatteryBoxes);
            selectedStorage.clear();
            selectedBatteryBoxes.clear();
        }
    }

    private void handleCubeMalusConfirmation() {
        if (remainingCubesToRemove > 0) {
            showMessage("You still need to remove " + remainingCubesToRemove + " more items.", false);
            return;
        }

        //sending the selection to the server
        Platform.runLater(() -> bottomHBox.getChildren().clear());
        boardsController.removeHighlightColor();

        clientController.playerChoseStorageAndBattery(clientController.getNickname(), selectedStorage, selectedBatteryBoxes);

        selectedStorage.clear();
        mostPreciousCube.clear();
        isRemovingBatteries = false;
        remainingCubesToRemove = 0;

        showMessage("Cube malus applied successfully!", false);
    }


    private void highlightMostPreciousCubes() {
        boardsController.removeHighlightColor();

        for (Coordinates coords : mostPreciousCube) {
            boardsController.applyHighlightEffect(coords, Color.RED);
        }
    }

    /**
     * Mostra un overlay informativo con il contenuto di una batteria box colorata
     */
    private void showBatteryBoxesWithColor() {
        boardsController.removeHighlightColor();

        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (BatteryBox batteryBox : shipboard.getBatteryBoxes()) {
            if (batteryBox.getRemainingBatteries() > 0) {
                Set<Coordinates> coords = shipboard.getCoordinatesOfComponents(List.of(batteryBox));
                for (Coordinates coord : coords) {
                    boardsController.applyHighlightEffect(coord, Color.PURPLE);
                }
            }
        }

        showMessage("Select a battery box to remove a battery from:", true);
    }

    /**
     * Restituisce una lista con le coordinate delle battery box che hanno almeno una batteria.
     * Ogni coordinata appare tante volte quante sono le batterie disponibili in quella battery box.
     *
     * @return Lista di coordinate con ripetizioni basate sul numero di batterie
     */
    private List<Coordinates> getAllAvailableBatteriesCoordinates() {
        List<Coordinates> batteriesCoordinates = new ArrayList<>();
        ShipBoardClient shipboard = clientModel.getMyShipboard();

        for (BatteryBox batteryBox : shipboard.getBatteryBoxes()) {
            if (batteryBox.getRemainingBatteries() > 0) {
                Set<Coordinates> coords = shipboard.getCoordinatesOfComponents(new ArrayList<>(List.of(batteryBox)));

                for (Coordinates coord : coords) {
                    for (int i = 0; i < batteryBox.getRemainingBatteries(); i++) {
                        batteriesCoordinates.add(coord);
                    }
                }
            }
        }

        return batteriesCoordinates;
    }

    public void showCrewMembersInfo() {
        showInfoPopupWithCallback("""
                The number of your crew member will be evaluated.
                If the result is poor, you will be punished""",
                clientModel.getCurrAdventureCard(),
                () -> clientController.evaluatedCrewMembers());

    }

    public void showDisconnectMessage(String message) {
        showOverlayPopup("warning message", message,
                () -> System.exit(0));
    }


    private void createOtherPlayersDisplay(String nickname) {
        String warningMessage = String.format("""
                📢 FLIGHT ANNOUNCEMENT 📢
                %s has abandoned the race!
                Their ship has made an early landing.
                From the next card, they will no longer participate in adventures.
                """, nickname);

        showOverlayPopup("warning message", warningMessage, () -> {});
    }

    public void showPlayerLanded() {
        String warningMessage = ("""
        🛬 EARLY LANDING 🛬
        Your ship marker has been removed from the flight board!
        You have abandoned the space race and landed safely.
        You can continue to follow the game as a spectator until the end.
        
        You can still win if you have accumulated enough credits!
        Continue watching the game and see how it ends!""");

        showOverlayPopup("warning Message", warningMessage, () ->
                Platform.runLater(() -> landButton.setVisible(false))
        );
    }
}
