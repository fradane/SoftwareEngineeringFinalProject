package it.polimi.ingsw.is25am33.model.card;


import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Pirates extends AdvancedEnemies implements playerMover {

    private List<Shot> shots;

    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    @Override
    public void effect(Game game) {

        ArrayList<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();
        Set<ShipBoard> defeatedShipBoards = new HashSet<>();

        for (Player p : playersRanking) {

            int currPlayerTotalCannonPower = p.getPersonalBoard().countTotalCannonPower();

            if(currPlayerTotalCannonPower > requiredFirePower) {

                movePlayer(game.getFlyingBoard, p, stepsBack);
                p.addCredits(reward);
                break;

            } else if(currPlayerTotalCannonPower < requiredFirePower) {

                defeatedPlayers.add(p.getPersonalBoard());

            }

        }

        for (Shot s : shots) {

            s.setCoordinates(Game.throwDices());
            defeatedShipBoards.stream()
                    .forEach(b -> b.handleDangerousObj(s));

        }

    }

}
