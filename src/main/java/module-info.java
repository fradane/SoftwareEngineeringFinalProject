module it.polimi.ingsw.is25am33 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.smartcardio;
    requires java.logging;


    opens it.polimi.ingsw.is25am33 to javafx.fxml;
    opens it.polimi.ingsw.is25am33.model to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.component to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33;
}