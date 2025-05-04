package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShipBoardClient {
    private Component[][] shipBoardMatrix;
    private Component focusedComponent;
    private List<Component> bookedComponent;
    private final Map<Class<? extends Component>, List<? extends Component>> componentTypes = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry(BatteryBox.class, new ArrayList<BatteryBox>()),
            Map.entry(Cabin.class, new ArrayList<Cabin>()),
            Map.entry(Cannon.class, new ArrayList<Cannon>()),
            Map.entry(DoubleCannon.class, new ArrayList<DoubleCannon>()),
            Map.entry(DoubleEngine.class, new ArrayList<DoubleEngine>()),
            Map.entry(Engine.class, new ArrayList<Engine>()),
            Map.entry(LifeSupport.class, new ArrayList<LifeSupport>()),
            Map.entry(Shield.class, new ArrayList<Shield>()),
            Map.entry(SpecialStorage.class, new ArrayList<SpecialStorage>()),
            Map.entry(StandardStorage.class, new ArrayList<StandardStorage>()),
            Map.entry(StructuralModules.class, new ArrayList<StructuralModules>())
    ));

    public ShipBoardClient() {
        focusedComponent = null;
        bookedComponent = new ArrayList<>();
        shipBoardMatrix = new Component[12][12];
    }

    @SuppressWarnings("unchecked")
    public void addComponent(Component component, Coordinates coordinates) {
        shipBoardMatrix[coordinates.getX()][coordinates.getY()] = component;
        Class<? extends Component> clazz = component.getClass().asSubclass(Component.class);
        ((List<Component>) componentTypes.get(clazz)).add(clazz.cast(component));
    }

    public List<? extends Component> getComponentByType(Class<? extends Component> clazz) {
        return componentTypes.get(clazz);
    }

    public List<Component> getBookedComponent() {
        return bookedComponent;
    }

    public void setBookedComponent(List<Component> bookedComponent) {
        this.bookedComponent = bookedComponent;
    }

    public Component getFocusedComponent() {
        return focusedComponent;
    }

    public void setFocusedComponent(Component focusedComponent) {
        this.focusedComponent = focusedComponent;
    }

    public void setShipBoardMatrix(Component[][] shipBoardMatrix) {
        this.shipBoardMatrix = shipBoardMatrix;
    }

    public Component[][] getShipBoardMatrix(){
        return shipBoardMatrix;
    }

    public Component getComponentAt(Coordinates coords) {
        return shipBoardMatrix[coords.getX()][coords.getY()];
    }
}
