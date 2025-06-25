package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;

import java.util.*;

public class Level1BoardsController extends BoardsController {

    @FXML
    public Pane pawnsPane;
    @FXML
    public StackPane boardsPane;
    @FXML
    public Button myShipButton, showFlyingBoardButton;
    @FXML
    public HBox shipNavigationBar;
    @FXML
    public GridPane myShipBoardGrid;
    @FXML
    private Button button04_03, button04_04, button04_05, button04_06, button04_07, button04_08, button04_09;
    @FXML
    private Button button05_03, button05_04, button05_05, button05_06, button05_07, button05_08, button05_09;
    @FXML
    private Button button06_03, button06_04, button06_05, button06_06, button06_07, button06_08, button06_09;
    @FXML
    private Button button07_03, button07_04, button07_05, button07_06, button07_07, button07_08, button07_09;
    @FXML
    private Button button08_03, button08_04, button08_05, button08_06, button08_07, button08_08, button08_09;

    @FXML
    public GridPane player2Grid, player3Grid, player4Grid;

    private static final Map<Integer, Pair<Integer, Integer>> flyingBoardRelativePositions = Map.ofEntries(
            Map.entry(Integer.MIN_VALUE, new Pair<>(9, 7)),
            Map.entry(0, new Pair<>(10, 14)),
            Map.entry(1, new Pair<>(8, 20)),
            Map.entry(2, new Pair<>(7, 25)),
            Map.entry(3, new Pair<>(7, 31)),
            Map.entry(4, new Pair<>(8, 37)),
            Map.entry(5, new Pair<>(10, 43)),
            Map.entry(6, new Pair<>(8, 39)),
            Map.entry(7, new Pair<>(20, 50)),
            Map.entry(8, new Pair<>(26, 47)),
            Map.entry(9, new Pair<>(29, 42)),
            Map.entry(10, new Pair<>(31, 36)),
            Map.entry(11, new Pair<>(31, 31)),
            Map.entry(12, new Pair<>(31, 25)),
            Map.entry(13, new Pair<>(30, 19)),
            Map.entry(14, new Pair<>(29, 14)),
            Map.entry(15, new Pair<>(25, 9)),
            Map.entry(16, new Pair<>(19, 6)),
            Map.entry(17, new Pair<>(13, 9))
    );


    @Override
    protected Map<Integer, Pair<Integer, Integer>> getFlyingBoardRelativePositions() {
        return flyingBoardRelativePositions;
    }

    @Override
    public void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel) {
        this.modelFxAdapter = modelFxAdapter;
        this.boardsEventHandler = boardsEventHandler;
        this.clientModel = clientModel;
        initializeButtonMap();
        setupFlyingBoardBinding();
        setupShipBoardNavigationBarAndBoards();
        setupGridBindings(modelFxAdapter.getMyObservableMatrix());
        //setupChangedAttributesBinding();
    }

    public void initialize() {

    }

    // TODO togliere non usati: togliere da qua quelli non usati e dal fxml il fx:id a quelli che non si usano
    @Override
    protected void initializeButtonMap() {

        // Riga 4 del modello -> Riga 0 della griglia UI
        buttonMap.put("4_3", button04_03);   buttonMap.put("4_4", button04_04);   buttonMap.put("4_5", button04_05);
        buttonMap.put("4_6", button04_06);   buttonMap.put("4_7", button04_07);   buttonMap.put("4_8", button04_08);   buttonMap.put("4_9", button04_09);

        // Riga 5 del modello -> Riga 1 della griglia UI
        buttonMap.put("5_3", button05_03);   buttonMap.put("5_4", button05_04);   buttonMap.put("5_5", button05_05);
        buttonMap.put("5_6", button05_06);   buttonMap.put("5_7", button05_07);   buttonMap.put("5_8", button05_08);   buttonMap.put("5_9", button05_09);

        // Riga 6 del modello -> Riga 2 della griglia UI (CENTRO!)
        buttonMap.put("6_3", button06_03);   buttonMap.put("6_4", button06_04);   buttonMap.put("6_5", button06_05);
        buttonMap.put("6_6", button06_06);   buttonMap.put("6_7", button06_07);   buttonMap.put("6_8", button06_08);   buttonMap.put("6_9", button06_09);

        // Riga 7 del modello -> Riga 3 della griglia UI
        buttonMap.put("7_3", button07_03);   buttonMap.put("7_4", button07_04);   buttonMap.put("7_5", button07_05);
        buttonMap.put("7_6", button07_06);   buttonMap.put("7_7", button07_07);   buttonMap.put("7_8", button07_08);   buttonMap.put("7_9", button07_09);

        // Riga 8 del modello -> Riga 4 della griglia UI
        buttonMap.put("8_3", button08_03);   buttonMap.put("8_4", button08_04);   buttonMap.put("8_5", button08_05);
        buttonMap.put("8_6", button08_06);   buttonMap.put("8_7", button08_07);   buttonMap.put("8_8", button08_08);   buttonMap.put("8_9", button08_09);

    }

}
