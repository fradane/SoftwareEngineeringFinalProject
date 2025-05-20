package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class StartViewController extends GuiController{

    private ClientModel clientModel;

    @FXML
    private TextField nicknameField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button startButton;

    /**
     * Triggerato quando l'utente clicca sul pulsante "Avvia".
     */
    @FXML
    private void handleSubmit() {
        String nickname = nicknameField.getText().trim();

        if (nickname.length() < 3) {
            showError("Nickname cannot be less than 3 characters long. Please try again.");
            return;
        }

        clearError();
        clientController.register(nickname);

    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    public void askNickname() {

        Platform.runLater(() -> {
            nicknameField.setText("");
            errorLabel.setVisible(false);
            nicknameField.setVisible(true);
            startButton.setVisible(true);
        });

    }

    public void showServerError(String errorMessage) {
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
    }
}