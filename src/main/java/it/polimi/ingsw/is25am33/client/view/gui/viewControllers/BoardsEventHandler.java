package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

/**
 * Interfaccia per gestire gli eventi provenienti dalla griglia delle shipboard.
 * Permette di separare la logica di gestione degli eventi dal componente riusabile.
 */
public interface BoardsEventHandler {


    /**
     * Chiamato quando viene cliccato un pulsante della griglia principale.
     *
     * @param row la riga del modello (4-8)
     * @param column la colonna del modello (3-9)
     */
    void onGridButtonClick(int row, int column);

}