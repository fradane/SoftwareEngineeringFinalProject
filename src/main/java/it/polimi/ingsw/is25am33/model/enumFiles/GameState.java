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

    SETUP {
        @Override
        public void run(GameModel gameModel) {}
    },

    BUILD_SHIPBOARD {
        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().setUpLittleDecks(gameModel);
        }
    },

    CHECK_SHIPBOARD {

        @Override
        public void run(GameModel gameModel) {
            if (!gameModel.isTestFlight())
                gameModel.notifyStopHourglass();

            // First, check all shipboards to identify incorrect components
            gameModel.getPlayers().values().forEach(player -> {
                player.getPersonalBoard().checkShipBoard();
            });

            // After checking all shipboards, send notifications
            gameModel.notifyInvalidShipBoards();
            gameModel.notifyValidShipBoards();

            //Controlla se tutte le navi sono corrette e cambia fase se necessario
            gameModel.checkAndTransitionToNextPhase();
        }

    },

    PLACE_CREW {
        @Override
        public void run(GameModel gameModel) {
            gameModel.handleCrewPlacementPhase();
        }
    },

    CREATE_DECK {

        @Override
        public void run(GameModel gameModel) {
            //TODO
            //gameModel.getDeck().mergeIntoGameDeck();
            gameModel.getDeck().createGameDeck(gameModel.isTestFlight());
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    },

    DRAW_CARD {

        @Override
        public void run(GameModel gameModel) {
            try {
                if(gameModel.getDeck().hasFinishedCards()){
                    gameModel.setCurrGameState(GameState.END_GAME);
                    return;
                }


                gameModel.setCurrAdventureCard(gameModel.getDeck().drawCard());
                gameModel.setCurrGameState(GameState.PLAY_CARD);
            } catch (EmptyStackException e) {
                //TODO
                e.printStackTrace();
                gameModel.setCurrAdventureCard(null);
            }
        }

    },

    PLAY_CARD {

        @Override
        public void run(GameModel gameModel) {
            gameModel.startCard();
        }

    },

    // TODO aggiungere al gioco
    CHECK_PLAYERS {

        @Override
        public void run(GameModel gameModel) {
            if(gameModel.getFlyingBoard().getRanking().isEmpty()) { // tutti i giocatori sono stati eliminati, per esempio con freeSpace in caso nessuno avesse motori
                gameModel.setCurrGameState(GameState.END_GAME);
                return;
            }

            // check doubled players
            gameModel.getFlyingBoard().getDoubledPlayers();

            // check if there are any human members alive
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

            if(gameModel.getFlyingBoard().getRanking().isEmpty()) // potrebbe essere stato eliminato qualche nuovo giocatore durante la check_players
                gameModel.setCurrGameState(GameState.END_GAME);
            else
                gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    },

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

    public abstract void run(GameModel gameModel);

}
