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

    public Map<String, Planet> getPlayerPlanet() {
        return playerPlanet;
    }


    // Setters
    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setPlayerPlanet(String playerNickname, Planet planet) {
        playerPlanet.put(playerNickname, planet);
    }

    @Override
    public CardState getFirstState() {
        return CardState.CHOOSE_PLANET;
    }

    @Override
    public String getCardType() {
        return "Planets";
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
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────────────────────┐\n");
        sb.append("│              PLANETS               │\n");
        sb.append("├────────────────────────────────────┤\n");
        sb.append(String.format("│ Available Planets:       %-8d │\n", availablePlanets != null ? availablePlanets.size() : 0));
        sb.append(String.format("│ Flight Days Cost:         %-8d │\n", stepsBack));
        sb.append("└────────────────────────────────────┘\n");
        
        if (availablePlanets != null && !availablePlanets.isEmpty()) {
            sb.append("Planet Details:\n");
            for (int i = 0; i < availablePlanets.size(); i++) {
                Planet planet = availablePlanets.get(i);
                String status = planet.isBusy() ? "OCCUPIED" : "AVAILABLE";
                sb.append(String.format("  %d. Status: %-9s | Reward: ", i + 1, status));
                if (planet.getReward() != null && !planet.getReward().isEmpty()) {
                    planet.getReward().forEach(cube -> sb.append(cube.name()).append(" "));
                } else {
                    sb.append("None");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        if (!playerPlanet.isEmpty()) {
            sb.append("Player Choices:\n");
            playerPlanet.forEach((player, planet) -> {
                sb.append(String.format("  %s -> ", player));
                if (planet.getReward() != null && !planet.getReward().isEmpty()) {
                    planet.getReward().forEach(cube -> sb.append(cube.name()).append(" "));
                } else {
                    sb.append("No reward");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }
        
        sb.append("Effects: Each planet shows 2-4 different worlds with cargo\n");
        sb.append("rewards and flight day costs. Players choose in route order\n");
        sb.append("whether to land or skip. Landing loads goods and moves you\n");
        sb.append("back by the specified days. Each planet can only host one ship.");
        
        return sb.toString();
    }

    public List<CargoCube> getPlayerReward(String playerNickname) {
        return playerPlanet.get(playerNickname).getReward();
    }

}