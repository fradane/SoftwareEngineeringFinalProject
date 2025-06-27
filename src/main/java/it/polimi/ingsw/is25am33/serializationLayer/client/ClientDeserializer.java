package it.polimi.ingsw.is25am33.serializationLayer.client;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.Planets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientDeserializer extends KeyDeserializer {

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

    public static <T> T deserialize(String json, Class<T> type) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        // simulate a file with two JSON lines per line
        BufferedReader reader = new BufferedReader(new StringReader(json));

        // 1° JSON: Coordinates
        String jsonLine = reader.readLine();
        T result = mapper.readValue(jsonLine, type);

        reader.close();

        return result;
    }

    public static <T> List<T> deserializeListOfObj(String json, Class<T> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Create a type reference for a List of T objects
        CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);

        // Deserialize the JSON array into a List<T>

        return mapper.readValue(json, listType);
    }


    public static void main(String[] args) {

        try {
//            ClientDeserializer.deserialize("""
//            {"level":1,"cardName":"Planets","availablePlanets":[{"reward":["RED","RED"],"busy":false},{"reward":["RED","BLUE","BLUE"],"busy":false},{"reward":["YELLOW"],"busy":false}],"stepsBack":-2}
//            """, Planets.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}



