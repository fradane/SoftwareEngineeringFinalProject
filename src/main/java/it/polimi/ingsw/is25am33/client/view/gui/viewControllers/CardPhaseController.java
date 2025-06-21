package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.model.card.*;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.client.view.tui.StorageSelectionManager;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Pirates;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.card.Planets;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
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

import javax.smartcardio.Card;
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

    // TODO fare per il livello 1
    private Level2BoardsController boardsController;
    private ModelFxAdapter modelFxAdapter;
    private final List<Coordinates> selectedDoubleEngines = new ArrayList<>();
    private final List<Coordinates> selectedDoubleCannons = new ArrayList<>();
    private final List<Coordinates> selectedBatteryBoxes = new ArrayList<>();
    private StorageSelectionManager storageManager;
    private int planetChoice;
    private boolean hasChosenDoubleEngine = false;
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

        modelFxAdapter = new ModelFxAdapter(clientModel, true, boardsController);

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

        showMessage("Stardust has been detected in your flight path!", true);
        int exposedConnector = clientModel.getMyShipboard().countExposed();
        if (exposedConnector > 0) {
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
            showMessage(String.format("WARNING: You only have %d crew members, you cannot accept the reward", totalCrew), false);
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


    private void showCanVisitLocationMenu(){
        showMessage("Do you want to visit the abandoned ship?", true);
        Button continueButton = new Button("Continue");
        Button skipButton = new Button("Skip");
        continueButton.getStyleClass().add("action-button");
        skipButton.getStyleClass().add("action-button");
        continueButton.setOnAction(_ -> {
            clientController.playerWantsToVisitLocation(clientController.getNickname(), true);
            // TODO finire con la rimozione dei crew member
        });
        skipButton.setOnAction(_ -> {
            clientController.playerWantsToVisitLocation(clientController.getNickname(), false);
        });


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
            clientController.playerChoseDoubleCannons(clientModel.getMyNickname(), new ArrayList<>(), new ArrayList<>());
            return;
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
            clientController.playerChoseDoubleEngines(clientModel.getMyNickname(), selectedDoubleCannons, selectedBatteryBoxes);
        });

        hasChosenDoubleCannon = false;
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
        hasChosenDoubleEngine = true;
        showChooseBatteryBoxMenu();
    }

    public void showRewardMenu(){
        showMessage("Well done, you beat them!", true);

        ClientCard card = clientModel.getCurrAdventureCard();
        // TODO finire


    }





    //-------------------- FREE SPACE ----------------------

    public void showFreeSpaceMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientFreeSpace freeSpace)) {
            showMessage("Error: Expected FreeSpace card", false);
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

    private void highlightAvailableStorages() {

        Set<Coordinates> selectableStoragesCoords = storageManager.getSelectableCoordinates();

        selectableStoragesCoords.forEach(coords -> boardsController.applyHighlightEffect(coords, Color.GREEN));
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
            case CardState.CHOOSE_CANNONS -> {
                if (hasChosenDoubleCannon)
                    handleDoubleCannonSelection(coordinates);
                else
                    handleBatteryBoxSelection(coordinates);
            }
            case HANDLE_CUBES_REWARD ->
                handleChooseStorageSelection(coordinates);


            // TODO aggiungere stati

            default -> System.err.println("Unknown card state: " + clientModel.getCurrCardState());
        }

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


}
