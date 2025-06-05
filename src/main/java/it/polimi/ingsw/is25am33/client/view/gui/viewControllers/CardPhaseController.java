package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.view.gui.ClientGuiController;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class CardPhaseController extends GuiController implements BoardsEventHandler{

    @FXML
    public StackPane centerStackPane;

    private Level2BoardsController boardsController;
    private ModelFxAdapter modelFxAdapter;

//    public void initialize() {
//
//        // loading boards from a different fxml file
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Level2Boards.fxml"));
//            VBox mainBoardBox = loader.load();
//            centerStackPane.getChildren().addFirst(mainBoardBox);
//            this.boardsController = loader.getController();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error loading boards: " + e.getMessage());
//        }
//
//        //borderPane.setVisible(true);
//        modelFxAdapter = new ModelFxAdapter(clientModel);
//
//        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);
//
//        // initial shipboards refresh
//        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
//        clientModel.getPlayerClientData().keySet().forEach(nickname -> modelFxAdapter.refreshShipBoardOf(nickname));
//    }

    @Override
    public void onGridButtonClick(int row, int column) {

    }

    public ModelFxAdapter getModelFxAdapter() {
        return modelFxAdapter;
    }

    public void initialize() {
        // Caricamento delle board
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Level2Boards.fxml"));
            VBox mainBoardBox = loader.load();
            centerStackPane.getChildren().addFirst(mainBoardBox);
            this.boardsController = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading boards: " + e.getMessage());
        }

        // Prova a utilizzare un ModelFxAdapter condiviso
        ClientGuiController guiController = ClientGuiController.getInstance();
        if (guiController != null) {
            modelFxAdapter = guiController.getSharedModelFxAdapter();
        } else {
            // Fallback: crea un nuovo adapter
            modelFxAdapter = new ModelFxAdapter(clientModel);
        }

        // Binding e setup
        this.boardsController.bindBoards(modelFxAdapter, this, clientModel);

        // Refresh iniziale
        modelFxAdapter.refreshShipBoardOf(clientModel.getMyNickname());
        clientModel.getPlayerClientData().keySet().forEach(nickname ->
                modelFxAdapter.refreshShipBoardOf(nickname));
    }

}
