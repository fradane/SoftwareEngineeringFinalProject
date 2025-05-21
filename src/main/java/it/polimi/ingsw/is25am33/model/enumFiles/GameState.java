package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.Serializable;

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

    CREATE_DECK {

        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().mergeIntoGameDeck();
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }

    },

    DRAW_CARD {

        @Override
        public void run(GameModel gameModel) {
            gameModel.setCurrAdventureCard(gameModel.getDeck().drawCard());
            gameModel.setCurrGameState(GameState.PLAY_CARD);
        }

    },

    PLAY_CARD {

        @Override
        public void run(GameModel gameModel) {
            gameModel.startCard();

        }

    },

    CHECK_PLAYERS {

        @Override
        public void run(GameModel gameModel) {
            gameModel.getFlyingBoard().getDoubledPlayers();
        }

    },

    END_GAME {

        @Override
        public void run(GameModel gameModel) {
            gameModel.calculatePlayersCredits();
        }

    };

    public abstract void run(GameModel gameModel);

}
