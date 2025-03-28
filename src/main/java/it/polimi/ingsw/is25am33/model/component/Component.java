package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;
import it.polimi.ingsw.is25am33.model.ComponentState;

import java.util.*;

/**
 * Abstract representation of a generic component within the system.
 * A component has a state, orientation, and directional connectors.
 */
public abstract class Component {

    /** Current operational state of the component. */
    private ComponentState currState;

    /** Current rotation/orientation of the component. */
    private int rotation;

    /** Map associating directions with their respective connector types. */
    private Map<Direction, ConnectorType> connectors;

    /**
     * Constructs a Component with specified directional connectors.
     *
     * <p>The initial state is set to {@link ComponentState#FREE}, and initial rotation is zero.</p>
     *
     * @param connectors a map associating directions to their connector types
     */
    public Component(Map<Direction, ConnectorType> connectors) {
        this.currState = ComponentState.FREE;
        this.connectors = connectors;
        this.rotation = 0;
    }

    /**
     * Retrieves the current operational state of this component.
     *
     * @return the current state
     */
    public ComponentState getCurrState() {
        return currState;
    }

    /**
     * Sets the operational state of this component.
     *
     * @param currState the new state to set
     */
    public void setCurrState(ComponentState currState) {
        this.currState = currState;
    }

    /**
     * Retrieves the current rotation value of the component.
     *
     * @return the rotation value
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Increments the rotation of this component by one unit.
     */
    public void rotate() {
        this.rotation = getRotation() + 1;
    }

    /**
     * Retrieves the map of connectors for this component, associating directions to connector types.
     *
     * @return the map of directional connectors
     */
    public Map<Direction, ConnectorType> getConnectors() {
        return connectors;
    }

    /**
     * Adjusts the orientation of connectors based on the component's current rotation.
     *
     * <p>If rotation is a multiple of 4 (original orientation), no changes occur. Otherwise,
     * connectors' positions are rotated accordingly.</p>
     */
    public void changeOrientation() {
        int rotation = getRotation() % 4;
        if (rotation != 0) {
            List<Direction> keys = new ArrayList<>(connectors.keySet());
            List<ConnectorType> values = new ArrayList<>(connectors.values());

            Collections.rotate(values, rotation);

            for (int i = 0; i < keys.size(); i++) {
                connectors.put(keys.get(i), values.get(i));
            }
        }
    }

    public void insertInComponentsMap(Map<Class<?>, List<Object>> map){
        map.computeIfAbsent(this.getClass(), k -> new ArrayList<>()).add(this);
    }
}
