package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelFxAdapter {
    private final ClientModel clientModel;
    private final ObjectProperty<Component>[][] mineObservableMatrix;
    private final ObjectProperty<Component> observableFocusedComponent;
    private final ObjectProperty<Component> observableReservedComponent1;
    private final ObjectProperty<Component> observableReservedComponent2;
    private final ObjectProperty<Integer> observableTimer;
    private final ObjectProperty<Integer> observableFlipsLeft;
    private final ObservableList<String> observableVisibleComponents;
    private final Map<String, ObjectProperty<Component>[][]> observableShipBoards;
    private final Map<PlayerColor, ObjectProperty<Integer>> observableColorRanking;

    @SuppressWarnings("unchecked")
    public ModelFxAdapter(ClientModel clientModel) {
        this.clientModel = clientModel;
        clientModel.setModelFxAdapter(this);

        this.mineObservableMatrix = new ObjectProperty[12][12];
        this.observableFocusedComponent = new SimpleObjectProperty<>();
        this.observableReservedComponent1 = new SimpleObjectProperty<>();
        this.observableReservedComponent2 = new SimpleObjectProperty<>();
        this.observableTimer = new SimpleObjectProperty<>();
        this.observableFlipsLeft = new SimpleObjectProperty<>();
        this.observableVisibleComponents = FXCollections.observableArrayList();
        this.observableShipBoards = new ConcurrentHashMap<>();
        this.observableColorRanking = new ConcurrentHashMap<>();

        // initialize other shipboards
        clientModel.getPlayerClientData()
                .forEach((nickname, playerClientData) -> {
                    observableShipBoards.put(nickname, new ObjectProperty[12][12]);
                    Component[][] shipMatrix = playerClientData.getShipBoard().getShipMatrix();
                    for (int i = 0; i < 12; i++) {
                        for (int j = 0; j < 12; j++) {
                            if (i == 6 && j == 6) System.out.println(shipMatrix[i][j]);
                            SimpleObjectProperty<Component> prop = new SimpleObjectProperty<>(shipMatrix[i][j]);
                            observableShipBoards.get(nickname)[i][j] = prop;
                        }
                    }
                });

        // initialize my shipboard
        Component[][] rawMatrix = clientModel.getMyShipboard().getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                SimpleObjectProperty<Component> prop = new SimpleObjectProperty<>(rawMatrix[i][j]);
                mineObservableMatrix[i][j] = prop;
            }
        }

    }

    public ObjectProperty<Component>[][] getObservableShipBoardOf(String nickname) {
        return observableShipBoards.get(nickname);
    }

    public ObjectProperty<Component>[][] getMineObservableMatrix() {
        return mineObservableMatrix;
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

    public ObservableList<String> getObservableVisibleComponents() {
        return observableVisibleComponents;
    }

    public Map<PlayerColor, ObjectProperty<Integer>> getObservableColorRanking() {
        return observableColorRanking;
    }

    /**
     * Syncs the observable matrix with the shipBoard matrix.
     * Call this method after model updates.
     */
    private void refreshMyShipBoard() {
        Component[][] rawMatrix = clientModel.getMyShipboard().getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                mineObservableMatrix[i][j].set(rawMatrix[i][j]);
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
        observableFlipsLeft.set(flipsLeft);
    }

    public void refreshVisibleComponents() {
        observableVisibleComponents.clear();
        observableVisibleComponents.addAll(
                clientModel.getVisibleComponents()
                        .keySet()
                        .stream()
                        .map(index -> clientModel.getVisibleComponents().get(index))
                        .map(component -> component.toString().trim().split("\\n")[0])
                        .toList()
        );
    }

    public void refreshShipBoardOf(String nickname) {

        if (nickname.equals(clientModel.getMyNickname())) {
            refreshMyShipBoard();
            return;
        }

        Component[][] rawMatrix = clientModel.getShipboardOf(nickname).getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                if (i == 6 && j == 6) System.out.println(rawMatrix[i][j]);
                observableShipBoards.get(nickname)[i][j].set(rawMatrix[i][j]);
            }
        }

        // TODO booked component

//        List<Component> bookedComponents = clientModel.getShipboardOf(nickname).getBookedComponents();
//
//        observableReservedComponent1.set(
//                !bookedComponents.isEmpty() ? bookedComponents.getFirst() : null
//        );
//        observableReservedComponent2.set(
//                bookedComponents.size() > 1 ? bookedComponents.get(1) : null
//        );

    }

    public void refreshRanking() {
        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    if (observableColorRanking.containsKey(playerColor))
                        observableColorRanking.get(playerColor).set(position);
                });
    }

}