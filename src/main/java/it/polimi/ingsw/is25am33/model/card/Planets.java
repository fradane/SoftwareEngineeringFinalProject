package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;

import java.util.List;

public class Planets extends AdventureCard implements PlayerMover {

    private List<Planet> availablePlanets;
    private int stepsBack;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_PLANET, CardState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets(List<Planet> availablePlanets, int stepsBack) {
        this.availablePlanets = availablePlanets;
        this.stepsBack = stepsBack;
    }

    public Planets() {
        this.cardName = this.getClass().getSimpleName();
    }

    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    @JsonIgnore
    public Planet getCurrentPlanet() {
        return currentPlanet;
    }

    @Override
    public CardState getFirstState() {
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

            currentPlanet.isNoMoreAvailable();

            setCurrState(CardState.HANDLE_CUBES_REWARD);

        } else if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
        } else {
            setCurrState(CardState.END_OF_CARD);
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

            if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.CHOOSE_PLANET);
            } else {
                setCurrState(CardState.END_OF_CARD);
            }

        }
    }


    @Override
    public String toString() {
        String firstString = String.format("""
           ┌────────────────────────────┐
           │          Planets           │
           ├────────────────────────────┤
           │ Planets:           x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """,
                availablePlanets != null ? availablePlanets.size() : 0,
                stepsBack);

        StringBuilder secondString = new StringBuilder("   ");
        if (availablePlanets != null && !availablePlanets.isEmpty()) {
            secondString.append("Planet Rewards:\n");
            for (int i = 0; i < availablePlanets.size(); i++) {
                List<CargoCube> reward = availablePlanets.get(i).getReward();
                String cubes = reward
                        .stream()
                        .map(Enum::name)
                        .toList()
                        .toString()
                        .replaceAll("[\\[\\]]", "");
                secondString.append(String.format("   Planet %d: %s%n", i + 1, cubes));
            }
        }

        return firstString + secondString;
    }

    public static void main(String[] args) {
        new Planets();
        AdventureCard x;
        Deck deck = new Deck();
        deck.loadCards();
        x = deck.getAllCards().stream().filter(card -> card instanceof Planets).toList().get(4);
        System.out.println(x);
    }
}
