package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class GameModel {
    private final String gameId;
    private final boolean isTestFlight;
    private final int maxPlayers;
    private boolean isStarted;

    private AdventureCard currAdventureCard;
    private final FlyingBoard flyingBoard;
    private final ConcurrentHashMap<String, Player> players;
    private List<Player> currRanking;
    private Player currPlayer;
    private Iterator<Player> playerIterator;
    private DangerousObj currDangerousObj;
    private GameState currGameState;
    private final Deck deck;
    private final ComponentTable componentTable;
    private GameClientNotifier gameClientNotifier;

    // attributes useful for hourglass restarting
    private final Object hourglassLock = new Object();
    private Integer flipsLeft;
    private Integer numClientsFinishedTimer = 0;
    private Boolean isRestartInProgress = false;

    // Lock per la transizione di stato
    private final Object stateTransitionLock = new Object();

    public GameModel(String gameId, int maxPlayers, boolean isTestFlight) {
        this.gameId = gameId;
        this.maxPlayers = maxPlayers;
        this.isTestFlight = isTestFlight;
        this.flyingBoard = isTestFlight ? new Level1FlyingBoard() : new Level2FlyingBoard();
        currAdventureCard = null;
        currRanking = new ArrayList<>();
        currPlayer = null;
        currDangerousObj = null;
        currGameState = GameState.SETUP;
        this.players = new ConcurrentHashMap<>();
        deck = new Deck();
        isStarted = false;
        componentTable = new ComponentTable();
        flipsLeft = isTestFlight ? 1 : 2;
    }

    /**
     * Restarts the hourglass for a game session. This method ensures that all players have completed
     * their timers before notifying them about the restart, and it decrements the number of flips left.
     * Throws a RemoteException if there are no flips left or if another player is already restarting the hourglass.
     *
     * @param nickname the nickname of the player who initiated the restart
     * @throws RemoteException if there are no more flips available, a restart is already in progress,
     *                         or an interruption occurs during the waiting process
     */
    public void restartHourglass(String nickname) throws RemoteException {

        synchronized (hourglassLock) {
            if (flipsLeft == 0)
                throw new RemoteException("No more flips available");
            if (isRestartInProgress)
                throw new RemoteException("Another player is already restarting the hourglass. Please wait.");

            isRestartInProgress = true;

            try {
                while (numClientsFinishedTimer < players.size()) {
                    try {
                        hourglassLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RemoteException("Interrupted while waiting for all clients to finish the timer.");
                    }
                }

                gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                        clientController.notifyHourglassRestarted(nicknameToNotify, nickname, flipsLeft);
                });

                flipsLeft--;
                numClientsFinishedTimer = 0;

            } finally {
                isRestartInProgress = false;
            }
        }

    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public ComponentTable getComponentTable() {
        return componentTable;
    }

    public void createGameContext(ConcurrentHashMap<String, CallableOnClientController> clientControllers) {
        this.gameClientNotifier = new GameClientNotifier(this,clientControllers);
        deck.setGameContext(gameClientNotifier);
        flyingBoard.setGameContext(gameClientNotifier);
        componentTable.setGameContext(gameClientNotifier);
    }

    public String getGameId() {
        return gameId;
    }

    public ConcurrentHashMap<String, Player> getPlayers() {
        return players;
    }

    public boolean isTestFlight() {
        return isTestFlight;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setCurrGameState(GameState currGameState) {
        synchronized (stateTransitionLock) {

            if (this.currGameState == currGameState) return;

            this.currGameState = currGameState;

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyGameState(nicknameToNotify, currGameState);
            });

            currGameState.run(this);
        }
    }

    public GameClientNotifier getGameContext() {
        return gameClientNotifier;
    }

    public GameState getCurrGameState() {
        return currGameState;
    }

    public Deck getDeck() {
        return deck;
    }

    public static int throwDices() {
        return (int) (Math.random() * 12) + 1;
    }

    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    public void setCurrDangerousObj(DangerousObj dangerousObj) {

        this.currDangerousObj = dangerousObj;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyDangerousObjAttack(nicknameToNotify, currDangerousObj);
        });

    }

    public Boolean hasNextPlayer() {
        return playerIterator.hasNext();
    }

    /**
     * Resets the iterator for the player ranking and sets the current player
     * to the first player in the ranking.
     * This method initializes the player iterator using the current player ranking
     * and subsequently sets the current player to the first player determined by the iterator.
     */
    public void resetPlayerIterator() {
        playerIterator = currRanking.iterator();
        currPlayer = playerIterator.next();
    }

    public void setCurrRanking(List<Player> currRanking) {
        this.currRanking = currRanking;
    }

    public void setCurrPlayer(Player player){

        this.currPlayer = player;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCurrPlayerChanged(nicknameToNotify, player.getNickname());
        });


    }

    public void nextPlayer() {
        setCurrPlayer(playerIterator.next());
    }

    public Player getCurrPlayer() {
        return currPlayer;
    }

    public FlyingBoard getFlyingBoard() {
        return flyingBoard;
    }

    public List<Player> getCurrRanking() {
        return currRanking;
    }

    public void setCurrAdventureCard(AdventureCard currAdventureCard) {
            this.currAdventureCard = currAdventureCard;

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyCurrAdventureCard(nicknameToNotify, currAdventureCard.toString());
            });

    }

    public AdventureCard getCurrAdventureCard() {
        return currAdventureCard;
    }

    /**
     * Starts the current card phase: updates the gameModel's currState and the card's currState to
     * the first of the actual card, sets the currPlayer to the first based on the provided
     * ranking.
     *
     * @throws IllegalStateException if the card phase is not started yet.
     */
    public void startCard() throws IllegalStateException {

        if (currAdventureCard == null || currAdventureCard.getCurrState() != CardState.START_CARD)
            throw new IllegalStateException("Not the right state");

        setCurrRanking(flyingBoard.getCurrentRanking());
        currAdventureCard.setGame(this);
        playerIterator = currRanking.iterator();
        setCurrPlayer(playerIterator.next());
        currAdventureCard.setCurrState(currAdventureCard.getFirstState());

    }

    public List<Player> getPlayerWithPrettiestShip() {

        Map<Player, Integer> x = new HashMap<>();

        players.values().forEach(player -> {
            x.put(player, player.getPersonalBoard().countExposed());
        });

        Integer minValue = Collections.min(x.values());

        return x.keySet().stream().filter(player -> x.get(player).equals(minValue)).toList();

    }

    public void calculatePlayersCredits() {

        players.values().forEach(player -> {

            int credits = player.getOwnedCredits();

            credits += flyingBoard.getCreditsForPosition(player);

            if (getPlayerWithPrettiestShip().contains(player))
                credits += flyingBoard.getPrettiestShipReward();

            credits += player.getPersonalBoard().getStorages()
                                .stream()
                                .flatMap(storage -> storage.getStockedCubes().stream())
                                .mapToInt(stockedCube -> {
                                    switch (stockedCube) {
                                        case BLUE -> {
                                            return 1;
                                        }
                                        case GREEN -> {
                                            return 2;
                                        }
                                        case YELLOW -> {
                                            return 3;
                                        }
                                        case RED -> {
                                            return 4;
                                        }
                                        default -> {
                                            return 0;
                                        }
                                    }
                                }).sum();

            credits -= player.getPersonalBoard().getNotActiveComponents().size();

            player.setOwnedCredits(credits);
        });

    }

    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController){
        gameClientNotifier.getClientControllers().put(nickname, clientController);
        ShipBoard shipBoard = isTestFlight ? new Level1ShipBoard(color, gameClientNotifier, false) : new Level2ShipBoard(color, gameClientNotifier, false);
        Player player = new Player(nickname, shipBoard, color);
        player.setGameContext(gameClientNotifier);
        players.put(nickname, player);
        shipBoard.setPlayer(player);
    }

    public void removePlayer(String nickname) {
        players.remove(nickname);
    }

    /**
     * Handles the event when the hourglass timer has ended for a game session.
     * It synchronizes actions across players to ensure appropriate game state transitions.
     * <p>
     * This method increments the count of clients that have finished their timers
     * and performs the following actions:
     * 1. If flips are still available or a restart is in progress, it notifies all threads waiting on the hourglass.
     * 2. If no flips are left and no restart is in progress:
     *      - Sets the restart process flag to true.
     *      - Waits until all players have finished their timers.
     *      - Depending on the game state:
     *          a. Transitions to the CHECK_SHIPBOARD state if all players are ranked.
     *          b. Notifies players about the first player to enter if not all players are ranked.
     * <p>
     * Exceptions are handled for interruptions during waiting, ensuring the thread's interrupted status
     * is maintained and errors are logged appropriately.
     * <p>
     * Thread-safety is ensured using synchronization on the hourglassLock object.
     */
    public void hourglassEnded() {

        synchronized (hourglassLock) {
            numClientsFinishedTimer++;

            if (flipsLeft > 0 || isRestartInProgress)
                hourglassLock.notifyAll();

            else {
                isRestartInProgress = true;

                while (numClientsFinishedTimer < players.size()) {
                    try {
                        hourglassLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println(e.getMessage());
                    }
                }

                if (flyingBoard.getCurrentRanking().size() == maxPlayers)
                    setCurrGameState(GameState.CHECK_SHIPBOARD);
                else
                    gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
                        clientController.notifyFirstToEnter(nicknameToNotify);
                    });
            }
        }
    }

    public Set<ShipBoard> getInvalidShipBoards(){
        return players.values().stream()
                .map(Player::getPersonalBoard)
                .filter(shipBoard -> !shipBoard.isShipCorrect())
                .collect(Collectors.toSet());
    }

    public Set<ShipBoard> getValidShipBoards(){
        return players.values().stream()
                .map(Player::getPersonalBoard)
                .filter(ShipBoard::isShipCorrect)
                .collect(Collectors.toSet());
    }

    public void notifyInvalidShipBoards() {
        Set<ShipBoard> invalidShipBoards = getInvalidShipBoards();
        Set<String> playersNicknameToBeNotified = players.values().stream()
                .filter(player -> invalidShipBoards.contains(player.getPersonalBoard()))
                .map(Player::getNickname)
                .collect(Collectors.toSet());

        gameClientNotifier.notifyClients(playersNicknameToBeNotified, (nicknameToNotify, clientController) -> {
                Player player = players.get(nicknameToNotify);
                ShipBoard shipBoard = player.getPersonalBoard();
                Component[][] shipMatrix = shipBoard.getShipMatrix();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();

                clientController.notifyInvalidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
        });
    }

    public void notifyValidShipBoards() {
        Set<ShipBoard> validShipBoards = getValidShipBoards();
        Set<String> playersNicknameToBeNotified = players.values().stream()
                .filter(player -> validShipBoards.contains(player.getPersonalBoard()))
                .map(Player::getNickname)
                .collect(Collectors.toSet());

        gameClientNotifier.notifyClients(playersNicknameToBeNotified, (nicknameToNotify, clientController) -> {
                Player player = players.get(nicknameToNotify);
                ShipBoard shipBoard = player.getPersonalBoard();
                Component[][] shipMatrix = shipBoard.getShipMatrix();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();

                clientController.notifyValidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
        });
    }

    public boolean areAllShipsCorrect() {
        return players.values().stream()
                .allMatch(player -> player.getPersonalBoard().isShipCorrect());
    }

    public void checkAndTransitionToNextPhase() {

        synchronized (stateTransitionLock) {
            if (areAllShipsCorrect()) {
                // Cambia allo stato successivo
                setCurrGameState(GameState.CREATE_DECK);
            }
        }
    }

    public void setGameContext(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    public void notifyStopHourglass() {
        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyStopHourglass(nicknameToNotify);
        });
    }

}
