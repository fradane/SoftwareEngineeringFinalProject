package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.board.Coordinates;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents a structured collection of player-selected options used to communicate
 * the player's decisions to the gameModel model. It encapsulates all possible choices made by the user
 * during various phases of gameplay, including component selections, planet visits, and reward acceptance.
 *
 * <p>The contained choices are utilized by the model to process and execute player actions accordingly.</p>
 */
public class PlayerChoicesDataStructure implements Serializable {

    private final List<Coordinates> chosenDoubleEngines;
    private final List<Coordinates> chosenBatteryBoxes;
    private final int chosenPlanetIndex;
    private final boolean wantsToVisit;
    private final List<Coordinates> chosenCabins;
    private final List<Coordinates> chosenStorage;
    private final boolean hasAcceptedTheReward;
    private final List<Coordinates> chosenShields;
    private final List<Coordinates> chosenDoubleCannons;
    @JsonDeserialize(keyUsing = ServerDeserializer.class)
    private final Map<Coordinates, List<CargoCube>> storageUpdates;

    public PlayerChoicesDataStructure() {
        this.chosenDoubleEngines = new ArrayList<>();
        this.chosenBatteryBoxes = new ArrayList<>();
        this.chosenPlanetIndex = 0;
        this.wantsToVisit = false;
        this.chosenCabins = new ArrayList<>();
        this.chosenStorage = new ArrayList<>();
        this.hasAcceptedTheReward = false;
        this.chosenDoubleCannons = new ArrayList<>();
        this.chosenShields = new ArrayList<>();
        this.storageUpdates = new HashMap<>();
    }

    private PlayerChoicesDataStructure(Builder builder) {
        this.chosenDoubleEngines = builder.chosenDoubleEngines;
        this.chosenBatteryBoxes = builder.chosenBatteryBoxes;
        this.chosenPlanetIndex = builder.chosenPlanetIndex;
        this.wantsToVisit = builder.wantsToVisit;
        this.chosenCabins = builder.chosenCabins;
        this.chosenStorage = builder.chosenStorage;
        this.hasAcceptedTheReward = builder.hasAcceptedTheReward;
        this.chosenShields = builder.chosenShields;
        this.chosenDoubleCannons = builder.chosenDoubleCannons;
        this.storageUpdates = builder.storageUpdates;
    }

    /**
     * Returns the list of double engines selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link Engine} components, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenDoubleEngines() {
        return Optional.ofNullable(chosenDoubleEngines);
    }

    /**
     * Returns the list of battery boxes selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link BatteryBox} components, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenBatteryBoxes() {
        return Optional.ofNullable(chosenBatteryBoxes);
    }

    /**
     * Returns the index of the planet selected by the player.
     *
     * @return the chosen planet index as an integer, if '0' means no planet was selected.
     */
    public int getChosenPlanetIndex() {
        return chosenPlanetIndex;
    }

    /**
     * Indicates whether the player wants to visit the abandoned location.
     *
     * @return {@code true} if the player wishes to visit the abandoned location; {@code false} otherwise.
     */
    public boolean isWantsToVisit() {
        return wantsToVisit;
    }

    /**
     * Returns the list of cabins selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link Cabin} components, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenCabins() {
        return Optional.ofNullable(chosenCabins);
    }

    /**
     * Returns the storage unit selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link Storage} component, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenStorage() {
        return Optional.ofNullable(chosenStorage);
    }

    /**
     * Indicates whether the player has accepted the offered reward.
     *
     * @return {@code true} if the player has accepted the reward; {@code false} otherwise.
     */
    public boolean isHasAcceptedTheReward() {
        return hasAcceptedTheReward;
    }

    /**
     * Returns the shield selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link Shield} component, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenShield() {
        return Optional.ofNullable(chosenShields);
    }

    /**
     * Returns the list of double cannons selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link Cannon} components, or empty if not set.
     */
    @JsonIgnore
    public Optional<List<Coordinates>> getChosenDoubleCannons() {
        return Optional.ofNullable(chosenDoubleCannons);
    }

    // Getter per la serializzazione JSON
   public List<Coordinates> getCabins() {
        return chosenCabins;
    }
    public List<Coordinates> getStorage() {
        return chosenStorage;
    }
    public List<Coordinates> getDoubleCannons() {
        return chosenDoubleCannons;
    }
    public List<Coordinates> getDoubleEngines() {
        return chosenDoubleEngines;
    }
    public List<Coordinates> getBatteryBoxes() {
        return chosenBatteryBoxes;
    }
    public List<Coordinates> getShields() {
        return chosenShields;
    }

    /**
     * Returns the storage updates map selected by the player, if any.
     *
     * @return an {@link Optional} containing the map of storage updates, or empty if not set.
     */
    @JsonIgnore
    public Optional<Map<Coordinates, List<CargoCube>>> getStorageUpdates() {
        return Optional.ofNullable(storageUpdates);
    }

    /**
     * Returns the storage updates map for JSON serialization.
     *
     * @return the storage updates map.
     */
    @JsonDeserialize(keyUsing = ServerDeserializer.class)
    public Map<Coordinates, List<CargoCube>> getStorageUpdatesMap() {
        return storageUpdates;
    }


    /**
     * Builder class for constructing instances of {@link PlayerChoicesDataStructure}.
     * This builder allows selective setting of various player choices during gameplay.
     */
    public static class Builder {
        private List<Coordinates> chosenDoubleEngines;
        private List<Coordinates> chosenBatteryBoxes;
        private int chosenPlanetIndex;
        private boolean wantsToVisit;
        private List<Coordinates> chosenCabins;
        private List<Coordinates> chosenStorage;
        private boolean hasAcceptedTheReward;
        private List<Coordinates> chosenShields;
        private List<Coordinates> chosenDoubleCannons;
        private Map<Coordinates, List<CargoCube>> storageUpdates;
        /**
         * Sets the list of double engines selected by the player.
         *
         * @param chosenDoubleEngines the list of chosen {@link Engine} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenDoubleEngines(List<Coordinates> chosenDoubleEngines) {
            this.chosenDoubleEngines = chosenDoubleEngines;
            return this;
        }

        /**
         * Sets the list of battery boxes selected by the player.
         *
         * @param chosenBatteryBoxes the list of chosen {@link BatteryBox} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenBatteryBoxes(List<Coordinates> chosenBatteryBoxes) {
            this.chosenBatteryBoxes = chosenBatteryBoxes;
            return this;
        }

        /**
         * Sets the index of the planet selected by the player.
         *
         * @param chosenPlanetIndex the chosen planet index as an integer.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenPlanetIndex(int chosenPlanetIndex) {
            this.chosenPlanetIndex = chosenPlanetIndex;
            return this;
        }

        /**
         * Sets whether the player wants to visit the abandoned location.
         *
         * @param wantsToVisit {@code true} if the player wishes to visit the abandoned location; {@code false} otherwise.
         * @return this builder instance for method chaining.
         */
        public Builder setWantsToVisit(boolean wantsToVisit) {
            this.wantsToVisit = wantsToVisit;
            return this;
        }

        /**
         * Sets the list of cabins selected by the player.
         *
         * @param chosenCabins the list of chosen {@link Coordinates} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenCabins(List<Coordinates> chosenCabins) {
            this.chosenCabins = chosenCabins;
            return this;
        }

        /**
         * Sets the storage unit selected by the player.
         *
         * @param chosenStorage the chosen {@link Storage} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenStorage(List<Coordinates> chosenStorage) {
            this.chosenStorage = chosenStorage;
            return this;
        }

        /**
         * Sets whether the player has accepted the offered reward.
         *
         * @param hasAcceptedTheReward {@code true} if the player has accepted the reward; {@code false} otherwise.
         * @return this builder instance for method chaining.
         */
        public Builder setHasAcceptedTheReward(boolean hasAcceptedTheReward) {
            this.hasAcceptedTheReward = hasAcceptedTheReward;
            return this;
        }

        /**
         * Sets the shield selected by the player.
         *
         * @param chosenShields the chosen {@link Shield} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenShield(List<Coordinates> chosenShields) {
            this.chosenShields = chosenShields;
            return this;
        }

        /**
         * Sets the single battery box selected by the player.
         *
         * @param chosenBatteryBox the chosen {@link BatteryBox} component.
         * @return this builder instance for method chaining.
         */
//        public Builder setChosenBatteryBox(BatteryBox chosenBatteryBox) {
//            this.chosenBatteryBox = chosenBatteryBox;
//            return this;
//        }

        /**
         * Sets the double cannon selected by the player.
         *
         * @param chosenDoubleCannon the chosen {@link DoubleCannon} component.
         * @return this builder instance for method chaining.
         */
//        public Builder setChosenDoubleCannon(DoubleCannon chosenDoubleCannon) {
//            this.chosenDoubleCannon = chosenDoubleCannon;
//            return this;
//        }

        /**
         * Sets the list of double cannons selected by the player.
         *
         * @param chosenDoubleCannons the list of chosen {@link Cannon} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenDoubleCannons(List<Coordinates> chosenDoubleCannons) {
            this.chosenDoubleCannons = chosenDoubleCannons;
            return this;
        }

        /**
         * Sets the storage updates map for cube redistribution.
         *
         * @param storageUpdates the map of storage coordinates to cube lists.
         * @return this builder instance for method chaining.
         */
        public Builder setStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
            this.storageUpdates = storageUpdates;
            return this;
        }

        /**
         * Builds and returns a new instance of {@link PlayerChoicesDataStructure}
         * using the values set in this builder.
         *
         * @return a new {@link PlayerChoicesDataStructure} instance.
         */
        public PlayerChoicesDataStructure build() {
            // Inizializza tutte le liste
            if (chosenDoubleEngines == null) chosenDoubleEngines = new ArrayList<Coordinates>();
            if (chosenBatteryBoxes == null) chosenBatteryBoxes = new ArrayList<Coordinates>();
            if (chosenCabins == null) chosenCabins = new ArrayList<>();
            if (chosenStorage == null) chosenStorage = new ArrayList<>();
            if (chosenDoubleCannons == null) chosenDoubleCannons = new ArrayList<>();
            if (chosenShields == null) chosenShields = new ArrayList<>();
            if (storageUpdates == null) storageUpdates = new HashMap<>();

            return new PlayerChoicesDataStructure(this);
        }
    }
}