package it.polimi.ingsw.is25am33.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientDangerousObject;
import it.polimi.ingsw.is25am33.client.model.card.ClientWarField;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.card.interfaces.*;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.dangerousObj.Shot;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.GameModel;
import it.polimi.ingsw.is25am33.model.game.Player;
import javafx.util.Pair;

import java.util.*;

public class WarField extends AdventureCard implements PlayerMover, DoubleCannonActivator, CrewMemberRemover, HowToDefend {

    private int cubeMalus;
    private int stepsBack;
    private int crewMalus;
    private List<Shot> shots;
    private List<String> shotIDs;
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<CardState, CardState> categories;
    private Pair<Player, Double> leastResourcedPlayer = null;
    private Iterator<CardState> phasesIterator;
    private Iterator<Shot> shotIterator;

    public WarField() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    @JsonIgnore
    public CardState getFirstState() {
        phasesIterator = categories.keySet().iterator();
        return phasesIterator.next();
    }

    @Override
    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) {

        switch (currState) {
            case CHOOSE_CANNONS:
                this.currPlayerChoseCannonsToActivate(playerChoices.getChosenDoubleCannons().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case CHOOSE_ENGINES:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case EVALUATE_CREW_MEMBERS:
                this.countCrewMembers();

                if (gameModel.hasNextPlayer()) {
                    gameModel.nextPlayer();
                    setCurrState(CardState.EVALUATE_CREW_MEMBERS);
                } else {
                    gameModel.resetPlayerIterator();
                    gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                        clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least members of all");
                    });
                    handleMalus();
                }

                break;
            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;
            case HANDLE_CUBES_MALUS:
                this.currPlayerChoseStorageToRemove(playerChoices.getChosenStorage().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;
            case THROW_DICES:
                this.throwDices();
                break;
            case DANGEROUS_ATTACK:
                ((Shot) gameModel.getCurrDangerousObj()).startAttack(playerChoices, this);
                break;
            case CHECK_SHIPBOARD_AFTER_ATTACK:
                this.checkShipBoardAfterAttack();
                break;
            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        List<ClientDangerousObject> clientDangerousObjects = new ArrayList<>();
        for(Shot shot : shots) {
            clientDangerousObjects.add(new ClientDangerousObject(shot.getDangerousObjType(),shot.getDirection(), -1));
        }
        return new ClientWarField(cardName,imageName, crewMalus, stepsBack, cubeMalus, clientDangerousObjects);
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

        this.shotIterator = shots.iterator();
    }

    public int getCubeMalus() {
        return cubeMalus;
    }

    public void setCubeMalus(int cubeMalus) {
        this.cubeMalus = cubeMalus;
    }

    public void setCategories(Map<CardState, CardState> categories) {
        this.categories = categories;
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

    public Map<CardState, CardState> getCategories() {
        return categories;
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

    public void checkShipBoardAfterAttack(){
        gameModel.notifyInvalidShipBoards();
        if(gameModel.areAllShipsCorrect()) {

            if (shotIterator.hasNext()) {
                setCurrState(CardState.THROW_DICES);
            } else if(phasesIterator.hasNext()){
                gameModel.resetPlayerIterator();
                setCurrState(phasesIterator.next());
            }
            else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        }
    }

    private void currPlayerChoseCannonsToActivate(List<Coordinates> chosenDoubleCannonsCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        Player currentPlayer=gameModel.getCurrPlayer();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();
        List<Cannon> chosenDoubleCannons = new ArrayList<>();

        for(Coordinates chosenDoubleCannonCoord : chosenDoubleCannonsCoords) {
            chosenDoubleCannons.add((Cannon) currentPlayer.getPersonalBoard().getComponentAt(chosenDoubleCannonCoord));
        }

        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        double currPlayerCannonPower = activateDoubleCannonsProcess(chosenDoubleCannons, chosenBatteryBoxes, gameModel.getCurrPlayer());
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (leastResourcedPlayer == null || currPlayerCannonPower < leastResourcedPlayer.getValue() ||
                (currPlayerCannonPower == leastResourcedPlayer.getValue() &&
                        gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey())<gameModel.getFlyingBoard().getPlayerPosition(currentPlayer)))
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), (double) currPlayerCannonPower);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_CANNONS);
        }
        else{
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least fire power of all");
            });
            handleMalus();
        }
    }

    public void currPlayerChoseEnginesToActivate(List<Coordinates> chosenDoubleEnginesCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        Player currentPlayer=gameModel.getCurrPlayer();
        if (chosenDoubleEnginesCoords == null || chosenBatteryBoxesCoords == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEnginesCoords.size() != chosenBatteryBoxesCoords.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        List<Engine> chosenDoubleEngines = new ArrayList<>();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenDoubleEnginesCoord : chosenDoubleEnginesCoords) {
            chosenDoubleEngines.add((Engine) currentPlayer.getPersonalBoard().getComponentAt(chosenDoubleEnginesCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        chosenBatteryBoxes.forEach(BatteryBox::useBattery);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });
        int currPlayerEnginePower = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);

        if (leastResourcedPlayer == null || currPlayerEnginePower < leastResourcedPlayer.getValue() ||
                (currPlayerEnginePower == leastResourcedPlayer.getValue() &&
                gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey())<gameModel.getFlyingBoard().getPlayerPosition(currentPlayer)))
            leastResourcedPlayer = new Pair<>(gameModel.getCurrPlayer(), (double) currPlayerEnginePower);

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_ENGINES);
        }
        else{
            gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
                clientController.notifyLeastResourcedPlayer(nicknameToNotify, leastResourcedPlayer.getKey().getNickname() + " has the least engine power of all");
            });
            handleMalus();
        }

    }

    public void countCrewMembers() {

        Player player =  gameModel.getCurrPlayer();

        if (
                leastResourcedPlayer == null ||
                leastResourcedPlayer.getValue() > player.getPersonalBoard().getCrewMembers().size() ||
                (leastResourcedPlayer.getValue() == player.getPersonalBoard().getCrewMembers().size() &&
                        gameModel.getFlyingBoard().getPlayerPosition(leastResourcedPlayer.getKey()) < gameModel.getFlyingBoard().getPlayerPosition(player))
        )
            leastResourcedPlayer = new Pair<>(player, (double) player.getPersonalBoard().getCrewMembers().size());

    }

    private void handleMalus() {

        if (categories.get(currState) == CardState.STEPS_BACK) {
            movePlayer(gameModel.getFlyingBoard(), leastResourcedPlayer.getKey(), stepsBack);
            if (phasesIterator.hasNext()) {
                gameModel.resetPlayerIterator();
                setCurrState(phasesIterator.next());
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
            }
        } else {
            gameModel.setCurrPlayer(leastResourcedPlayer.getKey());
            if (categories.get(currState) == CardState.DANGEROUS_ATTACK)
                setCurrState(CardState.THROW_DICES);
            else
                setCurrState(categories.get(currState));
        }
        leastResourcedPlayer=null;
    }

    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException {
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        Player currentPlayer=gameModel.getCurrPlayer();
        //non viene fatto il controllo se sono tutte cabine perchè già fatto lato client
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();

        removeMemberProcess(chosenCabins, crewMalus);
        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (phasesIterator.hasNext()) {
            gameModel.resetPlayerIterator();
            setCurrState(phasesIterator.next());

        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

    private void throwDices() {

        if (shotIterator == null) shotIterator = shots.iterator();

        Shot currShot = shotIterator.next();
        currShot.setCoordinates(GameModel.throwDices());
        gameModel.setCurrDangerousObj(currShot);
        setCurrState(CardState.DANGEROUS_ATTACK);

    }

    private void currPlayerChoseStorageToRemove(List<Coordinates> chosenStorageCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        //non viene fatto il controllo se sono tutte storage perchè già fatto lato client
        Player currentPlayer=gameModel.getCurrPlayer();
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        List<Storage> chosenStorages = new ArrayList();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenStorageCoord : chosenStorageCoords) {
            chosenStorages.add((Storage) currentPlayer.getPersonalBoard().getComponentAt(chosenStorageCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) currentPlayer.getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

        if(!chosenStorages.isEmpty()) {

            chosenStorages.forEach(storage -> {
                List<CargoCube> sortedStorage = storage.getStockedCubes();
                sortedStorage.sort(CargoCube.byValue);
                CargoCube moreValuableCargoCube = sortedStorage.getLast();
                storage.removeCube(moreValuableCargoCube);
            });

        }

        if(!chosenBatteryBoxes.isEmpty())
            chosenBatteryBoxes.forEach(BatteryBox::useBattery);

        gameModel.getGameClientNotifier().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currentPlayer.getNickname(), currentPlayer.getPersonalBoardAsMatrix(), currentPlayer.getPersonalBoard().getComponentsPerType(), currentPlayer.getPersonalBoard().getNotActiveComponents());
        });

        if (phasesIterator.hasNext()) {
            gameModel.resetPlayerIterator();
            setCurrState(phasesIterator.next());
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.CHECK_PLAYERS);
        }

    }

}
