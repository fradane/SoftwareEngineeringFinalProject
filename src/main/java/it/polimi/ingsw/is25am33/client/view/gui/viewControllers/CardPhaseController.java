package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.client.view.tui.StorageSelectionManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.DoubleCannon;
import it.polimi.ingsw.is25am33.model.component.DoubleEngine;
import it.polimi.ingsw.is25am33.model.card.Planets;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.BigMeteorite;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.smartcardio.Card;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.*;
import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.CHOOSE_CANNONS_SELECT_BATTERY;
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

        // Refresh iniziale
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
                                cardImageView.setImage(cardImage);
                            } else {
                                cardImageView.setImage(null);
                            }

                            System.err.println("Updated: " + cardImageView.getImage());

                        }));

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
                Button planetButton = new Button();
                planetButton.setText("Planet " + (i + 1));
                planetButton.getStyleClass().add("action-button");

                // Set button action
                final int planetIndex = i + 1; // Store planet index (1-based)
                planetButton.setOnAction(_ -> {
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

                Platform.runLater(() -> bottomHBox.getChildren().add(planetButton));
            }
        }

        // Add skip button
        Button skipButton = new Button("Skip (don't land on any planet)");
        skipButton.getStyleClass().add("action-button");
        skipButton.setOnAction(_ -> {
            Platform.runLater(() -> {
                showMessage("You won't land on any planet", false);
                bottomHBox.getChildren().clear();
            });
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
                    You won't lose any flight days""");
        }

        showInfoPopupWithCallback(warningMessage, starDust, () ->{
                Button continueButton = new Button("Continue");
                continueButton.getStyleClass().add("action-button");
                continueButton.setOnAction(_ -> {
                    Platform.runLater(() -> bottomHBox.getChildren().clear());
                    clientController.spreadEpidemic(clientModel.getMyNickname());
                });
            Platform.runLater(() -> bottomHBox.getChildren().add(continueButton));
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
                    requiredCrew + " to visit this ship. You cannot accept the reward.", false);
            showCannotVisitLocationMenu();
        } else {
            showCanVisitLocationMenu();
        }
    }

    private void showCannotVisitLocationMenu() {
        showMessage("You cannot visit this location!", true);

        Button continueButton = new Button("Continue");
        continueButton.getStyleClass().add("action-button");
        continueButton.setOnAction(_ -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
            showWaitingMessage();
        });

        Platform.runLater(() -> bottomHBox.getChildren().add(continueButton));
    }

    private void showCanVisitLocationMenu() {
        showMessage("Do you want to visit the abandoned ship?", true);

        Button visitButton = new Button("Visit Ship");
        Button skipButton = new Button("Skip");
        visitButton.getStyleClass().add("action-button");
        skipButton.getStyleClass().add("action-button");

        visitButton.setOnAction(_ -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), true);
        });

        skipButton.setOnAction(_ -> {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
            showWaitingMessage();
        });

        Platform.runLater(() -> {
            bottomHBox.getChildren().add(visitButton);
            bottomHBox.getChildren().add(skipButton);
        });
    }

    public void showChooseCabinMenu() {
        CrewMalusCard card = (CrewMalusCard) clientModel.getCurrAdventureCard();
        int crewToRemove = card.getCrewMalus();

        selectedCrewPerCabin.clear();

        showMessage("You need to remove " + crewToRemove + " crew member(s). Warning: if you run out of humans, you'll be eliminated.", true);

        Map<Coordinates, Cabin> cabinsWithCrew = clientModel.getMyShipboard().getCoordinatesAndCabinsWithCrew();

        // Non si dovrebbe mai arrivare qui
        if (cabinsWithCrew.isEmpty()) {
            showMessage("ILLEGAL STATE: You have no occupied cabins. You cannot sacrifice crew members.", false);
            return;
        }

        Button confirmButton = new Button("Confirm Selection");
        Button resetButton = new Button("Reset All");

        confirmButton.getStyleClass().add("action-button");
        resetButton.getStyleClass().add("action-button");

        confirmButton.setOnAction(_ -> handleCabinConfirmation(crewToRemove));
        resetButton.setOnAction(_ -> {
            selectedCrewPerCabin.clear();
            updateCabinSelectionMessage(crewToRemove);
            highlightCabinsWithSelection();
            showMessage("Selection reset. Start selecting crew members again.", false);
        });

        Platform.runLater(() -> {
            bottomHBox.getChildren().clear();
            bottomHBox.getChildren().addAll(confirmButton, resetButton);
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

        // Converti la selezione nel formato richiesto dal server
        List<Coordinates> cabinSelectionForServer = convertSelectionForServer();

        // Invia la selezione al server
        boolean success = clientController.playerChoseCabins(
                clientController.getNickname(),
                cabinSelectionForServer
        );

        if (success) {
            Platform.runLater(() -> bottomHBox.getChildren().clear());
            boardsController.removeHighlightColor();
            selectedCrewPerCabin.clear();
            showMessage("Crew members removed successfully!", true);
            showWaitingMessage();
            showAbandonedShipReward();
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

            // Aggiungi le coordinate tante volte quanti crew members sono selezionati
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
                // Cabina non selezionata - evidenzia in giallo (selezionabile)
                highlightColor = Color.YELLOW;
            } else if (selectedCrew == maxCrew) {
                // Tutti i crew members selezionati - evidenzia in rosso (prossimo click deseleziona)
                highlightColor = Color.RED;
            } else {
                // Parzialmente selezionata - evidenzia in verde (prossimo click aggiunge)
                highlightColor = Color.GREEN;
            }

            boardsController.applyHighlightEffect(coords, highlightColor);
        }
    }

    private void showAbandonedShipReward() {
        ClientAbandonedShip abandonedShip = (ClientAbandonedShip) clientModel.getCurrAdventureCard();

        String message = "Great! You've successfully explored the abandoned ship and received " +
                abandonedShip.getReward() + " credits!";
        showInfoPopup(message, abandonedShip);

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
            showInfoPopup("""
                    No double cannons available.
                    You can use only single engine.
                    """, pirates);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        if (clientModel.getMyShipboard().getBatteryBoxes().isEmpty()){
            showInfoPopup("""
                    No battery boxes available so you can't activate double cannon
                    You can use only single cannon
                    """, pirates);
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
        if(!isThereAvailableBattery()) {
            showInfoPopup("""
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

    public void showThrowDicesMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (card.getCardType().equals("Pirates") || card.getCardType().equals("SlaveTraders")) {
            showMessage("\nThe enemies are firing at you!", false);
        } else if (card.getCardType().equals("MeteoriteStorm")) {
            showMessage("\nMeteors are heading your way!", false);
        } else {
            System.err.println("Invalid card type!");
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

                showWaitingMessage();
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

    // -------------------- EPIDEMIC ----------------------
    public void showEpidemicMenu(){
        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientEpidemic epidemic)) {
            showMessage("Error: Expected Epidemic card", false);
            return;
        }

        showMessage("An epidemic is spreading throughout the fleet!", true);
        showInfoPopupWithCallback("Each occupied cabin connected to another occupied cabin will lose one crew member. " +
                "Press confirm to see how epidemic is going to spread", epidemic , () -> {
                    Button continueButton = new Button("Continue");
                    continueButton.getStyleClass().add("action-button");
                    continueButton.setOnAction(_ -> {
                        Platform.runLater(() -> bottomHBox.getChildren().clear());
                        clientController.spreadEpidemic(clientModel.getMyNickname());
                    });
                    Platform.runLater(() -> bottomHBox.getChildren().add(continueButton));
                });

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
            case CardState.HANDLE_CUBES_REWARD ->
                handleChooseStorageSelection(coordinates);

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
            case CardState.VISIT_LOCATION ->
                showCanVisitLocationMenu();

            case CardState.REMOVE_CREW_MEMBERS ->
                    handleCabinSelection(coordinates);

            case CardState.STARDUST ->
                showStardustMenu();

            case CardState.EPIDEMIC ->
                showEpidemicMenu();

            default -> System.err.println("Unknown card state: " + clientModel.getCurrCardState());
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

    private void showInfoPopup(String message, ClientCard card) {
        showOverlayPopup(card.getCardType(), message, null);
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

        //se quel box non contiene batterie non posso selezionarlo
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
            showMessage("No available storages on your ship. You cannot accept any reward.", false);
            // TODO aggiungere il popup di ali con il bottone per confermare
            List<Coordinates> emptyList = new ArrayList<>();
            clientController.playerChoseStorage(clientController.getNickname(), emptyList);
            return;
        }

        // display reward info
        ClientCard currCard = clientModel.getCurrAdventureCard();
        if (currCard instanceof ClientPlanets)
            showMessage("You have chosen planet " + planetChoice + " look at your rewards!!!", true);
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

        Button skipCubeButton = new Button("Skip " + currentCube + " cube");
        skipCubeButton.getStyleClass().add("action-button");
        skipCubeButton.setOnAction(_ -> {
            bottomHBox.getChildren().clear();
            boardsController.removeHighlightColor();
            if (!storageManager.skipCurrentCube())
                clientController.playerChoseStorage(clientModel.getMyNickname(), storageManager.getSelectedStorageCoordinates());
            else
                storageSelectionPhase();
        });

        Platform.runLater(() -> {
            bottomHBox.getChildren().clear();
            bottomHBox.getChildren().add(skipCubeButton);
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
                        showInfoPopup("You have no available shields on your ship, you cannot defend", clientModel.getCurrAdventureCard());
                        clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes);
                        return;
                    }

                    if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
                        showInfoPopup("No batteries available, you will only defend your ship with single ones...", clientModel.getCurrAdventureCard());
                        clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes);
                        return;
                    }

                    //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
                    if (!isThereAvailableBattery()) {
                        showInfoPopup("No batteries available, you will only defend your ship with single ones...", clientModel.getCurrAdventureCard());
                        clientController.playerHandleSmallDanObj(clientModel.getMyNickname(), selectedShield, selectedBatteryBoxes);
                        return;
                    }

                    showMessage("You can activate a shield or let the object hit your ship (reminder " + smallObject.getDirection() + " " + (smallObject.getCoordinate() + 1) + ")", true);
                    highlightShields();
                    Button letItHitShipBoardButton = new Button("Skip shield selection");
                    letItHitShipBoardButton.getStyleClass().add("action-button");
                    Platform.runLater(() -> bottomHBox.getChildren().add(letItHitShipBoardButton));
                    letItHitShipBoardButton.setOnAction(_ -> {
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
                        showInfoPopup("No double cannons available, you will only defend your ship with single ones...", clientModel.getCurrAdventureCard());
                        clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
                        return;
                    }

                    if (clientModel.getShipboardOf(clientController.getNickname()).getBatteryBoxes().isEmpty()) {
                        showInfoPopup("No batteries available, you will only defend your ship with single ones...", clientModel.getCurrAdventureCard());
                        clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
                        return;
                    }

                    //se non ci sono batterie disponibili nei box allora non puoi attivare i doppi cannoni
                    if (!isThereAvailableBattery()) {
                        showInfoPopup("No batteries available, you will only defend your ship with single ones...", clientModel.getCurrAdventureCard());
                        clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
                        return;
                    }

                    showMessage("You can use a double or single cannon to destroy it or let it hit your ship (reminder " + bigMeteorite.getDirection() + " " + (bigMeteorite.getCoordinate() + 1) + ")", true);

                    highlightDoubleCannon();
                    Button letItHitShipBoardButton = new Button("Skip double engine selection");
                    letItHitShipBoardButton.getStyleClass().add("action-button");
                    Platform.runLater(() -> bottomHBox.getChildren().add(letItHitShipBoardButton));
                    letItHitShipBoardButton.setOnAction(_ -> {
                        bottomHBox.getChildren().clear();
                        clientController.playerHandleBigMeteorite(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
                    });
                });
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
//            if (onClose == null) {
//                PauseTransition delay = new PauseTransition(Duration.seconds(2));
//                delay.setOnFinished(_ -> {
//                    centerStackPane.getChildren().remove(overlay);
//                });
//                delay.play();
//            }
        });
    }

    private void showInfoPopupWithCallback(String message, ClientCard card, Runnable onClose) {
        showOverlayPopup(card.getCardType(), message, onClose);
    }

    public void checkShipBoardAfterAttackMenu() {
        if (hitComponent != null) {
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
            hitComponent = null;
        } else {
            showMessage("Your ship was not hit, now wait for the others to repair their ship", true);
            clientController.startCheckShipBoardAfterAttack(clientModel.getMyNickname(), hitComponent);
        }
    }

    public void showComponentHitInfo(Coordinates coordinates) {
        this.hitComponent = coordinates;
        boardsController.removeHighlightColor();
        showInfoPopup("""
                YOUR SHIP WAS HIT!!!
                REPAIR IT SO YOU CAN CONTINUE YOUR FLIGHT.""",
                clientModel.getCurrAdventureCard());
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
}
