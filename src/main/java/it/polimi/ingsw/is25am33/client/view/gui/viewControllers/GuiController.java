package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import javafx.scene.control.Label;

public abstract class GuiController {

    protected static ClientController clientController;
    protected static ClientModel clientModel;

    private String currentPermanentMessage = "";

    /**
     * Sets the static client controller instance for all GUI controllers.
     *
     * @param clientController the client controller to set
     */
    public static void setClientController(ClientController clientController) {
        GuiController.clientController = clientController;
    }

    public abstract String getControllerType();

    /**
     * Sets the static client model instance for all GUI controllers.
     *
     * @param clientModel the client model to set
     */
    public static void setClientModel(ClientModel clientModel) {
        GuiController.clientModel = clientModel;
    }

    /**
     * Shows a message to the user in the GUI.
     *
     * @param message the message to display
     * @param isPermanent whether the message should persist or fade out
     */
    public abstract void showMessage(String message, boolean isPermanent);

    /**
     * Displays a permanent message that remains visible until manually changed.
     *
     * @param message the message text to display
     * @param messageLabel the label component to show the message in
     */
    public void showPermanentMessage(String message, Label messageLabel) {
        messageLabel.getTransforms().clear();
        messageLabel.setText(message);
        messageLabel.setOpacity(1.0);
        this.currentPermanentMessage = message;
    }

    /**
     * Displays a temporary message with fade-in, pause, and fade-out animation.
     *
     * @param message the message text to display
     * @param messageLabel the label component to show the message in
     */
    public void showNonPermanentMessage(String message, Label messageLabel) {
        messageLabel.setText(message);
        messageLabel.setOpacity(0.0);

        // Fade in
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.5), messageLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Pause before fading out
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.0));

        // Fade out
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.5), messageLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(_ -> {
            messageLabel.setText(currentPermanentMessage);
            messageLabel.setOpacity(1.0);
        });

        // Play sequence
        javafx.animation.SequentialTransition sequence = new javafx.animation.SequentialTransition(fadeIn, pause, fadeOut);
        sequence.play();
    }

    public abstract void showDisconnectMessage(String message);
}
