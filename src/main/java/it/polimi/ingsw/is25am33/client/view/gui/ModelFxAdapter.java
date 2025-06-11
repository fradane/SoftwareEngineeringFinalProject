package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
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

    @SuppressWarnings("unchecked")
    public ModelFxAdapter(ClientModel clientModel) {
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

        // initialize shipboards
        clientModel.getPlayerClientData()
                .forEach((nickname, playerClientData) -> {
                    observableShipBoards.put(nickname, new ObjectProperty[12][12]);
                    Component[][] shipMatrix = playerClientData.getShipBoard().getShipMatrix();
                    for (int i = 0; i < 12; i++) {
                        for (int j = 0; j < 12; j++) {
                            if (i == 6 && j == 6)
                                System.out.println(shipMatrix[i][j]);
                            SimpleObjectProperty<Component> prop = new SimpleObjectProperty<>(shipMatrix[i][j]);
                            observableShipBoards.get(nickname)[i][j] = prop;
                        }
                    }
                    observableBookedComponents.put(nickname, new Pair<>(new SimpleObjectProperty<>(), new SimpleObjectProperty<>()));
                });

    }



    /* -------------- GETTERS -------------- */

    public ObjectProperty<Component>[][] getObservableShipBoardOf(String nickname) {
        return observableShipBoards.get(nickname);
    }

    public ObjectProperty<Component>[][] getMyObservableMatrix() {
        return observableShipBoards.get(clientModel.getMyNickname());
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
        return observableBookedComponents.get(nickname);
    }



    /* -------------- REFRESH METHODS -------------- */

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

        if (nickname.equals(clientModel.getMyNickname()))
            observableFocusedComponent.set(clientModel.getMyShipboard().getFocusedComponent());

        // refresh the shipboard
        Component[][] rawMatrix = clientModel.getShipboardOf(nickname).getShipMatrix();
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                if (i == 6 && j == 6) System.out.println(rawMatrix[i][j]);
                observableShipBoards.get(nickname)[i][j].set(rawMatrix[i][j]);
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

    public void refreshRanking() {
        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    if (observableColorRanking.containsKey(playerColor))
                        observableColorRanking.get(playerColor).set(position);
                });
    }

    public void refreshCurrAdventureCard() {
        observableCurrAdventureCard.set(clientModel.getCurrAdventureCard());
    }

}