package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelFxAdapter {
    private final ClientModel clientModel;
    private final ObjectProperty<Component> observableFocusedComponent;
    private final ObjectProperty<Integer> observableTimer;
    private final ObjectProperty<Integer> observableFlipsLeft;
    private final ObservableList<String> observableVisibleComponents;
    private final Map<String, ObjectProperty<Component>[][]> observableShipBoards;
    private final Map<String, Pair<ObjectProperty<Component>, ObjectProperty<Component>>> observableBookedComponents;
    private final Map<PlayerColor, ObjectProperty<Integer>> observableColorRanking;
    private final ObjectProperty<ClientCard> observableCurrAdventureCard;
    private final ObjectProperty<Pair<String, Coordinates>> observableChangedAttributes;
    private final Boolean isCardAdapter;
    private final Object rankingLock = new Object();
    private final Object visibleComponentsLock  = new Object();

    @SuppressWarnings("unchecked")
    public ModelFxAdapter(ClientModel clientModel, Boolean isCardAdapter) {
        this.clientModel = clientModel;
        clientModel.setModelFxAdapter(this);

        this.observableFocusedComponent = new SimpleObjectProperty<>();
        this.observableTimer = new SimpleObjectProperty<>();
        this.observableFlipsLeft = new SimpleObjectProperty<>();
        this.observableVisibleComponents = FXCollections.observableArrayList();
        this.observableShipBoards = new ConcurrentHashMap<>();
        this.observableColorRanking = new ConcurrentHashMap<>();
        this.observableBookedComponents = new ConcurrentHashMap<>();
        this.observableCurrAdventureCard = new SimpleObjectProperty<>();
        this.observableChangedAttributes = new SimpleObjectProperty<>();
        this.isCardAdapter = isCardAdapter;

        // initialize shipboards
        clientModel.getPlayerClientData()
                .forEach((nickname, _) -> {
                    observableShipBoards.put(nickname, new ObjectProperty[12][12]);
                    for (int i = 0; i < 12; i++) {
                        for (int j = 0; j < 12; j++) {
                            //if (i == 6 && j == 6) System.out.println(shipMatrix[i][j]);
                            SimpleObjectProperty<Component> prop = new SimpleObjectProperty<>();
                            observableShipBoards.get(nickname)[i][j] = prop;
                        }
                    }
                    observableBookedComponents.put(nickname, new Pair<>(new SimpleObjectProperty<>(), new SimpleObjectProperty<>()));
                });

    }



    /* -------------- GETTERS -------------- */

    public ObjectProperty<Component>[][] getObservableShipBoardOf(String nickname) {
        synchronized (observableShipBoards.get(nickname)) {
            return observableShipBoards.get(nickname);
        }
    }

    public ObjectProperty<Component>[][] getMyObservableMatrix() {
        synchronized (observableShipBoards.get(clientModel.getMyNickname())) {
            return observableShipBoards.get(clientModel.getMyNickname());
        }
    }

    public ObjectProperty<ClientCard> getObservableCurrAdventureCard() {
        return observableCurrAdventureCard;
    }

    public ObjectProperty<Component> getObservableFocusedComponent() {
        return observableFocusedComponent;
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

    public Pair<ObjectProperty<Component>, ObjectProperty<Component>> getObservableBookedComponentsOf(String nickname) {
        synchronized (observableShipBoards.get(nickname)) {
            return observableBookedComponents.get(nickname);
        }
    }

    public ObjectProperty<Pair<String, Coordinates>> getObservableChangedAttributesProperty() {
        return observableChangedAttributes;
    }

    public Boolean isCardAdapter() {
        return isCardAdapter;
    }

    /* -------------- REFRESH METHODS -------------- */

    public void refreshTimer(int timeLeft, int flipsLeft) {
        observableTimer.set(timeLeft);
        observableFlipsLeft.set(flipsLeft);
    }

    public void refreshVisibleComponents() {
        observableVisibleComponents.clear();
        synchronized (visibleComponentsLock) {
            observableVisibleComponents.addAll(
                    clientModel.getVisibleComponents()
                            .keySet()
                            .stream()
                            .map(index -> clientModel.getVisibleComponents().get(index))
                            .map(component -> component.toString().trim().split("\\n")[0])
                            .toList()
            );
        }
    }

    public void refreshShipBoardOf(String nickname) {

        if (nickname.equals(clientModel.getMyNickname()))
            observableFocusedComponent.set(clientModel.getMyShipboard().getFocusedComponent());

        synchronized (observableShipBoards.get(nickname)) {

            // refresh the shipboard
            Component[][] rawMatrix = clientModel.getShipboardOf(nickname).getShipMatrix();

            for (int i = 0; i < 12; i++) {
                for (int j = 0; j < 12; j++) {

                    Component oldComponent = getObservableShipBoardOf(nickname)[i][j].get();
                    Component newComponent = rawMatrix[i][j];

                    if ((oldComponent == null || newComponent == null)) {
                        if (oldComponent == null && newComponent == null)
                            continue;
                        getObservableShipBoardOf(nickname)[i][j].set(newComponent);
                    } else if (!oldComponent.getGuiHash().equals(newComponent.getGuiHash())) {
                        synchronized (observableChangedAttributes) {
                            observableChangedAttributes.set(new Pair<>(nickname, new Coordinates(i, j)));
                        }
                    }

                }
            }

            // refresh the booked components
            List<Component> bookedComponents = clientModel.getShipboardOf(nickname).getBookedComponents();


            observableBookedComponents.get(nickname)
                    .getKey()
                    .set(
                            !bookedComponents.isEmpty() ? bookedComponents.getFirst() : null
                    );

            observableBookedComponents.get(nickname)
                    .getValue()
                    .set(
                            bookedComponents.size() > 1 ? bookedComponents.get(1) : null
                    );
        }

    }

    public void refreshRanking() {
        synchronized (rankingLock) {
            clientModel.getColorRanking()
                    .forEach((playerColor, position) -> {
                        if (observableColorRanking.containsKey(playerColor))
                            observableColorRanking.get(playerColor).set(position);
                    });
        }
    }

    public void refreshCurrAdventureCard() {
        observableCurrAdventureCard.set(clientModel.getCurrAdventureCard());
    }

}