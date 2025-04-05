package it.polimi.ingsw.is25am33.model.component;

import it.polimi.ingsw.is25am33.model.ComponentState;
import it.polimi.ingsw.is25am33.model.ConnectorType;
import it.polimi.ingsw.is25am33.model.Direction;

import java.util.Map;

public class ComponentEvent {
    private Component component;
    private String attribute;
    private Object newValue;

    public ComponentEvent(Component component, String attribute, Object newValue) {
        this.component = component;
        this.attribute = attribute;
        this.newValue = newValue;
    }

    public Component getComponent() {
        return component;
    }

    public String getAttribute() {
        return attribute;
    }

    public Object getNewValue() {
        return newValue;
    }

}
