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
import java.util.stream.Collectors;

public class GameController extends UnicastRemoteObject implements CallableOnGameController {
    private final GameModel gameModel;
    private final ConcurrentHashMap<String, CallableOnClientController> clientControllers = new ConcurrentHashMap<>();
    private final DNS dns;

    private final Map<String, Set<Set<Coordinates>>> temporaryShipParts = new ConcurrentHashMap<>();

    /**
     * Displays a message on the server console.
     *
     * @param string The message to display
     * @throws RemoteException If there is an error in remote communication
     */
    @Override
    public void showMessage(String string) throws RemoteException {
        System.out.println("Show message: " + string);
    }

    /**
     * Creates a new GameController instance.
     *
     * @param gameId       The unique identifier for this game
     * @param maxPlayers   Maximum number of players allowed in the game
     * @param isTestFlight Whether this is a test flight game mode
     * @param dns          The DNS service for network communication
     * @throws RemoteException If there is an error in remote communication
     */
    public GameController(String gameId, int maxPlayers, boolean isTestFlight, DNS dns) throws RemoteException {
        this.gameModel = new GameModel(gameId, maxPlayers, isTestFlight);
        this.gameModel.createGameClientNotifier(clientControllers);
        this.dns = dns;
    }

    /**
     * Gets the game model associated with this controller.
     *
     * @return The GameModel instance
     */
    public GameModel getGameModel() {
        return gameModel;
    }

    /**
     * Adds a new player to the game.
     *
     * @param nickname         The player's unique nickname
     * @param color            The player's chosen color
     * @param clientController The client controller associated with this player
     */
    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController) {
        clientControllers.put(nickname, clientController);
        gameModel.addPlayer(nickname, color, clientController);
    }

    /**
     * Gets all client controllers currently in the game.
     *
     * @return Map of player nicknames to their client controllers
     */
    public ConcurrentHashMap<String, CallableOnClientController> getClientControllers() {
        return clientControllers;
    }

    /**
     * Removes a player from the game.
     *
     * @param nickname The nickname of the player to remove
     */
    public void removePlayer(String nickname) {
        gameModel.removePlayer(nickname);
    }

    /**
     * Gets current game information including players and game state.
     *
     * @return GameInfo object containing current game state
     */
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

    /**
     * Starts the game and notifies all clients.
     */
    public void startGame() {
        gameModel.setStarted(true);
        GameInfo gameInfo = getGameInfo();
        gameModel.setCurrGameState(GameState.BUILD_SHIPBOARD);
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyGameStarted(nicknameToNotify, getGameInfo());
        });
        System.out.println("[" + gameInfo.getGameId() + "] Game started");
    }

    /**
     * Handles a player leaving the game after it was created.
     *
     * @param nickname The nickname of the leaving player
     */
    @Override
    public void leaveGameAfterCreation(String nickname) {
        dns.handleDisconnection(nickname);
    }

    /**
     * Handles a player picking a hidden component during ship building.
     *
     * @param nickname The nickname of the player picking the component
     */
    @Override
    public void playerPicksHiddenComponent(String nickname) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerPicksHiddenComponent in state " + gameModel.getCurrGameState());
                return;
            }

            Component pickedComponent = gameModel.getComponentTable().pickHiddenComponent();
            if (pickedComponent == null) {
                gameModel.getGameClientNotifier()
                        .notifyClients(Set.of(nickname), (nicknameToNotify, clientController) -> {
                            clientController.notifyNoMoreHiddenComponents(nicknameToNotify);
                        });
                return;
            }
            gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(pickedComponent);
        }
    }

    /**
     * Handles a player's request to place their focused component.
     *
     * @param nickname    The player's nickname
     * @param coordinates Where to place the component
     * @param rotation    How many times to rotate the component
     */
    @Override
    public void playerWantsToPlaceFocusedComponent(String nickname, Coordinates coordinates, int rotation) {

        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerWantsToPlaceFocusedComponent in state " + gameModel.getCurrGameState());
                return;
            }

            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
            for (int i = 0; i < rotation; i++)
                shipBoard.getFocusedComponent().rotate();
            shipBoard.placeComponentWithFocus(coordinates.getX(), coordinates.getY());
        }

    }

    @Override
    public void playerWantsToReserveFocusedComponent(String nickname) {

        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerWantsToReserveFocusedComponent in state " + gameModel.getCurrGameState());
                return;
            }

            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
            ((Level2ShipBoard) shipBoard).book();
        }

    }

    @Override
    public void playerWantsToReleaseFocusedComponent(String nickname) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerWantsToReleaseFocusedComponent in state " + gameModel.getCurrGameState());
                return;
            }

            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
            Component component = shipBoard.releaseFocusedComponent();
            if (!shipBoard.getNotActiveComponents().contains(component))
                gameModel.getComponentTable().addVisibleComponent(component);
        }
    }

    /**
     * Handles a player's request to end their ship board building phase.
     * This method is called when a player has finished building their ship and wants to proceed.
     * If all players have ended their building phase, the game state changes to CHECK_SHIPBOARD.
     *
     * @param nickname The nickname of the player ending their building phase
     */
    @Override
    public void playerEndsBuildShipBoardPhase(String nickname) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerEndsBuildShipBoardPhase in state " + gameModel.getCurrGameState());
                return;
            }

            if (gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname)) == gameModel.getMaxPlayers())
                gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
        }
    }

    /**
     * Handles a player placing their pawn on the game board.
     * When all players have placed their pawns, the game state changes to CHECK_SHIPBOARD.
     *
     * @param nickname The nickname of the player placing their pawn
     */
    @Override
    public void playerPlacesPawn(String nickname) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerPlacesPawn in state " + gameModel.getCurrGameState());
                return;
            }

            if (gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname)) == gameModel.getMaxPlayers())
                gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
        }
    }

    /**
     * Handles a client's choice during gameplay.
     * Processes the player's choice through the current adventure card.
     *
     * @param nickname The nickname of the player making the choice
     * @param choice   The data structure containing the player's choices
     * @throws IOException If there is an error processing the choice
     */
    @Override
    public void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException {
        gameModel.getCurrAdventureCard().play(choice);
    }

    /**
     * Submits crew placement choices for a player.
     * Validates and applies the crew placement choices, then notifies all clients.
     *
     * @param nickname The nickname of the player submitting crew choices
     * @param choices  Map of coordinates to crew member placements
     * @throws IOException If there is an error processing the choices
     */
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

    /**
     * Handles a player's request to pick a visible component from the component table.
     * The component becomes the player's focused component if successfully picked.
     *
     * @param nickname The nickname of the player picking the component
     * @param choice   The index of the chosen visible component
     */
    @Override
    public void playerPicksVisibleComponent(String nickname, Integer choice) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerPicksVisibleComponent in state " + gameModel.getCurrGameState());
                return;
            }

            Component chosenComponent = gameModel.getComponentTable().pickVisibleComponent(choice);
            if (chosenComponent == null) {
                gameModel.getGameClientNotifier()
                        .notifyClients(Set.of(nickname), (nicknameToNotify, clientController) -> {
                            clientController.notifyStolenVisibleComponent(nicknameToNotify);
                        });
                return;
            }
            gameModel.getPlayers().get(nickname).getPersonalBoard().setFocusedComponent(chosenComponent);
        }
    }

    /**
     * Handles a player's decision to visit or skip a location card event.
     * Only valid during certain adventure card states.
     *
     * @param nickname The nickname of the player making the choice
     * @param choice   True to visit the location, false to skip
     */
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

    /**
     * Handles a player's request to throw dice during card events.
     * Creates and processes an empty choice data structure.
     *
     * @param nickname The nickname of the player throwing dice
     */
    @Override
    public void playerWantsToThrowDices(String nickname) {
        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure.Builder().build();
        gameModel.getCurrAdventureCard().play(playerChoice);
    }

    /**
     * Handles a player's choice to visit a planet during the Planets card event.
     * Only valid during the CHOOSE_PLANET state of the Planets card.
     *
     * @param nickname The nickname of the player making the choice
     * @param choice   The index of the chosen planet
     */
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

    /**
     * Handles a player's decision to accept or reject a reward.
     * Valid during reward offering events like Smugglers or Pirates cards.
     *
     * @param nickname The nickname of the player making the choice
     * @param choice   True to accept the reward, false to reject
     */
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

    /**
     * Handles a player's selection of double engines and battery boxes during card events.
     * Used in FreeSpace, WarField and Pirates card events.
     *
     * @param nickname            The nickname of the player making the selection
     * @param doubleEnginesCoords List of coordinates for chosen double engines
     * @param batteryBoxesCoords  List of coordinates for chosen battery boxes
     */
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


    /**
     * Handles a player's selection of double cannons and battery boxes.
     * Used in MeteoriteStorm, WarField and Pirates card events.
     *
     * @param nickname            The nickname of the player making the selection
     * @param doubleCannonsCoords List of coordinates for chosen double cannons
     * @param batteryBoxesCoords  List of coordinates for chosen battery boxes
     */
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

    /**
     * Handles a player's selection of cabins during crew-related events.
     * Used in various card events that affect crew members.
     *
     * @param nickname    The nickname of the player choosing the cabins
     * @param cabinCoords List of coordinates for the chosen cabins
     */
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

    /**
     * Handles a player's response to small dangerous objects.
     * Used in WarField, Pirates, and MeteoriteStorm events.
     *
     * @param nickname         The nickname of the player handling the threat
     * @param shieldCoords     List of coordinates for shields to use
     * @param batteryBoxCoords List of coordinates for battery boxes to use
     */
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

    /**
     * Handles a player's response to a big meteorite threat.
     * Used specifically in the MeteoriteStorm card event.
     *
     * @param nickname           The nickname of the player handling the meteorite
     * @param doubleCannonCoords List of coordinates for double cannons to use
     * @param batteryBoxCoords   List of coordinates for battery boxes to use
     */
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

    /**
     * Handles a player's response to a big shot threat.
     * Used in WarField and Pirates card events.
     *
     * @param nickname The nickname of the player handling the big shot
     * @throws RemoteException If there is an error in remote communication
     */
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

    /**
     * Handles a player's selection of storage components.
     * Used during cube reward events in various cards.
     *
     * @param nickname      The nickname of the player choosing storage
     * @param storageCoords List of coordinates for chosen storage components
     */
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

    /**
     * Handles the spread of an epidemic during the Epidemic card event.
     *
     * @param nickname The nickname of the player affected by the epidemic
     * @throws RemoteException If there is an error in remote communication
     */
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

    /**
     * Handles the Stardust card event for a player.
     *
     * @param nickname The nickname of the player experiencing the stardust event
     * @throws RemoteException If there is an error in remote communication
     */
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

    /**
     * Evaluates crew members during the WarField card event.
     *
     * @param nickname The nickname of the player whose crew is being evaluated
     * @throws RemoteException If there is an error in remote communication
     */
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

    /**
     * Handles a player's request to restart the hourglass timer.
     * This can only be done during the BUILD_SHIPBOARD phase.
     *
     * @param nickname The nickname of the player requesting the restart
     * @throws RemoteException If there is an error in remote communication
     */
    @Override
    public void playerWantsToRestartHourglass(String nickname) throws RemoteException {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to playerWantsToRestartHourglass in state " + gameModel.getCurrGameState());
            return;
        }

        gameModel.restartHourglass(nickname);
    }

    /**
     * Notifies the game that the hourglass timer has ended for a player.
     * <p>
     * Only allowed during the BUILD_SHIPBOARD phase. If the call occurs in a different phase,
     * the method logs an error and returns without taking action.
     *
     * @param nickname The nickname of the player whose timer ended
     */
    @Override
    public void notifyHourglassEnded(String nickname) {

        if(gameModel.getCurrGameState()!=GameState.BUILD_SHIPBOARD) {
            System.err.println("Player " + nickname + " tried to notifyHourglassEnded in state " + gameModel.getCurrGameState());
            return;
        }

        gameModel.hourglassEnded();
    }

    /**
     * Handles a player's request to remove a component from their ship.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates that the current game state allows removal.</li>
     *   <li>Recalculates ship parts after removal and stores them for later player choice.</li>
     *   <li>Updates the ship board by ejecting aliens and identifying disconnected parts.</li>
     *   <li>Sends the updated ship board view to all clients, indicating whether it's valid or not.</li>
     *   <li>Checks and triggers a phase transition if the board is valid and connected.</li>
     * </ul>
     *
     * @param nickname   The nickname of the player requesting removal
     * @param coordinates The coordinates of the component to remove
     */
    @Override
    public void playerWantsToRemoveComponent(String nickname, Coordinates coordinates) {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.CHECK_SHIPBOARD && gameModel.getCurrGameState() != GameState.PLAY_CARD) {
                System.err.println("Player " + nickname + " tried to playerWantsToRemoveComponent in state " + gameModel.getCurrGameState());
                return;
            }

            if (coordinates != null) {
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
                        List<Component> notActiveComponentsList = shipBoard.getNotActiveComponents();
                        if (shipParts.size() <= 1) {
                            shipBoard.checkShipBoard();
                            if (incorrectlyPositionedComponentsCoordinates.isEmpty()) {
                                clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);

                            } else
                                clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);
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
    }

    /**
     * Handles the player's decision to keep a specific ship part after a component removal caused disconnection.
     * <p>
     * This method removes all other disconnected parts from the player's ship, leaving only the chosen one.
     * It then notifies all clients with the updated ship status, indicating whether it is valid or not.
     * Finally, it clears temporary ship parts and checks whether the phase should advance.
     *
     * @param nickname         The nickname of the player making the selection
     * @param chosenShipPart   The coordinates of the ship part the player chooses to keep
     * @throws RemoteException If a remote communication error occurs
     */
    @Override
    public void playerChoseShipPart(String nickname, Set<Coordinates> chosenShipPart) throws RemoteException {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.CHECK_SHIPBOARD && gameModel.getCurrGameState() != GameState.PLAY_CARD) {
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


                gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                    try {
                        Component[][] shipMatrix = shipBoard.getShipMatrix();
                        Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
                        Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                        List<Component> notActiveComponentsList = shipBoard.getNotActiveComponents();
                        if (shipBoard.isShipCorrect())
                            clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);
                        else
                            clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);
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
    }

    /**
     * Handles a player's request to focus on a previously reserved component.
     * Only available during the BUILD_SHIPBOARD phase.
     *
     * @param nickname The nickname of the player focusing the component
     * @param choice   The index of the reserved component to focus
     */
    @Override
    public void playerWantsToFocusReservedComponent(String nickname, int choice){
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
                System.err.println("Player " + nickname + " tried to playerWantsToFocusReservedComponent in state " + gameModel.getCurrGameState());
                return;
            }

            ((Level2ShipBoard) gameModel.getPlayers().get(nickname).getPersonalBoard()).focusReservedComponent(choice);
        }
    }

    /**
     * Handles a player's request to view available prefabricated ships.
     * Only available during the BUILD_SHIPBOARD phase.
     *
     * @param nickname The nickname of the player requesting prefab ships
     * @throws RemoteException If there is an error in remote communication
     */
    @Override
    public void requestPrefabShips(String nickname) throws RemoteException {
        // Retrieve the list of prefab ships
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
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
    }

    /**
     * Initiates the ship board check after an attack event.
     * Used in MeteoriteStorm, Pirates, and WarField events.
     *
     * @param nickname The nickname of the player whose ship is being checked
     */
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

    /**
     * Handles a player's request to select and apply a prefabricated ship design.
     * Validates the selection and applies it to the player's ship board.
     *
     * @param nickname     The nickname of the player selecting the prefab ship
     * @param prefabShipId The identifier of the chosen prefab ship
     * @throws RemoteException If there is an error in remote communication
     */
    @Override
    public void requestSelectPrefabShip(String nickname, String prefabShipId) throws RemoteException {
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (gameModel.getCurrGameState() != GameState.BUILD_SHIPBOARD) {
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
                    clientController.notifyShipBoardUpdate(nicknameToNotify, nickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType(), shipBoard.getNotActiveComponents());
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
    }

    /**
     * Handles a player's request to land their ship.
     * This action is only available during certain game states.
     * If all players have landed, the game transitions to END_GAME state.
     *
     * @param nickname The nickname of the player requesting to land
     */
    @Override
    public void playerWantsToLand(String nickname){
        synchronized (gameModel.getPlayers().get(nickname)) {
            if (!(gameModel.getCurrGameState() != GameState.PLAY_CARD
                    || gameModel.getCurrGameState() != GameState.CHECK_PLAYERS
                    || gameModel.getCurrGameState() != GameState.DRAW_CARD))
                return;

            Player player = gameModel.getPlayers().get(nickname);
            gameModel.getFlyingBoard().addOutPlayer(player, true);

            if (gameModel.getFlyingBoard().getRanking().isEmpty())
                gameModel.setCurrGameState(GameState.END_GAME);
        }
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

    /**
     * Debug method to skip to the last card in the deck.
     * Only works in certain game states and when the game has started.
     *
     * @throws RemoteException If there is an error in remote communication
     */
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
        return currentState != GameState.SETUP &&
                currentState != GameState.BUILD_SHIPBOARD &&
                currentState != GameState.CHECK_SHIPBOARD &&
                currentState != GameState.PLACE_CREW &&
                currentState != GameState.END_GAME;
    }
}
