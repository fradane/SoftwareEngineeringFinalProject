package it.polimi.ingsw.is25am33.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class PrefabShipInfo implements Serializable {
    private final String id;
    private final String name;
    private final String description;
    private final boolean forTestFlight;

    @JsonCreator
    public PrefabShipInfo(
            @JsonProperty("id") String id, 
            @JsonProperty("name") String name, 
            @JsonProperty("description") String description, 
            @JsonProperty("forTestFlight") boolean forTestFlight) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.forTestFlight = forTestFlight;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isForTestFlight() { return forTestFlight; }
}