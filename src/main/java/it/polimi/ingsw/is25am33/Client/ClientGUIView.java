package it.polimi.ingsw.is25am33.Client;

import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.game.GameInfo;

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
}
