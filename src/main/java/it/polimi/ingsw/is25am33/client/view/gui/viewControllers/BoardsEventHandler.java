package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

/**
 * Interface for handling events from the ship board grid.
 * Allows separation of event handling logic from the reusable component.
 */
public interface BoardsEventHandler {


    /**
     * Called when a button on the main grid is clicked.
     *
     * @param row the model row (4-8)
     * @param column the model column (3-9)
     */
    void onGridButtonClick(int row, int column);

}