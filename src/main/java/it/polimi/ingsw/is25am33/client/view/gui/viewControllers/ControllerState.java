package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

public enum ControllerState {
    START_CONTROLLER (0, "/gui/StartView.fxml", "StartViewController"),
    MAIN_MENU_CONTROLLER (1, "/gui/MainMenuView.fxml", "MainMenuViewController"),
    BUILD_SHIPBOARD_CONTROLLER (2, "/gui/BuildShipBoardView.fxml", "BuildAndCheckShipBoardController"),
    CARD_PHASE_CONTROLLER (3, "/gui/CardPhaseView.fxml", "CardPhaseController"),
    END_GAME_CONTROLLER (4, "/gui/EndGameView.fxml", "EndGameController");

    private final int order;
    private final String fxmlPath;
    private final String controllerTypeName;

    ControllerState(int order, String fxmlPath, String controllerTypeName) {
        this.order = order;
        this.fxmlPath = fxmlPath;
        this.controllerTypeName = controllerTypeName;
    }

    /**
     * Gets the numerical order of the controller state.
     *
     * @return the order value of this controller state
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gets the FXML file path associated with this controller state.
     *
     * @return the FXML file path as a String
     */
    public String getFxmlPath() {
        return fxmlPath;
    }

    /**
     * Converts a controller type name to its corresponding ControllerState enum value.
     *
     * @param controllerType the name of the controller type to convert
     * @return the matching ControllerState enum value
     * @throws IllegalArgumentException if no matching controller type is found
     */
    public static ControllerState fromString(String controllerType) {
        for (ControllerState state : values()) {
            if (state.controllerTypeName.equals(controllerType)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown controller type: " + controllerType);
    }
}