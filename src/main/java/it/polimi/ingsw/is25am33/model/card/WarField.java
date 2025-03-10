package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.Category;
import it.polimi.ingsw.is25am33.model.board.FlyingBoard;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;

public class WarField extends AdventureCard implements playerMover{

    private ArrayList<Category> minimunCategories;
    private int stepsBack;
    private int crewMalus;
    private ArrayList<Shoot> shoots;

    @Override
    public void effect(Game game) {

        FlyingBoard flyingBoard = game.getFlyingBoard();

        minimunCategories.forEach(cat -> {
            Player minPlayer = cat.getMinimumPlayer(flyingBoard.getCurrentRanking());

            switch (minimunCategories.indexOf(cat)) {
                case 0:
                    movePlayer(game.getFlyingBoard, minPlayer, stepsBack);
                    break;

                case 1:
                    minPlayer.getPersonalBoard().removeCrewMembers(crewMalus);
                    break;

                case 2:

                    for(Shot s : shoots) {

                        s.setCoordinates(Game.throwDices());
                        minPlayer.getPersonalBoard().handleDangerousObj(s);

                    }

                    break;
            }

        });


    }
}
