// ------------------------------------------------------------
// Package: visualization
// File: JavaFXSimulationVisualizer.java
package Visualization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A single-file JavaFX visualizer that expects the Recorder JSON (list of Step objects with NodeState entries).
 *
 * - Draws static edges using neighborIds from the first step (so edges never disappear).
 * - Colors nodes by their color integer.
 * - Highlights the current node (vCurId) and draws a purple parent edge when parent != -1.
 * - Shows the active color in the UI.
 */
public class JavaFXSimulationVisualizer extends Application {
    private List<Step> steps = new ArrayList<>();
    private Map<Integer, VisualNode> visualNodes = new HashMap<>();
    private Pane graphPane;
    private Text stepLabel;
    private Rectangle activeColorIndicator;
    private Text activeColorLabel;
    private Timeline player;
    private int stepIndex = 0;

    private Slider scrubSlider;
    private Slider speedSlider;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        graphPane = new Pane();
        graphPane.setPrefSize(900, 700);
        graphPane.setStyle("-fx-background-color: white;");

        root.setCenter(graphPane);

        // Controls
        Button loadBtn = new Button("Load JSON");
        Button prevBtn = new Button("<< Prev");
        Button playBtn = new Button("Play");
        Button nextBtn = new Button("Next >>");

        stepLabel = new Text("No data loaded");
        
        // Active color indicator
        activeColorIndicator = new Rectangle(30, 30);
        activeColorIndicator.setStroke(Color.BLACK);
        activeColorIndicator.setStrokeWidth(2);
        activeColorLabel = new Text("Active Color:");

        scrubSlider = new Slider(0, 1, 0);
        scrubSlider.setPrefWidth(300);

        speedSlider = new Slider(0.1, 3.0, 1.0);
        speedSlider.setPrefWidth(120);

        HBox controls = new HBox(10, loadBtn, prevBtn, playBtn, nextBtn,
                new Label("Speed:"), speedSlider,
                new Label("Step:"), scrubSlider, stepLabel,
                activeColorLabel, activeColorIndicator);
        controls.setPadding(new Insets(8));

        root.setBottom(controls);

        // Button actions
        loadBtn.setOnAction(e -> onLoad(primaryStage));
        prevBtn.setOnAction(e -> goTo(stepIndex - 1));
        nextBtn.setOnAction(e -> goTo(stepIndex + 1));

        playBtn.setOnAction(e -> {
            if (player == null || player.getStatus() != Timeline.Status.RUNNING) {
                startPlayer();
                playBtn.setText("Pause");
            } else {
                stopPlayer();
                playBtn.setText("Play");
            }
        });

        scrubSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (!scrubSlider.isValueChanging()) return;
            if (steps.isEmpty()) return;
            int idx = (int) Math.round(newV.doubleValue());
            goTo(idx);
        });

        root.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.RIGHT) goTo(stepIndex + 1);
            if (ev.getCode() == KeyCode.LEFT) goTo(stepIndex - 1);
            if (ev.getCode() == KeyCode.SPACE) playBtn.fire();
        });

        Scene scene = new Scene(root);
        primaryStage.setTitle("Simulation Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void onLoad(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open simulation JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        java.io.File file = fc.showOpenDialog(stage);
        if (file == null) return;

        try (Reader r = new FileReader(file)) {
            Gson gson = new Gson();
            Type t = new TypeToken<List<Step>>() {}.getType();
            steps = gson.fromJson(r, t);

            if (steps.isEmpty()) {
                stepLabel.setText("Empty steps");
                return;
            }

            buildStaticGraph();
            stepIndex = 0;
            scrubSlider.setMin(0);
            scrubSlider.setMax(steps.size() - 1);
            scrubSlider.setMajorTickUnit(1);
            scrubSlider.setBlockIncrement(1);
            goTo(0);

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Failed to load JSON", ex.getMessage());
        }
    }

    private void buildStaticGraph() {
        graphPane.getChildren().clear();
        visualNodes.clear();

        // gather unique node ids from first step
        Set<Integer> nodeIds = steps.get(0).nodeStates.stream().map(ns -> ns.id).collect(Collectors.toSet());
        int n = nodeIds.size();
        double cx = graphPane.getPrefWidth() / 2.0;
        double cy = graphPane.getPrefHeight() / 2.0;
        double radius = Math.min(cx, cy) - 120;

        List<Integer> sorted = new ArrayList<>(nodeIds);
        Collections.sort(sorted);

        // Create VisualNodes (with coordinates on a circle)
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            double angle = 2 * Math.PI * i / Math.max(1, sorted.size());
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            VisualNode vn = new VisualNode(id, x, y, graphPane);
            visualNodes.put(id, vn);
        }

        // Draw static edges from neighborIds of step 0
        Map<String, Line> drawn = new HashMap<>();
        for (NodeState ns : steps.get(0).nodeStates) {
            for (int nb : ns.neighborIds) {
                int a = ns.id;
                int b = nb;
                String key = a < b ? a + "-" + b : b + "-" + a;
                if (drawn.containsKey(key)) continue;
                VisualNode va = visualNodes.get(a);
                VisualNode vb = visualNodes.get(b);
                if (va == null || vb == null) continue;
                Line line = new Line(va.x, va.y, vb.x, vb.y);
                line.setStroke(Color.LIGHTGRAY);
                line.setStrokeWidth(2);
                drawn.put(key, line);
                graphPane.getChildren().add(line);
            }
        }

        // add nodes (edgeToParent behind circle and label)
        for (VisualNode v : visualNodes.values()) {
            graphPane.getChildren().addAll(v.edgeToParent, v.circle, v.label);
        }
    }

    private void goTo(int idx) {
        if (steps.isEmpty()) return;
        if (idx < 0) idx = 0;
        if (idx >= steps.size()) idx = steps.size() - 1;
        stepIndex = idx;
        Step step = steps.get(stepIndex);
        applyStepToVisual(step);
        stepLabel.setText("Step " + step.stepID + " (index=" + stepIndex + ")");
        
        // Update active color indicator
        activeColorIndicator.setFill(mapColorInt(step.activeColor));
        activeColorLabel.setText("Active Color: " + step.activeColor);
        
        scrubSlider.valueProperty().removeListener((obs, ov, nv) -> {});
        scrubSlider.setValue(stepIndex);
    }

    private void applyStepToVisual(Step step) {
        for (NodeState ns : step.nodeStates) {
            VisualNode vn = visualNodes.get(ns.id);
            if (vn == null) continue;
            vn.setColor(mapColorInt(ns.color));
            vn.setParent(ns.parent);
            vn.setAsCurrent(step.vCurId == ns.id);
        }
    }

    private Color mapColorInt(int colorInt) {
        // map arbitrary color integers to visible colors; expand as needed
        switch (colorInt) {
            case 0: return Color.LIGHTGRAY;
            case 1: return Color.LIGHTGREEN;
            case 2: return Color.ORANGE;
            case 3: return Color.DODGERBLUE;
            case 4: return Color.CRIMSON;
            case 5: return Color.MEDIUMPURPLE;
            case 6: return Color.GOLD;
            default:
                // generate deterministic color from integer
                int r = (colorInt * 97) % 200 + 20;
                int g = (colorInt * 61) % 200 + 20;
                int b = (colorInt * 43) % 200 + 20;
                return Color.rgb(r, g, b);
        }
    }

    private void startPlayer() {
        if (player != null) player.stop();
        double rate = 1.0 / speedSlider.getValue();
        player = new Timeline(new KeyFrame(Duration.seconds(rate), ev -> {
            if (stepIndex < steps.size() - 1) goTo(stepIndex + 1);
            else stopPlayer();
        }));
        player.setCycleCount(Timeline.INDEFINITE);
        player.play();
    }

    private void stopPlayer() {
        if (player != null) player.stop();
        player = null;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    // --- Helper classes ---
    private static class VisualNode {
        final int id;
        final Circle circle;
        final Text label;
        final Line edgeToParent; // dynamic parent indicator
        final Pane graphPane;
        double x, y;

        VisualNode(int id, double x, double y, Pane graphPane) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.graphPane = graphPane;
            circle = new Circle(x, y, 18, Color.LIGHTGRAY);
            circle.setStroke(Color.BLACK);
            label = new Text(x - 6, y + 5, Integer.toString(id));
            edgeToParent = new Line(x, y, x, y);
            edgeToParent.setStrokeWidth(3);
            edgeToParent.setStroke(Color.TRANSPARENT);
        }

        void setColor(Color c) {
            circle.setFill(c);
        }

        void setParent(int parentId) {
            //Intentionally empty
        }

        void setAsCurrent(boolean yes) {
            if (yes) {
                circle.setStrokeWidth(4);
                circle.setStroke(Color.GOLD);
            } else {
                circle.setStrokeWidth(1);
                circle.setStroke(Color.BLACK);
            }
        }
    }

    // --- JSON mapping classes ---
    private static class Step {
        int stepID;
        int vCurId;
        int activeColor;  // NEW: active color field
        List<NodeState> nodeStates;
    }

    private static class NodeState {
        int id;
        int color;
        int parent;
        List<Integer> neighborIds;
    }

    public static void main(String[] args) {
        launch(args);
    }
}