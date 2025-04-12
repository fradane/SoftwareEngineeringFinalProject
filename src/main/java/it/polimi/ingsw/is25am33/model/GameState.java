package it.polimi.ingsw.is25am33.model;

import it.polimi.ingsw.is25am33.model.game.Game;


public enum GameState {

    SETUP {
        @Override
        public void run(Game game) {
            game.getDeck().setUpLittleDecks(game);
            //TODO creazione tessere e posizionamento tessere nel tavolo
        }
    },

    BUILD_SHIPBOARD {

        @Override
        public void run(Game game) {

        }

    },

    CHECK_SHIPBOARD {

        @Override
        public void run(Game game) {

        }

    },

    CREATE_DECK {

        @Override
        public void run(Game game) {
            game.getDeck().mergeIntoGameDeck();
        }

    },

    DRAW_CARD {

        @Override
        public void run(Game game) {
            game.setCurrAdventureCard(game.getDeck().drawCard());
        }

    },

    PLAY_CARD {

        @Override
        public void run(Game game) {
            game.startCard();
        }

    },

    CHECK_PLAYERS {

        @Override
        public void run(Game game) {
            game.getFlyingBoard().getDoubledPlayers();
        }

    },

    END_GAME {

        @Override
        public void run(Game game) {
            game.calculatePlayersCredits();
        }

    };

    public abstract void run(Game game);

}
