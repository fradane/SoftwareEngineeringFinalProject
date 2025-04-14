module it.polimi.ingsw.is25am33 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.smartcardio;
    requires java.desktop;
    requires java.management.rmi;
    requires java.logging;
    requires java.rmi;

    // Aperture per JavaFX e Jackson
    opens it.polimi.ingsw.is25am33 to javafx.fxml;
    opens it.polimi.ingsw.is25am33.model to com.fasterxml.jackson.databind, java.rmi;  // Combinato
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

    // Esportazioni
    exports it.polimi.ingsw.is25am33;
    exports it.polimi.ingsw.is25am33.model.component;
    exports it.polimi.ingsw.is25am33.model.card;
    exports it.polimi.ingsw.is25am33.client;

    // Esportazioni aggiuntive per RMI
    exports it.polimi.ingsw.is25am33.network.common to java.rmi;  // AGGIUNTO
    exports it.polimi.ingsw.is25am33.network.rmi.server to java.rmi;
    exports it.polimi.ingsw.is25am33.network.rmi.client to java.rmi;

    // Aperture aggiuntive per RMI
    opens it.polimi.ingsw.is25am33.network.common to java.rmi;  // AGGIUNTO
    opens it.polimi.ingsw.is25am33.network.rmi.server to java.rmi;
    opens it.polimi.ingsw.is25am33.network.rmi.client to java.rmi;
    opens it.polimi.ingsw.is25am33.model.board to java.rmi;
}