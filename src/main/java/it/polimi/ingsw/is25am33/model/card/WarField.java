package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.Category;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;

import java.util.List;

public class WarField extends AdventureCard implements PlayerMover {

    private List<Category> minimunCategories;
    private int stepsBack;
    private int crewMalus;
    private List<Shot> shots;

    public WarField(List<Category> minimunCategories, List<Shot> shots) {
        this.minimunCategories = minimunCategories;
        this.shots = shots;
    }

    public WarField() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return null;
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {
        return;
    }

    public void setMinimunCategories(List<Category> minimunCategories) {
        this.minimunCategories = minimunCategories;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    // TODO
//    public void effect(GameModel gameModel) {
//
//        FlyingBoard flyingBoard = gameModel.getFlyingBoard();
//
//        minimunCategories.forEach(cat -> {
//            Player minPlayer = cat.getMinimumPlayer(flyingBoard.getCurrentRanking());
//
//            switch (minimunCategories.indexOf(cat)) {
//                case 0:
//                    movePlayer(gameModel.getFlyingBoard, minPlayer, stepsBack);
//                    break;
//
//                case 1:
//                    minPlayer.getPersonalBoard().removeCrewMembers(crewMalus);
//                    break;
//
//                case 2:
//
//                    for(Shot s : shots) {
//
//                        s.setCoordinates(GameModel.throwDices());
//                        minPlayer.getPersonalBoard().handleDangerousObj(s);
//
//                    }
//
//                    break;
//            }
//
//        });
//
//
//    }
}
