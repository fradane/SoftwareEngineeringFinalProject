package it.polimi.ingsw.is25am33.client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.game.ComponentTable;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.network.rmi.server.RMIServerNetworkManager;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ClientGUIView implements ClientView {
    @Override
    public void initialize() {

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void showError(String errorMessage) {

    }

    @Override
    public String askNickname() {
        return "";
    }

    @Override
    public String askServerAddress() {
        return "";
    }

    @Override
    public void showAvailableGames(Iterable<GameInfo> games) {

    }

    @Override
    public int[] askCreateGame() {
        return new int[0];
    }

    @Override
    public String[] askJoinGame(Iterable<GameInfo> games) {
        return new String[0];
    }

    @Override
    public int showMainMenu() {
        return 0;
    }

    @Override
    public int showGameMenu() {
        return 0;
    }

    @Override
    public void notifyPlayerJoined(String nickname, GameInfo gameInfo) {

    }

    @Override
    public void notifyPlayerLeft(String nickname, GameInfo gameInfo) {

    }

    @Override
    public void notifyGameStarted(GameState gameState) {

    }

    @Override
    public void notifyGameEnded(String reason) {

    }

    @Override
    public void showNewGameState(String gameState) {

    }

    @Override
    public void showNewCardState(String cardState) {

    }

    @Override
    public void showComponentTable() {

    }

    @Override
    public ComponentTable getLatestComponentTable() {
        return null;
    }

    @Override
    public void setLatestComponentTable(ComponentTable latestComponentTable) {

    }

    @Override
    public void setCurrAdventureCard(AdventureCard card) {

    }

    @Override
    public void showCurrAdventureCard(boolean isFirstTime) {

    }

    @Override
    public void setPlayersNickname(List<String> playersNickname) {

    }

    @Override
    public BiFunction<RMIServerNetworkManager, String, Boolean> showBuildShipBoardMenu() {
        return null;
    }

    @Override
    public BiFunction<RMIServerNetworkManager, String, Boolean> showShipBoardsMenu() {
        return null;
    }

    @Override
    public BiConsumer<RMIServerNetworkManager, String> showChosenComponentAndMenu(Component component) {
        return null;
    }

}
