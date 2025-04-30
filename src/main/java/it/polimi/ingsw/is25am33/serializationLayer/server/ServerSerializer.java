package it.polimi.ingsw.is25am33.serializationLayer.server;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerSerializer{

    public static <T> String serialize(T objToSerialize) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(objToSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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



