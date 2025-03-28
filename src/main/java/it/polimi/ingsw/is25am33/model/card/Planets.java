package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.CargoCube;
import it.polimi.ingsw.is25am33.model.GameState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.game.Game;

import java.util.List;

public class Planets extends AdventureCard implements PlayerMover {

    private List<Planet> availablePlanets;
    private int stepsBack;
    private static final List<GameState> cardStates = List.of(GameState.CHOOSE_PLANET, GameState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets(List<Planet> availablePlanets, int stepsBack, Game game) {
        super(game);
        this.availablePlanets = availablePlanets;
        this.stepsBack = stepsBack;
    }

    @Override
    public GameState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_PLANET:
                try {
                    this.currPlayerWantsToVisit(playerChoices.getChosenPlanetIndex());
                } catch (IllegalIndexException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLE_CUBES_REWARD:
                this.currPlayerChoseCargoCubeStorage(playerChoices.getChosenStorage().orElseThrow());
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    private void currPlayerWantsToVisit (int chosenPlanetIndex) throws IllegalIndexException, IndexOutOfBoundsException {

        if (chosenPlanetIndex != 0) {

            currentPlanet = availablePlanets.get(chosenPlanetIndex - 1);

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

    private void currPlayerChoseCargoCubeStorage (Storage chosenStorage) {

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
