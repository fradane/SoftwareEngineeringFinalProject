package it.polimi.ingsw.is25am33.client.view.gui;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.view.gui.viewControllers.BoardsController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import javafx.application.Platform;
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
    private final Map<String, ObjectProperty<Integer>> observableLostComponents;
    private final ObjectProperty<ClientCard> observableCurrAdventureCard;
    private final BoardsController boardsController;
    private final Boolean isCardAdapter;
    private final Object rankingLock = new Object();
    private final Object visibleComponentsLock  = new Object();
    private final ObjectProperty<Integer> observableCosmicCredits;

    /**
     * Creates a new ModelFxAdapter instance.
     *
     * @param clientModel the client model to adapt
     * @param isCardAdapter whether this adapter is for the card-related phase
     * @param boardsController the boards controller for UI updates
     */
    @SuppressWarnings("unchecked")
    public ModelFxAdapter(ClientModel clientModel, Boolean isCardAdapter, BoardsController boardsController) {
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
        this.observableLostComponents = new ConcurrentHashMap<>();
        this.observableCosmicCredits = new SimpleObjectProperty<>(0);
        this.boardsController = boardsController;
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
                    observableLostComponents.put(nickname, new SimpleObjectProperty<>());
                    observableBookedComponents.put(nickname, new Pair<>(new SimpleObjectProperty<>(), new SimpleObjectProperty<>()));
                });

    }



    /* -------------- GETTERS -------------- */

    /**
     * Gets the observable ship board matrix for a specific player.
     *
     * @param nickname the nickname of the player
     * @return the observable ship board matrix
     */
    public ObjectProperty<Component>[][] getObservableShipBoardOf(String nickname) {
        synchronized (observableShipBoards.get(nickname)) {
            return observableShipBoards.get(nickname);
        }
    }

    /**
     * Gets the observable ship board matrix for the current player.
     *
     * @return the observable ship board matrix for the current player
     */
    public ObjectProperty<Component>[][] getMyObservableMatrix() {
        synchronized (observableShipBoards.get(clientModel.getMyNickname())) {
            return observableShipBoards.get(clientModel.getMyNickname());
        }
    }

    /**
     * Gets the observable current adventure card.
     *
     * @return the observable current adventure card property
     */
    public ObjectProperty<ClientCard> getObservableCurrAdventureCard() {
        return observableCurrAdventureCard;
    }

    /**
     * Gets the observable focused component.
     *
     * @return the observable focused component property
     */
    public ObjectProperty<Component> getObservableFocusedComponent() {
        return observableFocusedComponent;
    }

    /**
     * Gets the observable timer value.
     *
     * @return the observable timer property
     */
    public ObjectProperty<Integer> getObservableTimer() {
        return observableTimer;
    }

    /**
     * Gets the observable hourglass flips left value.
     *
     * @return the observable flips left property
     */
    public ObjectProperty<Integer> getObservableFlipsLeft() {
        return observableFlipsLeft;
    }

    /**
     * Gets the observable list of visible components.
     *
     * @return the observable visible components list
     */
    public ObservableList<String> getObservableVisibleComponents() {
        return observableVisibleComponents;
    }

    /**
     * Gets the observable color ranking map.
     *
     * @return the observable color ranking map
     */
    public Map<PlayerColor, ObjectProperty<Integer>> getObservableColorRanking() {
        return observableColorRanking;
    }

    /**
     * Gets the observable booked components for a specific player.
     *
     * @param nickname the nickname of the player
     * @return a pair of observable component properties representing booked components
     */
    public Pair<ObjectProperty<Component>, ObjectProperty<Component>> getObservableBookedComponentsOf(String nickname) {
        synchronized (observableShipBoards.get(nickname)) {
            return observableBookedComponents.get(nickname);
        }
    }

    /**
     * Gets the observable lost components map.
     *
     * @return the observable lost components map
     */
    public Map<String, ObjectProperty<Integer>> getObservableLostComponents() {
        return observableLostComponents;
    }

    /**
     * Gets the observable cosmic credits value.
     *
     * @return the observable cosmic credits property
     */
    public ObjectProperty<Integer> getObservableCosmicCredits() {
        return observableCosmicCredits;
    }

    /**
     * Checks if this adapter is for card-related operations.
     *
     * @return true if this is a card adapter, false otherwise
     */
    public Boolean isCardAdapter() {
        return isCardAdapter;
    }

    /* -------------- REFRESH METHODS -------------- */

    /**
     * Refreshes the timer and flips left values.
     *
     * @param timeLeft the time remaining in seconds
     * @param flipsLeft the number of hourglass flips remaining
     */
    public void refreshTimer(int timeLeft, int flipsLeft) {
        observableTimer.set(timeLeft);
        observableFlipsLeft.set(flipsLeft);
    }

    /**
     * Refreshes the visible components list from the client model.
     */
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

    /**
     * Refreshes the ship board data for a specific player.
     *
     * @param nickname the nickname of the player whose ship board to refresh
     */
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
                    } else { //if (!oldComponent.getGuiHash().equals(newComponent.getGuiHash())) {
                        boardsController.updateShipBoards(nickname, i, j, newComponent);
                    }

                }
            }

            // refresh the booked components
            List<Component> bookedComponents = clientModel.getShipboardOf(nickname).getBookedComponents();

            observableLostComponents.get(nickname).set(bookedComponents.size());

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

    /**
     * Refreshes the color ranking from the client model.
     */
    public void refreshRanking() {
        synchronized (rankingLock) {
            clientModel.getColorRanking()
                    .forEach((playerColor, position) -> {
                        if (observableColorRanking.containsKey(playerColor))
                            observableColorRanking.get(playerColor).set(position);
                    });
        }
    }

    /**
     * Refreshes the current adventure card from the client model.
     */
    public void refreshCurrAdventureCard() {
        observableCurrAdventureCard.set(clientModel.getCurrAdventureCard());
    }

    /**
     * Refreshes the cosmic credits value from the client model.
     */
    public void refreshCosmicCredits() {
        if (clientModel.getMyNickname() != null) {
            int currentCredits = clientModel.getMyCosmicCredits();
            observableCosmicCredits.set(currentCredits);
        }
    }
}