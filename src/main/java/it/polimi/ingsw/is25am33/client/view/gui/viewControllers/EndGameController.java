package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller semplificato per la gestione della schermata finale del gioco
 */
public class EndGameController extends GuiController implements BoardsEventHandler {

    @FXML
    private StackPane centerStackPane;

    private boolean gameEnded = false;


    public void initialize(URL location, ResourceBundle resources) {
        if (centerStackPane == null) {
            centerStackPane = new StackPane();
        }
    }

    @Override
    public void showMessage(String message, boolean isPermanent) {
        System.out.println("EndGame message: " + message);
    }

    /**
     * Mostra le informazioni finali del gioco
     */
    public void showEndGameInfoMenu(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        Platform.runLater(() -> {
            gameEnded = true;
            createEndGameDisplay(finalRanking, playersNicknamesWithPrettiestShip);
        });
    }

    /**
     * Mostra il popup per l'atterraggio anticipato del giocatore corrente
     */
    public void showPlayerEarlyLanded() {
        if (!gameEnded) {
            Platform.runLater(() -> {
                createEarlyLandingDisplay();
            });
        }
    }

    /**
     * Notifica che un altro giocatore è atterrato anticipatamente
     */
    public void notifyOtherPlayerEarlyLanded(String nickname) {
        if (!gameEnded) {
            Platform.runLater(() -> {
                createOtherPlayerLandingNotification(nickname);
            });
        }
    }

    /**
     * Crea la visualizzazione principale dei risultati finali
     */
    private void createEndGameDisplay(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        centerStackPane.getChildren().clear();

        // Overlay di sfondo
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("endgame-overlay");
        overlay.setAlignment(Pos.CENTER);

        // Container principale del popup
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("endgame-popup-container");
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(800);

        // Ottieni dati del giocatore corrente
        String myNickname = clientModel.getMyNickname();
        PlayerFinalData myData = findPlayerData(finalRanking, myNickname);

        // Crea mappa per lookup veloce
        Map<String, PlayerFinalData> nicknameToData = createNicknameDataMap(finalRanking);

        // Header con titolo
        VBox headerSection = createHeaderSection(myData);
        mainContainer.getChildren().add(headerSection);

        // Sezione classifica
        VBox rankingSection = createRankingSection(finalRanking, nicknameToData, myNickname);
        mainContainer.getChildren().add(rankingSection);

        // Sezione dettagli personali
        if (myData != null) {
            VBox personalSection = createPersonalSection(myData, finalRanking, playersNicknamesWithPrettiestShip);
            mainContainer.getChildren().add(personalSection);
        }

        // Bottone per uscire
        Button exitButton = new Button("Esci dal Gioco");
        exitButton.getStyleClass().add("popup-button");
        exitButton.setOnAction(e -> handleGameExit());
        mainContainer.getChildren().add(exitButton);

        // ScrollPane per contenuto lungo con stile spaziale
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.getStyleClass().add("endgame-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxWidth(850);
        scrollPane.setMaxHeight(600);

        StackPane.setAlignment(scrollPane, Pos.CENTER);
        overlay.getChildren().add(scrollPane);

        centerStackPane.getChildren().add(overlay);
        overlay.toFront();
    }

    /**
     * Crea la visualizzazione per l'atterraggio anticipato
     */
    private void createEarlyLandingDisplay() {
        // Overlay di sfondo
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");
        overlay.setAlignment(Pos.CENTER);

        // Container principale del popup
        VBox container = new VBox(15);
        container.getStyleClass().add("popup-container");
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(600);

        // Titolo
        Label titleLabel = new Label("🛬 ATTERRAGGIO ANTICIPATO 🛬");
        titleLabel.getStyleClass().add("popup-title");

        // Messaggio principale
        Label mainMessage = new Label("Il tuo razzo segna-rotta è stato rimosso dalla plancia di volo!");
        mainMessage.getStyleClass().add("popup-message");
        mainMessage.setWrapText(true);

        Label subMessage = new Label("Hai abbandonato la corsa spaziale e sei atterrato in sicurezza.\n" +
                "A partire dalla prossima carta sarai solo uno spettatore.");
        subMessage.getStyleClass().add("popup-message");
        subMessage.setWrapText(true);

        // Sezione avvertimenti
        VBox warningBox = createWarningSection();

        Label encouragement = new Label("Potrai ancora vincere se avrai accumulato abbastanza crediti!");
        encouragement.getStyleClass().add("popup-message");
        encouragement.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        encouragement.setWrapText(true);

        // Bottone OK
        Button okButton = new Button("Ho capito");
        okButton.getStyleClass().add("popup-button");
        okButton.setOnAction(e -> centerStackPane.getChildren().remove(overlay));

        container.getChildren().addAll(titleLabel, mainMessage, subMessage, warningBox, encouragement, okButton);

        // ScrollPane per potenziale overflow
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.getStyleClass().add("components-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxWidth(650);
        scrollPane.setMaxHeight(500);

        StackPane.setAlignment(scrollPane, Pos.CENTER);
        overlay.getChildren().add(scrollPane);

        centerStackPane.getChildren().add(overlay);
        overlay.toFront();
    }

    /**
     * Crea notifica per l'atterraggio di un altro giocatore
     */
    private void createOtherPlayerLandingNotification(String nickname) {
        // Overlay di sfondo (più trasparente)
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        overlay.setAlignment(Pos.CENTER);

        // Container della notifica
        VBox container = new VBox(15);
        container.getStyleClass().add("popup-container");
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(500);

        Label titleLabel = new Label("📢 ANNUNCIO DI VOLO 📢");
        titleLabel.getStyleClass().add("popup-title");

        Label messageLabel = new Label(nickname + " ha abbandonato la corsa!");
        messageLabel.getStyleClass().add("popup-message");
        messageLabel.setStyle("-fx-font-weight: bold;");

        Label subLabel = new Label("Il suo razzo ha effettuato un atterraggio anticipato.\n" +
                "Dalla prossima carta non parteciperà più alle avventure.");
        subLabel.getStyleClass().add("popup-message");
        subLabel.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("popup-button");
        okButton.setOnAction(e -> centerStackPane.getChildren().remove(overlay));

        container.getChildren().addAll(titleLabel, messageLabel, subLabel, okButton);
        overlay.getChildren().add(container);

        centerStackPane.getChildren().add(overlay);
        overlay.toFront();

        // Auto-rimozione dopo 5 secondi
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (centerStackPane.getChildren().contains(overlay)) {
                centerStackPane.getChildren().remove(overlay);
            }
        }));
        timeline.play();
    }

    /**
     * Crea l'header della schermata finale
     */
    private VBox createHeaderSection(PlayerFinalData myData) {
        VBox headerBox = new VBox(10);
        headerBox.getStyleClass().add("endgame-header");
        headerBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label();
        if (myData != null && myData.isEarlyLanded()) {
            titleLabel.setText("🛬 FINE DEL VIAGGIO 🛬");
        } else {
            titleLabel.setText("🚀 FINE DEL VIAGGIO 🚀");
        }
        titleLabel.getStyleClass().add("endgame-title");

        // Separator ornamentale
        VBox separatorBox = new VBox();
        separatorBox.getStyleClass().add("endgame-separator");
        separatorBox.setMaxWidth(400);

        headerBox.getChildren().addAll(titleLabel, separatorBox);
        return headerBox;
    }

    /**
     * Crea la sezione della classifica
     */
    private VBox createRankingSection(List<PlayerFinalData> finalRanking,
                                      Map<String, PlayerFinalData> nicknameToData,
                                      String myNickname) {
        VBox rankingBox = new VBox(15);
        rankingBox.getStyleClass().add("endgame-ranking-section");
        rankingBox.setAlignment(Pos.CENTER);

        Label rankingTitle = new Label("🏆 CLASSIFICA FINALE");
        rankingTitle.getStyleClass().add("endgame-section-title");

        VBox playersBox = new VBox(8);
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

            HBox playerRow = createPlayerRow(data, nickname, myNickname, displayPosition, maxCredits, medals);
            playersBox.getChildren().add(playerRow);

            if (!data.isEarlyLanded()) {
                displayPosition++;
            }
        }

        rankingBox.getChildren().addAll(rankingTitle, playersBox);
        return rankingBox;
    }

    /**
     * Crea una riga per un giocatore nella classifica
     */
    private HBox createPlayerRow(PlayerFinalData data, String nickname, String myNickname,
                                 int displayPosition, int maxCredits, String[] medals) {
        HBox playerRow = new HBox(15);
        playerRow.getStyleClass().add("endgame-player-row");
        playerRow.setAlignment(Pos.CENTER_LEFT);
        playerRow.setPadding(new Insets(8, 15, 8, 15));

        // Medal/Position
        Label medalLabel = new Label();
        medalLabel.getStyleClass().add("endgame-medal");
        Label positionLabel = new Label();
        positionLabel.getStyleClass().add("endgame-position");

        if (data.isEarlyLanded()) {
            medalLabel.setText("🛬");
            positionLabel.setText(" - ");
        } else {
            String medal = displayPosition <= 3 ? medals[displayPosition - 1] : "  ";
            medalLabel.setText(medal);
            positionLabel.setText(displayPosition + "°");
        }

        // Nome giocatore
        String playerName = nickname.equals(myNickname) ? "TU (" + nickname + ")" : nickname;
        Label nameLabel = new Label(playerName);
        nameLabel.getStyleClass().add("endgame-player-name");
        if (nickname.equals(myNickname)) {
            nameLabel.getStyleClass().add("endgame-my-name");
        }

        // Crediti
        Label creditsLabel = new Label(data.getTotalCredits() + " crediti cosmici");
        creditsLabel.getStyleClass().add("endgame-credits");

        // Status
        Label statusLabel = new Label();
        if (data.isEarlyLanded()) {
            statusLabel.setText("[ATTERRATO ANTICIPATAMENTE]");
            statusLabel.getStyleClass().add("endgame-status-landed");
        } else if (data.getTotalCredits() == maxCredits && data.getTotalCredits() > 0) {
            statusLabel.setText("🎉 VINCITORE ASSOLUTO!");
            statusLabel.getStyleClass().add("endgame-status-winner");
        } else if (nickname.equals(myNickname) && data.getTotalCredits() > 0) {
            statusLabel.setText("✨ Tra i vincitori!");
            statusLabel.getStyleClass().add("endgame-status-winner");
        }

        playerRow.getChildren().addAll(medalLabel, positionLabel, nameLabel, creditsLabel, statusLabel);
        return playerRow;
    }

    /**
     * Crea la sezione dei dettagli personali
     */
    private VBox createPersonalSection(PlayerFinalData myData,
                                       List<PlayerFinalData> finalRanking,
                                       List<String> playersNicknamesWithPrettiestShip) {
        VBox personalBox = new VBox(15);
        personalBox.getStyleClass().add("endgame-personal-section");
        personalBox.setAlignment(Pos.CENTER);

        Label sectionTitle = new Label();
        if (myData.isEarlyLanded()) {
            sectionTitle.setText("📊 IL TUO RIEPILOGO (Atterrato Anticipatamente)");
        } else {
            sectionTitle.setText("📊 IL TUO RIEPILOGO");
        }
        sectionTitle.getStyleClass().add("endgame-section-title");

        // Separator
        VBox separatorBox = new VBox();
        separatorBox.getStyleClass().add("endgame-detail-separator");
        separatorBox.setMaxWidth(300);

        VBox breakdownBox = createBreakdownDetails(myData, finalRanking, playersNicknamesWithPrettiestShip);
        VBox finalMessageBox = createFinalMessage(myData, finalRanking);

        personalBox.getChildren().addAll(sectionTitle, separatorBox, breakdownBox, finalMessageBox);
        return personalBox;
    }

    /**
     * Crea i dettagli del breakdown
     */
    private VBox createBreakdownDetails(PlayerFinalData myData,
                                        List<PlayerFinalData> finalRanking,
                                        List<String> playersNicknamesWithPrettiestShip) {
        VBox detailsBox = new VBox(8);
        detailsBox.getStyleClass().add("endgame-breakdown");

        int myPosition = findPlayerPosition(myData, finalRanking);
        boolean hasPrettiestShip = playersNicknamesWithPrettiestShip.contains(clientModel.getMyNickname());
        int initialCredits = calculateInitialCredits(myData, myPosition, hasPrettiestShip);

        // Crediti iniziali
        Label initialLabel = new Label("💰 Crediti iniziali: " + initialCredits);
        initialLabel.getStyleClass().add("endgame-detail-item");
        detailsBox.getChildren().add(initialLabel);

        // Bonus/Penalità
        if (!myData.isEarlyLanded()) {
            addNormalPlayerBonuses(detailsBox, myData, myPosition, hasPrettiestShip);
        } else {
            addEarlyLandedPlayerResults(detailsBox, myData);
        }

        // Penalità componenti persi
        if (myData.getLostComponents() > 0) {
            Label lostLabel = new Label("❌ Componenti persi (" + myData.getLostComponents() + "): -" +
                    myData.getLostComponents() + " 💰");
            lostLabel.getStyleClass().add("endgame-detail-penalty");
            detailsBox.getChildren().add(lostLabel);
        }

        // Totale finale
        Label totalLabel = new Label("💎 TOTALE FINALE: " + myData.getTotalCredits() + " 💰");
        totalLabel.getStyleClass().add("endgame-total");
        detailsBox.getChildren().add(totalLabel);

        return detailsBox;
    }

    /**
     * Aggiunge i bonus per giocatori normali
     */
    private void addNormalPlayerBonuses(VBox detailsBox, PlayerFinalData myData, int myPosition, boolean hasPrettiestShip) {
        int positionBonus = getPositionBonus(myPosition);
        Label positionLabel = new Label("✅ Ricompensa arrivo (" + myPosition + "° posto): +" + positionBonus + " 💰");
        positionLabel.getStyleClass().add("endgame-detail-bonus");
        detailsBox.getChildren().add(positionLabel);

        int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), false);
        String cubesText = formatCubes(myData.getAllOwnedCubes());
        Label cubesLabel = new Label("✅ Vendita merci " + cubesText + ": +" + cubesValue + " 💰");
        cubesLabel.getStyleClass().add("endgame-detail-bonus");
        detailsBox.getChildren().add(cubesLabel);

        if (hasPrettiestShip) {
            int prettiestBonus = getPrettiestShipBonus();
            Label prettiestLabel = new Label("✅ Nave più bella: +" + prettiestBonus + " 💰");
            prettiestLabel.getStyleClass().add("endgame-detail-bonus");
            detailsBox.getChildren().add(prettiestLabel);
        }
    }

    /**
     * Aggiunge i risultati per giocatori atterrati anticipatamente
     */
    private void addEarlyLandedPlayerResults(VBox detailsBox, PlayerFinalData myData) {
        Label noPositionLabel = new Label("❌ Ricompensa arrivo: -- (atterrato anticipatamente)");
        noPositionLabel.getStyleClass().add("endgame-detail-penalty");
        detailsBox.getChildren().add(noPositionLabel);

        int cubesValue = calculateCubesValue(myData.getAllOwnedCubes(), true);
        String cubesText = formatCubes(myData.getAllOwnedCubes());
        Label cubesLabel = new Label("⚠️ Vendita merci " + cubesText + " (DIMEZZATA): +" + cubesValue + " 💰");
        cubesLabel.getStyleClass().add("endgame-detail-warning");
        detailsBox.getChildren().add(cubesLabel);

        Label noPrettiestLabel = new Label("❌ Nave più bella: -- (atterrato anticipatamente)");
        noPrettiestLabel.getStyleClass().add("endgame-detail-penalty");
        detailsBox.getChildren().add(noPrettiestLabel);
    }

    /**
     * Crea il messaggio finale
     */
    private VBox createFinalMessage(PlayerFinalData myData, List<PlayerFinalData> finalRanking) {
        VBox messageBox = new VBox(10);
        messageBox.getStyleClass().add("endgame-final-message");
        messageBox.setAlignment(Pos.CENTER);

        int maxCredits = finalRanking.stream()
                .mapToInt(PlayerFinalData::getTotalCredits)
                .max().orElse(0);

        Label messageLabel = new Label();
        messageLabel.getStyleClass().add("popup-message");

        if (myData.isEarlyLanded()) {
            if (myData.getTotalCredits() > 0) {
                messageLabel.setText("🛬 Hai abbandonato la corsa anticipatamente, ma hai comunque dei crediti!");
            } else {
                messageLabel.setText("🛬 Hai abbandonato la corsa anticipatamente e non hai guadagnato crediti.");
            }
            messageLabel.getStyleClass().add("endgame-status-landed");
        } else if (myData.getTotalCredits() == maxCredits && myData.getTotalCredits() > 0) {
            messageLabel.setText("🏆 Complimenti! Sei il vincitore assoluto! 🎊");
            messageLabel.getStyleClass().add("endgame-message-winner");
        } else if (myData.getTotalCredits() > 0) {
            messageLabel.setText("🎉 Complimenti! Sei tra i vincitori!");
            messageLabel.getStyleClass().add("endgame-message-winner");
        } else {
            messageLabel.setText("😔 Purtroppo non sei tra i vincitori questa volta...");
            messageLabel.getStyleClass().add("endgame-detail-penalty");
        }

        messageLabel.setWrapText(true);
        messageBox.getChildren().add(messageLabel);
        return messageBox;
    }

    /**
     * Crea la sezione degli avvertimenti
     */
    private VBox createWarningSection() {
        VBox warningBox = new VBox(8);
        warningBox.setPadding(new Insets(15));
        warningBox.setStyle("-fx-background-color: rgba(220, 53, 69, 0.1); -fx-background-radius: 8px; -fx-border-color: #dc3545; -fx-border-radius: 8px; -fx-border-width: 1px;");

        Label warningTitle = new Label("⚠️ RICORDA:");
        warningTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545; -fx-font-size: 14px;");

        String[] warningPoints = {
                "• Nessuna carta avrà più effetto su di te",
                "• Non riceverai ricompense per l'ordine di arrivo",
                "• Non potrai competere per la nave più bella",
                "• Le tue merci saranno vendute a metà prezzo",
                "• Pagherai comunque le penalità per i componenti persi"
        };

        warningBox.getChildren().add(warningTitle);
        for (String point : warningPoints) {
            Label pointLabel = new Label(point);
            pointLabel.getStyleClass().add("popup-message");
            pointLabel.setStyle("-fx-font-size: 12px;");
            warningBox.getChildren().add(pointLabel);
        }

        return warningBox;
    }

    /**
     * Gestisce l'uscita dal gioco
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

    // Metodi helper per i calcoli (copiati dalla CLI)
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

    private int getPositionBonus(int position) {
        // Controlla se è test flight usando il clientController se disponibile
        boolean isTestFlight = false;
        try {
            isTestFlight = clientController.getCurrentGameInfo().isTestFlight();
        } catch (Exception e) {
            // Fallback - assume non sia test flight
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
        // Controlla se è test flight usando il clientController se disponibile
        boolean isTestFlight = false;
        try {
            isTestFlight = clientController.getCurrentGameInfo().isTestFlight();
        } catch (Exception e) {
            // Fallback - assume non sia test flight
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

    private String formatCubes(List<CargoCube> cubes) {
        int red = 0, yellow = 0, green = 0, blue = 0;

        for (CargoCube cube : cubes) {
            switch (cube) {
                case RED -> red++;
                case YELLOW -> yellow++;
                case GREEN -> green++;
                case BLUE -> blue++;
            }
        }

        return String.format("%d 🟥 %d 🟨 %d 🟩 %d 🟦", red, yellow, green, blue);
    }

    @Override
    public void onGridButtonClick(int row, int column) {
    }
}
