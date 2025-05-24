package it.polimi.ingsw.is25am33.client.model.card;

import it.polimi.ingsw.is25am33.model.card.Planet;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side representation of a Planets adventure card.
 * Contains all information needed for the UI to display and interact with the card,
 * without requiring server-side logic.
 */
public class ClientPlanets extends ClientCard {

    private List<Planet> availablePlanets;
    private Map<String, Planet> playerPlanet = new ConcurrentHashMap<>();
    private int stepsBack;

    public ClientPlanets() {
        super();
        this.availablePlanets = new ArrayList<>();
    }

    public ClientPlanets(String cardName, String imageName, List<Planet> availablePlanets, Map<String, Planet> playerPlanet, int stepsBack) {
        super(cardName, imageName);
        this.availablePlanets = availablePlanets;
        this.playerPlanet = playerPlanet;
        this.stepsBack = stepsBack;
    }

    // Getters
    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getPlanetCount() {
        return availablePlanets.size();
    }

    // Setters
    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_PLANET;
    }

    @Override
    public String getCardType() {
        return "PLANETS";
    }

    @Override
    public boolean hasStepsBack() {
        return true;
    }

    @Override
    public boolean hasCubeReward() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Planets Card: ");
        sb.append(cardName).append("\n");
        sb.append("Available Planets: ").append(availablePlanets.size()).append("\n");
        sb.append("Steps Back: ").append(stepsBack).append("\n");

        sb.append("Planet Rewards:\n");
        for (int i = 0; i < availablePlanets.size(); i++) {
            Planet planet = availablePlanets.get(i);
            if (!planet.isBusy()) {
                sb.append("  Planet ").append(i + 1).append(": ");
                planet.getReward().forEach(cube -> sb.append(cube.name()).append(" "));
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public List<CargoCube> getPlayerReward(String playerNickname) {
        return playerPlanet.get(playerNickname).getReward();
    }
}