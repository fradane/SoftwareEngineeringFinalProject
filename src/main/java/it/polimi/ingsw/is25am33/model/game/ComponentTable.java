package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.ComponentLoader;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ComponentTable {

    private final Stack<Component> hiddenComponents = new Stack<>();
    private final Map<Integer, Component> visibleComponents = new ConcurrentHashMap<>();
    private Integer currVisibleIndex = 1;

    public ComponentTable() {
        hiddenComponents.addAll(ComponentLoader.loadComponents());
        Collections.shuffle(hiddenComponents);
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
            currVisibleIndex++;
        }
    }

    public Component pickVisibleComponent(int index){
        return visibleComponents.remove(index);
    }

    public Stream<Pair<Integer, Component>> getVisibleComponentsAsStream(){
        return visibleComponents.keySet().stream().map(index -> new Pair<>(index, visibleComponents.get(index)));
    }

}