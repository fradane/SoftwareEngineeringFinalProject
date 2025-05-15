package it.polimi.ingsw.is25am33.model.enumFiles;

import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.game.GameModel;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;

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
            gameModel.notifyInvalidShipBoards();

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
