package it.polimi.ingsw.is25am33.serializationLayer.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.game.DTO;
import it.polimi.ingsw.is25am33.model.game.GameEvent;

import java.util.Map;
import java.util.function.Function;

public class ServerSerializer{

    // TODO aggiornamenti della view, guardare la classe gameModel e simili e il diagramma di marco il negro

    // TODO serializzare i cambiamenti sulle shipBoard
    private static final Map<String, Function<GameEvent, String>> serializers = Map.ofEntries(
            Map.entry("drawnCard", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getAdventureCard())),
            Map.entry("playerWatchesLittleDeck", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getLittleDeck())),
            Map.entry("changeGameState", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getGameState())),
            Map.entry("changeCardState", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getCardState())),
            Map.entry("currPlayerUpdate", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getPlayer())),
            Map.entry("creditsUpdate", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getNum())),
            Map.entry("dangerousObjAttack", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getDangerousObj())),
            Map.entry("flyingBoardUpdate", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getFlyingBoard())),
            Map.entry("shipBoardUpdate", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getShipBoard())),
            Map.entry("checkOnShipBoard", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getShipboardOK())),
            Map.entry("updateComponentTable", (gameEvent) -> ServerSerializer.serializeObj(gameEvent.getDTO().getComponentTable()))
            // TODO id relativi ai components
    );

    public static <T> String serialize(T objToSerialize) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializeGameEvent(GameEvent gameEvent) {
        return serializers.get(gameEvent.getEventId()).apply(gameEvent);
    }

    private static <T> String serializeObj(T objToSerialize) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    public static void main(String[] args) {

        Deck deck = new Deck();
        deck.loadCards();
        DTO dto = new DTO();
        deck.getAllCards().forEach(card -> {
            dto.setAdventureCard(card);
            System.out.println(ServerSerializer.serializeGameEvent(new GameEvent("drawnCard", dto)));
        });



        FlyingBoard f = new Level1FlyingBoard();

        System.out.println(ServerSerializer.serializeObj(f));

        ShipBoard s = new Level2ShipBoard(PlayerColor.BLUE);
        System.out.println(ServerSerializer.serializeObj(s));

    }

    /*

        *---------*
        |   ^^^   |
        | <     > |
        | < DCC > |
        | <     > |
        |   vvv   |
        *---------*

     */


    /*

    TODO:
        - cambio di currGameState o currCardState
            id: "changeState",
            attributo: GameState/CardState
        - scelta tessera nella matrice
            id: "pickUpCoveredComponent"
            attributo: Player, Coordinates, Component
        - scelta cosa il giocatore fa con la tessera che ha in focus
            id: "playerChoiceAboutFocusComponent"
            attributo: Player, Coordinates (se USED della shipboard, se FREE del tavolo), Scelta (BOOKED - USED - FREE)
        - giocatore sceglie di vedere il littleDeck
            id: "playerWatchesLittleDeck"
            attributo: Player, Integer (scelta del mazzetto)
        - esito check correttezza shipboard
            id: "checkOnShipBoard"
            attributo: Player, Boolean
        - aggiorno shipBoard ogni volta che il giocatore cerca di correggerla
            id: "shipBoardUpdate"
            attributo: Player, ShipBoard
        - aggiornamento flyingBoard
            id: "flyingBoardUpdate"
            attributo: FlyingBoard
        - pesca nuova carta
            id: "drawnCard"
            attributo: AdventureCard
        - notifica meteorite o sparo
            id: "dangerousObjAttack"
            attributo: DangerousObj
        - notifica cambio giocatore corrente
            id: "currPlayerUpdate"
            attributo: Player
        - aggiornamento crediti
            id: "creditsUpdate"
            attributo: Player, Integer

     */

}



