package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.GameContext;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.ComponentLoader;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ComponentTable {

    private final Stack<Component> hiddenComponents = new Stack<>();
    private final Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private Integer currVisibleIndex = 1;
    private GameContext gameContext;

    public ComponentTable() {
        hiddenComponents.addAll(ComponentLoader.loadComponents());
        Collections.shuffle(hiddenComponents);
    }

    public void setGameContext(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    public Component pickHiddenComponent(){
        synchronized (hiddenComponents) {
            try {
                return hiddenComponents.pop();
            } catch (EmptyStackException e) {
                return null;
            }
        }
    }

    public void addVisibleComponent(Component component){
        // although visibleComponents it's already synchronized, this method needs currVisibleIndex to be synchronized too,
        synchronized (visibleComponents) {
            visibleComponents.put(currVisibleIndex, component);

            gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyAddVisibleComponents(nicknameToNotify, currVisibleIndex, component);
            });
            currVisibleIndex++;
        }
    }

    public Component pickVisibleComponent(int index) {

        Component pickedComponent = visibleComponents.remove(index);

        if (pickedComponent == null) {
            // TODO notifica al singolo giocatore che non è piu disponibile
            return null;
        }

        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRemoveVisibleComponents(nicknameToNotify, index);
        });

        return pickedComponent;
    }

    public Stream<Pair<Integer, Component>> getVisibleComponentsAsStream(){
        return visibleComponents.keySet().stream().map(index -> new Pair<>(index, visibleComponents.get(index)));
    }

    public Map<Integer, Component> getVisibleComponents() {
        return visibleComponents;
    }

}