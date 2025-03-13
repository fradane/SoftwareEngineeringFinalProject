package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;

import java.util.ArrayList;
import java.util.List;

public interface cargoCubesHandler {

    default void handleCargoCubesReward(List<CargoCube> reward, Player player) {

        ArrayList<Storage> playerPersonalStorage = player.getPersonalBoard().getStorage();

        for (CargoCube c : reward) {

            // restituisce una coppia di interi: uno per lo storage selezionato e uno per il cargocube da sostituire
            // se non è presente non fa nulla
            game.getController()
                    .wantsToStoreCargoCube(c, player, playerPersonalStorage)
                    .ifPresent(val -> {
                        playerPersonalStorage.get(val.getStorage()).removeCubeAtIndex(val.getCargoCubeIndex());
                        playerPersonalStorage.get(val.getStorage()).addCube(c);
                    });

        }

    }

}
