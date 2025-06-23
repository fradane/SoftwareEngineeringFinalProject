package it.polimi.ingsw.is25am33.model.dangerousObj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.is25am33.model.enumFiles.Direction;

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

    /**
     * Represents the direction of movement or orientation of the dangerous object.
     * This variable is of type {@link Direction}, an enumeration that defines the possible
     * directions (NORTH, EAST, SOUTH, WEST). The direction determines how the object moves or
     * interacts within the game environment.
     */
    protected Direction direction;
    /**
     * Represents the coordinate value of the dangerous object on a
     * predefined game axis or grid. This variable is used to track
     * the object's position and can be updated as it moves.
     */
    protected int coordinate;
    /**
     * Represents the type identifier of a dangerous object in the system.
     * It is used as a discriminator for serialization and deserialization of subclasses
     * that inherit from the {@link DangerousObj} class.
     * The value of this field is set by specific subclasses, acting as a unique identifier
     * for objects such as "bigMeteorite", "smallMeteorite", "bigShot", and "smallShot".
     * This field is also utilized by serialization formats, such as JSON, to include the
     * type information for polymorphic type resolution.
     */
    protected String dangerousObjType;

    /**
     * Constructs a DangerousObj with a specified direction.
     * Initializes the object's direction and sets its coordinate to 0.
     *
     * @param direction the initial direction of the DangerousObj
     */
    public DangerousObj(Direction direction) {
        this.direction = direction;
        this.coordinate = 0;
    }

    /**
     * Default constructor for the DangerousObj class.
     * Initializes an instance of DangerousObj without setting any specific direction or coordinate.
     * This constructor can be used in situations where no initial configuration is needed.
     */
    public DangerousObj() {}

    /**
     * Retrieves the type of the dangerous object.
     *
     * @return a String representing the type of the dangerous object (e.g., "bigMeteorite", "smallMeteorite", "bigShot", "smallShot").
     */
    public abstract String getDangerousObjType();

    /**
     * Retrieves the direction associated with this dangerous object.
     *
     * @return the direction of the dangerous object, indicating its movement or attack orientation.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Retrieves the current coordinate of the dangerous object.
     *
     * @return the current coordinate as an integer value.
     */
    public int getCoordinate() {
        return coordinate;
    }

    /**
     * Sets the coordinate value for this dangerous object.
     *
     * @param coordinate the new coordinate value to be assigned
     */
    public void setCoordinates(int coordinate) {
        this.coordinate = coordinate;
    }

}


