package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameController extends UnicastRemoteObject implements CallableOnGameController {
    private final GameModel gameModel;
    private final Map<String, CallableOnClientController> clientControllers;

    // TODO metodo di debug
    @Override
    public void showMessage(String string) throws RemoteException {
        System.out.println(string);
    }



    public GameController(String gameId, int maxPlayers, boolean isTestFlight) throws RemoteException {
        this.gameModel = new GameModel(gameId, maxPlayers, isTestFlight);
        clientControllers = new ConcurrentHashMap<>();
    }


    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController) {
        gameModel.addPlayer(nickname, color);
        clientControllers.put(nickname, clientController);
    }

    public void removePlayer(String nickname) {
        gameModel.removePlayer(nickname);
    }

    public void playCard(String jsonString) throws IOException {

        AdventureCard currCard = gameModel.getCurrAdventureCard();
        currCard.play(currCard.getCurrState().handleJsonDeserialization(gameModel, jsonString));

    }

    public GameInfo getGameInfo() {
        Collection<Player> players = gameModel.getPlayers().values();
        Map<String, PlayerColor> playerAndColors = new HashMap<>();
        for (Player player : players) {
            playerAndColors.put(player.getNickname(), player.getPlayerColor());
        }

        return new GameInfo(
                gameModel.getGameId(),
                this,
                gameModel.getMaxPlayers(),
                playerAndColors,
                gameModel.isStarted(),
                gameModel.isTestFlight()

        );
    }

    public Thread notifyNewPlayerJoined(String gameId, String newPlayerNickname, PlayerColor color) {
        return new Thread( () -> {
            clientControllers.keySet()
                    .stream()
                    .filter(nickname -> !nickname.equals(newPlayerNickname))
                    .forEach(nickname -> {
                        try {
                            clientControllers.get(nickname).notifyNewPlayerJoined(nickname, gameId, newPlayerNickname, color);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
        });
    }

    public void notifyGameStarted (Set<String> players, GameState currGameState) {
        new Thread( () -> {
            clientControllers.keySet()
                    .stream()
                    .forEach(nickname -> {
                        try {
                            clientControllers.get(nickname).notifyGameStarted(nickname, currGameState);
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }).start();
    }

    public void startGame() {
        GameInfo gameInfo = getGameInfo();
        notifyGameStarted(gameInfo.getConnectedPlayers().keySet(), gameModel.getCurrGameState());
        System.out.println("[" + gameInfo.getGameId() + "] Game started");
    }

}
