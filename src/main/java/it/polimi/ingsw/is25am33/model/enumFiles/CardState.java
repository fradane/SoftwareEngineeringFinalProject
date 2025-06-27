package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.serializationLayer.server.ServerDeserializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Represents the various states that a card can transition through during game play.
 * Each state corresponds to a specific phase or action in the game's card resolution process.
 * These states are used to control game flow and determine valid actions at different points
 * in card execution.
 */
public enum CardState {

    /**
     * Initial state when a card is first activated.
     */
    START_CARD,

    /**
     * State for selecting cannon components.
     */
    CHOOSE_CANNONS,

    /**
     * State for selecting engine components.
     */
    CHOOSE_ENGINES,

    /**
     * State for removing crew members from the ship.
     */
    REMOVE_CREW_MEMBERS,

    /**
     * State for handling cube rewards.
     */
    HANDLE_CUBES_REWARD,

    /**
     * State for handling cube penalties.
     */
    HANDLE_CUBES_MALUS,

    /**
     * State for choosing a planet.
     */
    CHOOSE_PLANET,

    /**
     * State for visiting a location.
     */
    VISIT_LOCATION,

    /**
     * State for resolving dangerous object attacks.
     */
    DANGEROUS_ATTACK,

    /**
     * State for dice-rolling actions.
     */
    THROW_DICES,

    /**
     * State for handling epidemic events.
     */
    EPIDEMIC,

    /**
     * State for waiting for confirmation that removal has been handled.
     */
    WAIT_FOR_CONFIRM_REMOVAL_HANDLED,

    /**
     * State for handling stardust effects.
     */
    STARDUST,

    /**
     * State for accepting rewards.
     */
    ACCEPT_THE_REWARD,

    /**
     * State for evaluating crew members.
     */
    EVALUATE_CREW_MEMBERS,

    /**
     * State for handling backward movement.
     */
    STEPS_BACK,

    /**
     * State for checking shipboard status after an attack.
     */
    CHECK_SHIPBOARD_AFTER_ATTACK,

    /**
     * Final state indicating the card's effect has been fully resolved.
     */
    END_OF_CARD
}