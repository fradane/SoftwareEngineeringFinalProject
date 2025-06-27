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

/**
 * Controller class for the start view of the application.
 * Handles the initial user interaction where players enter their nickname
 * and attempt to connect to the game server.
 * <p>
 * This controller manages:
 * - Nickname input validation
 * - Server connection attempts
 * - Visual feedback through animations
 * - Error message display
 * - Initial game setup process
 * <p>
 * The view contains a text field for nickname input, a start button
 * to initiate the connection, and an error label for feedback messages.
 */
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

    /**
     * Handles the submit button click event.
     * Validates the nickname and initiates server registration.
     */
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

        // Register with server
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
            // Reset button state
            resetStartButton();

            // Show error message in label
            showError(message);
        });
    }

    @Override
    public String getControllerType() {
        return "startViewController";
    }

    /**
     * Shows an error message with fade-in animation.
     *
     * @param message the error message to display
     */
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


    /**
     * Clears the current error message with fade-out animation.
     */
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

    /**
     * Resets the start button to its default state.
     */
    private void resetStartButton() {
        startButton.setDisable(false);
        startButton.setText("START MISSION");
    }

    /**
     * Applies a shake animation to the specified node.
     *
     * @param node the JavaFX node to animate
     */
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

            // Focus on nickname field
            nicknameField.requestFocus();
        });
    }

    /**
     * Displays a disconnect message to the user, ensuring the message is shown
     * with priority for disconnection events, and then terminates the application.
     *
     * @param message the message to be displayed to the user explaining the disconnection reason.
     */
    public void showDisconnectMessage(String message) {
        showMessage(message, true);
        System.exit(0);
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