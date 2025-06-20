package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.io.Serializable;
import java.util.List;

public class PlayerFinalData implements Serializable {
    private final String nickname;
    private final int totalCredits;
    private final boolean isEarlyLanded;
    private final List<CargoCube> allOwnedCubes;
    private final int lostComponents;


    public PlayerFinalData (String nickname, int totalCredits, boolean isEarlyLanded, List<CargoCube> allOwnedCubes, int lostComponents) {
        this.nickname = nickname;
        this.totalCredits = totalCredits;
        this.isEarlyLanded = isEarlyLanded;
        this.allOwnedCubes = allOwnedCubes;
        this.lostComponents = lostComponents;
    }

    public boolean isEarlyLanded() {
        return isEarlyLanded;
    }

    public List<CargoCube> getAllOwnedCubes() {
        return allOwnedCubes;
    }

    public int getLostComponents() {
        return lostComponents;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public String getNickname() {
        return nickname;
    }
}
