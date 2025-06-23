package it.polimi.ingsw.is25am33.model.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.is25am33.model.enumFiles.ConnectorType;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;
import it.polimi.ingsw.is25am33.model.enumFiles.ComponentState;
import org.jetbrains.annotations.NotNull;

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

    /**
     * The name of the image associated with this component.
     *
     * This variable holds a string representing the identifier or file name of the
     * image used to visually represent this component. It can be set or retrieved
     * using the appropriate getter and setter methods.
     */
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

    /**
     * Retrieves the name of the image associated with this component.
     *
     * @return a string representing the image name
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Sets the name of the image associated with this component.
     *
     * @param imageName the name of the image to associate with this component
     */
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

    /**
     * Updates the map of directional connectors for this component.
     *
     * @param connectors a map associating {@code Direction} enums to {@code ConnectorType} enums
     *                   representing the new configuration of connectors for the component
     */
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

    /**
     * Compares this Component to the specified object for equality.
     * The comparison is based on the `imageName` field.
     *
     * @param o the object to compare with this Component
     * @return {@code true} if the specified object is equal to this Component;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Component component = (Component) o;
        return Objects.equals(imageName, component.imageName);
    }

    /**
     * Retrieves the name of the component.
     * This method must be implemented by subclasses of the {@code Component} class.
     *
     * @return a string representing the name of the component
     */
    protected abstract String getComponentName();

    /**
     * Provides a string representation of the component, detailing its visual layout,
     * directional connectors, associated image name, and component name.
     *
     * The format includes a representation of the connectors in the north, south,
     * west, and east directions, as well as the component's visual details.
     *
     * @return a string representation of the component including the image name,
     *         component name, and details about the directional connectors
     */
    @Override
    public String toString() {
        String north = getConnectors().get(Direction.NORTH) != null
                ? String.valueOf(getConnectors().get(Direction.NORTH).fromConnectorTypeToValue())
                : " ";
        String south = getConnectors().get(Direction.SOUTH) != null
                ? String.valueOf(getConnectors().get(Direction.SOUTH).fromConnectorTypeToValue())
                : " ";
        String west  = getConnectors().get(Direction.WEST) != null
                ? String.valueOf(getConnectors().get(Direction.WEST).fromConnectorTypeToValue())
                : " ";
        String east  = getConnectors().get(Direction.EAST) != null
                ? String.valueOf(getConnectors().get(Direction.EAST).fromConnectorTypeToValue())
                : " ";

        return String.format("""
            %s
            %s
            +---------+
            |    %s    |
            | %s     %s |
            |    %s    |
            +---------+
            """, imageName,getComponentName(), north, west, east, south);
    }

    /**
     * The Method is used in the GUI to recognize whether this component has changed
     * one of its attributes, needs override if one of the attributes must be shown
     * in the graphical interface.
     *
     * @return the hash code of the attribute that might have changed or -1 by default
     */
    @NotNull
    @JsonIgnore
    public Integer getGuiHash() {
        return Objects.hash(imageName, rotation);
    }

    /**
     * Provides a label identifier for the component.
     *
     * @return a string representing the label of the component
     */
    @JsonIgnore
    public abstract String getLabel();

    /**
     * Retrieves the main attribute of the component.
     * The exact meaning of the main attribute may vary depending on the specific implementation in a subclass.
     *
     * @return the main attribute of the component as a String
     */
    @JsonIgnore
    public abstract String getMainAttribute();

}