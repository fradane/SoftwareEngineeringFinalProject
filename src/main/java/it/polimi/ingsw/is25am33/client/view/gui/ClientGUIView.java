package it.polimi.ingsw.is25am33.client.view.gui;


import it.polimi.ingsw.is25am33.client.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.board.Coordinates;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ClientGUIView implements ClientView {

    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {
        //TODO
    }

    @Override
    public void showValidShipBoardMenu() {
        //TODO
    }

    @Override
    public void showChooseComponentToRemoveMenu() {
        //TODO
    }

    @Override
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts) {
        //TODO
    }

}
