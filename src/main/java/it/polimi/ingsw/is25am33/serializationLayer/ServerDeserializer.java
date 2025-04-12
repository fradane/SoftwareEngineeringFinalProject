package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import javafx.util.Pair;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ServerDeserializer {

    // TODO guardare la classe playerChoicesDataStructure che sicuramente quelle cose vanno deserializzate

    // TODO prende le scelte, la deserializza e crea l'instanza di playerChoicesStructure da passare al controller

    private static final Map<String, Function<String, ?>> idMapper = Map.of(
            "batteryBoxesCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, true),
            "doubleCannonsCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, false),
            "enginesCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, false),
            "cabinsCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, false),
            "storageCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, false),
            "shieldCoordinates", (jsonString) -> ServerDeserializer.deserializeCoordinates(jsonString, false),
            "planetIndex", (jsonString) -> ServerDeserializer.deserializeObj(jsonString, Integer.class),
            "playerChoice", (jsonString) -> ServerDeserializer.deserializeObj(jsonString, Boolean.class)
            );

    public static <R> R deserialize(String jsonString) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        BufferedReader reader = new BufferedReader(new StringReader(jsonString));
        String line;

        // reads the id of the json file (first line) to understand what follows
        line = reader.readLine();
        String id = mapper.readValue(line, String.class);

        @SuppressWarnings("unchecked")
        Function<String, R> function = (Function<String, R>) idMapper.get(id);

        // reads the second line with data and deserialize
        line = reader.readLine();
        reader.close();

        return function.apply(line);

    }


    public static <T> T deserializeObj(String jsonStringWithData, Class<T> type) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStringWithData, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public static <R> Pair<List<R>, Boolean> deserializeCoordinates(String jsonStringWithCoordinates, boolean isBatteryBox) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Coordinates> coordinatesList =  mapper.readValue(jsonStringWithCoordinates, new TypeReference<List<Coordinates>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // TODO dalle coordinate prendi la lista di componenti

        return new Pair<>(null, isBatteryBox);

    }








}
