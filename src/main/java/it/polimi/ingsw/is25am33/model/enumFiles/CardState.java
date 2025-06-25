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

public enum CardState {

    START_CARD ,
    CHOOSE_CANNONS,
    CHOOSE_ENGINES,
    REMOVE_CREW_MEMBERS,
    HANDLE_CUBES_REWARD,
    HANDLE_CUBES_MALUS,
    CHOOSE_PLANET,
    VISIT_LOCATION,
    DANGEROUS_ATTACK,
    THROW_DICES,
    EPIDEMIC,
    WAIT_FOR_CONFIRM_REMOVAL_HANDLED,
    STARDUST ,
    ACCEPT_THE_REWARD,
    EVALUATE_CREW_MEMBERS,
    STEPS_BACK,
    CHECK_SHIPBOARD_AFTER_ATTACK,
    END_OF_CARD
}
