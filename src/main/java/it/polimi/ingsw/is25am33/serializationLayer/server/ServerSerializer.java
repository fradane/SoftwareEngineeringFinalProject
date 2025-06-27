package it.polimi.ingsw.is25am33.serializationLayer.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class ServerSerializer{

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.registerModule(new Jdk8Module());
    }

    public static <T> String serialize(T objToSerialize) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}



