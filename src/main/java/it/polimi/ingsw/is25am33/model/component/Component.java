package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract representation of a generic component within the system.
 * A component has state, orientation, and directional connectors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatteryBox.class, name = "BatteryBox"),
        @JsonSubTypes.Type(value = Cabin.class, name = "Cabin"),
        @JsonSubTypes.Type(value = Cannon.class, name = "Cannon"),
        @JsonSubTypes.Type(value = DoubleCannon.class, name = "DoubleCannon"),
        @JsonSubTypes.Type(value = DoubleEngine.class, name = "DoubleEngine"),
        @JsonSubTypes.Type(value = Engine.class, name = "Engine"),
        @JsonSubTypes.Type(value = LifeSupport.class, name = "LifeSupport"),
        @JsonSubTypes.Type(value = MainCabin.class, name = "MainCabin"),
        @JsonSubTypes.Type(value = Shield.class, name = "Shield"),
        @JsonSubTypes.Type(value = SpecialStorage.class, name = "SpecialStorage"),
        @JsonSubTypes.Type(value = StandardStorage.class, name = "StandardStorage"),
        @JsonSubTypes.Type(value = StructuralModules.class, name = "StructuralModules")
})
public abstract class Component implements Serializable {

    protected String imageName;

    /**
     * Current operational state of the component.
     */
    private ComponentState currState = ComponentState.HIDDEN;

    /**
     * Current rotation/orientation of the component.
     */
    private int rotation = 0;

    /**
     * Map associating directions with their respective connector types.
     */
    private Map<Direction, ConnectorType> connectors;

    @com.fasterxml.jackson.annotation.JsonProperty("type")
    protected String type;

    /**
     * Default constructor for {@code Component}.
     */
    public Component() {
    }

    /**
     * Constructs a Component with specified directional connectors.
     *
     * <p>The initial rotation is zero.</p>
     *
     * @param connectors a map associating directions to their connector types
     */
    public Component(Map<Direction, ConnectorType> connectors) {
        this.connectors = connectors;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
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
     * Retrieves the map of connectors for this component, associating directions to connector types.
     *
     * @return the map of directional connectors
     */
    public Map<Direction, ConnectorType> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<Direction, ConnectorType> connectors) {
        this.connectors = connectors;
    }

    /**
     * Adjusts the orientation of connectors based on the component's current rotation.
     *
     * <p>If rotation is a multiple of 4 (original orientation), no changes occur. Otherwise,
     * connectors' positions are rotated accordingly.</p>
     */
    public void rotate() {

        List<Direction> keys = new ArrayList<>(connectors.keySet());
        List<ConnectorType> values = new ArrayList<>(connectors.values());
        Collections.rotate(values, 1);
        for (int i = 0; i < keys.size(); i++) {
            connectors.put(keys.get(i), values.get(i));
        }
        rotation = (rotation + 1) % 4;

    }

    /**
     * Inserts the current instance into a map of components, where the key is the class type of the instance,
     * and the value is a list of objects of that class.
     * If the class type is not already present in the map, a new list is created.
     *
     * @param map a map where the key is a {@code Class<?>} representing the component's class type,
     *            and the value is a list of objects of that class type
     */
    public void insertInComponentsMap(Map<Class<?>, List<Component>> map) {
        map.computeIfAbsent(this.getClass(), k -> new ArrayList<>()).add(this);
    }

    @JsonIgnore
    public abstract String getLabel();

    @JsonIgnore
    public abstract String getMainAttribute();

}