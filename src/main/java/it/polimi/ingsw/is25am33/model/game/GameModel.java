package it.polimi.ingsw.is25am33.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.controller.CallableOnClientController;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.model.*;
import it.polimi.ingsw.is25am33.model.board.*;
import it.polimi.ingsw.is25am33.model.card.PlayerChoicesDataStructure;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.enumFiles.*;
import it.polimi.ingsw.is25am33.model.card.AdventureCard;
import it.polimi.ingsw.is25am33.model.card.Deck;
import it.polimi.ingsw.is25am33.model.dangerousObj.DangerousObj;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class GameModel {
    /**
     * The unique identifier for a game instance.
     *
     * This variable represents the unique string that identifies each
     * game session within the system. It is used to distinguish between
     * different game instances and is immutable once set. Typically,
     * this identifier is assigned at the time of game creation.
     */
    private final String gameId;
    /**
     * Indicates whether the game is running in a test flight mode.
     *
     * This flag is used to determine if the current instance of the game
     * is configured for internal testing or experimental features.
     * When set to true, certain game behaviors or configurations may
     * differ from those in the standard production environment.
     *
     * This is a read-only field and its value is determined at the
     * instantiation of the GameModel object.
     */
    private final boolean isTestFlight;

    private boolean isStarted;
    /**
     * Represents the maximum number of players allowed in the game.
     *
     * This value is determined when the game is initialized and remains immutable throughout
     * the lifecycle of the game. It ensures that the number of players participating does not
     * exceed the predefined limit, maintaining the integrity of the game setup.
     */
    private final int maxPlayers;

    /**
     * Represents the current adventure card in the game.
     *
     * This variable holds a reference to the current {@link AdventureCard}
     * being played or processed during the game. It is updated as the game
     * transitions between different cards. The {@link AdventureCard} contains
     * the relevant details and behavior for the specific card, such as its
     * name, level, current state, and related game logic.
     *
     * This field is part of the {@code GameModel} class and plays a crucial role
     * in managing the state and actions associated with an adventure card.
     */
    private AdventureCard currAdventureCard;
    /**
     * Represents the flying platform or mechanism used within the game.
     * The FlyingBoard is a central component that might impact players'
     * interactions, positioning, or gameplay mechanics tied to the flight dynamics.
     *
     * This field is immutable once initialized and provides shared access
     * through getter methods within the GameModel class.
     */
    private final FlyingBoard flyingBoard;
    /**
     * A thread-safe map that contains the players participating in the game.
     * The keys represent the nicknames of the players, while the values are
     * Player objects that encapsulate the details and state of each player.
     *
     * This mapping is used to manage and retrieve player information
     * efficiently throughout the game's lifecycle. It supports concurrent
     * operations to ensure consistency and integrity in a multiplayer
     * environment.
     */
    private final ConcurrentHashMap<String, Player> players;
    /**
     * Represents the current ranking of players in the game.
     *
     * This list maintains the order of players based on their current
     * position in the game ranking. It is primarily used to determine
     * player turns and game progression. The list may be updated dynamically
     * throughout the game to reflect changes in ranking.
     */
    private List<Player> currRanking;
    /**
     * Represents the current player in the game.
     * This variable holds a reference to the player who is currently taking their turn
     * or is otherwise the focus of the gameplay at a specific moment. It may be updated
     * during the game as turns progress or game events occur.
     */
    private Player currPlayer;
    /**
     * An iterator for traversing through the list of players in the game.
     * This iterator provides a mechanism for sequential access to the Player objects,
     * typically used for determining the current player or iterating through the
     * ranking order during game phases.
     *
     * The iterator is initialized and reset using the corresponding methods in the
     * GameModel class and is closely tied to the current ranking of players in the game.
     *
     * Thread-safety must be considered when accessing or resetting the player iterator
     * in a concurrent environment.
     */
    private Iterator<Player> playerIterator;
    /**
     * Represents the current active instance of a DangerousObj in the game.
     * A DangerousObj can be of various types including meteorites or shots, each with specific behavior
     * and properties. This variable is updated during gameplay to reflect the currently impacting or
     * relevant dangerous object in the game's state.
     */
    private DangerousObj currDangerousObj;
    /**
     * Represents the current state of the game.
     * This variable defines the phase or status the game is in,
     * and is used to facilitate transitions and operations specific
     * to that game state.
     *
     * The possible values for this field are defined in the {@link GameState} enumeration,
     * which outlines various stages such as SETUP, BUILD_SHIPBOARD, CHECK_SHIPBOARD,
     * PLACE_CREW, CREATE_DECK, DRAW_CARD, PLAY_CARD, CHECK_PLAYERS, and END_GAME.
     *
     * The game state is essential for coordinating game logic,
     * ensuring proper execution, and synchronizing actions among players.
     */
    private GameState currGameState;
    /**
     * Represents the deck of cards used within the game model.
     * The deck is immutable and managed internally within the GameModel class.
     * It is primarily utilized to manage and provide access to the cards relevant to the game's logic.
     */
    private final Deck deck;
    /**
     * Represents a mapping of game components to their respective unique identifiers
     * or configurations within the game model. This structure is integral for managing
     * and organizing various components used during the gameplay, enabling efficient
     * access and modification of game elements.
     *
     * This field is immutable and initialized upon the creation of the GameModel object,
     * ensuring that the same ComponentTable instance is consistently referenced throughout
     * the lifecycle of the game. Its operations are thread-safe and tailored to the multithreaded
     * nature of the game environment.
     */
    private final ComponentTable componentTable;
    /**
     * Represents the notifier responsible for communicating game updates
     * and events to the connected client controllers in the GameModel.
     *
     * This variable serves as an intermediary mechanism to notify all
     * client controllers participating in the game about significant
     * changes in the game state, such as player actions, game phase transitions,
     * and other gameplay-related events.
     *
     * The notifier is initialized and managed through the {@code GameModel}
     * lifecycle and is critical in ensuring real-time synchronization
     * between server-side state changes and client-side visual representation.
     *
     * Thread-safety and concurrency control are essential aspects of this
     * notifier to prevent race conditions and ensure consistent game updates.
     */
    private GameClientNotifier gameClientNotifier;

    /**
     * GLOBAL LOCK ORDERING TO PREVENT DEADLOCKS:
     * 1. stateTransitionLock (ALWAYS ACQUIRED FIRST)
     * 2. hourglassLock (ACQUIRED SECOND)
     * 3. crewPlacementCompletedLock (ACQUIRED THIRD)
     * 
     * This ordering must be respected in ALL methods to prevent circular lock dependencies.
     */
    
    /**
     * A final lock object used to ensure thread safety during operations
     * related to the hourglass mechanism within the game.
     *
     * This lock is primarily used for managing synchronization in scenarios
     * where multiple threads interact with the hourglass feature, such as
     * restarting the hourglass, waiting for timers to finish, or transitioning
     * game states based on hourglass-related events.
     *
     * IMPORTANT: This lock must ALWAYS be acquired AFTER stateTransitionLock
     * to maintain global lock ordering and prevent deadlocks.
     */
    // attributes useful for hourglass restarting
    private final Object hourglassLock = new Object();
    /**
     * Represents the number of remaining attempts or flips available in the game.
     * This variable is used to track how many times certain actions, like restarting
     * the hourglass, can still be performed before they are exhausted.
     */
    private Integer flipsLeft;
    /**
     * Tracks the number of clients that have finished their timers in the current game session.
     *
     * This variable is primarily used to synchronize game state transitions and manage
     * the hourglass timer mechanism. It is incremented every time a client completes their timer,
     * playing a key role in determining when to proceed to the next phase of the game or to
     * handle specific game events such as restarts or transitions based on the related conditions.
     *
     * Thread-safety for this variable is ensured through the use of synchronization mechanisms
     * like locks associated with relevant game operations (e.g., hourglassLock).
     */
    private Integer numClientsFinishedTimer = 0;
    /**
     * A flag indicating whether a restart process is currently in progress within the game.
     * This variable is used to prevent multiple simultaneous restarts and ensure proper
     * synchronization during state transitions, such as hourglass restarts or phase transitions.
     * It is managed and checked in methods that handle game state changes, especially
     * those involving synchronized actions across players.
     *
     * Initially set to {@code false}, it is updated to {@code true} when a restart process
     * begins and reset to {@code false} upon completion of the restart.
     */
    private Boolean isRestartInProgress = false;

    /**
     * A concurrent map that tracks the completion status of the crew placement phase for each player.
     *
     * Keys represent player nicknames, and values are booleans indicating whether the player
     * has completed the crew placement phase (true if completed, false if not).
     *
     * This map ensures thread-safe operations to handle updates and checks across multiple players
     * during the crew placement phase in the game.
     */
    private final Map<String, Boolean> crewPlacementCompleted = new ConcurrentHashMap<>();

    /**
     * A lock object used to synchronize state transitions in a multi-threaded environment.
     * Ensures that modifications to the state are performed atomically and without interference
     * from other threads, preserving thread safety during state changes.
     */
    // Lock per la transizione di stato
    private final Object stateTransitionLock = new Object();

    /**
     * Constructs a new GameModel instance with the specified parameters and initializes game state.
     *
     * @param gameId        the unique identifier for the game.
     * @param maxPlayers    the maximum number of players allowed in the game.
     * @param isTestFlight  a flag indicating whether the game is a test flight or a standard game.
     */
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
        componentTable = new ComponentTable();
        flipsLeft = isTestFlight ? 1 : 2;
        isStarted=false;
    }

    /**
     * Retrieves the current instance of the {@code ComponentTable} associated with the game model.
     *
     * @return the {@code ComponentTable} object used within the game model
     */
    public ComponentTable getComponentTable() {
        return componentTable;
    }

    /**
     * Checks if the component or process has been started.
     *
     * @return true if the component or process is started, false otherwise.
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Sets the started state of the object.
     *
     * @param started boolean value indicating whether the object is started or not
     */
    public void setStarted(boolean started) {
        this.isStarted=started;
    }

    /**
     * Creates and initializes the GameClientNotifier with the given client controllers.
     * The notifier is associated with multiple components such as the deck, flying board,
     * and component table to handle communication with the client controllers.
     *
     * @param clientControllers a ConcurrentHashMap containing a mapping of client identifiers
     *                          to their respective CallableOnClientController instances
     */
    public void createGameClientNotifier(ConcurrentHashMap<String, CallableOnClientController> clientControllers) {
        this.gameClientNotifier = new GameClientNotifier(this, clientControllers);
        deck.setGameClientNotifier(gameClientNotifier);
        flyingBoard.setGameClientNotifier(gameClientNotifier);
        componentTable.setGameClientNotifier(gameClientNotifier);
    }

    /**
     * Retrieves the unique identifier for the game.
     *
     * @return the unique game ID as a String
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Retrieves the current mapping of player identifiers to their associated Player objects.
     *
     * @return a ConcurrentHashMap containing player identifiers as keys and their corresponding Player objects as values
     */
    public ConcurrentHashMap<String, Player> getPlayers() {
        return players;
    }

    /**
     * Determines if the application is running in a TestFlight environment.
     *
     * @return true if the application is running in TestFlight, false otherwise.
     */
    public boolean isTestFlight() {
        return isTestFlight;
    }

    /**
     * Retrieves the maximum number of players allowed.
     *
     * @return the maximum number of players as an integer.
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Updates the current game state to the specified value and notifies all clients about the change.
     * This method ensures thread-safe state transition handling.
     *
     * @param currGameState the new game state to be set
     */
    public void setCurrGameState(GameState currGameState) {
        synchronized (stateTransitionLock) {
            if (this.currGameState == currGameState) return;

            this.currGameState = currGameState;

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyGameState(nicknameToNotify, currGameState);
            });
        }
        currGameState.run(this);
    }

    /**
     * Retrieves the GameClientNotifier instance associated with the current context.
     *
     * @return the GameClientNotifier instance used for client notifications.
     */
    public GameClientNotifier getGameClientNotifier() {
        return gameClientNotifier;
    }

    /**
     * Retrieves the current state of the game.
     *
     * @return the current GameState object representing the game's current state.
     */
    public GameState getCurrGameState() {
        return currGameState;
    }

    /**
     * Retrieves the deck.
     *
     * @return the deck associated with the current object
     */
    public Deck getDeck() {
        return deck;
    }

    /**
     * Simulates the throw of two six-sided dice and calculates the sum of their faces.
     *
     * @return the sum of the two dice faces, which is a random number between 2 and 12 inclusive.
     */
    public static int throwDices() {
        return (int) (Math.random() * 11) + 1;
    }

    /**
     * Retrieves the current dangerous object.
     *
     * @return the current instance of DangerousObj
     */
    public DangerousObj getCurrDangerousObj() {
        return currDangerousObj;
    }

    /**
     * Sets the current dangerous object and notifies all clients about the new dangerous object.
     *
     * @param dangerousObj the DangerousObj instance to be set as the current dangerous object
     */
    public void setCurrDangerousObj(DangerousObj dangerousObj) {

        this.currDangerousObj = dangerousObj;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyDangerousObjAttack(nicknameToNotify, new ClientDangerousObject(currDangerousObj.getDangerousObjType(), currDangerousObj.getDirection(), currDangerousObj.getCoordinate()));
        });

    }

    /**
     * Determines if there is another player in the iterator sequence.
     *
     * @return true if there is a next player in the sequence, false otherwise.
     */
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
        setCurrPlayer(playerIterator.next());
    }

    /**
     * Sets the current ranking of players in the game.
     *
     * @param currRanking a list of {@code Player} objects representing the current ranking of players.
     */
    public void setCurrRanking(List<Player> currRanking) {
        this.currRanking = currRanking;
    }

    /**
     * Sets the current player for the game and notifies all connected clients about the change.
     *
     * @param player the Player object to be set as the current player
     */
    public void setCurrPlayer(Player player){

        this.currPlayer = player;

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyCurrPlayerChanged(nicknameToNotify, player.getNickname());
        });

    }

    /**
     * Advances to the next player in the game by setting the current player to the next player
     * in the player iterator. If the iterator has no next element, its behavior depends on
     * the iterator's implementation.
     *
     * This method modifies the current player by calling {@link #setCurrPlayer(Player)} with
     * the next player returned by the player iterator. The method is essential for managing
     * the sequential turn-taking mechanism in the game.
     */
    public void nextPlayer() {
        setCurrPlayer(playerIterator.next());
    }

    /**
     * Retrieves the current player in the game.
     *
     * @return the player who is currently active
     */
    public Player getCurrPlayer() {
        return currPlayer;
    }

    /**
     * Retrieves the FlyingBoard instance associated with this object.
     *
     * @return the FlyingBoard instance
     */
    public FlyingBoard getFlyingBoard() {
        return flyingBoard;
    }

    /**
     * Retrieves the current ranking of players in the game.
     *
     * @return a list of {@code Player} objects representing the current player ranking
     */
    public List<Player> getCurrRanking() {
        return currRanking;
    }

    /**
     * Sets the current AdventureCard for the game, updates its client representation,
     * and notifies all connected clients about the updated AdventureCard.
     *
     * @param currAdventureCard the AdventureCard instance to set as the current card in the game model
     */
    public void setCurrAdventureCard(AdventureCard currAdventureCard) {
        this.currAdventureCard = currAdventureCard;

        ClientCard clientCard = currAdventureCard.toClientCard();

        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyCurrAdventureCard(nicknameToNotify, clientCard, true);
        });

    }

    /**
     * Retrieves the current adventure card used in the game model.
     *
     * @return the current instance of AdventureCard
     */
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
        if (!playerIterator.hasNext()) {    // TODO sistemare per andare alla fase finale
            System.err.println("NON CI SONO PIù GIOCATORI VIVI");
            return;
        }
        setCurrPlayer(playerIterator.next());
        currAdventureCard.setCurrState(currAdventureCard.getFirstState());

        getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyCardStarted(nicknameToNotify);
        });
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

    /**
     * Retrieves a list of players who own the ship with the fewest exposed components,
     * based on the current state of their personal boards. Players who have landed early
     * are excluded from consideration.
     *
     * @return a list of players who possess the "prettiest" ships, defined as those
     *         with the least number of exposed components.
     */
    public List<Player> getPlayerWithPrettiestShip() {

        Map<Player, Integer> x = new HashMap<>();

        players.values().stream().filter(player -> !player.isEarlyLanded()).forEach(player -> {
            x.put(player, player.getPersonalBoard().countExposed());
        });

        if(x.isEmpty()) // tutti i giocatori hanno fatto early landing
            return Collections.emptyList();

        Integer minValue = Collections.min(x.values());

        return x.keySet()
                .stream()
                .filter(player -> x.get(player).equals(minValue))
                .toList();

    }

    /**
     * Calculates and updates the total credits for each player based on various criteria.
     * This method processes each player from the collection `players`,
     * computes their credits considering multiple factors, and sets their updated credit value.
     *
     * The calculation includes:
     * - Adding the player's previously owned credits.
     * - Adding credits based on the player's position on the flying board.
     * - Adding additional credits if the player has the prettiest ship.
     * - Adding credits based on cubes stored in the player's personal board storage,
     *   where each cube type (e.g., BLUE, GREEN, YELLOW, RED) has a specific credit value.
     *   If the player is marked as "early landed", the credits from stocked cubes are halved.
     * - Deducting credits for each not active component in the player's personal board.
     *
     * The result is directly set as the player's updated credit value.
     */
    public void calculatePlayersCredits() {
        System.out.println("=== INIZIO CALCOLO CREDITI GIOCATORI ===");

        players.values().forEach(player -> {
            System.out.println("\n--- Calcolo crediti per: " + player.getNickname() + " ---");

            int credits = player.getOwnedCredits();
            System.out.println("Crediti iniziali: " + credits);

            // gestisce già il fatto che un player potrebbe essere earlyLanded
            int positionCredits = flyingBoard.getCreditsForPosition(player);
            credits += positionCredits;
            System.out.println("Crediti da posizione: +" + positionCredits + " (totale: " + credits + ")");

            // gestisce già il fatto che un player potrebbe essere earlyLanded
            List<Player> prettiestShipPlayers = getPlayerWithPrettiestShip();
            boolean hasPrettiestShip = prettiestShipPlayers.contains(player);
            if (hasPrettiestShip) {
                int prettiestShipReward = flyingBoard.getPrettiestShipReward();
                credits += prettiestShipReward;
                System.out.println("Crediti nave più bella: +" + prettiestShipReward + " (totale: " + credits + ")");
            } else {
                System.out.println("Crediti nave più bella: +0 (non ha la nave più bella)");
            }

            int creditsForStockedCubes = player.getPersonalBoard().getStorages()
                    .stream()
                    .flatMap(storage -> storage.getStockedCubes().stream())
                    .mapToInt(stockedCube -> {
                        int cubeValue = switch (stockedCube) {
                            case BLUE -> 1;
                            case GREEN -> 2;
                            case YELLOW -> 3;
                            case RED -> 4;
                            default -> 0;
                        };
                        System.out.println("  Cubo " + stockedCube + ": " + cubeValue + " crediti");
                        return cubeValue;
                    }).sum();

            System.out.println("Crediti totali da cubi stockati: " + creditsForStockedCubes);

            boolean isEarlyLanded = player.isEarlyLanded();
            int finalCubeCredits = isEarlyLanded ? (int) Math.ceil(creditsForStockedCubes/2.0) : creditsForStockedCubes;
            credits += finalCubeCredits;

            if (isEarlyLanded) {
                System.out.println("Player è early landed - crediti cubi dimezzati: " + finalCubeCredits + " (totale: " + credits + ")");
            } else {
                System.out.println("Crediti da cubi: +" + finalCubeCredits + " (totale: " + credits + ")");
            }

            int notActiveComponentsPenalty = player.getPersonalBoard().getNotActiveComponents().size();
            credits -= notActiveComponentsPenalty;
            System.out.println("Penalità componenti non attivi: -" + notActiveComponentsPenalty + " (totale: " + credits + ")");

            player.setOwnedCredits(credits);
            System.out.println("CREDITI FINALI per " + player.getNickname() + ": " + credits);
        });

        System.out.println("\n=== FINE CALCOLO CREDITI GIOCATORI ===");
    }

    /**
     * Adds a player to the game with the specified nickname, color, and client controller.
     *
     * @param nickname          the unique nickname of the player to be added
     * @param color             the color representing the player
     * @param clientController  the client controller associated with the player,
     *                          used for communication with the client
     */
    public void addPlayer(String nickname, PlayerColor color, CallableOnClientController clientController){
        if(clientController!=null)
            gameClientNotifier.getClientControllers().put(nickname, clientController);
        ShipBoard shipBoard = isTestFlight ? new Level1ShipBoard(color, gameClientNotifier, false) : new Level2ShipBoard(color, gameClientNotifier, false);
        Player player = new Player(nickname, shipBoard, color);
        player.setGameClientNotifier(gameClientNotifier);
        players.put(nickname, player);
        shipBoard.setPlayer(player);
    }


    /**
     * Removes a player from the players collection based on the given nickname.
     *
     * @param nickname the nickname of the player to be removed
     */
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
     * Thread-safety is ensured using proper lock ordering (stateTransitionLock then hourglassLock).
     */
    public void hourglassEnded() {
        boolean shouldTransitionState = false;
        Set<String> notRankedPlayers = null;

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

                if (flyingBoard.getCurrentRanking().size() == maxPlayers) {
                    shouldTransitionState = true;
                } else {
                    notRankedPlayers = players.keySet()
                            .stream()
                            .filter(nickname -> !flyingBoard.getRanking().containsKey(players.get(nickname)))
                            .collect(Collectors.toSet());
                }
            }
        }

        // Perform state transition outside of hourglassLock to respect lock ordering
        if (shouldTransitionState) {
            setCurrGameState(GameState.CHECK_SHIPBOARD);
        } else if (notRankedPlayers != null) {
            gameClientNotifier.notifyClients(notRankedPlayers, (nicknameToNotify, clientController) ->
                    clientController.notifyFirstToEnter(nicknameToNotify));
        }
    }

    /**
     * Retrieves a set of invalid ship boards from the players' personal boards.
     * A ship board is considered invalid if the method isShipCorrect() for that board returns false.
     *
     * @return a set of ShipBoard objects representing invalid ship boards
     */
    @JsonIgnore
    private Set<ShipBoard> getInvalidShipBoards(){
        return players.values().stream()
                .map(Player::getPersonalBoard)
                .filter(shipBoard -> !shipBoard.isShipCorrect())
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves a set of valid ship boards from the players' personal boards.
     * A ship board is considered valid if it satisfies the condition determined
     * by the isShipCorrect method.
     *
     * @return a set of valid ShipBoard objects from all players.
     */
    @JsonIgnore
    private Set<ShipBoard> getValidShipBoards(){
        return players.values().stream()
                .map(Player::getPersonalBoard)
                .filter(ShipBoard::isShipCorrect)
                .collect(Collectors.toSet());
    }

    /**
     * Notifies players with invalid ship boards about the issues in their respective ship configurations.
     *
     * This method identifies all players with invalid ship boards and sends them a notification
     * containing detailed information about the issues in their configurations. The notification
     * includes the ship matrix, the coordinates of incorrectly positioned components, and a map
     * of components categorized by their type.
     *
     * The process involves the following steps:
     * 1. Retrieve the set of invalid ship boards.
     * 2. Identify the players whose ship boards are invalid.
     * 3. Notify the respective clients associated with these players, providing detailed information
     *    about their invalid ship boards.
     *
     * The provided notifier is utilized to send notifications to the respective client controllers.
     */
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
            Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
            Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
            List<Component> notActiveComponentsList = shipBoard.getNotActiveComponents();

            clientController.notifyInvalidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);
        });
    }

    /**
     * Notifies players with valid ship boards about the state of their boards.
     *
     * This method retrieves all valid ship boards and identifies the players whose
     * personal ship boards are valid. It then notifies these players using the
     * game client notifier with detailed information about their ship boards.
     *
     * The notifications include:
     * - The ship matrix of the player's ship board.
     * - Coordinates of incorrectly positioned components on the ship board.
     * - A mapping of components grouped by their types.
     *
     * The notification process involves:
     * 1. Determining the set of valid ship boards.
     * 2. Filtering players whose ship boards match the valid boards.
     * 3. Sending notifications to these players through the client controller.
     */
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
            Map<Class<?>, List<Component>> componentsPerType = shipBoard.getComponentsPerType();
            Set<Coordinates> incorrectlyPositionedComponentsCoordinates = shipBoard.getIncorrectlyPositionedComponentsCoordinates();
            List<Component> notActiveComponentsList = shipBoard.getNotActiveComponents();

            clientController.notifyValidShipBoard(nicknameToNotify, nicknameToNotify, shipMatrix, incorrectlyPositionedComponentsCoordinates, componentsPerType, notActiveComponentsList);

        });
    }

    /**
     * Checks if all ships on the game board for all players are correctly positioned
     * according to the game rules.
     *
     * @return true if all players have their ships correctly positioned, false otherwise
     */
    public boolean areAllShipsCorrect() {
        return players.values().stream()
                .allMatch(player -> player.getPersonalBoard().isShipCorrect());
    }

    /**
     * Checks the current state and transitions to the next game phase if conditions are met.
     *
     * This method ensures thread-safe state transitions by synchronizing on the
     * {@code stateTransitionLock} object. If all ships are in the correct state,
     * it transitions the game to the next phase based on the current game state:
     *
     * - If the current game state is {@code GameState.CHECK_SHIPBOARD}, it transitions to {@code GameState.PLACE_CREW}.
     * - If the current game state is {@code GameState.PLAY_CARD}, the {@code currAdventureCard}'s play method is invoked
     *   with a new instance of {@code PlayerChoicesDataStructure}.
     */
    public void checkAndTransitionToNextPhase() {

        synchronized (stateTransitionLock) {
            if (areAllShipsCorrect()) {
                // Cambia allo stato successivo
                if(currGameState==GameState.CHECK_SHIPBOARD)
                    setCurrGameState(GameState.PLACE_CREW);
                else if(currGameState==GameState.PLAY_CARD )
                    currAdventureCard.play(new PlayerChoicesDataStructure());
            }
        }
    }

    /**
     * Sets the notifier for the game client.
     *
     * @param gameClientNotifier the GameClientNotifier instance to be set, responsible for handling
     *        client notifications and updates.
     */
    public void setGameClientNotifier(GameClientNotifier gameClientNotifier) {
        this.gameClientNotifier = gameClientNotifier;
    }

    /**
     * Updates the shipboard of the current player after it has been hit.
     *
     * This method handles the aftermath of an attack on the player's shipboard.
     * It processes the impact caused by a dangerous object and updates the shipboard accordingly.
     *
     * The method performs the following steps:
     * 1. Determines whether an object hit the shipboard by invoking the `handleDangerousObject` method.
     * 2. If there was no hit, the current adventure card's state is set to `CHECK_SHIPBOARD_AFTER_ATTACK` and the method terminates.
     * 3. If there was a hit, it adds the hit coordinates to the list of incorrectly positioned components on the shipboard.
     * 4. Notifies the affected player about the hit coordinates via the game client notifier.
     * 5. Updates the current adventure card's state to `CHECK_SHIPBOARD_AFTER_ATTACK`.
     */
    public void updateShipBoardAfterBeenHit() {
        ShipBoard shipBoard = currPlayer.getPersonalBoard();
        int[] hitCoordinates = shipBoard.handleDangerousObject(currDangerousObj);
        if(hitCoordinates == null) {
            currAdventureCard.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
            return;
        }
        shipBoard.getIncorrectlyPositionedComponentsCoordinates().add(new Coordinates(hitCoordinates[0], hitCoordinates[1]));
        gameClientNotifier.notifyClients(Set.of(currPlayer.getNickname()), (nicknameToNotify, clientController) -> {
            clientController.notifyCoordinateOfComponentHit(nicknameToNotify,currPlayer.getNickname(),new Coordinates(hitCoordinates[0], hitCoordinates[1]));
        });
        currAdventureCard.setCurrState(CardState.CHECK_SHIPBOARD_AFTER_ATTACK);
    }

    /**
     * Notifies all connected clients to stop the hourglass timer.
     * This method sends a stop-hourglass notification to each client controller
     * using the provided nickname for identification.
     * The notification is dispatched through the game client notifier.
     */
    public void notifyStopHourglass() {
        gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyStopHourglass(nicknameToNotify);
        });
    }


    /**
     * Handles the crew placement phase of the game.
     * Depending on whether it is a test flight or not, it either places the crew automatically
     * or notifies players to perform the crew placement manually.
     *
     * The method initializes the {@code crewPlacementCompleted} map by setting all players'
     * placement statuses to false. If the game is in test flight mode, crew placement is
     * performed automatically and the game state transitions to {@code CREATE_DECK}.
     * Otherwise, players are notified to manually complete their respective crew placements.
     *
     * This method interacts with the game client notifier to ensure all players
     * receive a notification to start the crew placement phase in non-test scenarios.
     */
    public void handleCrewPlacementPhase() {
        players.keySet().forEach(nickname -> crewPlacementCompleted.put(nickname, false));

        if (isTestFlight) {
            placeCrewAutomatically();
            setCurrGameState(GameState.CREATE_DECK);
        } else {
            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCrewPlacementPhase(nicknameToNotify);
            });
        }
    }


    /**
     * Automatically places crew members in unoccupied cabins across all players' ship boards.
     *
     * This method iterates over all players and checks each cabin on their personal ship board.
     * For any cabin without inhabitants, it assigns a human crew member to that cabin.
     */
    private void placeCrewAutomatically() {
        for (Player player : players.values()) {
            ShipBoard shipBoard = player.getPersonalBoard();

            for (Cabin cabin : shipBoard.getCabin()) {
                if (!cabin.hasInhabitants()) {
                    cabin.fillCabin(CrewMember.HUMAN);
                }
            }

            if(shipBoard.getMainCabin()!=null)
                shipBoard.getMainCabin().fillCabin(CrewMember.HUMAN);

            gameClientNotifier.notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify, player.getNickname(), shipBoard.getShipMatrix(), shipBoard.getComponentsPerType(), shipBoard.getNotActiveComponents());
            });
        }
    }

    /**
     * A lock object used to synchronize the crew placement completion process.
     * This ensures thread-safe operations and prevents concurrent modifications
     * or access during the crew placement completion phase.
     */
    private final Object crewPlacementCompletedLock = new Object();

    /**
     * Marks the crew placement for a specific user as completed and evaluates if all crew placements are completed.
     * If all users have completed their crew placements, the game state is updated to the next phase.
     *
     * @param nickname the nickname of the user who has completed their crew placement
     */
    public void markCrewPlacementCompleted(String nickname) {
        crewPlacementCompleted.put(nickname, true);

        synchronized (crewPlacementCompletedLock) {
            boolean allCompleted = crewPlacementCompleted.values().stream().allMatch(Boolean::booleanValue);
            if (allCompleted) {
                setCurrGameState(GameState.CREATE_DECK);
            }
        }
    }

    /**
     * Generates and retrieves a list of player final data objects, each representing
     * the final ranking information for a player including their credits, early landing status,
     * owned cargo cubes, and lost components.
     *
     * @return a list of PlayerFinalData objects, each containing the calculated final ranking
     *         data for the players.
     */
    public List<PlayerFinalData> getRankingWithPlayerFinalData() {


        List<PlayerFinalData> finalRankingWithPlayerFinalData = players.values().stream().map(player -> {
            int credits = player.getOwnedCredits();
            boolean isEarlyLanded = player.isEarlyLanded();
            List<CargoCube> allOwnedCubes = player.getPersonalBoard().getStorages().stream().flatMap(storage -> storage.getStockedCubes().stream()).collect(Collectors.toList());
            int lostComponents = player.getPersonalBoard().getNotActiveComponents().size();
            return new PlayerFinalData(player.getNickname(), credits, isEarlyLanded, allOwnedCubes, lostComponents);
        }).collect(Collectors.toList());

        return finalRankingWithPlayerFinalData;
    }
}
