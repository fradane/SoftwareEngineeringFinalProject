package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameClientNotifier;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.ComponentLoader;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * ComponentTable manages a collection of hidden and visible components, acting as a central data structure
 * to handle component visibility and selection in a synchronized manner.
 * It provides functionality to shuffle, add, remove, and retrieve components while ensuring thread safety.
 */
public class ComponentTable {

    /**
     * A stack used to store components that are currently hidden and not visible to clients.
     * Components are managed in last-in-first-out (LIFO) order, ensuring that the last component
     * added to the stack is the first one to be retrieved.
     *
     * This stack is primarily used for managing the lifecycle of components in the system, allowing
     * hidden components to be revealed or processed upon request. The stack is synchronized where necessary
     * to maintain thread safety during component interactions.
     */
    private final Stack<Component> hiddenComponents = new Stack<>();
    /**
     * A thread-safe map that maintains the currently visible components in the system.
     * The map associates a unique integer index with each visible {@code Component}.
     * This structure enables efficient addition, removal, and lookup of visible components
     * while ensuring data consistency in a multi-threaded environment.
     */
    private final Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    /**
     * Tracks the current index used to store visible components in the collection of visible components.
     * This index is incremented after a new component is added to ensure uniqueness
     * for each component's position within the visible components map.
     *
     * It plays a crucial role in assigning and managing the position of components
     * visible to the game clients and ensures synchronization when used in
     * multi-threaded environments.
     */
    private Integer currVisibleIndex = 1;
    /**
     * Manages communication with connected game clients by sending notifications about game state changes,
     * such as adding or removing visible components or handling player disconnections.
     * It ensures consistent updates to all clients by leveraging the {@link GameClientNotifier}.
     *
     * This variable enables interaction between the ComponentTable and game clients, ensuring that
     * changes in the game state are communicated reliably while accounting for potential client-side
     * latency or disconnection issues.
     */
    private GameClientNotifier gameClientNotifier;

    /**
     * Constructs a new ComponentTable instance.
     *
     * This constructor initializes the `hiddenComponents` stack by loading a collection
     * of components through the `ComponentLoader` and shuffling their order to ensure randomness.
     */
    public ComponentTable() {
        hiddenComponents.addAll(ComponentLoader.loadComponents());
        Collections.shuffle(hiddenComponents);
    }

    /**
     * Sets the {@code GameClientNotifier} instance for this {@code ComponentTable}.
     * The {@code GameClientNotifier} is responsible for notifying all connected clients
     * about updates or changes to the visible components.
     *
     * @param gameClientNotifier the {@code GameClientNotifier} instance to be used for client notifications
     */
    public void setGameClientNotifier(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    /**
     * Retrieves and removes the top component from the stack of hidden components
     * managed by the ComponentTable. If the stack of hidden components is empty,
     * the method returns null. This operation is thread-safe.
     *
     * @return the next hidden Component if available, or null if the stack of hidden components is empty
     */
    public Component pickHiddenComponent(){
        synchronized (hiddenComponents) {
            try {
                return hiddenComponents.pop();
            } catch (EmptyStackException e) {
                return null;
            }
        }
    }

    /**
     * Adds a new visible component to the collection of visible components.
     * The method is synchronized to ensure thread safety for both the visibleComponents map
     * and the currVisibleIndex counter.
     * After adding the component, it notifies all connected clients about the new visible component.
     *
     * @param component the component to be added to the collection of visible components
     */
    public void addVisibleComponent(Component component){
        synchronized (visibleComponents) {
            visibleComponents.put(currVisibleIndex, component);

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyAddVisibleComponents(nicknameToNotify, currVisibleIndex, component);
            });
            currVisibleIndex++;
        }
    }

    /**
     * Removes and returns the visible component at the specified index.
     * If no component exists at the given index, the method returns null and notifies
     * the affected player that the component is no longer available.
     * Additionally, all connected clients are notified about the removal of the component.
     *
     * @param index the index of the visible component to be picked
     * @return the component removed from the specified index, or null if no component exists at the index
     */
    public Component pickVisibleComponent(int index) {

        Component pickedComponent = visibleComponents.remove(index);

        // no more components available
        if (pickedComponent == null)
            return null;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRemoveVisibleComponents(nicknameToNotify, index);
        });

        return pickedComponent;
    }

    /**
     * Retrieves a map of all currently visible components.
     *
     * The map's keys represent unique indices associated with each visible component,
     * while the values are the corresponding {@code Component} objects.
     *
     * @return a map containing the indices and associated visible components.
     */
    public Map<Integer, Component> getVisibleComponents() {
        return visibleComponents;
    }

}