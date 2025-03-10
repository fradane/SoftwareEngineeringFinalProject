package it.polimi.ingsw.is25am33.model.game;

import it.polimi.ingsw.is25am33.model.card.AdventureCard;

import java.util.ArrayList;

public abstract class Game {

    public static int throwDices() {

        double random = Math.random();

        return (int) (Math.random() * 12) + 1;

    }

}
