package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.game.Game;
import it.polimi.ingsw.is25am33.serializationLayer.ServerDeserializer;

import java.io.IOException;

public class GameController {

    Game game;

    public void playCard(String jsonString) throws IOException {

        AdventureCard currCard = game.getCurrAdventureCard();
        currCard.play(currCard.getCurrState().handleJsonDeserialization(game, jsonString));

    }



}
