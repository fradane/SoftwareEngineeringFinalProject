package it.polimi.ingsw.is25am33.model.dangerousObj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.controller.CallableOnGameController;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

import java.util.function.BiConsumer;
import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "dangerousObjType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BigMeteorite.class, name = "bigMeteorite"),
        @JsonSubTypes.Type(value = SmallMeteorite.class, name = "smallMeteorite"),
        @JsonSubTypes.Type(value = BigShot.class, name = "bigShot"),
        @JsonSubTypes.Type(value = SmallShot.class, name = "smallShot")
})
public abstract class DangerousObj implements Serializable {

    protected Direction direction;
    protected int coordinate;
    protected String dangerousObjType;

    public DangerousObj(Direction direction) {
        this.direction = direction;
        this.coordinate = 0;
    }

    public DangerousObj() {}

    public abstract String getDangerousObjType();

    public Direction getDirection() {
        return direction;
    }

    public int getCoordinate() {
        return coordinate;
    }

    public void setCoordinates(int coordinate) {
        this.coordinate = coordinate;
    }

    public abstract BiConsumer<CallableOnGameController, String> showRelatedMenu(ClientView view);
}


