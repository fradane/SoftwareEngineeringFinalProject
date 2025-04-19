package it.polimi.ingsw.is25am33.serializationLayer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import it.polimi.ingsw.is25am33.model.card.Planets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class ClientDeserializer {

    // TODO aggiornamenti della view, guardare la classe game e simili e il diagramma di marco
    // TODO aggiornamenti della view, guardare la classe gameModel e simili e il diagramma di marco ilnegro

    // TODO serializzare i cambiamenti sulle shipBoard

    // TODO serializzare i cambiamenti sulla flyingBoard

    // TODO serializzare le carte

    // TODO serializzare i dangerousObj


    public static <T> T deserialize(String json, Class<T> type) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        // Simuliamo un file con due JSON riga per riga
        BufferedReader reader = new BufferedReader(new StringReader(json));

        // 1° JSON: Coordinates
        String jsonLine = reader.readLine();
        T result = mapper.readValue(jsonLine, type);
        //System.out.println(result.toString());

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
            ClientDeserializer.deserialize("""
            {"level":1,"cardName":"Planets","availablePlanets":[{"reward":["RED","RED"],"busy":false},{"reward":["RED","BLUE","BLUE"],"busy":false},{"reward":["YELLOW"],"busy":false}],"stepsBack":-2}
            """, Planets.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}



