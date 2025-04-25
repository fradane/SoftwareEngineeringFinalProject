package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level1ShipBoard;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.component.Shield;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
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
                            clientControllers.get(nickname).notifyGameStarted(nickname, currGameState, getGameInfo());
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }).start();
    }

    public void startGame() {
        GameInfo gameInfo = getGameInfo();
        gameModel.setCurrGameState(GameState.BUILD_SHIPBOARD);
        notifyGameStarted(gameInfo.getConnectedPlayers().keySet(), gameModel.getCurrGameState());
        System.out.println("[" + gameInfo.getGameId() + "] Game started");
    }

    @Override
    public Component playerPicksHiddenComponent(String nickname) {
        Component pickedComponent = gameModel.getComponentTable().pickHiddenComponent();
        if (pickedComponent == null) return null;
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(pickedComponent);
        return pickedComponent;
    }

    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates) {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        shipBoard.placeComponentWithFocus(coordinates.getX(), coordinates.getY());
    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        ((Level2ShipBoard) shipBoard).book();
    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        Component component = shipBoard.releaseFocusedComponent();
        gameModel.getComponentTable().addVisibleComponent(component);
    }

    @Override
    public void playerChoseToEndBuildShipBoardPhase(String nickname) {
        // TODO
    }

    @Override
    public Component[][] getShipBoardOf(String otherPlayerNickname, String askerNickname) {
        return gameModel.getPlayers().get(otherPlayerNickname).getPersonalBoardAsMatrix();
    }

    @Override
    public Map<Integer, Component> showPlayerVisibleComponent(String nickname) {
        return gameModel.getComponentTable().getVisibleComponents();
    }

    @Override
    public Component playerPicksVisibleComponent(String nickname, Integer choice) {
        Component chosenComponent = gameModel.getComponentTable().pickVisibleComponent(choice);
        if (chosenComponent == null) return null;
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(chosenComponent);
        return chosenComponent;
    }

    @Override
    public void playerWantsToVisitLocation(String nickname, Boolean choice) {

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setWantsToVisit(choice)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerWantsToThrowDices(String nickname) {
        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure.Builder().build();
        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerWantsToVisitPlanet(String nickname, int choice){

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenPlanetIndex(choice)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException {

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

        List<Engine> engines = doubleEnginesCoords
                        .stream()
                        .map(shipBoard::getComponentAt)
                        .map(Engine.class::cast)
                        .toList();

        List<BatteryBox> batteryBoxes = batteryBoxesCoords
                .stream()
                .map(shipBoard::getComponentAt)
                .map(BatteryBox.class::cast)
                .toList();

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleEngines(engines)
                .setChosenBatteryBoxes(batteryBoxes)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

}
