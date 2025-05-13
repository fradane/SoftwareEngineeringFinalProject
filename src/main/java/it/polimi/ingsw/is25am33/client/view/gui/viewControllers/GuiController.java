package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.controller.ClientController;

public abstract class GuiController {

    protected static ClientController clientController;

    public static void setClientController(ClientController clientController) {
        GuiController.clientController = clientController;
    }

    public static ClientController getClientController() {
        return clientController;
    }

}
