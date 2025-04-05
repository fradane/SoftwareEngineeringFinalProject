module it.polimi.ingsw.is25am33 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.smartcardio;
    requires java.logging;
    requires java.desktop;


    opens it.polimi.ingsw.is25am33 to javafx.fxml;
    exports it.polimi.ingsw.is25am33;
}