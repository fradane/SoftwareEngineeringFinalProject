package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.card.AbandonedShip;
import it.polimi.ingsw.is25am33.model.card.AbandonedStation;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Cannon;
import it.polimi.ingsw.is25am33.model.component.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class ClientDeserializer {

    // TODO aggiornamenti della view, guardare la classe game e simili e il diagramma di marco ilnegro

    // TODO serializzare i cambiamenti sulle shipBoard

    // TODO serializzare i cambiamenti sulla flyingBoard

    // TODO serializzare le carte

    // TODO serializzare i dangerousObj


    public static <T> void deserialize(String json, Class<T> type) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        // Simuliamo un file con due JSON riga per riga
        BufferedReader reader = new BufferedReader(new StringReader(json));

        // 1° JSON: Coordinates
        String jsonLine = reader.readLine();
        Object result = mapper.readValue(jsonLine, type);
        System.out.println(result.toString());

        reader.close();
    }

    public static void main(String[] args) {

        try {
            ClientDeserializer.deserialize("""
            {"level":1,"cardName":"AbandonedStation","stepsBack":-1,"requiredCrewMembers":5,"reward":["YELLOW","GREEN"]}
            """, AbandonedStation.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
