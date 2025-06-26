package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.client.view.ClientView;
import it.polimi.ingsw.is25am33.model.card.MeteoriteStorm;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.model.game.PlayerFinalData;

import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.List;
import java.util.stream.Collectors;

public enum GameState implements Serializable {

    /**
     * The SETUP state in the game lifecycle, representing the initialization
     * phase where the game's setup logic is executed.
     * It involves preparing the game's model for subsequent gameplay stages.
     */
    SETUP {
        @Override
        public void run(GameModel gameModel) {}
    },

    /**
     * Represents the state in which the shipboard is constructed within the game model.
     * This state is responsible for initializing and setting up the shipboard decks,
     * typically by invoking the appropriate method on the game model's deck.
     *
     * This is part of the overall game state management and transitions within the game flow.
     */
    BUILD_SHIPBOARD {
        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().setUpLittleDecks(gameModel);
        }
    },

    /**
     * CHECK_SHIPBOARD represents a game state within the {@code GameState} enumeration
     * that performs operations related to validating and checking players' ship boards.
     *
     * This state transitions the game model by executing the following actions:
     * - Stops the hourglass timer if the game is not running in test flight mode.
     * - Iterates through all players in the game to validate their personal ship boards.
     * - Notifies the game model of invalid and valid ship boards.
     * - Transitions the game to the next appropriate phase based on the current state.
     *
     * This game state is invoked through the overridden {@code run} method,
     * which takes the {@code GameModel} as its parameter for execution. The operation
     * ensures game-wide compliance of ship board states before proceeding to subsequent phases.
     */
    CHECK_SHIPBOARD {

        @Override
        public void run(GameModel gameModel) {
            if (!gameModel.isTestFlight())
                gameModel.notifyStopHourglass();

            gameModel.getPlayers().values().forEach(player -> {
                player.getPersonalBoard().checkShipBoard();
            });

            gameModel.notifyInvalidShipBoards();
            gameModel.notifyValidShipBoards();

            //check if all ship are valid and change
            gameModel.checkAndTransitionToNextPhase();
        }

    },

    /**
     * Represents the game state in which players place their crew members
     * onto the game board during the crew placement phase.
     * This state is responsible for handling all mechanics related to
     * the placement of crew members by invoking the appropriate method
     * in the game model.
     */
    PLACE_CREW {
        @Override
        public void run(GameModel gameModel) {
            gameModel.handleCrewPlacementPhase();
        }
    },

    /**
     * Represents the state within the game where a deck of cards is created.
     * This state initializes the game's deck based on the game's parameters,
     * such as whether the game is operating in a test environment or not.
     * Once the deck is created, the game transitions to the {@code DRAW_CARD} state.
     *
     * This state is part of the game's state management system and is invoked
     * when the game needs to prepare a deck for gameplay.
     *
     * Responsibilities:
     * - Creates the game's deck based on the current game configuration.
     * - Updates the current game state to {@code DRAW_CARD}.
     */
    CREATE_DECK {

        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().createGameDeck(gameModel.isTestFlight());
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    },

    /**
     * Represents the game state where the next adventure card is drawn from the deck.
     * Transitions the game to either the PLAY_CARD state if a card is successfully drawn,
     * or to the END_GAME state if the deck no longer contains valid cards.
     *
     * The behavior of this state includes:
     * - Checking if the deck has remaining cards to draw.
     * - Determining the number of players still in the game for drawing the appropriate card type.
     * - Updating the current state of the game based on the outcome of the card draw.
     *
     * Handles the EmptyStackException if it occurs during the card drawing process.
     */
    DRAW_CARD {

        @Override
        public void run(GameModel gameModel) {
            try {
                if(gameModel.getDeck().hasFinishedCards()){
                    gameModel.setCurrGameState(GameState.END_GAME);
                    return;
                }

                int inGamePlayers = gameModel.getPlayers().size() - gameModel.getFlyingBoard().getOutPlayers().size();
                gameModel.setCurrAdventureCard(gameModel.getDeck().drawCard(inGamePlayers));
                gameModel.setCurrGameState(GameState.PLAY_CARD);
            } catch (EmptyStackException e) {
                e.getMessage();
            }
        }
    },

    /**
     * Represents the game state where a card is played during the game process.
     * This state triggers the execution of the logic required to handle the
     * "play card" phase within the game's progression.
     *
     * The logic is executed by invoking the {@code startCard} method on the
     * provided {@code GameModel} instance, which encapsulates the state
     * and behavior of the game.
     */
    PLAY_CARD {

        @Override
        public void run(GameModel gameModel) {
            gameModel.startCard();
        }

    },

    /**
     * Represents a game state where the system checks the status of players during
     * the game session and updates the game logic accordingly based on their current
     * state. This state ensures proper continuation or ending of the game depending
     * on the players' conditions.
     *
     * In this state:
     * - If there are no valid players remaining in the game (e.g., all players are
     *   eliminated), the game transitions to the END_GAME state.
     * - It checks players' personal boards to determine if the essential `HUMAN`
     *   crew members are missing:
     *   - If a player has no `HUMAN` crew members, they are marked as out and
     *     removed from the gameplay.
     *   - The player iterator is reset after a player is marked out.
     * - After evaluations, it checks if there are still active players:
     *   - If active players exist, the state transitions to DRAW_CARD.
     *   - If no players remain, the state transitions to END_GAME.
     *
     * This state is critical for ensuring that the game logic is accurately
     * synchronized with the status of each player, enabling smooth gameplay and
     * conditions for ending the game.
     */
    CHECK_PLAYERS {

        @Override
        public void run(GameModel gameModel) {
            if(gameModel.getFlyingBoard().getRanking().isEmpty()) { // tutti i giocatori sono stati eliminati, per esempio con freeSpace in caso nessuno avesse motori
                gameModel.setCurrGameState(GameState.END_GAME);
                return;
            }

            gameModel.getFlyingBoard().getDoubledPlayers();

            gameModel.getPlayers().forEach((_, player) -> {
                if (player.getPersonalBoard()
                        .getCrewMembers()
                        .stream()
                        .noneMatch(crewMember -> crewMember.equals(CrewMember.HUMAN))
                ) {
                    gameModel.getFlyingBoard().addOutPlayer(player, false);
                    gameModel.resetPlayerIterator();
                }
            });
            // during the check_player someone could be eliminated
            if(gameModel.getFlyingBoard().getRanking().isEmpty())
                gameModel.setCurrGameState(GameState.END_GAME);
            else
                gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    },

    /**
     * Represents the final state of the game lifecycle, where the game concludes,
     * players' final credits and rankings are calculated, and notification is sent
     * to all players with the results.
     *
     * This state is responsible for:
     * - Calculating the credits of all players at the end of the game.
     * - Obtaining the final rankings along with detailed player data.
     * - Identifying the players with the "prettiest ship."
     * - Sending notifications to all players with their final data, rankings, and other results.
     */
    END_GAME {

        @Override
        public void run(GameModel gameModel) {
            gameModel.calculatePlayersCredits();

            List<PlayerFinalData> finalRankingWithPlayerFinalData = gameModel.getRankingWithPlayerFinalData();
            List<String> playersNicknamesWithPrettiestShip = gameModel.getPlayerWithPrettiestShip().stream().map(Player::getNickname).collect(Collectors.toList());

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayersFinalData(nicknameToNotify, finalRankingWithPlayerFinalData, playersNicknamesWithPrettiestShip);
            });
        }

    };

    /**
     * Executes the specific behavior associated with the current game state.
     *
     * @param gameModel the model representing the current state of the game, which contains all game-related data and logic
     */
    public abstract void run(GameModel gameModel);

}
