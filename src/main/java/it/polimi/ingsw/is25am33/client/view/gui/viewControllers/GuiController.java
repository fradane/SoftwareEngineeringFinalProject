package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;
import javafx.scene.control.Label;

public abstract class GuiController {

    protected static ClientController clientController;
    protected static ClientModel clientModel;

    public static void setClientController(ClientController clientController) {
        GuiController.clientController = clientController;
    }

    public static void setClientModel(ClientModel clientModel) {
        GuiController.clientModel = clientModel;
    }

    abstract void showMessage(String message, boolean isPermanent);

    public void showPermanentMessage(String message, Label messageLabel) {
        messageLabel.getTransforms().clear();
        messageLabel.setText(message);
        messageLabel.setOpacity(1.0);
    }

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
        fadeOut.setOnFinished(_ -> messageLabel.setText(""));

        // Play sequence
        javafx.animation.SequentialTransition sequence = new javafx.animation.SequentialTransition(fadeIn, pause, fadeOut);
        sequence.play();
    }

}
