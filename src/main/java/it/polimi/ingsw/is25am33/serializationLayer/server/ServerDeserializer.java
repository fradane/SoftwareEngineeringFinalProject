package it.polimi.ingsw.is25am33.serializationLayer.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServerDeserializer extends KeyDeserializer {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.registerModule(new Jdk8Module());       // <-- aggiungi
        // eventuali altri moduli (JavaTimeModule, ecc.)
    }

    @Override
    public Coordinates deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        Pattern COORDINATE_PATTERN = Pattern.compile("\\{x = (-?\\d+), y = (-?\\d+)\\}");

        if (key == null) {
            throw new IOException("Null key cannot be deserialized to Coordinates");
        }

        Matcher matcher = COORDINATE_PATTERN.matcher(key);
        if (!matcher.matches()) {
            throw new IOException("Invalid Coordinates key format: \"" + key + "\". Expected format: {x = X, y = Y}");
        }

        try {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            return new Coordinates(x, y);
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing integers from Coordinates key: " + key, e);
        }
    }

    public static <T> T deserializeObj(String jsonStringWithData, Class<T> type) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStringWithData, type);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
            return null;
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



