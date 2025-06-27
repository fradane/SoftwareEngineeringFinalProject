package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipFactory;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.IOException;

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

    @Override
    public void showMessage(String string) throws RemoteException {
        System.out.println("Show message: " + string);
    }

    public GameController(String gameId, int maxPlayers, boolean isTestFlight, DNS dns) throws RemoteException {
        this.gameModel = new GameModel(gameId, maxPlayers, isTestFlight);
        this.gameModel.createGameClientNotifier(clientControllers);
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

    public void startGame() {
        gameModel.setStarted(true);
        GameInfo gameInfo = getGameInfo();
        gameModel.setCurrGameState(GameState.BUILD_SHIPBOARD);
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyGameStarted(nicknameToNotify, getGameInfo());
        });
        System.out.println("[" + gameInfo.getGameId() + "] Game started");
    }

    @Override
    public void leaveGameAfterCreation(String nickname, Boolean isFirst) {
        // If I want to leave, notify all other players in the game that I’m disconnecting and exit.
        // At that point, they will disconnect via the game context
        dns.getClientGame().remove(nickname);
        clientControllers.remove(nickname);
        System.out.println("[" + getGameInfo().getGameId() + "] Player " + nickname + " left the game");
        // If I am the first to call and there are other clients, notify and close. Otherwise, if all clients have already been removed, close the game.
        if(isFirst && !gameModel.getGameClientNotifier().getClientControllers().isEmpty())
            gameModel.getGameClientNotifier().notifyDisconnection(nickname,gameModel);
        else if(clientControllers.isEmpty()) {
            dns.removeGame(getGameInfo().getGameId());
            System.out.println("[" + getGameInfo().getGameId() + "] Deleted!");
        }
        // If a client has crashed, the game state is automatically set to false upon receiving the disconnection
    }

    @Override
    public void playerPicksHiddenComponent(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerPicksHiddenComponent in state " + gameModel.getCurrGameState());
            return;
        }

        Component pickedComponent = gameModel.getComponentTable().pickHiddenComponent();
        if (pickedComponent == null) return; //TODO notifica al singolo giocatore che sono finiti
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(pickedComponent);
    }

    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) {
        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToPlaceFocusedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        for (int i = 0; i < rotation; i++)
            shipBoard.getFocusedComponent().rotate();
        shipBoard.placeComponentWithFocus(coordinates.getX(), coordinates.getY());
    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToReserveFocusedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        ((Level2ShipBoard) shipBoard).book();
    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) {

        if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD){
            System.err.println("Player " + nickname + " tried to playerWantsToReleaseFocusedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        Component component = shipBoard.releaseFocusedComponent();
        if(!shipBoard.getNotActiveComponents().contains(component))
            gameModel.getComponentTable().addVisibleComponent(component);
    }

    @Override
    public void playerEndsBuildShipBoardPhase(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerEndsBuildShipBoardPhase in state " + gameModel.getCurrGameState());
            return;
        }

        if (gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname)) == gameModel.getMaxPlayers())
            gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
    }

    @Override
    public void playerPlacesPawn(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerPlacesPawn in state " + gameModel.getCurrGameState());
            return;
        }

        if (gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname)) == gameModel.getMaxPlayers())
            gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
    }

    @Override
    public void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException {
        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException {

        if(gameModel.getCurrGameState()!=GameState.PLACE_CREW) {
            System.err.println("Player " + nickname + " tried to submitCrewChoices in state " + gameModel.getCurrGameState());
            return;
        }

        Player player = gameModel.getPlayers().get(nickname);
        ShipBoard shipBoard = player.getPersonalBoard();

        try {
            // Validate the choices
            validateCrewChoices(shipBoard, choices);

            // Apply the choices
            applyCrewChoices(shipBoard, choices);

            // Notify all clients
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCrewPlacementComplete(nicknameToNotify, nickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType());
            });

            // Mark the player as completed
            gameModel.markCrewPlacementCompleted(nickname);
        } catch (IllegalArgumentException e) {
            // Notify the error only to the client who sent invalid choices
            gameModel.getGameClientNotifier().notifyClients(Set.of(nickname), (nicknameToNotify, clientController) -> {
                System.out.println("ERROR submitCrewChoices: " + e.getMessage());
                e.printStackTrace();
            });
        }
    }

    private void validateCrewChoices(ShipBoard shipBoard, Map<Coordinates, CrewMember> choices) {
        if(choices.isEmpty())
            return;

        // Check cabins with life support
        Map<Coordinates, Set<ColorLifeSupport>> cabinsWithLifeSupport = shipBoard.getCabinsWithLifeSupport();

        // Check coordinates and alien/life support compatibility
        for (Map.Entry<Coordinates, CrewMember> entry : choices.entrySet()) {
            Coordinates coords = entry.getKey();
            CrewMember crew = entry.getValue();

            // Check that it is a cabin with life support
            if (!cabinsWithLifeSupport.containsKey(coords)) {
                throw new IllegalArgumentException("Invalid cabin at coordinates " + coords);
            }

            // Check compatibility
            if (!shipBoard.canAcceptAlien(coords, crew)) {
                throw new IllegalArgumentException("This cabin cannot accept this type of alien");
            }
        }

        // Check for a maximum of 1 alien per color
        long purpleCount = choices.values().stream().filter(c -> c == CrewMember.PURPLE_ALIEN).count();
        long brownCount = choices.values().stream().filter(c -> c == CrewMember.BROWN_ALIEN).count();

        if (purpleCount > 1) {
            throw new IllegalArgumentException("You can have at most 1 purple alien");
        }
        if (brownCount > 1) {
            throw new IllegalArgumentException("You can have at most 1 brown alien");
        }
    }

    private void applyCrewChoices(ShipBoard shipBoard, Map<Coordinates, CrewMember> choices) {
        // Apply choices for aliens
        for (Map.Entry<Coordinates, CrewMember> entry : choices.entrySet()) {
            Coordinates coords = entry.getKey();
            CrewMember crew = entry.getValue();

            Component component = shipBoard.getComponentAt(coords);
            if (component instanceof Cabin) {
                Cabin cabin = (Cabin) component;
                cabin.fillCabin(crew);
            }
        }

        // Automatically place humans in the remaining cabins
        for (Cabin cabin : shipBoard.getCabin()) {
            if (!cabin.hasInhabitants()) {
                cabin.fillCabin(CrewMember.HUMAN);
            }
        }

        if (shipBoard.getMainCabin() != null) {
            shipBoard.getMainCabin().fillCabin(CrewMember.HUMAN);
        }
    }

    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerPicksVisibleComponent in state " + gameModel.getCurrGameState());
            return;
        }

        Component chosenComponent = gameModel.getComponentTable().pickVisibleComponent(choice);
        if (chosenComponent == null) {

            gameModel.getGameClientNotifier()
                    .notifyClients(Set.of(nickname), (nicknameToNotify, clientController) -> {
                        clientController.notifyNoMoreHiddenComponents(nicknameToNotify);
                    });

            return;
        }
        gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(chosenComponent);
    }

    @Override
    public void playerWantsToVisitLocation(String nickname, Boolean choice) {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToVisitLocation in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("AbandonedShip") || gameModel.getCurrAdventureCard().getCardName().equals("AbandonedStation"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.VISIT_LOCATION))){
            System.err.println("Player " + nickname + " tried to playerWantsToVisitLocation in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }


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
        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToVisitPlanet in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("Planets"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.CHOOSE_PLANET))){
            System.err.println("Player " + nickname + " tried to playerWantsToVisitPlanet in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenPlanetIndex(choice)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    @Override
    public void playerWantsToAcceptTheReward(String nickname, Boolean choice) {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToVisitPlanet in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("Smugglers")||(gameModel.getCurrAdventureCard().getCardName().equals("SlaveTraders") || gameModel.getCurrAdventureCard().getCardName().equals("Pirates"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.ACCEPT_THE_REWARD)))){
            System.err.println("Player " + nickname + " tried to playerWantsToVisitPlanet in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setHasAcceptedTheReward(choice)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);

    }

    @Override
    public void playerChoseDoubleEngines(String nickname, List<Coordinates> doubleEnginesCoords, List<Coordinates> batteryBoxesCoords){

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerChoseDoubleEngines in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("FreeSpace")||(gameModel.getCurrAdventureCard().getCardName().equals("WarField") || gameModel.getCurrAdventureCard().getCardName().equals("Pirates"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.CHOOSE_ENGINES)))){
            System.err.println("Player " + nickname + " tried to playerChoseDoubleEngines in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleEngines(doubleEnginesCoords)
                .setChosenBatteryBoxes(batteryBoxesCoords)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);
    }


    @Override
    public void playerChoseDoubleCannons(String nickname, List<Coordinates> doubleCannonsCoords, List<Coordinates> batteryBoxesCoords){

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerChoseDoubleEngine in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("MeteoriteStorm")||(gameModel.getCurrAdventureCard().getCardName().equals("WarField") || gameModel.getCurrAdventureCard().getCardName().equals("Pirates"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.CHOOSE_CANNONS)))){
            System.err.println("Player " + nickname + " tried to playerChoseDoubleEngine in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenDoubleCannons(doubleCannonsCoords)
                .setChosenBatteryBoxes(batteryBoxesCoords)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);

    }

    @Override
    public void playerChoseCabins(String nickname, List<Coordinates> cabinCoords) {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerChoseDoubleEngine in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("AbandonedShip")||(gameModel.getCurrAdventureCard().getCardName().equals("SlaveTraders")
                || gameModel.getCurrAdventureCard().getCardName().equals("Epidemic") || gameModel.getCurrAdventureCard().getCardName().equals("WarField"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.REMOVE_CREW_MEMBERS)))){
            System.err.println("Player " + nickname + " tried to playerChoseCabin in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenCabins(cabinCoords)
                .build();

        gameModel.getCurrAdventureCard().play(playerChoice);

    }

    @Override
    public void playerHandleSmallDanObj(String nickname, List<Coordinates> shieldCoords, List<Coordinates> batteryBoxCoords) {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerSmallDanObj in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("WarField")||(gameModel.getCurrAdventureCard().getCardName().equals("Pirates")
                || gameModel.getCurrAdventureCard().getCardName().equals("MeteoriteStorm"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.DANGEROUS_ATTACK)))){
            System.err.println("Player " + nickname + " tried to playerSmallDanObj in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenBatteryBoxes(batteryBoxCoords)
                .setChosenShield(shieldCoords)
                .build();

        gameModel.getCurrAdventureCard().play(choice);

    }

    @Override
    public void playerHandleBigMeteorite(String nickname, List<Coordinates> doubleCannonCoords, List<Coordinates> batteryBoxCoords) {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerBigMeteorite in state " + gameModel.getCurrGameState());
            return;
        }

        if(!(gameModel.getCurrAdventureCard().getCardName().equals("MeteoriteStorm")
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.DANGEROUS_ATTACK))){
            System.err.println("Player " + nickname + " tried to playerBigMeteorite in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure choice= new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenBatteryBoxes(batteryBoxCoords)
                    .setChosenDoubleCannons(doubleCannonCoords)
                    .build();

        gameModel.getCurrAdventureCard().play(choice);

    }

    @Override
    public void playerHandleBigShot(String nickname) throws RemoteException {

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerBigShot in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("WarField") || gameModel.getCurrAdventureCard().getCardName().equals("Pirates"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.DANGEROUS_ATTACK))){
            System.err.println("Player " + nickname + " tried to playerBigShot in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .build();

        gameModel.getCurrAdventureCard().play(choice);

    }

    @Override
    public void playerChoseStorage(String nickname, List<Coordinates> storageCoords){

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to playerChoseStorage in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("Planets") || gameModel.getCurrAdventureCard().getCardName().equals("AbandonedStation")
                || gameModel.getCurrAdventureCard().getCardName().equals("Smugglers"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.HANDLE_CUBES_REWARD))){
            System.err.println("Player " + nickname + " tried to playerChoseStorage in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        PlayerChoicesDataStructure choice = new PlayerChoicesDataStructure
                .Builder()
                .setChosenStorage(storageCoords)
                .build();

        gameModel.getCurrAdventureCard().play(choice);

    }

    @Override
    public void spreadEpidemic(String nickname) throws RemoteException{

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to spreadEpidemic in state " + gameModel.getCurrGameState());
            return;
        }

        if(!(gameModel.getCurrAdventureCard().getCardName().equals("Epidemic")
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.EPIDEMIC))){
            System.err.println("Player " + nickname + " tried to spreadEpidemic in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        gameModel.getCurrAdventureCard().play(new PlayerChoicesDataStructure());
    }

    @Override
    public void stardustEvent(String nickname) throws RemoteException{

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to stardustEvent in state " + gameModel.getCurrGameState());
            return;
        }

        if(!(gameModel.getCurrAdventureCard().getCardName().equals("Stardust")
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.STARDUST))){
            System.err.println("Player " + nickname + " tried to stardustEvent in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        gameModel.getCurrAdventureCard().play(new PlayerChoicesDataStructure());
    }

    public void evaluatedCrewMembers(String nickname) throws RemoteException{

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to evaluatedCrewMembers in state " + gameModel.getCurrGameState());
            return;
        }

        if(!(gameModel.getCurrAdventureCard().getCardName().equals("WarField")
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.EVALUATE_CREW_MEMBERS))){
            System.err.println("Player " + nickname + " tried to evaluatedCrewMembers in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        gameModel.getCurrAdventureCard().play(new PlayerChoicesDataStructure());
    }

    @Override
    public boolean playerWantsToWatchLittleDeck(String nickname, int littleDeckChoice) {

        return gameModel.getDeck().isLittleDeckAvailable(littleDeckChoice);
    }

    @Override
    public void playerWantsToReleaseLittleDeck(String nickname, int littleDeckChoice) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToReleaseLittleDeck in state " + gameModel.getCurrGameState());
            return;
        }

        gameModel.getDeck().releaseLittleDeck(littleDeckChoice);
    }

    @Override
    public void playerWantsToRestartHourglass(String nickname) throws RemoteException {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToRestartHourglass in state " + gameModel.getCurrGameState());
            return;
        }

        gameModel.restartHourglass(nickname);
    }

    @Override
    public void notifyHourglassEnded(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to notifyHourglassEnded in state " + gameModel.getCurrGameState());
            return;
        }

        gameModel.hourglassEnded();
    }

    @Override
    public void playerWantsToRemoveComponent(String nickname, Coordinates coordinates) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToRemoveComponent in state " + gameModel.getCurrGameState());
            return;
        }

        if(coordinates!=null) {
            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
            Set<Set<Coordinates>> shipParts = shipBoard.removeAndRecalculateShipParts(coordinates.getX(), coordinates.getY());

            // Store the ship parts for this player
            temporaryShipParts.put(nickname, shipParts);

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                try {
                    shipBoard.ejectAliens();
                    Component[][] shipMatrix = shipBoard.getShipMatrix();
                    Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
                    Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                    if (shipParts.size() <= 1) {
                        shipBoard.checkShipBoard();
                        if (incorrectlyPositionedComponentsCoordinates.isEmpty()) {
                            clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);

                        } else
                            clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);
                    } else
                        clientController.notifyShipPartsGeneratedDueToRemoval(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, shipParts, componentsPerType);
                } catch (RemoteException e) {
                    System.err.println("Remote Exception");
                }
            });

            if (shipParts.size() <= 1) // that is, I directly removed a component
                // Check if all ships are correct and if so, change the phase
                gameModel.checkAndTransitionToNextPhase();
        }
    }

    @Override
    public void playerChoseShipPart(String nickname, Set<Coordinates> chosenShipPart) throws RemoteException {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerChoseShipPart in state " + gameModel.getCurrGameState());
            return;
        }

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

        // Get all ship parts stored for this player
        Set<Set<Coordinates>> allShipParts = temporaryShipParts.get(nickname);

        if (allShipParts != null) {
            // Remove all ship parts EXCEPT the chosen one
            for (Set<Coordinates> shipPart : allShipParts) {
                if (!shipPart.equals(chosenShipPart)) {
                    shipBoard.removeShipPart(shipPart);
                }
            }

            shipBoard.checkShipBoard();


            gameModel.getGameClientNotifier().notifyAllClients( (nicknameToNotify, clientController) -> {
                try {
                    Component[][] shipMatrix = shipBoard.getShipMatrix();
                    Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
                    Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                    if(shipBoard.isShipCorrect())
                        clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);
                    else
                        clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);
                } catch (RemoteException e) {
                    System.err.println("Remote Exception");
                }
            });

            // Clear the temporary map
            temporaryShipParts.remove(nickname);
        }

        // Check if all ships are correct and change phase if necessary
        gameModel.checkAndTransitionToNextPhase();
    }

    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice){

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToFocusReservedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        ((Level2ShipBoard) gameModel.getPlayers().get(nickname).getPersonalBoard()).focusReservedComponent(choice);
    }

    @Override
    public void requestPrefabShips(String nickname) throws RemoteException {
        // Retrieve the list of prefab ships

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToFocusReservedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        List<PrefabShipInfo> prefabShips = PrefabShipFactory.getAvailablePrefabShips(gameModel.isTestFlight());

        // Notify the client asynchronously
        gameModel.getGameClientNotifier().notifyClients(
                Set.of(nickname),
                (nicknameToNotify, clientController) -> {
                    try {
                        clientController.notifyPrefabShipsAvailable(nicknameToNotify, prefabShips);
                    } catch (IOException e) {
                        System.err.println("Error notifying client about prefab ships: " + e.getMessage());
                    }
                }
        );
    }

    public void startCheckShipBoardAfterAttack(String nickname){

        if(gameModel.getCurrGameState()!=GameState.PLAY_CARD) {
            System.err.println("Player " + nickname + " tried to startCheckShipBoardAfterAttack in state " + gameModel.getCurrGameState());
            return;
        }

        if(!((gameModel.getCurrAdventureCard().getCardName().equals("MeteoriteStorm") || gameModel.getCurrAdventureCard().getCardName().equals("Pirates")
             || gameModel.getCurrAdventureCard().getCardName().equals("WarField"))
                && gameModel.getCurrAdventureCard().getCurrState().equals(CardState.CHECK_SHIPBOARD_AFTER_ATTACK))){
            System.err.println("Player " + nickname + " tried to startCheckShipBoardAfterAttack in Card state " + gameModel.getCurrAdventureCard().getCurrState());
        }

        gameModel.getCurrAdventureCard().play(new PlayerChoicesDataStructure());
    }

    @Override
    public void requestSelectPrefabShip(String nickname, String prefabShipId) throws RemoteException {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToFocusReservedComponent in state " + gameModel.getCurrGameState());
            return;
        }

        try {
            // Get information about the prefab ship
            PrefabShipInfo prefabShipInfo = PrefabShipFactory.getPrefabShipInfo(prefabShipId);
            if (prefabShipInfo == null) {
                notifySelectionFailure(nickname, "Invalid prefab ship ID: " + prefabShipId);
                return;
            }

            // Check if it's a test flight ship
            if (prefabShipInfo.isForTestFlight() && !gameModel.isTestFlight()) {
                notifySelectionFailure(nickname, "This prefab ship is only available in test flight mode");
                return;
            }

            // Get the player's shipboard
            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

            // Apply the prefab configuration
            boolean success = PrefabShipFactory.applyPrefabShip(shipBoard, prefabShipId);
            if (!success) {
                notifySelectionFailure(nickname, "Failed to apply prefab ship configuration");
                return;
            }

            // Notify the client of success
            gameModel.getGameClientNotifier().notifyClients(
                    Set.of(nickname),
                    (nicknameToNotify, clientController) -> {
                        try {
                            clientController.notifyPrefabShipSelectionResult(nicknameToNotify, true, null);
                        } catch (IOException e) {
                            System.err.println("Error notifying client about selection result: " + e.getMessage());
                        }
                    }
            );

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, nickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType());
            });

            // Notify all clients about the selection
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerSelectedPrefabShip(nicknameToNotify, nickname, prefabShipInfo);
            });

            //playerEndsBuildShipBoardPhase(nickname);
        } catch (Exception e) {
            notifySelectionFailure(nickname, "Internal error: " + e.getMessage());
        }
    }

    @Override
    public void playerWantsToLand(String nickname){
        if(!(gameModel.getCurrGameState()!=GameState.PLAY_CARD
                || gameModel.getCurrGameState()!=GameState.CHECK_PLAYERS
                || gameModel.getCurrGameState()!=GameState.DRAW_CARD))
            return;

        Player player = gameModel.getPlayers().get(nickname);
        gameModel.getFlyingBoard().addOutPlayer(player, true);

        if(gameModel.getFlyingBoard().getRanking().isEmpty())
            gameModel.setCurrGameState(GameState.END_GAME);
    }

    private void notifySelectionFailure(String nickname, String errorMessage) {
        gameModel.getGameClientNotifier().notifyClients(
                Set.of(nickname),
                (nicknameToNotify, clientController) -> {
                    try {
                        clientController.notifyPrefabShipSelectionResult(nicknameToNotify, false, errorMessage);
                    } catch (IOException e) {
                        System.err.println("Error notifying client about selection failure: " + e.getMessage());
                    }
                }
        );
    }

    @Override
    public void debugSkipToLastCard() throws RemoteException {
        if (!canSkipCards()) {
            return;
        }
        
        try {
            gameModel.getDeck().skipToLastCard();
        } catch (IllegalStateException e) {
            throw new RemoteException(e.getMessage());
        }
        
        if (gameModel.getCurrGameState() != GameState.DRAW_CARD) {
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }
    }
    
    private boolean canSkipCards() {
        if (!gameModel.isStarted()) {
            return false;
        }
        
        GameState currentState = gameModel.getCurrGameState();
        if (currentState == GameState.SETUP || 
            currentState == GameState.BUILD_SHIPBOARD || 
            currentState == GameState.CHECK_SHIPBOARD || 
            currentState == GameState.PLACE_CREW || 
            currentState == GameState.END_GAME) {
            return false;
        }
        
        return true;
    }
}
