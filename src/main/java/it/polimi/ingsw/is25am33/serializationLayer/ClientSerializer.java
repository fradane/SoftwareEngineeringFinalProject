package it.polimi.ingsw.is25am33.serializationLayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.board.Coordinates;

import java.util.List;


public class ClientSerializer {

    public static <T> String serialize(T objToSerialize) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String prima = ClientSerializer.serialize(List.of(new Coordinates(1, 2)));
        String seconda = ClientSerializer.serialize(List.of(new Coordinates(1, 3)));

        // Serializza e stampa
        String finale = prima + "\n" + seconda;
        System.out.println("JSON serializzato:");
        System.out.println(finale);
    }
}
