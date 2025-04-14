package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.component.*;

import java.util.List;
import java.util.Optional;

/**
 * This class represents a structured collection of player-selected options used to communicate
 * the player's decisions to the gameModel model. It encapsulates all possible choices made by the user
 * during various phases of gameplay, including component selections, planet visits, and reward acceptance.
 *
 * <p>The contained choices are utilized by the model to process and execute player actions accordingly.</p>
 */
public class PlayerChoicesDataStructure {

    private final List<Engine> chosenDoubleEngines;
    private final List<BatteryBox> chosenBatteryBoxes;
    private final int chosenPlanetIndex;
    private final boolean wantsToVisit;
    private final List<Cabin> chosenCabins;
    private final Storage chosenStorage;
    private final boolean hasAcceptedTheReward;
    private final Shield chosenShield;
    private final BatteryBox chosenBatteryBox;
    private final DoubleCannon chosenDoubleCannon;
    private final List<Cannon> chosenDoubleCannons;

    private PlayerChoicesDataStructure(Builder builder) {
        this.chosenDoubleEngines = builder.chosenDoubleEngines;
        this.chosenBatteryBoxes = builder.chosenBatteryBoxes;
        this.chosenPlanetIndex = builder.chosenPlanetIndex;
        this.wantsToVisit = builder.wantsToVisit;
        this.chosenCabins = builder.chosenCabins;
        this.chosenStorage = builder.chosenStorage;
        this.hasAcceptedTheReward = builder.hasAcceptedTheReward;
        this.chosenShield = builder.chosenShield;
        this.chosenBatteryBox = builder.chosenBatteryBox;
        this.chosenDoubleCannon = builder.chosenDoubleCannon;
        this.chosenDoubleCannons = builder.chosenDoubleCannons;
    }

    /**
     * Returns the list of double engines selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link Engine} components, or empty if not set.
     */
    public Optional<List<Engine>> getChosenDoubleEngines() {
        return Optional.ofNullable(chosenDoubleEngines);
    }

    /**
     * Returns the list of battery boxes selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link BatteryBox} components, or empty if not set.
     */
    public Optional<List<BatteryBox>> getChosenBatteryBoxes() {
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
    public Optional<List<Cabin>> getChosenCabins() {
        return Optional.ofNullable(chosenCabins);
    }

    /**
     * Returns the storage unit selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link Storage} component, or empty if not set.
     */
    public Optional<Storage> getChosenStorage() {
        return Optional.ofNullable(chosenStorage);
    }

    /**
     * Indicates whether the player has accepted the offered reward.
     *
     * @return {@code true} if the player has accepted the reward; {@code false} otherwise.
     */
    public boolean hasAcceptedTheReward() {
        return hasAcceptedTheReward;
    }

    /**
     * Returns the shield selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link Shield} component, or empty if not set.
     */
    public Optional<Shield> getChosenShield() {
        return Optional.ofNullable(chosenShield);
    }

    /**
     * Returns the single battery box selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link BatteryBox} component, or empty if not set.
     */
    public Optional<BatteryBox> getChosenBatteryBox() {
        return Optional.ofNullable(chosenBatteryBox);
    }

    /**
     * Returns the double cannon selected by the player, if any.
     *
     * @return an {@link Optional} containing the chosen {@link DoubleCannon} component, or empty if not set.
     */
    public Optional<DoubleCannon> getChosenDoubleCannon() {
        return Optional.ofNullable(chosenDoubleCannon);
    }

    /**
     * Returns the list of double cannons selected by the player, if any.
     *
     * @return an {@link Optional} containing the list of chosen {@link Cannon} components, or empty if not set.
     */
    public Optional<List<Cannon>> getChosenDoubleCannons() {
        return Optional.ofNullable(chosenDoubleCannons);
    }

    /**
     * Builder class for constructing instances of {@link PlayerChoicesDataStructure}.
     * This builder allows selective setting of various player choices during gameplay.
     */
    public static class Builder {
        private List<Engine> chosenDoubleEngines;
        private List<BatteryBox> chosenBatteryBoxes;
        private int chosenPlanetIndex;
        private boolean wantsToVisit;
        private List<Cabin> chosenCabins;
        private Storage chosenStorage;
        private boolean hasAcceptedTheReward;
        private Shield chosenShield;
        private BatteryBox chosenBatteryBox;
        private DoubleCannon chosenDoubleCannon;
        private List<Cannon> chosenDoubleCannons;

        /**
         * Sets the list of double engines selected by the player.
         *
         * @param chosenDoubleEngines the list of chosen {@link Engine} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenDoubleEngines(List<Engine> chosenDoubleEngines) {
            this.chosenDoubleEngines = chosenDoubleEngines;
            return this;
        }

        /**
         * Sets the list of battery boxes selected by the player.
         *
         * @param chosenBatteryBoxes the list of chosen {@link BatteryBox} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenBatteryBoxes(List<BatteryBox> chosenBatteryBoxes) {
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
         * @param chosenCabins the list of chosen {@link Cabin} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenCabins(List<Cabin> chosenCabins) {
            this.chosenCabins = chosenCabins;
            return this;
        }

        /**
         * Sets the storage unit selected by the player.
         *
         * @param chosenStorage the chosen {@link Storage} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenStorage(Storage chosenStorage) {
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
         * @param chosenShield the chosen {@link Shield} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenShield(Shield chosenShield) {
            this.chosenShield = chosenShield;
            return this;
        }

        /**
         * Sets the single battery box selected by the player.
         *
         * @param chosenBatteryBox the chosen {@link BatteryBox} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenBatteryBox(BatteryBox chosenBatteryBox) {
            this.chosenBatteryBox = chosenBatteryBox;
            return this;
        }

        /**
         * Sets the double cannon selected by the player.
         *
         * @param chosenDoubleCannon the chosen {@link DoubleCannon} component.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenDoubleCannon(DoubleCannon chosenDoubleCannon) {
            this.chosenDoubleCannon = chosenDoubleCannon;
            return this;
        }

        /**
         * Sets the list of double cannons selected by the player.
         *
         * @param chosenDoubleCannons the list of chosen {@link Cannon} components.
         * @return this builder instance for method chaining.
         */
        public Builder setChosenDoubleCannons(List<Cannon> chosenDoubleCannons) {
            this.chosenDoubleCannons = chosenDoubleCannons;
            return this;
        }

        /**
         * Builds and returns a new instance of {@link PlayerChoicesDataStructure}
         * using the values set in this builder.
         *
         * @return a new {@link PlayerChoicesDataStructure} instance.
         */
        public PlayerChoicesDataStructure build() {
            return new PlayerChoicesDataStructure(this);
        }
    }
}