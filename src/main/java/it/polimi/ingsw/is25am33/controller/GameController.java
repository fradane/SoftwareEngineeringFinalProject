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

    private final Map<String, Set<Set<Coordinates>>> temporaryShipParts = new ConcurrentHashMap<>();

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

    public GameModel getGameModel() {
        return gameModel;
    }

    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController) {
        clientControllers.put(nickname, clientController);
        gameModel.addPlayer(nickname, color, clientController);
    }

    public void removePlayer(String nickname) {
        gameModel.removePlayer(nickname);
    }

    public ConcurrentHashMap<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
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
                        } catch (IOException e) {
                            e.printStackTrace();
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
                    } catch (IOException e) {
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
    public void leaveGameAfterCreation(String nickname, Boolean isFirst) {
        //se voglio uscire notifico a tutti gli altri giocatori nel game che mi sto disconnettendo ed esco.
        // a quel punto loro si disconnetteranno tramite il game context
        dns.getClientGame().remove(nickname);
        clientControllers.remove(nickname);
        System.out.println("[" + getGameInfo().getGameId() + "] Player " + nickname + " left the game");
        //se sono il primo a chiamare e ci sono altri client notifico e chiudo altrimenti se ho rimosso gia tutti i client chiudo il gioco
        if(isFirst && !gameModel.getGameContext().getClientControllers().isEmpty())
            gameModel.getGameContext().notifyDisconnection(nickname);
        else if(clientControllers.isEmpty()) {
            dns.removeGame(getGameInfo().getGameId());
            System.out.println("[" + getGameInfo().getGameId() + "] Deleted!");
        }
        //se un client è crashato lo stato del gioco viene settato a false in automatico alla ricezione della disconnessione
    }

    @Override
    public void playerPicksHiddenComponent(String nickname) {
        Component pickedComponent = gameModel.getComponentTable().pickHiddenComponent();
        if (pickedComponent == null) return; //TODO notifica al singolo giocatore che sono finiti
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(pickedComponent);
    }

    /**
     * Handles the player's action to place a focused component on their ship board at the specified coordinates
     * with the specified rotation.
     *
     * @param nickname the nickname of the player placing the component
     * @param coordinates the coordinates on the ship board where the component will be placed
     * @param rotation the number of clockwise rotations to apply to the component before placement
     */
    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        for (int i = 0; i < rotation; i++)
            shipBoard.getFocusedComponent().rotate();
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
    public void playerWantsToRemoveComponent(String nickname, Coordinates coordinates) throws RemoteException {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        Set<Set<Coordinates>> shipParts = shipBoard.removeAndRecalculateShipParts(coordinates.getX(), coordinates.getY());

        // Memorizza le ship parts per questo giocatore
        temporaryShipParts.put(nickname, shipParts);

        gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
            try {
                Component[][] shipMatrix = shipBoard.getShipMatrix();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                if(shipParts.size() == 1){
                    if(incorrectlyPositionedComponentsCoordinates.size()==0) {
                        clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates);

                    }else
                        clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates);
                }else if(shipParts.size() == 0)
                    clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates);
                else
                    clientController.notifyShipPartsGeneratedDueToRemoval(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, shipParts);
            } catch (RemoteException e) {
                System.err.println("Remote Exception");
            }
        });

        if(shipParts.size() == 1) //ovvero ho direttamente rimosso un componente
            // Controllo se tutte le navi sono corrette e in caso cambio la fase
            gameModel.checkAndTransitionToNextPhase();


    }

    @Override
    public void playerChoseShipPart(String nickname, Set<Coordinates> chosenShipPart) throws RemoteException {
        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

        // Ottieni tutte le ship parts memorizzate per questo giocatore
        Set<Set<Coordinates>> allShipParts = temporaryShipParts.get(nickname);

        if (allShipParts != null) {
            // Rimuovi tutte le ship parts TRANNE quella scelta
            for (Set<Coordinates> shipPart : allShipParts) {
                if (!shipPart.equals(chosenShipPart)) {
                    shipBoard.removeShipPart(shipPart);
                }
            }

            gameModel.getGameContext().notifyAllClients( (nicknameToNotify, clientController) -> {
                try {
                    Component[][] shipMatrix = shipBoard.getShipMatrix();
                    Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                    if(shipBoard.isShipCorrect())
                        clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates);
                    else
                        clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates);
                } catch (RemoteException e) {
                    System.err.println("Remote Exception");
                }
            });

            // Pulisci la mappa temporanea
            temporaryShipParts.remove(nickname);
        }

        //Controlla se tutte le navi sono corrette e cambia fase se necessario
        gameModel.checkAndTransitionToNextPhase();
    }

    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice) throws RemoteException {
        ((Level2ShipBoard) gameModel.getPlayers().get(nickname).getPersonalBoard()).focusReservedComponent(choice);
    }
}
