package it.polimi.ingsw.is25am33.serializationLayer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;



public class ClientSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.registerModule(new Jdk8Module());
        // <-- add
        // other modules (JavaTimeModule, ecc.)
    }

    public static <T> String serialize(T objToSerialize) {
        try {
            //ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

//        SocketMessage msg = new SocketMessage("fra", "action");
//        msg.setParamPlayerColor(PlayerColor.GREEN);;
//        msg.setParamBoolean(false);
//        msg.setParamInt(2);
//        msg.setParamString("giu");
//        msg.setParamGameInfo(List.of(new GameInfo("ciao", 4, new HashMap<>(), true, true)));
//
//        String serialize = ClientSerializer.serialize(msg);
//
//        System.out.println(serialize);



    }
}
