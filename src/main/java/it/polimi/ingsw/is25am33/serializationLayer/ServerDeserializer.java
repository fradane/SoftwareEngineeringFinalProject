package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.game.Player;
import javafx.util.Pair;

import java.util.List;
import java.util.stream.Stream;

public class ServerDeserializer {

    public static <T> T deserializeObj(String jsonStringWithData, Class<T> type) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStringWithData, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public static Stream<Coordinates> deserializeCoordinates(String jsonStringWithCoordinates) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStringWithCoordinates, new TypeReference<List<Coordinates>>() {}).stream();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Cannon> fromComponentToDoubleCannon(String jsonStringWithCoordinates, Player player) {

        return ServerDeserializer.deserializeCoordinates(jsonStringWithCoordinates)
                .map(coordinates -> player.getPersonalBoard().getComponentAt(coordinates))
                .map(Cannon.class::cast)
                .toList();

    }

    public static List<BatteryBox> fromComponentToBatteryBox(String jsonStringWithCoordinates, Player player) {

        return ServerDeserializer.deserializeCoordinates(jsonStringWithCoordinates)
                .map(coordinates -> player.getPersonalBoard().getComponentAt(coordinates))
                .map(BatteryBox.class::cast)
                .toList();

    }

    public static List<Engine> fromComponentToDoubleEngine(String jsonStringWithCoordinates, Player player) {

        return ServerDeserializer.deserializeCoordinates(jsonStringWithCoordinates)
                .map(coordinates -> player.getPersonalBoard().getComponentAt(coordinates))
                .map(Engine.class::cast)
                .toList();

    }


    public static List<Cabin> fromComponentToCabin(String jsonStringWithCoordinates, Player player) {

        return ServerDeserializer.deserializeCoordinates(jsonStringWithCoordinates)
                .map(coordinates -> player.getPersonalBoard().getComponentAt(coordinates))
                .map(Cabin.class::cast)
                .toList();

    }





}



