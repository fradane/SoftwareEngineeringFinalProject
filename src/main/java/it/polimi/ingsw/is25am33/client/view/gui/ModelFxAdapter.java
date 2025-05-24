package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.model.component.Component;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class ModelFxAdapter {
    private final ClientModel clientModel;
    private final ObjectProperty<Component>[][] observableMatrix;
    private final ObjectProperty<Component> observableFocusedComponent;
    private final ObjectProperty<Component> observableReservedComponent1;
    private final ObjectProperty<Component> observableReservedComponent2;
    private final ObjectProperty<Integer> observableTimer;
    private final ObjectProperty<Integer> observableFlipsLeft;
    private final ObservableList<String> observableVisibleComponets;

    @SuppressWarnings("unchecked")
    public ModelFxAdapter(ClientModel clientModel) {
        this.clientModel = clientModel;
        clientModel.setModelFxAdapter(this);
        this.observableMatrix = new ObjectProperty[12][12];
        this.observableFocusedComponent = new SimpleObjectProperty<>();
        this.observableReservedComponent1 = new SimpleObjectProperty<>();
        this.observableReservedComponent2 = new SimpleObjectProperty<>();
        this.observableTimer = new SimpleObjectProperty<>();
        this.observableFlipsLeft = new SimpleObjectProperty<>();
        this.observableVisibleComponets = FXCollections.observableArrayList();

        // Initialize an observable matrix and bind it to a model matrix
        Component[][] rawMatrix = clientModel.getMyShipboard().getShipMatrix();
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

    public ObjectProperty<Component> getObservableFocusedComponent() {
        return observableFocusedComponent;
    }

    public ObjectProperty<Component> getObservableReservedComponent1() {
        return observableReservedComponent1;
    }

    public ObjectProperty<Component> getObservableReservedComponent2() {
        return observableReservedComponent2;
    }

    public ObjectProperty<Integer> getObservableTimer() {
        return observableTimer;
    }

    public ObjectProperty<Integer> getObservableFlipsLeft() {
        return observableFlipsLeft;
    }

    public ObservableList<String> getObservableVisibleComponets() {
        return observableVisibleComponets;
    }

    /**
     * Syncs the observable matrix with the shipBoard matrix.
     * Call this method after model updates.
     */
    public void refreshShipBoard() {
        Component[][] rawMatrix = clientModel.getMyShipboard().getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                observableMatrix[i][j].set(rawMatrix[i][j]);
            }
        }

        List<Component> bookedComponents = clientModel.getMyShipboard().getBookedComponents();

        observableFocusedComponent.set(clientModel.getMyShipboard().getFocusedComponent());
        observableReservedComponent1.set(
                !bookedComponents.isEmpty() ? bookedComponents.getFirst() : null
        );
        observableReservedComponent2.set(
                bookedComponents.size() > 1 ? bookedComponents.get(1) : null
        );
    }

    public void refreshTimer(int timeLeft, int flipsLeft) {
        observableTimer.set(timeLeft);
        observableTimer.set(flipsLeft);
    }

    public void refreshVisibleComponents() {
        observableVisibleComponets.clear();
        observableVisibleComponets.addAll(
                clientModel.getVisibleComponents()
                        .keySet()
                        .stream()
                        .map(index -> clientModel.getVisibleComponents().get(index))
                        .map(component -> component.toString().trim().split("\\n")[0])
                        .toList()
        );
    }

}