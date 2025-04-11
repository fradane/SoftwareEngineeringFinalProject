package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.model.game.GameModel;

public enum GameState {

    SETUP {
        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().setUpLittleDecks();
            //TODO creazione tessere e posizionamento tessere nel tavolo
        }
    },

    BUILD_SHIPBOARD {

        @Override
        public void run(GameModel gameModel) {

        }

    },

    CHECK_SHIPBOARD {

        @Override
        public void run(GameModel gameModel) {

        }

    },

    CREATE_DECK {

        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().mergeIntoGameDeck();
        }

    },

    DRAW_CARD {

        @Override
        public void run(GameModel gameModel) {
            gameModel.setCurrAdventureCard(gameModel.getDeck().drawCard());
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
