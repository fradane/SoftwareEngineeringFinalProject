package it.polimi.ingsw.is25am33.model.card;

import it.polimi.ingsw.is25am33.client.model.card.ClientAbandonedShip;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.board.ShipBoard;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.IllegalDecisionException;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.CrewMemberRemover;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.Cabin;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;

import java.util.List;


public class AbandonedShip extends AdventureCard implements PlayerMover, CrewMemberRemover {

    private int crewMalus;
    private int stepsBack;
    private int reward;

    private static final List<CardState> cardStates = List.of(CardState.VISIT_LOCATION, CardState.REMOVE_CREW_MEMBERS);

    public AbandonedShip(int crewMalus, int stepsBack, int reward) {
        this.crewMalus = crewMalus;
        this.stepsBack = stepsBack;
        this.reward = reward;
    }

    public AbandonedShip() {
        this.cardName = this.getClass().getSimpleName();
    }

    public int getCrewMalus() {
        return crewMalus;
    }

    public int getStepsBack() {
        return stepsBack;
    }

    public int getReward() {
        return reward;
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case VISIT_LOCATION:
                try {
                    this.currPlayerWantsToVisit(playerChoices.isWantsToVisit());
                } catch (IllegalDecisionException e) {
                    e.printStackTrace();
                }
                break;

            case REMOVE_CREW_MEMBERS:
                this.currPlayerChoseRemovableCrewMembers(playerChoices.getChosenCabins().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        return new ClientAbandonedShip(cardName, imageName, crewMalus, stepsBack, reward);
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    public void setCrewMalus(int crewMalus) {
        this.crewMalus = crewMalus;
    }

    public void setStepsBack(int stepsBack) {
        this.stepsBack = stepsBack;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    private void currPlayerWantsToVisit(boolean wantsToVisit) throws IllegalDecisionException {
        try{
            if (wantsToVisit) {
                if (gameModel.getCurrPlayer().getPersonalBoard().getCrewMembers().size() < crewMalus)
                    //TODO bisogna gestire questo genere di eccezioni, teoricamente già controllate lato client, però boh
                    throw new IllegalDecisionException("Player has not enough crew members");
                setCurrState(CardState.REMOVE_CREW_MEMBERS);
            } else if (gameModel.hasNextPlayer()) {
                gameModel.nextPlayer();
                setCurrState(CardState.VISIT_LOCATION);
            } else {
                setCurrState(CardState.END_OF_CARD);
                gameModel.resetPlayerIterator();
                gameModel.setCurrGameState(GameState.DRAW_CARD);
            }
        }catch (Exception e){
            System.err.println("Error in currPlayerWantsToVisit: " + e.getMessage());
            e.printStackTrace();
        }


    }

    private void currPlayerChoseRemovableCrewMembers(List<Coordinates> chosenCabinsCoordinate) throws IllegalArgumentException {
        ShipBoard shipBoard = gameModel.getCurrPlayer().getPersonalBoard();
        //non viene fatto il controllo se sono tutte cabine perchè già fatto lato client
        List<Cabin> chosenCabins = chosenCabinsCoordinate
                .stream()
                .map(shipBoard::getComponentAt)
                .map(Cabin.class::cast)
                .toList();


        removeMemberProcess(chosenCabins, crewMalus);

        String currPlayerNickname = gameModel.getCurrPlayer().getNickname();
        gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
            clientController.notifyShipBoardUpdate(nicknameToNotify, currPlayerNickname, shipBoard.getShipMatrix(), shipBoard.getComponentsPerType());
        });

        //chosenCabins.forEach(Cabin::removeMember);

        gameModel.getCurrPlayer().addCredits(reward);
        movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsBack);

        setCurrState(CardState.END_OF_CARD);
        gameModel.setCurrGameState(GameState.DRAW_CARD);

    }

    @Override
    public String toString() {
        return String.format("""
           %s
           ┌────────────────────────────┐
           │       AbandonedShip        │
           ├────────────────────────────┤
           │ Crew Lost:         x%-2d     │
           │ Reward:            x%-2d     │
           │ Steps Back:        %-2d      │
           └────────────────────────────┘
           """, imageName, crewMalus, reward, stepsBack);
    }

}
