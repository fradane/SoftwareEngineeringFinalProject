package it.polimi.ingsw.is25am33.model.card;
import it.polimi.ingsw.is25am33.client.model.card.ClientCard;
import it.polimi.ingsw.is25am33.client.model.card.ClientFreeSpace;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.enumFiles.CardState;
import it.polimi.ingsw.is25am33.model.UnknownStateException;
import it.polimi.ingsw.is25am33.model.card.interfaces.PlayerMover;
import it.polimi.ingsw.is25am33.model.component.BatteryBox;
import it.polimi.ingsw.is25am33.model.component.Engine;
import it.polimi.ingsw.is25am33.model.enumFiles.GameState;
import it.polimi.ingsw.is25am33.model.game.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FreeSpace extends AdventureCard implements PlayerMover {

    private static final List<CardState> cardStates = List.of(CardState.CHOOSE_ENGINES);

    public FreeSpace() {
        this.cardName = this.getClass().getSimpleName();
    }

    @Override
    public CardState getFirstState() {
        return cardStates.getFirst();
    }

    @Override
    public void play(PlayerChoicesDataStructure playerChoices) throws UnknownStateException {

        switch (currState) {
            case CHOOSE_ENGINES:
                this.currPlayerChoseEnginesToActivate(playerChoices.getChosenDoubleEngines().orElseThrow(), playerChoices.getChosenBatteryBoxes().orElseThrow());
                break;

            default:
                throw new UnknownStateException("Unknown current state");
        }

    }

    @Override
    public ClientCard toClientCard() {
        return new ClientFreeSpace(cardName,imageName);
    }

    public void currPlayerChoseEnginesToActivate(List<Coordinates> chosenDoubleEnginesCoords, List<Coordinates> chosenBatteryBoxesCoords) throws IllegalArgumentException {

        if (chosenDoubleEnginesCoords == null || chosenBatteryBoxesCoords == null)
            throw new IllegalArgumentException("Null lists");

        if (chosenDoubleEnginesCoords.size() != chosenBatteryBoxesCoords.size())
            throw new IllegalArgumentException("The number of engines does not match the number of battery boxes");

        List<Engine> chosenDoubleEngines = new ArrayList<>();
        List<BatteryBox> chosenBatteryBoxes = new ArrayList<>();

        for (Coordinates chosenDoubleEnginesCoord : chosenDoubleEnginesCoords) {
            chosenDoubleEngines.add((Engine) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenDoubleEnginesCoord));
        }
        for (Coordinates chosenBatteryBoxCoord : chosenBatteryBoxesCoords) {
            chosenBatteryBoxes.add((BatteryBox) gameModel.getCurrPlayer().getPersonalBoard().getComponentAt(chosenBatteryBoxCoord));
        }

//        chosenDoubleEnginesCoords.stream().distinct().forEach(box -> {
//            if (Collections.frequency(chosenBatteryBoxesCoords, box) > box.getRemainingBatteries())
//                throw new IllegalArgumentException("The number of required batteries is not enough");
//        });

        int stepsForward = gameModel.getCurrPlayer().getPersonalBoard().countTotalEnginePower(chosenDoubleEngines);
        // check whether the declared engine power equals 0, in this case the player must be disqualified
        if (stepsForward == 0) {
            gameModel.getFlyingBoard().addOutPlayer(gameModel.getCurrPlayer(), false);
        } else {

            chosenBatteryBoxes.forEach(BatteryBox::useBattery);

            Player currentPlayer=gameModel.getCurrPlayer();

            gameModel.getGameContext().notifyAllClients((nicknameToNotify, clientController) -> {
               clientController.notifyShipBoardUpdate(nicknameToNotify,currentPlayer.getNickname(),currentPlayer.getPersonalBoardAsMatrix(),currentPlayer.getPersonalBoard().getComponentsPerType());
            });

            movePlayer(gameModel.getFlyingBoard(), gameModel.getCurrPlayer(), stepsForward);
        }

        if (gameModel.hasNextPlayer()) {
            gameModel.nextPlayer();
            setCurrState(CardState.CHOOSE_ENGINES);
        } else {
            setCurrState(CardState.END_OF_CARD);
            gameModel.resetPlayerIterator();
            gameModel.setCurrGameState(GameState.DRAW_CARD);
        }
    }

    @Override
    public String toString() {
        return String.format( """
           %s
           ┌────────────────────────────┐
           │         FreeSpace          │
           └────────────────────────────┘
           """, imageName);
    }

}
