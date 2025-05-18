package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.WarFieldCategories;

import java.util.ArrayList;
import java.util.List;

public class WarField extends AdventureCard implements PlayerMover {

    private int stepsBack;
    private int crewMalus;
    private List<Shot> shots;
    private List<String> shotIDs;
    private static final List<CardState> cardStates = List.of(CardState.DANGEROUS_ATTACK, CardState.REMOVE_CREW_MEMBERS, CardState.HANDLE_CUBES_MALUS);
    private List<WarFieldCategories> categories = new ArrayList<>();

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

    public void convertIdsToShots() {

        shots = shotIDs.stream()
                .map(id -> {
                    try {
                        return shotCreator.get(id).call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

    }

    public void setMinimunCategories(List<WarFieldCategories> minimunCategories) {
        this.categories = minimunCategories;
    }

    public List<String> getShotIDs() {
        return shotIDs;
    }

    public void setShotIDs(List<String> shotIDs) {
        this.shotIDs = shotIDs;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    public List<Shot> getShots() {
        return shots;
    }

    public List<WarFieldCategories> getCategories() {
        return categories;
    }

    public void setCategories(List<WarFieldCategories> categories) {
        this.categories = categories;
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





}
