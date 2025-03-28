package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.ComponentState;

import java.util.*;

public abstract class Component {
    private ComponentState currState;
    private int rotation;
    private Map<Direction, ConnectorType> connectors;

    public Component(Map<Direction, ConnectorType> connectors){
        currState = ComponentState.FREE;
        this.connectors = connectors;
        rotation = 0;
    }

    public ComponentState getCurrState() {
        return currState;
    }

    public void setCurrState(ComponentState currState) {
        this.currState = currState;
    }

    public int getRotation() {
        return rotation;
    }

    public void rotate() {
        this.rotation = getRotation() + 1;
    }

    public Map<Direction,ConnectorType> getConnectors() {

        return this.connectors;
    }


    public void changeOrientation() {
        int rotation = getRotation()%4;
        if(rotation!=0) {
            List<Direction> keys = new ArrayList<Direction>(connectors.keySet());
            List<ConnectorType> values= new ArrayList<>(connectors.values());

            Collections.rotate(values,rotation);

            for(int i=0; i< keys.size();i++){
                connectors.put(keys.get(i), values.get(i));
            }
        }
    }

    public void insertInComponentsMap(Map<Class<?>, List<Object>> map){
        map.computeIfAbsent(this.getClass(), k -> new ArrayList<>()).add(this);
    }
}