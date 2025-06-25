package it.polimi.ingsw.is25am33.client.view.gui.viewControllers;

import it.polimi.ingsw.is25am33.client.model.ClientModel;
import it.polimi.ingsw.is25am33.client.model.PlayerClientData;
import it.polimi.ingsw.is25am33.client.view.gui.ModelFxAdapter;
import it.polimi.ingsw.is25am33.model.board.Coordinates;
import it.polimi.ingsw.is25am33.model.component.*;
import it.polimi.ingsw.is25am33.model.enumFiles.CargoCube;
import it.polimi.ingsw.is25am33.model.enumFiles.CrewMember;
import it.polimi.ingsw.is25am33.model.enumFiles.PlayerColor;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.IntStream;

public abstract class BoardsController {

    @FXML public Button myShipButton, showFlyingBoardButton;
    @FXML public HBox shipNavigationBar;
    @FXML private StackPane flyingBoard;
    @FXML public GridPane flyingBoardGrid;
    @FXML private StackPane mainPlayerShipBoard, player2ShipBoard, player3ShipBoard, player4ShipBoard;
    @FXML public GridPane myShipBoardGrid;
    public ImageView redPawn, greenPawn, bluePawn, yellowPawn;

    @FXML private ImageView p2_img04_08, p2_img04_09;
    @FXML private ImageView p3_img04_08, p3_img04_09;
    @FXML private ImageView p4_img04_08, p4_img04_09;
    final int FIXED_COMPONENT_LENGTH = 70;

    protected ModelFxAdapter modelFxAdapter;
    protected BoardsEventHandler boardsEventHandler;
    protected ClientModel clientModel;

    private final Map<String, StackPane> otherPlayersShipBoards = new HashMap<>();
    private final Set<Button> navButtons = new HashSet<>();
    protected final Map<String, Button> buttonMap = new HashMap<>();
    private final Set<Button> shadowedButtons = new HashSet<>();
    private final Object highlightLock = new Object();

    abstract void bindBoards(ModelFxAdapter modelFxAdapter, BoardsEventHandler boardsEventHandler, ClientModel clientModel);

    protected abstract void initializeButtonMap();

    protected abstract Map<Integer, Pair<Integer, Integer>> getFlyingBoardRelativePositions();

    public void removeHighlightColor() {
        Set<Button> buttonsToRemove;

        // Crea una copia del set per evitare ConcurrentModificationException
        synchronized (highlightLock) {
            buttonsToRemove = new HashSet<>(shadowedButtons);
            shadowedButtons.clear(); // Pulisci il set originale
        }

        // Processa i button fuori dalla sincronizzazione
        buttonsToRemove.forEach(button ->
            Platform.runLater(() -> {
                // Non serve più sincronizzazione qui poiché abbiamo già pulito il set
                button.setEffect(null);
                button.getStyleClass().remove("no-hover");
            })
        );
    }


    private String fromCoordsToButtonId(Coordinates coords) {
        return coords.getX() + "_" + coords.getY();
    }

//    public void applyHighlightEffect(Coordinates coordinates, Color color) {
//
//        String buttonId = fromCoordsToButtonId(coordinates);
//        Button button = buttonMap.get(buttonId);
//        shadowedButtons.add(button);
//
//        Platform.runLater(() -> {
//
//            DropShadow shadow = new DropShadow();
//            shadow.setColor(color);
//            shadow.setRadius(10);
//            shadow.setSpread(0.5);
//            button.setEffect(shadow);
//
//            if (!button.getStyleClass().contains("no-hover")) {
//                button.getStyleClass().add("no-hover");
//            }
//        });
//    }

    public void applyHighlightEffect(Coordinates coordinates, Color color) {
        String buttonId = fromCoordsToButtonId(coordinates);
        Button button = buttonMap.get(buttonId);

        if (button == null) {
            return; // Controllo di sicurezza
        }

        // Aggiungi al set in modo thread-safe
        synchronized (highlightLock) {
            shadowedButtons.add(button);
        }

        // Applica l'effetto sul JavaFX thread
        Platform.runLater(() -> {
            DropShadow shadow = new DropShadow();
            shadow.setColor(color);
            shadow.setRadius(10);
            shadow.setSpread(0.5);
            button.setEffect(shadow);

            if (!button.getStyleClass().contains("no-hover")) {
                button.getStyleClass().add("no-hover");
            }
        });
    }

    protected StackPane getNodeFromGridPane(GridPane gridPane, int row, int column) {
        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            // Default values are 0 if null
            if ((rowIndex == null ? 0 : rowIndex) == row && (colIndex == null ? 0 : colIndex) == column) {
                return (StackPane) node;
            }
        }
        return null; // Nessun nodo trovato in quella posizione
    }

    protected void createPaws() {
        final int PAWNS_WIDTH = 30;
        Map<String, PlayerClientData> playersData = clientModel.getPlayerClientData();
        playersData.keySet()
                .forEach(player -> {

                    PlayerColor color = playersData.get(player).getColor();
                    switch (color) {
                        case RED:
                            redPawn = new ImageView(new Image(Objects.requireNonNull(getClass()
                                    .getResourceAsStream("/gui/graphics/pawns/shuttle-red.png"))));
                            redPawn.setFitWidth(PAWNS_WIDTH);
                            redPawn.setPreserveRatio(true);
                            applyShadowEffect(redPawn);
                            break;
                        case GREEN:
                            greenPawn = new ImageView(new Image(Objects.requireNonNull(getClass()
                                    .getResourceAsStream("/gui/graphics/pawns/shuttle-green.png"))));
                            greenPawn.setFitWidth(PAWNS_WIDTH);
                            greenPawn.setPreserveRatio(true);
                            applyShadowEffect(greenPawn);
                            break;
                        case YELLOW:
                            yellowPawn = new ImageView(new Image(Objects.requireNonNull(getClass()
                                    .getResourceAsStream("/gui/graphics/pawns/shuttle-yellow.png"))));
                            yellowPawn.setFitWidth(PAWNS_WIDTH);
                            yellowPawn.setPreserveRatio(true);
                            applyShadowEffect(yellowPawn);
                            break;
                        case BLUE:
                            bluePawn = new ImageView(new Image(Objects.requireNonNull(getClass()
                                    .getResourceAsStream("/gui/graphics/pawns/shuttle-blue.png"))));
                            bluePawn.setFitWidth(PAWNS_WIDTH);
                            bluePawn.setPreserveRatio(true);
                            applyShadowEffect(bluePawn);
                            break;
                    }
                });
    }

    private void applyShadowEffect(ImageView image) {
        DropShadow dropShadow = new DropShadow();

        dropShadow.setColor(Color.BLACK);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setSpread(0.6);
        dropShadow.setRadius(15);

        image.setEffect(dropShadow);
    }

    @FXML
    protected void handleShowFlyingBoardButton() {
        Platform.runLater(() -> {
            otherPlayersShipBoards.forEach((_, shipboardStackPane) -> shipboardStackPane.setVisible(false));
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().add("active");
            myShipButton.getStyleClass().remove("active");
            mainPlayerShipBoard.setVisible(false);
            flyingBoard.setVisible(true);
            mainPlayerShipBoard.setVisible(false);
        });
    }

    protected void setupFlyingBoardBinding() {

        clientModel.getColorRanking()
                .forEach((playerColor, position) -> {
                    modelFxAdapter.getObservableColorRanking().put(playerColor, new SimpleObjectProperty<>());
                });

        // Positioning of the spaceships on the flying board
        modelFxAdapter.getObservableColorRanking()
                .forEach((color, position) -> {
                    position.addListener((_, _, newVal) -> Platform.runLater(() -> {
                        int newValueMod = Math.floorMod(
                                newVal,
                                this instanceof Level2BoardsController ? 24 : 18
                        );
                        int x = getFlyingBoardRelativePositions().get(newValueMod).getValue();
                        int y = getFlyingBoardRelativePositions().get(newValueMod).getKey();

                        switch (color) {
                            case RED:
                                updatePawnPositions(redPawn, x, y);
                                break;
                            case GREEN:
                                updatePawnPositions(greenPawn, x, y);
                                break;
                            case BLUE:
                                updatePawnPositions(bluePawn, x, y);
                                break;
                            case YELLOW:
                                updatePawnPositions(yellowPawn, x, y);
                                break;
                        }
                        printGridPaneContent(flyingBoardGrid);
                    }));
                });
    }

    private void updatePawnPositions(ImageView pawn, int newX, int newY) {
        if (pawn.getParent() != null)
            flyingBoardGrid.getChildren().remove(pawn);

        flyingBoardGrid.add(pawn, newX, newY);
        GridPane.setHalignment(pawn, HPos.CENTER);
        GridPane.setValignment(pawn, VPos.CENTER);
        pawn.setVisible(true);
    }

    private void printGridPaneContent(GridPane gridPane) {
        System.out.println("Contenuto del GridPane:");

        if (gridPane.getChildren().isEmpty()) {
            System.out.println("  GridPane vuoto");
            return;
        }

        for (Node child : gridPane.getChildren()) {
            int row = GridPane.getRowIndex(child) != null ? GridPane.getRowIndex(child) : 0;
            int col = GridPane.getColumnIndex(child) != null ? GridPane.getColumnIndex(child) : 0;
            int rowSpan = GridPane.getRowSpan(child) != null ? GridPane.getRowSpan(child) : 1;
            int colSpan = GridPane.getColumnSpan(child) != null ? GridPane.getColumnSpan(child) : 1;

            System.out.printf("  Cella (%d,%d) span(%d,%d): %s%n",
                    row, col, rowSpan, colSpan, child.getClass().getSimpleName());
        }
    }

    protected void setupShipBoardNavigationBarAndBoards() {

        Map<StackPane, Pair<ImageView, ImageView>> shipBoardsAndReservedComponents = Map.of(
                player2ShipBoard, new Pair<>(p2_img04_08, p2_img04_09),
                player3ShipBoard, new Pair<>(p3_img04_08, p3_img04_09),
                player4ShipBoard, new Pair<>(p4_img04_08, p4_img04_09)
        );

        Iterator<StackPane> stackPaneIterator = shipBoardsAndReservedComponents.keySet().iterator();

        navButtons.add(myShipButton);

        clientModel.getPlayerClientData()
                .keySet()
                .forEach(nickname -> {
                    // my ship board is loaded by default
                    if (nickname.equals(clientModel.getMyNickname()))
                        return;

                    StackPane playerStackPane = stackPaneIterator.next();
                    otherPlayersShipBoards.put(nickname, playerStackPane);

                    // other players ship board binding
                    ObjectProperty<Component>[][] observableMatrix = modelFxAdapter.getObservableShipBoardOf(nickname);
                    for (int row = 4; row <= 8; row++) {
                        for (int column = 3; column <= 9; column++) {
                            int finalColumn = column;
                            int finalRow = row;
                            observableMatrix[row][column].addListener((_, _, newVal) ->
                                    Platform.runLater(() -> updateOtherShipBoardsAppearance(playerStackPane, newVal, finalRow, finalColumn)
                                    ));
                        }
                    }

                    if (this instanceof Level2BoardsController)
                        ((Level2BoardsController) this).setupOthersBookedComponentBinding(nickname, shipBoardsAndReservedComponents.get(playerStackPane));

                    // navigation button binding
                    setUpNavigationButton(nickname);
                });

    }

    private void setUpNavigationButton(String nickname) {
        Button shipBoardButton = new Button();
        navButtons.add(shipBoardButton);
        shipBoardButton.getStyleClass().add("ship-nav-button");
        shipBoardButton.setText(nickname);
        shipNavigationBar.getChildren().add(shipBoardButton);

        shipBoardButton.setOnAction(_ -> Platform.runLater(() -> {
            flyingBoard.setVisible(false);
            otherPlayersShipBoards.forEach((_, shipboardStackPane1) -> shipboardStackPane1.setVisible(false));
            otherPlayersShipBoards.get(nickname).setVisible(true);
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().remove("active");
            mainPlayerShipBoard.setVisible(false);
            enableMyShipBoardView(false);
            shipBoardButton.getStyleClass().add("active");
        }));
    }

    private void enableMyShipBoardView(boolean enable) {
        Platform.runLater(() -> {
            if (enable) {
                myShipBoardGrid.setManaged(true);
                myShipBoardGrid.setOpacity(1);
            } else {
                myShipBoardGrid.setManaged(false);
                myShipBoardGrid.setOpacity(0.1);
            }
        });
    }

    @FXML
    public void handleMyShipBoardButton() {
        Platform.runLater(() -> {
            flyingBoard.setVisible(false);
            otherPlayersShipBoards.forEach((_, shipboardStackPane) -> shipboardStackPane.setVisible(false));
            navButtons.forEach(button -> button.getStyleClass().remove("active"));
            showFlyingBoardButton.getStyleClass().remove("active");
            myShipButton.getStyleClass().add("active");
            mainPlayerShipBoard.setVisible(true);
            enableMyShipBoardView(true);
        });
    }

    @FXML
    public void handleGridButtonClick(ActionEvent actionEvent) {
        Button clickedButton = (Button) actionEvent.getSource();
        String id = clickedButton.getId();

        // Parsing corretto dell'ID del pulsante
        String[] parts = id.replace("button", "").split("_");
        int row = Integer.parseInt(parts[0]);
        int column = Integer.parseInt(parts[1]);

        boardsEventHandler.onGridButtonClick(row, column);
    }

    public void updateShipBoards(String nickname, int row, int column, Component newComponent) {
        if (nickname.equals(clientModel.getMyNickname())) {
            String buttonId = fromCoordsToButtonId(new Coordinates(row, column));
            Button button = buttonMap.get(buttonId);
            Platform.runLater(() -> updateButtonAppearance(button, newComponent));
        } else {
            StackPane playerStackPane = otherPlayersShipBoards.get(nickname);
            Platform.runLater(() -> updateOtherShipBoardsAppearance(playerStackPane, newComponent, row, column));
        }
    }

    private void updateOtherShipBoardsAppearance(StackPane playerStackPane, Component newVal, int row, int column) {
        try {
            GridPane gridPane = (GridPane) playerStackPane.getChildren().get(1);

            // Remove existing node first to avoid accumulation of old components
            Node existingNode = getNodeFromGridPane(gridPane, row - 4, column - 3);
            if (existingNode != null) {
                gridPane.getChildren().remove(existingNode);
            }

            if (newVal != null) {
                // Create new ImageView for the component
                ImageView imageView = new ImageView();
                String componentFile = newVal.toString().split("\\n")[0];
                Image image = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/gui/graphics/component/" + componentFile)));
                imageView.setImage(image);
                imageView.setRotate(newVal.getRotation() * 90);

                imageView.setFitWidth(FIXED_COMPONENT_LENGTH);
                imageView.setFitHeight(FIXED_COMPONENT_LENGTH);
                imageView.setPreserveRatio(true);

                // Create updated StackPane with all necessary images (component + features)
                StackPane newStackPane = getUpdatedStackPaneWithImages(imageView, newVal);

                // Set position and add the new StackPane
                GridPane.setRowIndex(newStackPane, row - 4);
                GridPane.setColumnIndex(newStackPane, column - 3);
                gridPane.getChildren().add(newStackPane);
            }
            // If newVal is null, we've already removed the existing node above,
            // so the cell will be empty (which is correct)

        } catch (Exception e) {
            System.err.println("Error updating component appearance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateButtonAppearance(Button button, Component component) {

        if (component != null) {
            try {
                String fileName = component.getImageName();
                Image img = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/gui/graphics/component/" + fileName)));
                ImageView imgView = new ImageView(img);
                button.setVisible(true);
                button.setManaged(true);
                imgView.setFitWidth(FIXED_COMPONENT_LENGTH);
                imgView.setFitHeight(FIXED_COMPONENT_LENGTH);
                imgView.setPreserveRatio(true);
                imgView.setRotate(component.getRotation() * 90);
                StackPane stackPane = getUpdatedStackPaneWithImages(imgView, component);
                button.setGraphic(stackPane);
            } catch (Exception e) {
                System.err.println("Error updating button appearance: " + e.getMessage());
                // Fallback: show at least a visual indicator
                button.setStyle("-fx-background-color: yellow;");
            }
        } else {
            // Apply destruction effect before clearing the button
            applyDestructionEffect(button, () -> {
                // This callback will be executed after the destruction animation completes
                button.setGraphic(null);
                button.setStyle("-fx-background-color: transparent;");
                // Ensure button is still visible and managed for potential future components
                button.setVisible(true);
                button.setManaged(true);
            });
        }
    }

    private void applyDestructionEffect(Button button, Runnable onComplete) {
        if (button.getGraphic() == null) {
            // No graphic to destroy, just run the completion callback
            onComplete.run();
            return;
        }

        // Create destruction visual effects
        Node graphic = button.getGraphic();

        // Create multiple destruction particles
        List<ImageView> particles = createDestructionParticles();

        // Add particles to the button's graphic (if it's a StackPane)
        StackPane destructionPane;
        if (graphic instanceof StackPane) {
            destructionPane = (StackPane) graphic;
        } else {
            // Wrap the existing graphic in a StackPane
            destructionPane = new StackPane(graphic);
            button.setGraphic(destructionPane);
        }

        // Add particles to the destruction pane
        particles.forEach(particle -> destructionPane.getChildren().add(particle));

        // Create and configure destruction animations
        javafx.animation.Timeline destructionTimeline = new javafx.animation.Timeline();

        // Animate the original component (fade out and scale down)
        if (graphic instanceof StackPane && !((StackPane) graphic).getChildren().isEmpty()) {
            Node originalComponent = ((StackPane) graphic).getChildren().get(0);

            javafx.animation.KeyFrame fadeOut = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(300),
                    new javafx.animation.KeyValue(originalComponent.opacityProperty(), 0.0),
                    new javafx.animation.KeyValue(originalComponent.scaleXProperty(), 0.8),
                    new javafx.animation.KeyValue(originalComponent.scaleYProperty(), 0.8)
            );
            destructionTimeline.getKeyFrames().add(fadeOut);
        }

        // Animate particles (scatter and fade)
        for (int i = 0; i < particles.size(); i++) {
            ImageView particle = particles.get(i);
            double angle = (360.0 / particles.size()) * i;
            double distance = 30 + Math.random() * 20; // Random scatter distance

            double targetX = Math.cos(Math.toRadians(angle)) * distance;
            double targetY = Math.sin(Math.toRadians(angle)) * distance;

            javafx.animation.KeyFrame particleAnimation = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(400),
                    new javafx.animation.KeyValue(particle.translateXProperty(), targetX),
                    new javafx.animation.KeyValue(particle.translateYProperty(), targetY),
                    new javafx.animation.KeyValue(particle.opacityProperty(), 0.0),
                    new javafx.animation.KeyValue(particle.rotateProperty(), 360 + Math.random() * 360)
            );
            destructionTimeline.getKeyFrames().add(particleAnimation);
        }

        // Add screen shake effect to the button
        applyScreenShakeEffect(button);

        // When animation completes, clean up and run callback
        destructionTimeline.setOnFinished(event -> {
            Platform.runLater(() -> {
                // Remove all particles
                particles.forEach(particle -> destructionPane.getChildren().remove(particle));
                onComplete.run();
            });
        });

        destructionTimeline.play();
    }

    private List<ImageView> createDestructionParticles() {
        List<ImageView> particles = new ArrayList<>();

        for (int i = 0; i < 8; i++) { // Create 8 particles
            ImageView particle = createProceduralParticle(i);

            // Set particle properties
            particle.setFitWidth(6 + Math.random() * 6); // Random size between 6-12
            particle.setFitHeight(6 + Math.random() * 6);
            particle.setPreserveRatio(true);

            // Initial position (center)
            particle.setTranslateX(0);
            particle.setTranslateY(0);

            // Add glow effect
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow();
            glow.setLevel(0.7);
            particle.setEffect(glow);

            particles.add(particle);
        }

        return particles;
    }

    private ImageView createProceduralParticle(int index) {
        // Create particles with different shapes and colors
        double size = 8 + Math.random() * 4; // Random size between 8-12
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // Different particle types
        Color[] sparkColors = {Color.ORANGE, Color.YELLOW, Color.WHITE, Color.LIGHTYELLOW};
        Color[] debrisColors = {Color.GRAY, Color.DARKGRAY, Color.BROWN, Color.DARKRED};

        if (index < 4) {
            // Create spark particles (bright colors, star-like shape)
            gc.setFill(sparkColors[index % sparkColors.length]);

            // Draw star-like spark
            double centerX = size / 2;
            double centerY = size / 2;
            double radius = size / 3;

            // Draw multiple overlapping circles for spark effect
            gc.fillOval(centerX - radius/2, centerY - radius/2, radius, radius);
            gc.setFill(Color.WHITE);
            gc.fillOval(centerX - radius/4, centerY - radius/4, radius/2, radius/2);

            // Add spark rays
            gc.setStroke(sparkColors[index % sparkColors.length]);
            gc.setLineWidth(1);
            gc.strokeLine(0, centerY, size, centerY); // Horizontal ray
            gc.strokeLine(centerX, 0, centerX, size); // Vertical ray

        } else {
            // Create debris particles (darker colors, irregular shapes)
            gc.setFill(debrisColors[index % debrisColors.length]);

            // Draw irregular debris shape
            double[] xPoints = new double[6];
            double[] yPoints = new double[6];

            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + Math.random() * 0.5;
                double distance = (size / 3) + Math.random() * (size / 6);
                xPoints[i] = (size / 2) + Math.cos(angle) * distance;
                yPoints[i] = (size / 2) + Math.sin(angle) * distance;
            }

            gc.fillPolygon(xPoints, yPoints, 6);

            // Add some texture to debris
            gc.setFill(debrisColors[index % debrisColors.length].darker());
            gc.fillOval(size/3, size/3, size/6, size/6);
        }

        // Convert canvas to image
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        Image particleImage = canvas.snapshot(params, null);

        return new ImageView(particleImage);
    }

    private void applyScreenShakeEffect(Button button) {
        // Store original position
        double originalX = button.getTranslateX();
        double originalY = button.getTranslateY();

        // Create shake animation
        javafx.animation.Timeline shakeTimeline = new javafx.animation.Timeline();

        // Create multiple shake keyframes
        for (int i = 0; i < 6; i++) {
            double shakeX = (Math.random() - 0.5) * 4; // Random shake between -2 and 2
            double shakeY = (Math.random() - 0.5) * 4;

            javafx.animation.KeyFrame shakeFrame = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(50 * i),
                    new javafx.animation.KeyValue(button.translateXProperty(), originalX + shakeX),
                    new javafx.animation.KeyValue(button.translateYProperty(), originalY + shakeY)
            );
            shakeTimeline.getKeyFrames().add(shakeFrame);
        }

        // Return to original position
        javafx.animation.KeyFrame returnFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(300),
                new javafx.animation.KeyValue(button.translateXProperty(), originalX),
                new javafx.animation.KeyValue(button.translateYProperty(), originalY)
        );
        shakeTimeline.getKeyFrames().add(returnFrame);

        shakeTimeline.play();
    }

    protected void setupGridBindings(ObjectProperty<Component>[][] observableMatrix) {

        for (Map.Entry<String, Button> entry : buttonMap.entrySet()) {
            String[] coords = entry.getKey().split("_");
            int modelRow = Integer.parseInt(coords[0]);
            int modelCol = Integer.parseInt(coords[1]);
            Button button = entry.getValue();
            ObjectProperty<Component> cellProperty = observableMatrix[modelRow][modelCol];

            cellProperty.addListener((_, _, newVal) ->
                    Platform.runLater(() ->
                            updateButtonAppearance(button, newVal)
                    ));

            Component initialComponent = cellProperty.get();
            if (initialComponent != null)
                Platform.runLater(() -> updateButtonAppearance(button, initialComponent));
        }
    }

    private StackPane getUpdatedStackPaneWithImages(ImageView componentImageView, Component component) {

        switch (component) {
            case Cabin cabin -> {
                return getCabinStackPane(componentImageView, cabin);
            }
            case BatteryBox batteryBox -> {
                return getBatteryBoxStackPane(componentImageView, batteryBox);
            }
            case Storage storage -> {
                return getStorageStackPane(componentImageView, storage);
            }
            case null, default -> {
                return new StackPane(componentImageView);
            }
        }

    }

    private StackPane getCabinStackPane(ImageView cabinImageView, Cabin cabin) {

        List<CrewMember> crewMembers = cabin.getInhabitants();

        ImageView featureImageView = new ImageView();
        Image featureImage;
        applyShadowEffect(featureImageView);
        featureImageView.setFitWidth(40);
        featureImageView.setFitHeight(40);
        featureImageView.setPreserveRatio(true);

        if (crewMembers.isEmpty()) {
            return new StackPane(cabinImageView);
        } else if (crewMembers.contains(CrewMember.PURPLE_ALIEN)) {
            featureImage = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/componentFeature/purple_alien.png")));
            featureImageView.setImage(featureImage);
            return new StackPane(cabinImageView, featureImageView);
        } else if (crewMembers.contains(CrewMember.BROWN_ALIEN)) {
            featureImage = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/componentFeature/brown_alien.png")));
            featureImageView.setImage(featureImage);
            return new StackPane(cabinImageView, featureImageView);
        } else if (crewMembers.size() == 1) {
            featureImage = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/componentFeature/human.png")));
            featureImageView.setImage(featureImage);
            return new StackPane(cabinImageView, featureImageView);
        } else {

            ImageView featureImageView1 = new ImageView();
            Image featureImage1;

            applyShadowEffect(featureImageView1);
            featureImageView1.setFitWidth(40);
            featureImageView1.setFitHeight(40);
            featureImageView1.setPreserveRatio(true);

            featureImage = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/componentFeature/human.png")));

            featureImage1 = new Image(Objects.requireNonNull(getClass()
                    .getResourceAsStream("/gui/graphics/componentFeature/human.png")));

            featureImageView.setImage(featureImage);
            featureImageView1.setImage(featureImage1);

            featureImageView.setTranslateX(-10);   // left
            featureImageView1.setTranslateX(10);    // right

            return new StackPane(cabinImageView, featureImageView1, featureImageView);
        }
    }

    private StackPane getBatteryBoxStackPane(ImageView batteryImageView, BatteryBox batteryBox) {

        final int BATTERY_BOX_WIDTH = 30;
        int remainingBatteries = batteryBox.getRemainingBatteries();

        ImageView featureImageView = new ImageView();
        Image featureImage;
        applyShadowEffect(featureImageView);
        featureImageView.setFitWidth(BATTERY_BOX_WIDTH);
        featureImageView.setFitHeight(BATTERY_BOX_WIDTH);
        featureImageView.setPreserveRatio(true);

        featureImage = new Image(Objects.requireNonNull(getClass()
                .getResourceAsStream("/gui/graphics/componentFeature/battery.png")));

        if (remainingBatteries == 0) {
            return new StackPane(batteryImageView);
        } else if (remainingBatteries == 1) {
            featureImageView.setImage(featureImage);
            return new StackPane(batteryImageView, featureImageView);
        } else if (remainingBatteries == 2) {
            ImageView featureImageView1 = new ImageView();

            applyShadowEffect(featureImageView1);
            featureImageView1.setFitWidth(BATTERY_BOX_WIDTH);
            featureImageView1.setFitHeight(BATTERY_BOX_WIDTH);
            featureImageView1.setPreserveRatio(true);

            featureImageView1.setImage(featureImage);
            featureImageView.setImage(featureImage);

            featureImageView.setTranslateY(-10);   // down
            featureImageView1.setTranslateY(10);    // up

            return new StackPane(batteryImageView, featureImageView1, featureImageView);
        } else {
            ImageView featureImageView1 = new ImageView();
            ImageView featureImageView2 = new ImageView();

            applyShadowEffect(featureImageView1);
            featureImageView1.setFitWidth(BATTERY_BOX_WIDTH);
            featureImageView1.setFitHeight(BATTERY_BOX_WIDTH);
            featureImageView1.setPreserveRatio(true);
            applyShadowEffect(featureImageView2);
            featureImageView2.setFitWidth(BATTERY_BOX_WIDTH);
            featureImageView2.setFitHeight(BATTERY_BOX_WIDTH);
            featureImageView2.setPreserveRatio(true);

            featureImageView2.setImage(featureImage);
            featureImageView1.setImage(featureImage);
            featureImageView.setImage(featureImage);

            featureImageView.setTranslateY(-10);
            featureImageView.setTranslateX(10);
            featureImageView1.setTranslateY(10);
            featureImageView2.setTranslateY(-10);
            featureImageView2.setTranslateX(-10);

            return new StackPane(batteryImageView, featureImageView2, featureImageView1, featureImageView);
        }
    }

    private StackPane getStorageStackPane(ImageView storageImageView, Storage storage) {

        List<CargoCube> cargoCubes = storage.getStockedCubes();
        StackPane storageStackPane = new StackPane(storageImageView);

        // FIXED RANGE: from 1 to list size (inclusive)
        IntStream.range(1, cargoCubes.size() + 1)
                .forEach(i -> {

                    ImageView featureImageView = new ImageView();
                    Image featureImage = null;
                    applyShadowEffect(featureImageView);
                    featureImageView.setFitWidth(20);
                    featureImageView.setFitHeight(20);
                    featureImageView.setPreserveRatio(true);

                    // Load image based on a cube type
                    switch (cargoCubes.get(i - 1)) {
                        case RED -> featureImage = new Image(Objects.requireNonNull(getClass()
                                .getResourceAsStream("/gui/graphics/componentFeature/red_cargo_cube.png")));
                        case GREEN -> featureImage = new Image(Objects.requireNonNull(getClass()
                                .getResourceAsStream("/gui/graphics/componentFeature/green_cargo_cube.png")));
                        case BLUE -> featureImage = new Image(Objects.requireNonNull(getClass()
                                .getResourceAsStream("/gui/graphics/componentFeature/blue_cargo_cube.png")));
                        case YELLOW -> featureImage = new Image(Objects.requireNonNull(getClass()
                                .getResourceAsStream("/gui/graphics/componentFeature/yellow_cargo_cube.png")));
                    }

                    // POSITIONING LOGIC BASED ON TOTAL CUBE COUNT
                    switch (cargoCubes.size()) {
                        case 1:
                            // Single cube: no translation (centered)
                            break;
                        case 2:
                            // Two cubes: one left, one right
                            switch (i) {
                                case 1:
                                    featureImageView.setTranslateX(-10);
                                    featureImageView.setTranslateY(0);
                                    break;
                                case 2:
                                    featureImageView.setTranslateX(10);
                                    featureImageView.setTranslateY(0);
                                    break;
                            }
                            break;
                        case 3:
                            // Three cubes: triangular layout
                            switch (i) {
                                case 1:
                                    featureImageView.setTranslateX(-10);
                                    featureImageView.setTranslateY(-5);
                                    break;
                                case 2:
                                    featureImageView.setTranslateX(10);
                                    featureImageView.setTranslateY(-5);
                                    break;
                                case 3:
                                    featureImageView.setTranslateX(0);
                                    featureImageView.setTranslateY(5);
                                    break;
                            }
                            break;
                    }

                    featureImageView.setImage(featureImage);
                    storageStackPane.getChildren().add(featureImageView);
                });

        return storageStackPane;
    }

//    protected void setupChangedAttributesBinding() {
//        modelFxAdapter.getObservableChangedAttributesProperty()
//                .addListener((_, _, newValue) -> {
//                    String nickname = newValue.getKey();
//                    Coordinates coords = newValue.getValue();
//                    Component updatedComponent = clientModel.getShipboardOf(nickname).getShipMatrix()[coords.getX()][coords.getY()];
//
//                    if (nickname.equals(clientModel.getMyNickname())) {
//                        Button button = buttonMap.get(fromCoordsToButtonId(coords));
//                        Platform.runLater(() -> updateButtonAppearance(button, updatedComponent));
//                    } else {
//                        StackPane playerStackPane = otherPlayersShipBoards.get(nickname);
//                        Platform.runLater(() -> updateOtherShipBoardsAppearance(playerStackPane, updatedComponent, coords.getX(), coords.getY()));
//                    }
//                });
//    }


}
