package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.component.Storage;

import java.util.List;

public class Planets extends AdventureCard implements PlayerMover {

    private List<Planet> availablePlanets;
    private int stepsBack;
    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_PLANET, GameState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets(List<Planet> availablePlanets, int stepsBack) {
        this.availablePlanets = availablePlanets;
        this.stepsBack = stepsBack;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    public void currPlayerWantsToVisit (int wantsToVisitIndex) throws IllegalStateException, IllegalIndexException, IndexOutOfBoundsException {

        if (currState != GameState.CHOOSE_PLANET)
            throw new IllegalStateException("Not the right state");

        if (wantsToVisitIndex != 0) {

            currentPlanet = availablePlanets.get(wantsToVisitIndex - 1);

            if (currentPlanet.isBusy())
                throw new IllegalIndexException("Planet has already been chosen");

            currentPlanet.noMoreAvailable();

            currState = GameState.HANDLE_CUBES_REWARD;
            game.setCurrState(currState);

        } else if (game.hasNextPlayer()){
            game.nextPlayer();
        } else {
            game.setCurrState(GameState.END_OF_CARD);
        }

    }

    public void currPlayerChoseCargoCubeStorage (Storage chosenStorage) throws IllegalStateException {

        if (currState != GameState.HANDLE_CUBES_REWARD) throw new IllegalStateException("Not the right state");

        if(chosenStorage.isFull()) {
            List<CargoCube> sortedStorage = chosenStorage.getStockedCubes();
            sortedStorage.sort(CargoCube.byValue);
            CargoCube lessValuableCargoCube = sortedStorage.getFirst();
            chosenStorage.removeCube(lessValuableCargoCube);
        }

        if (currentPlanet.hasNext()) {
            chosenStorage.addCube(currentPlanet.getCurrent());
        } else {
            chosenStorage.addCube(currentPlanet.getReward().getLast());

            if (game.hasNextPlayer()) {
                game.nextPlayer();
                currState = GameState.CHOOSE_PLANET;
                game.setCurrState(currState);
            } else {
                game.setCurrState(GameState.END_OF_CARD);
            }

        }

    }

}
