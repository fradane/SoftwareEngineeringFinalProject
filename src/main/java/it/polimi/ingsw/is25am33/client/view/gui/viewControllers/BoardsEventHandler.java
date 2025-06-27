package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

/**
 * Interfaccia per gestire gli eventi provenienti dalla griglia delle shipboard.
 * Permette di separare la logica di gestione degli eventi dal componente riusabile.
 */
public interface BoardsEventHandler {

    // TODO

    /**
     * Called when a button on the main grid is clicked.
     *
     * @param row the model row (4-8)
     * @param column the model column (3-9)
     */
    void onGridButtonClick(int row, int column);

//    /**
//     * Chiamato quando viene selezionata la shipboard di un giocatore.
//     *
//     * @param playerNickname il nickname del giocatore selezionato
//     */
//    void onPlayerShipBoardSelected(String playerNickname);
//
//    /**
//     * Chiamato quando viene selezionata la flying board.
//     */
//    void onFlyingBoardSelected();
//
//    /**
//     * Chiamato quando viene selezionata la shipboard del giocatore principale.
//     */
//    void onMyShipBoardSelected();
//
//    /**
//     * Chiamato per mostrare un messaggio temporaneo (opzionale).
//     *
//     * @param message il messaggio da mostrare
//     */
//    default void showMessage(String message) {
//        // Implementazione di default vuota
//    }
}