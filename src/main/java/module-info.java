module it.polimi.ingsw.is25am33 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.smartcardio;
    requires java.logging;
    requires java.desktop;
    requires java.management.rmi;


    opens it.polimi.ingsw.is25am33 to javafx.fxml;
    opens it.polimi.ingsw.is25am33.model to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.component to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.card to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.game to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.model.card;
    exports it.polimi.ingsw.is25am33.model.game;
    exports it.polimi.ingsw.is25am33.model.component;
    exports it.polimi.ingsw.is25am33.serializationLayer to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.model.dangerousObj;
    exports it.polimi.ingsw.is25am33.model;
    exports it.polimi.ingsw.is25am33.model.board to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33;
}