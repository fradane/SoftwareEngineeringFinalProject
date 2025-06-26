package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static it.polimi.ingsw.is25am33.client.view.tui.MessageType.ERROR;

/**
 * Simplified controller for managing the final game screen
 */
public class EndGameController extends GuiController implements Initializable {

    @FXML
    private StackPane centerStackPane;

    private boolean gameEnded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (centerStackPane == null) {
            centerStackPane = new StackPane();
        }
    }

    @Override
    public void showMessage(String message, boolean isPermanent) {
        System.out.println("EndGame message: " + message);
    }

    @Override
    public String getControllerType() {
        return "endGameController";
    }

    /**
     * Shows the final game information
     */
    public void showEndGameInfoMenu(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        Platform.runLater(() -> {
            gameEnded = true;
            createEndGameDisplay(finalRanking, playersNicknamesWithPrettiestShip);
        });
    }

    /**
     * Creates the main end game results display - NO SCROLL
     */
    private void createEndGameDisplay(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        centerStackPane.getChildren().clear();

        // Background overlay
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("endgame-overlay");
        overlay.setAlignment(Pos.CENTER);

        // Main container adapted to viewport
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("endgame-popup-container");
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(750);
        mainContainer.setMaxHeight(650);
        mainContainer.setPadding(new Insets(10));

        // Get current player data
        String myNickname = clientModel.getMyNickname();
        PlayerFinalData myData = findPlayerData(finalRanking, myNickname);
        Map<String, PlayerFinalData> nicknameToData = createNicknameDataMap(finalRanking);

        // Compact header
        VBox headerSection = createCompactHeader(myData);
        mainContainer.getChildren().add(headerSection);

        // Horizontal layout to optimize space
        HBox contentLayout = new HBox(20);
        contentLayout.setAlignment(Pos.TOP_CENTER);

        // Ranking section (left column)
        VBox rankingSection = createCompactRankingSection(finalRanking, nicknameToData, myNickname);
        rankingSection.setPrefWidth(350);

        // Personal details section (right column)
        VBox personalSection = null;
        if (myData != null) {
            personalSection = createCompactPersonalSection(myData, finalRanking, playersNicknamesWithPrettiestShip);
            personalSection.setPrefWidth(350);
        }

        contentLayout.getChildren().add(rankingSection);
        if (personalSection != null) {
            contentLayout.getChildren().add(personalSection);
        }

        // Set vertical growth for content
        VBox.setVgrow(contentLayout, Priority.ALWAYS);
        mainContainer.getChildren().add(contentLayout);

        // Exit button
        Button exitButton = new Button("Exit Game");
        exitButton.getStyleClass().add("popup-button");
        exitButton.setOnAction(e -> handleGameExit());
        mainContainer.getChildren().add(exitButton);

        overlay.getChildren().add(mainContainer);
        centerStackPane.getChildren().add(overlay);
        overlay.toFront();
    }

    /**
     * Creates compact header
     */
    private VBox createCompactHeader(PlayerFinalData myData) {
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label();
        if (myData != null && myData.isEarlyLanded()) {
            titleLabel.setText("🛬 END OF JOURNEY 🛬");
        } else {
            titleLabel.setText("🚀 END OF JOURNEY 🚀");
        }
        titleLabel.getStyleClass().add("endgame-title");
        titleLabel.setStyle("-fx-font-size: 24px;"); // Reduced from 32px

        headerBox.getChildren().add(titleLabel);
        return headerBox;
    }

    /**
     * Creates compact ranking section
     */
    private VBox createCompactRankingSection(List<PlayerFinalData> finalRanking,
                                             Map<String, PlayerFinalData> nicknameToData,
                                             String myNickname) {
        VBox rankingBox = new VBox(10);
        rankingBox.getStyleClass().add("endgame-ranking-section");
        rankingBox.setAlignment(Pos.TOP_CENTER);

        Label rankingTitle = new Label("🏆 FINAL RANKING");
        rankingTitle.getStyleClass().add("endgame-section-title");
        rankingTitle.setStyle("-fx-font-size: 16px;"); // Reduced

        VBox playersBox = new VBox(5);
        playersBox.setAlignment(Pos.CENTER);

        int maxCredits = finalRanking.stream()
                .mapToInt(PlayerFinalData::getTotalCredits)
                .max().orElse(0);

        List<String> sortedNicknames = clientModel.getSortedRanking();
        String[] medals = {"🥇", "🥈", "🥉"};

        int displayPosition = 1;
        for (String nickname : sortedNicknames) {
            PlayerFinalData data = nicknameToData.get(nickname);
            if (data == null) continue;

            HBox playerRow = createCompactPlayerRow(data, nickname, myNickname, displayPosition, maxCredits, medals);
            playersBox.getChildren().add(playerRow);

            if (!data.isEarlyLanded()) {
                displayPosition++;
            }
        }

        rankingBox.getChildren().addAll(rankingTitle, playersBox);
        return rankingBox;
    }

    /**
     * Creates compact player row
     */
    private HBox createCompactPlayerRow(PlayerFinalData data, String nickname, String myNickname,
                                        int displayPosition, int maxCredits, String[] medals) {
        HBox playerRow = new HBox(8);
        playerRow.getStyleClass().add("endgame-player-row");
        playerRow.setAlignment(Pos.CENTER_LEFT);
        playerRow.setPadding(new Insets(4, 8, 4, 8));

        // Medal + Position in one label
        Label positionLabel = new Label();
        if (data.isEarlyLanded()) {
            positionLabel.setText("🛬 -");
        } else {
            String medal = displayPosition <= 3 ? medals[displayPosition - 1] : "";
            positionLabel.setText(medal + " " + displayPosition + "°");
        }
        positionLabel.getStyleClass().add("endgame-medal");
        positionLabel.setMinWidth(50);

        // Player name
        String playerName = nickname.equals(myNickname) ? "YOU (" + nickname + ")" : nickname;
        Label nameLabel = new Label(playerName);
        nameLabel.getStyleClass().add("endgame-player-name");
        if (nickname.equals(myNickname)) {
            nameLabel.getStyleClass().add("endgame-my-name");
        }
        nameLabel.setPrefWidth(120);

        // Credits
        Label creditsLabel = new Label(data.getTotalCredits() + " 💰");
        creditsLabel.getStyleClass().add("endgame-credits");
        creditsLabel.setMinWidth(60);

        // Compact status
        Label statusLabel = new Label();
        if (data.isEarlyLanded()) {
            statusLabel.setText("[EARLY LANDED]");
            statusLabel.getStyleClass().add("endgame-status-landed");
        } else if (data.getTotalCredits() == maxCredits && data.getTotalCredits() > 0) {
            statusLabel.setText("🏆 WINNER!");
            statusLabel.getStyleClass().add("endgame-status-winner");
        } else if (nickname.equals(myNickname) && data.getTotalCredits() > 0) {
            statusLabel.setText("✨ Winner!");
            statusLabel.getStyleClass().add("endgame-status-winner");
        }

        playerRow.getChildren().addAll(positionLabel, nameLabel, creditsLabel, statusLabel);
        return playerRow;
    }

    /**
     * Creates compact personal section
     */
    private VBox createCompactPersonalSection(PlayerFinalData myData,
                                              List<PlayerFinalData> finalRanking,
                                              List<String> playersNicknamesWithPrettiestShip) {
        VBox personalBox = new VBox(10);
        personalBox.getStyleClass().add("endgame-personal-section");
        personalBox.setAlignment(Pos.TOP_CENTER);

        Label sectionTitle = new Label("📊 YOUR SUMMARY");
        sectionTitle.getStyleClass().add("endgame-section-title");
        sectionTitle.setStyle("-fx-font-size: 16px;"); // Reduced

        VBox breakdownBox = createCompactBreakdown(myData, finalRanking, playersNicknamesWithPrettiestShip);
        VBox finalMessageBox = createCompactFinalMessage(myData, finalRanking);

        personalBox.getChildren().addAll(sectionTitle, breakdownBox, finalMessageBox);
        return personalBox;
    }

    /**
     * Creates compact breakdown
     */
    private VBox createCompactBreakdown(PlayerFinalData myData,
                                        List<PlayerFinalData> finalRanking,
                                        List<String> playersNicknamesWithPrettiestShip) {
        VBox detailsBox = new VBox(4);
        detailsBox.getStyleClass().add("endgame-breakdown");

        int myPosition = findPlayerPosition(myData, finalRanking);
        boolean hasPrettiestShip = playersNicknamesWithPrettiestShip.contains(clientModel.getMyNickname());
        int initialCredits = calculateInitialCredits(myData, myPosition, hasPrettiestShip);

        // Compact labels with reduced font
        String smallFont = "-fx-font-size: 11px;";

        Label initialLabel = new Label("💰 Initial credits: " + initialCredits);
        initialLabel.getStyleClass().add("endgame-detail-item");
        initialLabel.setStyle(smallFont);
        detailsBox.getChildren().add(initialLabel);

        if (!myData.isEarlyLanded()) {
            int positionBonus = getPositionBonus(myPosition);
            Label positionLabel = new Label("✅ Arrival (" + myPosition + "°): +" + positionBonus);
            positionLabel.getStyleClass().add("endgame-detail-bonus");
            positionLabel.setStyle(smallFont);
            detailsBox.getChildren().add(positionLabel);

            int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), false);
            String cubesText = formatCubesCompact(myData.getAllOwnedCubes());
            Label cubesLabel = new Label("✅ Cargo " + cubesText + ": +" + cubesValue);
            cubesLabel.getStyleClass().add("endgame-detail-bonus");
            cubesLabel.setStyle(smallFont);
            detailsBox.getChildren().add(cubesLabel);

            if (hasPrettiestShip) {
                int prettiestBonus = getPrettiestShipBonus();
                Label prettiestLabel = new Label("✅ Prettiest ship: +" + prettiestBonus);
                prettiestLabel.getStyleClass().add("endgame-detail-bonus");
                prettiestLabel.setStyle(smallFont);
                detailsBox.getChildren().add(prettiestLabel);
            }
        } else {
            Label noPositionLabel = new Label("❌ Arrival: -- (early landed)");
            noPositionLabel.getStyleClass().add("endgame-detail-penalty");
            noPositionLabel.setStyle(smallFont);
            detailsBox.getChildren().add(noPositionLabel);

            int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), true);
            String cubesText = formatCubesCompact(myData.getAllOwnedCubes());
            Label cubesLabel = new Label("⚠️ Cargo " + cubesText + " (½): +" + cubesValue);
            cubesLabel.getStyleClass().add("endgame-detail-warning");
            cubesLabel.setStyle(smallFont);
            detailsBox.getChildren().add(cubesLabel);
        }

        if (myData.getLostComponents() > 0) {
            Label lostLabel = new Label("❌ Lost components: -" + myData.getLostComponents());
            lostLabel.getStyleClass().add("endgame-detail-penalty");
            lostLabel.setStyle(smallFont);
            detailsBox.getChildren().add(lostLabel);
        }

        Label totalLabel = new Label("💎 TOTAL: " + myData.getTotalCredits() + " 💰");
        totalLabel.getStyleClass().add("endgame-total");
        totalLabel.setStyle("-fx-font-size: 14px;"); // Reduced
        detailsBox.getChildren().add(totalLabel);

        return detailsBox;
    }

    /**
     * Compact final message
     */
    private VBox createCompactFinalMessage(PlayerFinalData myData, List<PlayerFinalData> finalRanking) {
        VBox messageBox = new VBox(5);
        messageBox.getStyleClass().add("endgame-final-message");
        messageBox.setAlignment(Pos.CENTER);

        int maxCredits = finalRanking.stream()
                .mapToInt(PlayerFinalData::getTotalCredits)
                .max().orElse(0);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 12px;");

        if (myData.isEarlyLanded()) {
            if (myData.getTotalCredits() > 0) {
                messageLabel.setText("🛬 You landed early, but you still have some credits!");
            } else {
                messageLabel.setText("🛬 You landed early without any credits.");
            }
            messageLabel.getStyleClass().add("endgame-status-landed");
        } else if (myData.getTotalCredits() == maxCredits && myData.getTotalCredits() > 0) {
            messageLabel.setText("🏆 Congratulations! You are the absolute winner! 🎊");
            messageLabel.getStyleClass().add("endgame-message-winner");
        } else if (myData.getTotalCredits() > 0) {
            messageLabel.setText("🎉 Congratulations! You are among the winners!");
            messageLabel.getStyleClass().add("endgame-message-winner");
        } else {
            messageLabel.setText("😔 You are not among the winners this time...");
            messageLabel.getStyleClass().add("endgame-detail-penalty");
        }

        messageBox.getChildren().add(messageLabel);
        return messageBox;
    }

    /**
     * Handles game exit
     */
    private void handleGameExit() {
        try {
            if (clientController != null) {
                clientController.leaveGame();
            }
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error during game exit: " + e.getMessage());
            System.exit(1);
        }
    }

    // Compact helper methods
    private String formatCubesCompact(List<CargoCube> cubes) {
        int red = 0, yellow = 0, green = 0, blue = 0;
        for (CargoCube cube : cubes) {
            switch (cube) {
                case RED -> red++;
                case YELLOW -> yellow++;
                case GREEN -> green++;
                case BLUE -> blue++;
            }
        }
        return String.format("(%d 🟥%d 🟨%d 🟩%d 🟦)", red, yellow, green, blue);
    }

    // Helper methods for calculations (copied from CLI)
    private PlayerFinalData findPlayerData(List<PlayerFinalData> finalRanking, String nickname) {
        return finalRanking.stream()
                .filter(data -> data.getNickname().equals(nickname))
                .findFirst()
                .orElse(null);
    }

    private Map<String, PlayerFinalData> createNicknameDataMap(List<PlayerFinalData> finalRanking) {
        Map<String, PlayerFinalData> map = new HashMap<>();
        for (PlayerFinalData data : finalRanking) {
            map.put(data.getNickname(), data);
        }
        return map;
    }

    private int findPlayerPosition(PlayerFinalData myData, List<PlayerFinalData> finalRanking) {
        if (myData.isEarlyLanded()) return -1;

        List<String> sortedNicknames = clientModel.getSortedRanking();
        Map<String, PlayerFinalData> nicknameToData = createNicknameDataMap(finalRanking);

        int positionCounter = 1;
        for (String nickname : sortedNicknames) {
            PlayerFinalData data = nicknameToData.get(nickname);
            if (data != null && !data.isEarlyLanded()) {
                if (nickname.equals(clientModel.getMyNickname())) {
                    return positionCounter;
                }
                positionCounter++;
            }
        }
        return -1;
    }

    private int calculateInitialCredits(PlayerFinalData data, int position, boolean hasPrettiestShip) {
        int totalCredits = data.getTotalCredits();

        if (!data.isEarlyLanded()) {
            totalCredits -= getPositionBonus(position);
        }

        int cubesValue = calculateCubesValue(data.getAllOwnedCubes(), data.isEarlyLanded());
        totalCredits -= cubesValue;

        if (!data.isEarlyLanded() && hasPrettiestShip) {
            totalCredits -= getPrettiestShipBonus();
        }

        totalCredits += data.getLostComponents();
        return totalCredits;
    }

    public void showDisconnectMessage(String message) {
        showMessage(message, true);
        System.exit(0);
    }

    private int getPositionBonus(int position) {
        boolean isTestFlight = false;
        try {
            isTestFlight = clientController.getCurrentGameInfo().isTestFlight();
        } catch (Exception e) {
            isTestFlight = false;
        }

        if (isTestFlight) {
            return switch (position) {
                case 1 -> 4;
                case 2 -> 3;
                case 3 -> 2;
                case 4 -> 1;
                default -> 0;
            };
        } else {
            return switch (position) {
                case 1 -> 8;
                case 2 -> 6;
                case 3 -> 4;
                case 4 -> 2;
                default -> 0;
            };
        }
    }

    private int getPrettiestShipBonus() {
        boolean isTestFlight = false;
        try {
            isTestFlight = clientController.getCurrentGameInfo().isTestFlight();
        } catch (Exception e) {
            isTestFlight = false;
        }
        return isTestFlight ? 2 : 4;
    }

    private int calculateCubesValue(List<CargoCube> cubes, boolean isHalved) {
        int total = 0;
        for (CargoCube cube : cubes) {
            total += switch (cube) {
                case BLUE -> 1;
                case GREEN -> 2;
                case YELLOW -> 3;
                case RED -> 4;
            };
        }
        return isHalved ? total / 2 : total;
    }

    private void showOverlayPopup(String title, String message, Runnable onClose) {
        // Overlay di sfondo
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        // Container del popup
        VBox popupContent = new VBox();
        popupContent.getStyleClass().add("popup-container");

        // Titolo (solo se fornito)
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
            if (onClose != null) {
                onClose.run();
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("popup-button", "cancel-button");
        cancelButton.setOnAction(e -> {
            centerStackPane.getChildren().remove(overlay);
            showMessage("Operation cancelled.", false);
        });

        buttonContainer.getChildren().addAll(confirmButton, cancelButton);
        popupContent.getChildren().add(buttonContainer);

        overlay.getChildren().add(popupContent);

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

}