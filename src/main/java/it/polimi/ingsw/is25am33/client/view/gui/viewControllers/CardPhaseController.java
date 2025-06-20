package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.client.view.tui.ClientState;
import it.polimi.ingsw.is25am33.model.card.Planet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Objects;

import static it.polimi.ingsw.is25am33.client.view.tui.ClientState.WAIT_PLAYER;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.*;
import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.STANDARD;

public class CardPhaseController extends GuiController implements BoardsEventHandler{


    @FXML public StackPane centerStackPane;
    @FXML public Label messageLabel;
    @FXML public ImageView cardImageView;
    @FXML public HBox bottomHBox;

    private Level2BoardsController boardsController;
    private ModelFxAdapter modelFxAdapter;

    @Override
    public void onGridButtonClick(int row, int column) {

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

    public void showChoosePlanetMenu() {

        ClientCard card = clientModel.getCurrAdventureCard();
        if (!(card instanceof ClientPlanets planets)) {
            showMessage("Error: Expected Planets card", false);
            return;
        }

        // Clear previous buttons
        bottomHBox.getChildren().clear();

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
            try {
                clientController.playerWantsToVisitPlanet(clientController.getNickname(), 0);
            } catch (Exception e) {
                showMessage("Error skipping planet: " + e.getMessage(), false);
            }
        });

        bottomHBox.getChildren().add(skipButton);
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
