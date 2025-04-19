package it.polimi.ingsw.is25am33.serializationLayer.client;

import com.fasterxml.jackson.databind.ObjectMapper;


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
