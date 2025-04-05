package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.Game;

public class ServerSerializer{

    // TODO aggiornamenti della view, guardare la classe game e simili e il diagramma di marco il negro

    // TODO serializzare i cambiamenti sulle shipBoard

    // TODO serializzare i cambiamenti sulla flyingBoard

    // TODO serializzare le carte

    // TODO serializzare i dangerousObj


    public static <T> String serialize(T objToSerialize) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        Deck deck = new Deck();
        deck.loadCards();
        AdventureCard card = deck.getAllCards().get(0);
        System.out.println(ServerSerializer.serialize(card));
    }

}
