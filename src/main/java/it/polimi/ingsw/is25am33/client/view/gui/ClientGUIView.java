package it.polimi.ingsw.is25am33.client.view.gui;


import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;

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

    @Override
    public void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors) {
        //TODO
    }

    @Override
    public void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {
        //TODO
    }

    @Override
    public void showPlayerEarlyLanded(String nickname) {
        //TODO
    }

}
