package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Smugglers extends Enemies implements PlayerMover {

    private int cubeMalus;
    private List<CargoCube> reward;

    public Smugglers() {
        this.cardName = this.getClass().getSimpleName();
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setReward(List<CargoCube> reward) {
        this.reward = reward;
    }

    @Override
    public CardState getFirstState() {
        return null;
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {
        return;
    }

    // TODO
//    public void effect(Game game) {
//
//        ArrayList<Player> playersRanking = game.getFlyingBoard().getCurrentRanking();
//
//        for (Player p : playersRanking) {
//
//            int currPlayerTotalCannonPower = p.getPersonalBoard().countTotalCannonPower();
//
//            if(currPlayerTotalCannonPower > requiredFirePower) {
//
//                movePlayer(game.getFlyingBoard(), p, stepsBack);
//                this.handleCargoCubesReward(reward, p);
//                break;
//
//            } else if(currPlayerTotalCannonPower < requiredFirePower) {
//
//                this.removeMostValuableCargoCubes(p, game.getController());
//
//            }
//
//        }
//
//    }

//    void removeMostValuableCargoCubes(Player player, Controller controller) {
//
//        ArrayList<Storage> playerPersonalStorage = p.getPersonalShip().getStorages();
//        int cargoCubesLeftToRemove = cubeMalus;
//        ArrayList<Storage> candidateStorages;
//
//        for(CargoCube cubeType : CargoCube.values()) {
//
//            int removableCargoCubes = playerPersonalStorage.stream()
//                    .flatMap(s -> s.getStackedCubes().stream())
//                    .filter(c -> c == cubeType)
//                    .count();
//
//            if(removableCargoCubes < cargoCubesLeftToRemove) {
//
//                cargoCubesLeftToRemove -= removableCargoCubes;
//                playerPersonalStorage.forEach(s -> s.removeAllCargoCubesOfType(cubeType));
//
//            } else if (removableCargoCubes == cargoCubesLeftToRemove) {
//
//                cargoCubesLeftToRemove -= removableCargoCubes;
//                playerPersonalStorage.forEach(s -> s.removeAllCargoCubesOfType(cubeType));
//                break;
//
//            } else {
//
//                candidateStorages = playerPersonalStorage.stream()
//                        .filter(s -> s.contains(cubeType))
//                        .toList();
//
//                Map<Storage, Integer> cargoCubesToRemove = controller.whichCargoCubesWantsToRemove(p, cargoCubesLeftToRemove, candidateStorages, cubeType);
//                cargoCubesToRemove.forEach((s, qnt) -> s.removeCargoCubesOfType(qnt, cubeType));
//
//                cargoCubesLeftToRemove -= removableCargoCubes;
//
//                break;
//            }
//
//        }
//
//        if (cargoCubesLeftToRemove == 0) return;
//
//        ArrayList<BatteryBox> availableBatteryBox = player.getPersonalBoard().getBatteryBoxes();
//        controller.whichBatteriesWantsToRemove(player, availableBatteryBox, cargoCubesLeftToRemove);
//
//    }


}
