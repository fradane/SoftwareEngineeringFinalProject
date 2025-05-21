package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.model.component.Component;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ShipBoardFxAdapter {
    private final ShipBoardClient shipBoardClient;
    private final ObjectProperty<Component>[][] observableMatrix;
    private final ObjectProperty<Component> focusedComponent;

    @SuppressWarnings("unchecked")
    public ShipBoardFxAdapter(ShipBoardClient shipBoardClient) {
        this.shipBoardClient = shipBoardClient;
        this.observableMatrix = new ObjectProperty[12][12];
        this.focusedComponent = new SimpleObjectProperty<>();

        // Initialize an observable matrix and bind it to a model matrix
        Component[][] rawMatrix = shipBoardClient.getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                SimpleObjectProperty<Component> prop = new SimpleObjectProperty<>(rawMatrix[i][j]);
                observableMatrix[i][j] = prop;
            }
        }

    }

    public ObjectProperty<Component>[][] getObservableMatrix() {
        return observableMatrix;
    }

    public ObjectProperty<Component> getFocusedComponentProperty() {
        return focusedComponent;
    }



    /**
     * Syncs the observable matrix with the shipBoard matrix.
     * Call this method after model updates.
     */
    public void refreshMatrix() {
        Component[][] rawMatrix = shipBoardClient.getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                observableMatrix[i][j].set(rawMatrix[i][j]);
            }
        }

        focusedComponent.set(shipBoardClient.getFocusedComponent());
    }

}