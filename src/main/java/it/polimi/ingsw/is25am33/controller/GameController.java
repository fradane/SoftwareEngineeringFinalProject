package it.polimi.ingsw.is25am33.controller;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.PrefabShipFactory;
import it.polimi.ingsw.is25am33.client.model.PrefabShipInfo;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.Level2ShipBoard;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.ColorLifeSupport;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
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
        System.out.println("Show message: " + string);
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
            clientControllers.forEach((playerNickname, clientController) -> {
                clientControllers.remove(playerNickname);
                DNS.gameControllers.remove(playerNickname);
                dns.getConnectionManager().getClients().remove(playerNickname);
            });
            dns.removeGame(getGameInfo().getGameId());
            System.out.println("[" + getGameInfo().getGameId() + "] Deleted!");
        }

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
    public void playerEndsBuildShipBoardPhase(String nickname) {
        gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname));
        if (gameModel.getFlyingBoard().getCurrentRanking().size() == gameModel.getMaxPlayers())
            gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
    }

    @Override
    public void playerPlacePlaceholder(String nickname) {
        if (gameModel.getFlyingBoard().insertPlayer(gameModel.getPlayers().get(nickname)) == gameModel.getMaxPlayers())
            gameModel.setCurrGameState(GameState.CHECK_SHIPBOARD);
    }

    @Override
    public void handleClientChoice(String nickname, PlayerChoicesDataStructure choice) throws IOException {
        gameModel.getCurrAdventureCard().play(choice);
    }

    @Override
    public void submitCrewChoices(String nickname, Map<Coordinates, CrewMember> choices) throws IOException {
        Player player = gameModel.getPlayers().get(nickname);
        ShipBoard shipBoard = player.getPersonalBoard();

        try {
            // Validazione delle scelte
            validateCrewChoices(shipBoard, choices);

            // Applica le scelte
            applyCrewChoices(shipBoard, choices);

            // Notifica tutti i client
            gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCrewPlacementComplete(nicknameToNotify, nickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType());
            });

            // Segna il giocatore come completato
            gameModel.markCrewPlacementCompleted(nickname);
        } catch (IllegalArgumentException e) {
            // Notifica l'errore solo al client che ha inviato scelte non valide
            gameModel.getGameContext().notifyClients(Set.of(nickname), (nicknameToNotify, clientController) -> {
                System.out.println("ERRORE submitCrewChoices: " + e.getMessage());
                e.printStackTrace();
                //TODO capire se ha senso aggiugnere un metodo notifyError per mostrare gli erorri generici
                //clientController.notifyError(nicknameToNotify, e.getMessage());
            });
        }
    }

    private void validateCrewChoices(ShipBoard shipBoard, Map<Coordinates, CrewMember> choices) {
        if(choices.isEmpty())
            return;

        // Verifica cabine con supporto vitale
        Map<Coordinates, Set<ColorLifeSupport>> cabinsWithLifeSupport = shipBoard.getCabinsWithLifeSupport();

        // Verifica coordinate e compatibilità alieno/supporto vitale
        for (Map.Entry<Coordinates, CrewMember> entry : choices.entrySet()) {
            Coordinates coords = entry.getKey();
            CrewMember crew = entry.getValue();

            // Verifica che sia una cabina con supporto vitale
            if (!cabinsWithLifeSupport.containsKey(coords)) {
                throw new IllegalArgumentException("Invalid cabin at coordinates " + coords);
            }

            // Verifica compatibilità
            if (!shipBoard.canAcceptAlien(coords, crew)) {
                throw new IllegalArgumentException("This cabin cannot accept this type of alien");
            }
        }

        // Verifica massimo 1 alieno per colore
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
        // Applica le scelte per gli alieni
        for (Map.Entry<Coordinates, CrewMember> entry : choices.entrySet()) {
            Coordinates coords = entry.getKey();
            CrewMember crew = entry.getValue();

            Component component = shipBoard.getComponentAt(coords);
            if (component instanceof Cabin) {
                Cabin cabin = (Cabin) component;
                cabin.fillCabin(crew);
            }
        }

        // Posiziona automaticamente umani nelle cabine rimanenti
        for (Cabin cabin : shipBoard.getCabin()) {
            if (!cabin.hasInhabitants()) {
                cabin.fillCabin(CrewMember.HUMAN);
            }
        }

        shipBoard.getMainCabin().fillCabin(CrewMember.HUMAN);
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
        //TODO probabilmente da eliminare
//        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
//
//        List<Cabin> cabins = cabinCoords
//                .stream()
//                .map(shipBoard::getComponentAt)
//                .map(Cabin.class::cast)
//                .toList();
//
//        PlayerChoicesDataStructure playerChoice = new PlayerChoicesDataStructure
//                .Builder()
//                .setChosenCabins(cabinsCoords)
//                .build();
//
//        gameModel.getCurrAdventureCard().play(playerChoice);
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

        PlayerChoicesDataStructure choice;

        // check whether the coordinates are valid
        if (!doubleCannonCoords.isCoordinateInvalid() && !batteryBoxCoords.isCoordinateInvalid()) {
            doubleCannon = ((DoubleCannon) shipBoard.getComponentAt(doubleCannonCoords));
            batteryBox = ((BatteryBox) shipBoard.getComponentAt(batteryBoxCoords));

            choice = new PlayerChoicesDataStructure
                    .Builder()
                    .setChosenBatteryBox(batteryBox)
                    .setChosenDoubleCannon(doubleCannon)
                    .build();
        }else {
            choice = new PlayerChoicesDataStructure
                    .Builder()
                    .build();
        }

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
    public void playerChoseStorage(String nickname, List<Coordinates> storageCoords) throws RemoteException {

        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
        List<Storage> storage = new ArrayList<>();
        if (!storageCoords.isEmpty())
            storageCoords.forEach(coords -> storage.add(coords.isCoordinateInvalid() ? null : ((Storage) shipBoard.getComponentAt(coords))));

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
                Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
                if(shipParts.size() == 1){
                    shipBoard.checkShipBoard();
                    if(incorrectlyPositionedComponentsCoordinates.isEmpty()) {
                        clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);

                    }else
                        clientController.notifyInvalidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);
                }else if(shipParts.isEmpty())
                    clientController.notifyValidShipBoard(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType);
                else
                    clientController.notifyShipPartsGeneratedDueToRemoval(nicknameToNotify, nickname, shipMatrix, incorrectlyPositionedComponentsCoordinates, shipParts, componentsPerType);
            } catch (RemoteException e) {
                System.err.println("Remote Exception");
            }
        });

        if(shipParts.size() == 1) //ovvero ho direttamente rimosso un componente
            // Controllo se tutte le navi sono corrette e in caso cambio la fase
            gameModel.checkAndTransitionToNextPhase();


    }

//    @Override
//    public void playerChoseShipPart(String nickname, Set<Coordinates> chosenShipPart) throws RemoteException {
//        ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();
//
//        // Obtain all ship parts memorized for this player
//        Set<Set<Coordinates>> allShipParts = temporaryShipParts.get(nickname);
//
//        if (allShipParts != null) {
//            // Remove all ship parts EXCEPT the chosen one
//            for (Set<Coordinates> shipPart : allShipParts) {
//                if (!shipPart.equals(chosenShipPart)) {
//                    shipBoard.removeShipPart(shipPart);
//                }
//            }
//
//            // After removing parts, check the entire ship board for incorrect components
//            shipBoard.checkShipBoard();
//
//            // Clean up the temporary map
//            temporaryShipParts.remove(nickname);
//        }
//
//        // Check if all ships are correct and change phase if necessary
//        gameModel.checkAndTransitionToNextPhase();
//    }

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

            // After removing parts, check the entire ship board for incorrect components
            shipBoard.checkShipBoard();


            gameModel.getGameContext().notifyAllClients( (nicknameToNotify, clientController) -> {
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

    @Override
    public void requestPrefabShips(String nickname) throws RemoteException {
        // Recupera la lista delle navi prefabbricate
        List<PrefabShipInfo> prefabShips = PrefabShipFactory.getAvailablePrefabShips(gameModel.isTestFlight());

        // Notifica il client in modo asincrono
        gameModel.getGameContext().notifyClients(
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

    @Override
    public void requestSelectPrefabShip(String nickname, String prefabShipId) throws RemoteException {
        try {
            // Ottieni informazioni sulla nave prefabbricata
            PrefabShipInfo prefabShipInfo = PrefabShipFactory.getPrefabShipInfo(prefabShipId);
            if (prefabShipInfo == null) {
                notifySelectionFailure(nickname, "Invalid prefab ship ID: " + prefabShipId);
                return;
            }

            // Verifica se è una nave per test flight
            if (prefabShipInfo.isForTestFlight() && !gameModel.isTestFlight()) {
                notifySelectionFailure(nickname, "This prefab ship is only available in test flight mode");
                return;
            }

            // Ottieni la shipboard del giocatore
            ShipBoard shipBoard = gameModel.getPlayers().get(nickname).getPersonalBoard();

            // Applica la configurazione prefabbricata
            boolean success = PrefabShipFactory.applyPrefabShip(shipBoard, prefabShipId);
            if (!success) {
                notifySelectionFailure(nickname, "Failed to apply prefab ship configuration");
                return;
            }

            // Notifica il client del successo
            gameModel.getGameContext().notifyClients(
                    Set.of(nickname),
                    (nicknameToNotify, clientController) -> {
                        try {
                            clientController.notifyPrefabShipSelectionResult(nicknameToNotify, true, null);
                        } catch (IOException e) {
                            System.err.println("Error notifying client about selection result: " + e.getMessage());
                        }
                    }
            );

            gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, nickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType());
            });

            // Notifica tutti i client della scelta
            gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerSelectedPrefabShip(nicknameToNotify, nickname, prefabShipInfo);
            });

            //playerEndsBuildShipBoardPhase(nickname);
        } catch (Exception e) {
            notifySelectionFailure(nickname, "Internal error: " + e.getMessage());
        }
    }

    private void notifySelectionFailure(String nickname, String errorMessage) {
        gameModel.getGameContext().notifyClients(
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
}
