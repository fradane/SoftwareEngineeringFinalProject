package it.polimi.ingsw.is25am33.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.ingsw.is25am33.model.card.Planet;

import java.io.Serializable;

public class PrefabShipInfo implements Serializable {
    private final String id;
    private final String name;
    private final String description;
    private final boolean forTestFlight;
    private final boolean forGui;

    @JsonCreator
    public PrefabShipInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("forTestFlight") boolean forTestFlight,
            @JsonProperty("forGui") boolean forGui) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.forTestFlight = forTestFlight;
        this.forGui = forGui;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isForTestFlight() { return forTestFlight; }
    public boolean isForGui() { return forGui; }
}