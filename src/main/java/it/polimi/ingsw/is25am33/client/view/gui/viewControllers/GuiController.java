package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.ClientModel;
import it.polimi.ingsw.is25am33.client.controller.ClientController;

public abstract class GuiController {

    protected static ClientController clientController;
    protected static ClientModel clientModel;

    public static void setClientController(ClientController clientController) {
        GuiController.clientController = clientController;
    }

    public static void setClientModel(ClientModel clientModel) {
        GuiController.clientModel = clientModel;
    }

}
