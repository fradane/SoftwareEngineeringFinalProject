package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;


import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;

public interface BoardsController {
    void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel);

    void removeHighlightColor();

    void applyHighlightEffect(Coordinates coordinates, Color color);
}
