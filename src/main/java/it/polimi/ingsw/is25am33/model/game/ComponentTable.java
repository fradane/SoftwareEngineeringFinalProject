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
        try {
            // although visibleComponents it's already synchronized, this method needs currVisibleIndex to be synchronized too,
            synchronized (visibleComponents) {
                component.setCurrState(ComponentState.VISIBLE);
                visibleComponents.put(currVisibleIndex, component);
                currVisibleIndex++;

                for (String s : gameContext.getClientControllers().keySet()) {
                    gameContext.getClientControllers().get(s).notifyVisibleComponents(s, visibleComponents);
                }

            }
        }
        catch(RemoteException e){
            System.err.println("Remote Exception");
        }
    }

    public Component pickVisibleComponent(int index) {
        try {
            for (String s : gameContext.getClientControllers().keySet()) {
                gameContext.getClientControllers().get(s).notifyVisibleComponents(s, visibleComponents);
            }
        } catch (RemoteException e) {
            System.err.println("Remote Exception");
        }
        return visibleComponents.remove(index);
    }

    public Stream<Pair<Integer, Component>> getVisibleComponentsAsStream(){
        return visibleComponents.keySet().stream().map(index -> new Pair<>(index, visibleComponents.get(index)));
    }

    public Map<Integer, Component> getVisibleComponents() {
        return visibleComponents;
    }

}