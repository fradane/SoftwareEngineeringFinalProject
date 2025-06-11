module it.polimi.ingsw.is25am33 {
    // Required external modules and libraries
    // JavaFX dependencies for the GUI
    requires javafx.controls;
    requires javafx.fxml;
    // JSON serialization library
    // Java standard libraries
    requires java.smartcardio;
    requires java.management.rmi;
    requires java.rmi;
    requires org.jetbrains.annotations;
    requires net.bytebuddy;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.databind;
    requires java.logging;
    requires java.desktop;

    // JavaFX Configuration
    // Allows JavaFX to access the main package for FXML loading
    opens it.polimi.ingsw.is25am33 to javafx.fxml;

    // JSON Serialization Configuration
    // Open model packages to Jackson for JSON serialization/deserialization
    opens it.polimi.ingsw.is25am33.model to com.fasterxml.jackson.databind, java.rmi;
    opens it.polimi.ingsw.is25am33.model.component to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.card to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.game to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.board to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.serializationLayer to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.serializationLayer.client to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.serializationLayer.server to com.fasterxml.jackson.databind;
    //opens it.polimi.ingsw.is25am33.model.dangerousObj to com.fasterxml.jackson.databind;

    // RMI (Remote Method Invocation) Configuration
    // Open packages needed for RMI functionality
    opens it.polimi.ingsw.is25am33.network.common to java.rmi;
    opens it.polimi.ingsw.is25am33.network.rmi to java.rmi;
    opens it.polimi.ingsw.is25am33.network to java.rmi;

    // Public API Exports
    // Main game model exports
    exports it.polimi.ingsw.is25am33.model;
    exports it.polimi.ingsw.is25am33.model.card;
    exports it.polimi.ingsw.is25am33.model.game;
    exports it.polimi.ingsw.is25am33.model.component;
    exports it.polimi.ingsw.is25am33.model.dangerousObj;
    exports it.polimi.ingsw.is25am33.model.board to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.is25am33.model.dangerousObj to com.fasterxml.jackson.databind;
    
    // Controller and network exports
    exports it.polimi.ingsw.is25am33.controller;
    exports it.polimi.ingsw.is25am33.network.common to java.rmi;
    exports it.polimi.ingsw.is25am33.network;
    
    // Client-side exports
    exports it.polimi.ingsw.is25am33.client;
    exports it.polimi.ingsw.is25am33.client.view;
    exports it.polimi.ingsw.is25am33.client.controller;
    
    // Serialization layer exports
    exports it.polimi.ingsw.is25am33.serializationLayer to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.serializationLayer.client to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.serializationLayer.server to com.fasterxml.jackson.databind;
    
    // Main application export
    //exports it.polimi.ingsw.is25am33;
    exports it.polimi.ingsw.is25am33.model.enumFiles;
    opens it.polimi.ingsw.is25am33.model.enumFiles to com.fasterxml.jackson.databind, java.rmi;
    opens it.polimi.ingsw.is25am33.client to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.client.view.gui;
    opens it.polimi.ingsw.is25am33.client.view.gui to javafx.fxml;
    exports it.polimi.ingsw.is25am33.client.view.gui.viewControllers;
    opens it.polimi.ingsw.is25am33.client.view.gui.viewControllers to javafx.fxml;
    exports it.polimi.ingsw.is25am33.client.view.tui;
    exports it.polimi.ingsw.is25am33.client.model;
    opens it.polimi.ingsw.is25am33.client.model to com.fasterxml.jackson.databind;
    exports it.polimi.ingsw.is25am33.client.model.card;
}