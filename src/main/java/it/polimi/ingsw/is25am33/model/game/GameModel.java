package it.polimi.ingsw.is25am33.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private GameContext gameContext;

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

                gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
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
        this.gameContext = new GameContext(this,clientControllers);
        deck.setGameContext(gameContext);
        flyingBoard.setGameContext(gameContext);
        componentTable.setGameContext(gameContext);
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
            this.currGameState = currGameState;

            gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
                    clientController.notifyGameState(nicknameToNotify, currGameState);
            });

            currGameState.run(this);
        }
    }

    public GameContext getGameContext() {
        return gameContext;
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

        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
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

        gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
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

            gameContext.notifyAllClients((nicknameToNotify, clientController) -> {
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
        currPlayer = playerIterator.next();
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
        gameContext.getClientControllers().put(nickname, clientController);
        ShipBoard shipBoard = isTestFlight ? new Level1ShipBoard(color,gameContext, false) : new Level2ShipBoard(color, gameContext, false);
        Player player = new Player(nickname, shipBoard, color);
        player.setGameContext(gameContext);
        players.put(nickname, player);
        shipBoard.setPlayer(player);
    }

    public void removePlayer(String nickname) {
        players.remove(nickname);
    }

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

                setCurrGameState(GameState.CHECK_SHIPBOARD);

            }

        }
    }

    @JsonIgnore
    public Set<ShipBoard> getInvalidShipBoards(){
        Set<ShipBoard> invalidShipBoards = players.values().stream()
                .map(player -> player.getPersonalBoard())
                .filter(shipBoard -> shipBoard.isShipCorrect() == false)
                .collect(Collectors.toSet());
        return invalidShipBoards;
    }

    @JsonIgnore
    public Set<ShipBoard> getValidShipBoards(){
        Set<ShipBoard> invalidShipBoards = players.values().stream()
                .map(player -> player.getPersonalBoard())
                .filter(shipBoard -> shipBoard.isShipCorrect() == true)
                .collect(Collectors.toSet());
        return invalidShipBoards;
    }

    public void notifyInvalidShipBoards() {
        Set<ShipBoard> invalidShipBoards = getInvalidShipBoards();
        Set<String> playersNicknameToBeNotified = players.values().stream()
                .filter(player -> invalidShipBoards.contains(player.getPersonalBoard()))
                .map(player->player.getNickname())
                .collect(Collectors.toSet());

        gameContext.notifyClients(playersNicknameToBeNotified, (nicknameToNotify, clientController) -> {
            try {
                Player player = players.get(nicknameToNotify);
                ShipBoard shipBoard = player.getPersonalBoard();
                Component[][] shipMatrix = shipBoard.getShipMatrix();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();

                clientController.notifyInvalidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
            } catch (RemoteException e) {
                System.err.println("Remote Exception");
            }
        });
    }

    public void notifyValidShipBoards() {
        Set<ShipBoard> validShipBoards = getValidShipBoards();
        Set<String> playersNicknameToBeNotified = players.values().stream()
                .filter(player -> validShipBoards.contains(player.getPersonalBoard()))
                .map(player->player.getNickname())
                .collect(Collectors.toSet());

        gameContext.notifyClients(playersNicknameToBeNotified, (nicknameToNotify, clientController) -> {
            try {
                Player player = players.get(nicknameToNotify);
                ShipBoard shipBoard = player.getPersonalBoard();
                Component[][] shipMatrix = shipBoard.getShipMatrix();
                Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();

                clientController.notifyValidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates);
            } catch (RemoteException e) {
                System.err.println("Remote Exception");
            }
        });
    }

    public boolean areAllShipsCorrect() {
        return players.values().stream()
                .allMatch(player -> player.getPersonalBoard().isShipCorrect());
    }

    public void checkAndTransitionToNextPhase() {

        synchronized (stateTransitionLock){
            if (areAllShipsCorrect()) {
                // Cambia allo stato successivo
                setCurrGameState(GameState.CREATE_DECK);
            }
        }
    }
    public void setGameContext(GameContext gameContext) {
        this.gameContext = gameContext;
    }
}
