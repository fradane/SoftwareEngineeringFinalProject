package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class StartViewController extends GuiController {

    @FXML
    private TextField nicknameField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button startButton;

    /**
     * Initializes the start view controller, called automatically by JavaFX.
     * Sets up initial animations and focuses the nickname field.
     */
    public void initialize() {
        Platform.runLater(() -> {
            nicknameField.requestFocus();

            startButton.setScaleX(0.8);
            startButton.setScaleY(0.8);
            startButton.setOpacity(0.7);

            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(500), startButton);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();

            FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), startButton);
            fadeTransition.setToValue(1.0);
            fadeTransition.play();
        });
    }

    @FXML
    private void handleSubmit() {
        String nickname = nicknameField.getText().trim();

        if (nickname.isEmpty()) {
            showError("Please enter a nickname to continue your mission.");
            return;
        }

        if (nickname.length() < 3) {
            showError("Your  nickname must be at least 3 characters long.");
            return;
        }

        clearError();

        startButton.setDisable(true);
        startButton.setText("CONNECTING...");

        ScaleTransition loadingAnimation = new ScaleTransition(Duration.millis(200), startButton);
        loadingAnimation.setToX(0.95);
        loadingAnimation.setToY(0.95);
        loadingAnimation.setCycleCount(2);
        loadingAnimation.setAutoReverse(true);
        loadingAnimation.play();

        // Registrazione con il server
        clientController.register(nickname);
    }

    /**
     * Shows a message to the user in the start view.
     *
     * @param message the message to display
     * @param isPermanent whether the message should persist or fade out
     */
    @Override
    public void showMessage(String message, boolean isPermanent) {
        Platform.runLater(() -> {
            // Ripristina lo stato del bottone
            resetStartButton();

            // Mostra il messaggio di errore nella label
            showError(message);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);

            errorLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), errorLabel);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            shakeNode(nicknameField);

            resetStartButton();
        });
    }


    private void clearError() {
        Platform.runLater(() -> {
            if (errorLabel.isVisible()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(10), errorLabel);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> errorLabel.setVisible(false));
                fadeOut.play();
            }
        });
    }

    private void resetStartButton() {
        startButton.setDisable(false);
        startButton.setText("START MISSION");
    }

    private void shakeNode(javafx.scene.Node node) {
        double originalX = node.getTranslateX();

        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        timeline.getKeyFrames().addAll(
                new javafx.animation.KeyFrame(Duration.millis(0),
                        new javafx.animation.KeyValue(node.translateXProperty(), originalX)),
                new javafx.animation.KeyFrame(Duration.millis(50),
                        new javafx.animation.KeyValue(node.translateXProperty(), originalX + 5)),
                new javafx.animation.KeyFrame(Duration.millis(100),
                        new javafx.animation.KeyValue(node.translateXProperty(), originalX - 5)),
                new javafx.animation.KeyFrame(Duration.millis(150),
                        new javafx.animation.KeyValue(node.translateXProperty(), originalX + 5)),
                new javafx.animation.KeyFrame(Duration.millis(200),
                        new javafx.animation.KeyValue(node.translateXProperty(), originalX))
        );
        timeline.play();
    }

    /**
     * Prompts the user to enter their nickname again.
     * Resets the form and focuses the nickname field.
     */
    public void askNickname() {
        Platform.runLater(() -> {
            nicknameField.setText("");
            clearError();
            nicknameField.setVisible(true);
            startButton.setVisible(true);
            resetStartButton();

            // Focus sul campo nickname
            nicknameField.requestFocus();
        });
    }

    /**
     * Displays a server connection error message to the user.
     *
     * @param errorMessage the error message from the server
     */
    public void showServerError(String errorMessage) {
        String formattedMessage = "Connection Error: " + errorMessage;
        showMessage(formattedMessage, false);
    }


}