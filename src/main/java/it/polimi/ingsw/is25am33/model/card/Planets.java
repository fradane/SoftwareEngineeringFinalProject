package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientPlanets;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.CubesRedistributionHandler;
import it.polimi.ingsw.is25am33.model.component.Component;
import it.polimi.ingsw.is25am33.model.component.SpecialStorage;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalIndexException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Storage;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Planets extends AdventureCard implements PlayerMover, CubesRedistributionHandler {
    private List<Planet> availablePlanets;
    private final Map<String, Planet> playerPlanet = new ConcurrentHashMap<>();
    private int stepsBack;
    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_PLANET, CardState.HANDLE_CUBES_REWARD);
    private Planet currentPlanet;

    public Planets() {
        this.cardName = this.getClass().getSimpleName();
    }

    public List<Planet> getAvailablePlanets() {
        return availablePlanets;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public Map<String, Planet> getPlayerPlanet() {
        return playerPlanet;
    }

    public void setCurrPlanet(Planet planet) {
        this.currentPlanet = planet;
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
        //TODO fix Planets with Socket. Currently not working, check again after merge with Luca

        switch (currState) {
            case CHOOSE_PLANET:
                try {
                    this.currPlayerWantsToVisit(playerChoices.getChosenPlanetIndex());
                } catch (IllegalIndexException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLE_CUBES_REWARD:
                if (playerChoices.getStorageUpdates().isPresent()) {
                    this.handleStorageUpdates(playerChoices.getStorageUpdates().orElseThrow());
                } else {
                    this.currPlayerChoseCargoCubeStorage(playerChoices.getChosenStorage().orElseThrow());
                }
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }
    }

    @JsonIgnore
    public List<CargoCube> getPlayerReward(String playerNickname) {
        return playerPlanet.get(playerNickname).getReward();
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setAvailablePlanets(List<Planet> availablePlanets) {
        this.availablePlanets = availablePlanets;
    }

    private void currPlayerWantsToVisit(int chosenPlanetIndex) throws IllegalIndexException, IndexOutOfBoundsException {
        if (chosenPlanetIndex != 0) {
            currentPlanet = availablePlanets.get(chosenPlanetIndex - 1);

            if (currentPlanet.isBusy())
                throw new IllegalIndexException("Planet has already been chosen");

            playerPlanet.put(gameModel.getCurrPlayer().getNickname(), currentPlanet);
            availablePlanets.get(chosenPlanetIndex - 1).setBusy(true);
            currentPlanet.setNoMoreAvailable();

            ClientCard clientCard = this.toClientCard();
            String currPlayerNickname = gameModel.getCurrPlayer().getNickname();

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyPlayerVisitedPlanet(nicknameToNotify, currPlayerNickname, clientCard);
            });

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyCurrAdventureCardUpdate(nicknameToNotify, clientCard);
            });

            setCurrState(CardState.HANDLE_CUBES_REWARD);

        } else if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    private void currPlayerChoseCargoCubeStorage(List<Coordinates> chosenStorageCoords) {
        List<CargoCube> planetRewards = new ArrayList<>(currentPlanet.getReward());

        // no check is performed to ensure they are all storage because it's already handled on the client side
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Storage> chosenStorages = new ArrayList();
        for (Coordinates coords : chosenStorageCoords) {
            if (coords.isCoordinateInvalid()) {
                chosenStorages.add(null);
            } else {
                Component component = shipBoard.getComponentAt(coords);
                if (component instanceof Storage) {
                    chosenStorages.add((Storage) component);
                } else {
                    chosenStorages.add(null);
                }
            }
        }

        // Case 1: no storage available
        if (chosenStorages.isEmpty()) {
            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " cannot accept any rewards due to lack of storage space");
            proceedToNextPlayerOrEndCard();
            return;
        }

        // Case 2: fewer storages than rewards
        if (chosenStorages.size() < planetRewards.size()) {
            List<CargoCube> rewardsToProcess = planetRewards.subList(0, chosenStorages.size());
            List<CargoCube> discardedRewards = planetRewards.subList(chosenStorages.size(), planetRewards.size());

            System.out.println("Player " + gameModel.getCurrPlayer().getNickname() +
                    " can only accept " + chosenStorages.size() +
                    " out of " + planetRewards.size() + " rewards");
            System.out.println("Discarded rewards: " + discardedRewards);

            planetRewards = rewardsToProcess;
        }

        // Case 3: more storages than rewards
        if (chosenStorages.size() > planetRewards.size()) {
            chosenStorages = chosenStorages.subList(0, planetRewards.size());
        }

        // Validation: RED cubes must go in SpecialStorage
        for (int i = 0; i < Math.min(chosenStorages.size(), planetRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = planetRewards.get(i);

            if (storage == null) continue;

            if (!(storage instanceof SpecialStorage) && cube == CargoCube.RED) {
                throw new IllegalArgumentException("Trying to store a RED cube in a non-special storage");
            }
        }

        // Apply cubes
        for (int i = 0; i < Math.min(chosenStorages.size(), planetRewards.size()); i++) {
            Storage storage = chosenStorages.get(i);
            CargoCube cube = planetRewards.get(i);

            if (storage == null) {
                System.out.println("Cube " + cube + " discarded - no valid storage selected");
                continue;
            }

            if (storage.isFull()) {
                List<CargoCube> sortedStorage = new ArrayList<>(storage.getStockedCubes());
                sortedStorage.sort(CargoCube.byValue);
                CargoCube lessValuable = sortedStorage.get(0);
                storage.removeCube(lessValuable);
                System.out.println("Removed " + lessValuable + " to make space for " + cube);
            }

            storage.addCube(cube);
            System.out.println("Added " + cube + " to storage");
        }

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify,
                    gameModel.getCurrPlayer().getNickname(),
                    gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(),
                    gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
        });

        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyRankingUpdate(nicknameToNotify,
                    gameModel.getCurrPlayer().getNickname(),
                    gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
        });

        proceedToNextPlayerOrEndCard();
    }

    /**
     * Handles storage updates through the new data structure.
     *
     * @param storageUpdates map of storage updates
     */
    private void handleStorageUpdates(Map<Coordinates, List<CargoCube>> storageUpdates) {
        try {
            validateStorageUpdates(storageUpdates, gameModel);
            applyStorageUpdates(storageUpdates, gameModel);

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify,
                        gameModel.getCurrPlayer().getNickname(),
                        gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(),
                        gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
            });

            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyRankingUpdate(nicknameToNotify,
                        gameModel.getCurrPlayer().getNickname(),
                        gameModel.getFlyingBoard().getPlayerPosition(gameModel.getCurrPlayer()));
            });

            proceedToNextPlayerOrEndCard();

        } catch (IllegalArgumentException e) {
            String currentPlayer = gameModel.getCurrPlayer().getNickname();
            gameModel.getGameClientNotifier().notifyClients(Set.of(currentPlayer),
                    (nickname, clientController) -> {
                        clientController.notifyStorageError(nickname, e.getMessage());
                    });

            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyShipBoardUpdate(nicknameToNotify,
                        gameModel.getCurrPlayer().getNickname(),
                        gameModel.getCurrPlayer().getPersonalBoard().getShipMatrix(),
                        gameModel.getCurrPlayer().getPersonalBoard().getComponentsPerType());
            });

            // Remain in HANDLE_CUBES_REWARD state for retry
        }
    }

    /**
     * Helper method to proceed to the next player or end the card
     */
    private void proceedToNextPlayerOrEndCard() {
        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_PLANET);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }
    }

    public ClientCard toClientCard() {
        return new ClientPlanets(cardName, imageName, availablePlanets, playerPlanet, stepsBack);
    }
}