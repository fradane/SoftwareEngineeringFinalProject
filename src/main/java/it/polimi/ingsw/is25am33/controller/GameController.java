package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.game.GameInfo;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.network.DNS;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameController extends UnicastRemoteObject implements CallableOnGameController {
    private final GameModel gameModel;
    private final ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
    private final DNS dns;

    // TODO metodo di debug
    @Override
    public void showMessage(String string) throws RemoteException {
        System.out.println(string);
    }

    public GameController(String gameId, int maxPlayers, boolean isTestFlight, DNS dns) throws RemoteException {
        this.gameModel = new GameModel(gameId, maxPlayers, isTestFlight);
        this.gameModel.createGameContext(clientControllers);
        this.dns = dns;
    }

    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController) {
        clientControllers.put(nickname, clientController);
        gameModel.addPlayer(nickname, color, clientController);
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

    public void notifyGameStarted() {
        new Thread( () -> {
            clientControllers.keySet()
                .stream()
                .forEach(nickname -> {
                    try {
                        clientControllers.get(nickname).notifyGameStarted(nickname, getGameInfo());
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
        }).start();
    }

    public void startGame() {
        gameModel.setStarted(true);
        GameInfo gameInfo = getGameInfo();
        gameModel.setCurrGameState(GameState.BUILD_SHIPBOARD);
        notifyGameStarted();
        System.out.println("[" + gameInfo.getGameId() + "] Game started");
    }

    @Override
    public void leaveGame(String nickname) {
        if (!gameModel.isStarted()) {

            clientControllers.remove(nickname);
            System.out.println("[" + getGameInfo().getGameId() + "] Player " + nickname + " left the game");
            if (clientControllers.isEmpty()) {
                dns.removeGame(getGameInfo().getGameId());
                System.out.println("[" + getGameInfo().getGameId() + "] Deleted!");
            }

            dns.getConnectionManager().getClients().remove(nickname);

        } else {
            // TODO: termina il gioco per tutti
        }

    }

    @Override
    public void playerPicksHiddenComponent(String nickname) {
        Component pickedComponent = gameModel.getComponentTable().pickHiddenComponent();
        if (pickedComponent == null) return; //TODO notifica al singolo giocatore che sono finiti
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(pickedComponent);
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
    public void playerWantsToReleaseFocusedComponent(String nickname) throws RemoteException {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        Component component = shipBoard.releaseFocusedComponent();
        gameModel.getComponentTable().addVisibleComponent(component);
    }

    @Override
    public void playerChoseToEndBuildShipBoardPhase(String nickname) {
        // TODO
    }

    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) {
        Component chosenComponent = gameModel.getComponentTable().pickVisibleComponent(choice);
        if (chosenComponent == null) return;
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(chosenComponent);
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
    public void playerWantsToAcceptTheReward(String nickname, Boolean choice) {

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(choice)
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


    @Override
    public void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords) throws RemoteException{

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

        List<Cannon> cannons = doubleCannonsCoords
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cannon.class::cast)
                .toList();

        List<BatteryBox> batteryBoxes = batteryBoxesCoords
                .stream()
                .map(shipBoard::getComponentAt)
                .map(BatteryBox.class::cast)
                .toList();

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleCannons(cannons)
                .setChosenBatteryBoxes(batteryBoxes)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerChoseCabin(String nickname, List<Coordinates> cabinCoords) throws RemoteException{
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

        List<Cabin> cabins = cabinCoords
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenCabins(cabins)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerHandleSmallDanObj(String nickname, Coordinates shieldCoords, Coordinates batteryBoxCoords) throws RemoteException {

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        BatteryBox batteryBox = null;
        Shield shield = null;

        // check whether the coordinates are valid
        if (!shieldCoords.isCoordinateInvalid() && !batteryBoxCoords.isCoordinateInvalid()) {
            shield = ((Shield) shipBoard.getComponentAt(shieldCoords));
            batteryBox = ((BatteryBox) shipBoard.getComponentAt(batteryBoxCoords));
        }

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBox(batteryBox)
                .setChosenShield(shield)
                .build();

        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void playerHandleBigMeteorite(String nickname, Coordinates doubleCannonCoords, Coordinates batteryBoxCoords) {

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        BatteryBox batteryBox = null;
        DoubleCannon doubleCannon = null;

        // check whether the coordinates are valid
        if (!doubleCannonCoords.isCoordinateInvalid() && !batteryBoxCoords.isCoordinateInvalid()) {
            doubleCannon = ((DoubleCannon) shipBoard.getComponentAt(doubleCannonCoords));
            batteryBox = ((BatteryBox) shipBoard.getComponentAt(batteryBoxCoords));
        }

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBox(batteryBox)
                .setChosenDoubleCannon(doubleCannon)
                .build();

        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void playerHandleBigShot(String nickname) throws RemoteException {

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .build();

        gameModel.getCurrAdventureCard().play(choice);

    }

    @Override
    public void playerChoseStorage(String nickname, Coordinates storageCoords) throws RemoteException {

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        Storage storage = storageCoords.isCoordinateInvalid() ? null : ((Storage) shipBoard.getComponentAt(storageCoords));

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(storage)
                .build();

        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void spreadEpidemic(String nickname) throws RemoteException{

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .build();

        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void stardustEvent(String nickname) throws RemoteException{

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .build();

        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public boolean playerWantsToWatchLittleDeck(String nickname, int littleDeckChoice) {
        return gameModel.getDeck().isLittleDeckAvailable(littleDeckChoice);
    }

    @Override
    public void playerWantsToReleaseLittleDeck(String nickname, int littleDeckChoice) {
        gameModel.getDeck().releaseLittleDeck(littleDeckChoice);
    }

    @Override
    public void playerWantsToRestartHourglass(String nickname) throws RemoteException {
        gameModel.restartHourglass(nickname);
    }

    @Override
    public void notifyHourglassEnded(String nickname) {
        gameModel.hourglassEnded();
    }

    @Override
    public void playerToRemoveComponent(String nickname, Component component) throws RemoteException {

    }

    @Override
    public void playerChooseShipPart(String nickname, List<Set<List<Integer>>> shipPart) throws RemoteException {

    }

    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice) throws RemoteException {
        ((Level2ShipBoard) gameModel.getPlayers().get(nickname).getPersonalBoard()).focusReservedComponent(choice);
    }
}
