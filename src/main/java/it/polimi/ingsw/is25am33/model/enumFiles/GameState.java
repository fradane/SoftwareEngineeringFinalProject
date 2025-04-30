package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.Serializable;
import java.rmi.RemoteException;

public enum GameState implements Serializable {

    SETUP {
        @Override
        public void run(GameModel gameModel) {
            gameModel.getDeck().setUpLittleDecks(gameModel);
            //TODO creazione tessere e posizionamento tessere nel tavolo
            gameModel.setCurrGameState(GameState.BUILD_SHIPBOARD);
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

    public abstract void run(GameModel gameModel) throws RemoteException;

}
