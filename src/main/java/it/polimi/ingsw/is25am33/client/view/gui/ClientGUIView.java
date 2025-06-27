package it.polimi.ingsw.is25am33.client.view.gui;


import it.polimi.ingsw.is25am33.client.model.ShipBoardClient;
import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ClientGUIView implements ClientView {

    /**
     * Shows a ship board with color mapping for components.
     *
     * @param shipBoardClient the ship board to display
     * @param shipBoardOwnerNickname the nickname of the ship board owner
     * @param colorMap mapping of colors to coordinate sets for visual representation
     */
    @Override
    public void showShipBoard(ShipBoardClient shipBoardClient, String shipBoardOwnerNickname, Map<String, Set<Coordinates>> colorMap) {}

    /**
     * Shows the valid ship board menu.
     */
    @Override
    public void showValidShipBoardMenu() {}

    /**
     * Shows the menu for choosing which component to remove.
     */
    @Override
    public void showChooseComponentToRemoveMenu() {}

    /**
     * Shows the menu for choosing ship parts.
     *
     * @param shipParts list of ship part coordinate sets to choose from
     */
    @Override
    public void showChooseShipPartsMenu(List<Set<Coordinates>> shipParts) {}

    /**
     * Shows information about infected crew members that were removed.
     *
     * @param cabinWithNeighbors coordinates of cabins with their neighbors where crew members were removed
     */
    @Override
    public void showInfectedCrewMembersRemoved(Set<Coordinates> cabinWithNeighbors) {}

    /**
     * Shows the end game information including final ranking and prettiest ship winners.
     *
     * @param finalRanking list of player final data sorted by ranking
     * @param playersNicknamesWithPrettiestShip list of player nicknames who have the prettiest ship
     */
    @Override
    public void showEndGameInfo(List<PlayerFinalData> finalRanking, List<String> playersNicknamesWithPrettiestShip) {}

    /**
     * Shows notification that a player has landed early.
     *
     * @param nickname the nickname of the player who landed early
     */
    @Override
    public void showPlayerEarlyLanded(String nickname) {}

}
