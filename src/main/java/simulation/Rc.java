package simulation;

import measuring.Metrics;
import persistence.Recorder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Rc {
    private final int c;
    // Recording is only necessary for visualization (but it would slow down the measuring process)
    private final Optional<Recorder> recorder;

    private Node vCur;
    private int pin;
    private int activeColor; // self.color in the paper

    // Metrics for analysis
    public Metrics metrics = new Metrics();

    private final int maxOuterIterations = 10_000;

    public Rc(int c, Recorder recorder) {
        if (c < 2)
            throw new IllegalArgumentException("c must be >= 2");
        this.c = c;
        this.recorder = Optional.ofNullable(recorder);
        this.activeColor = randomColor(c);
    }

    public static int randomColor(int c) {
        return (int) (Math.random() * c);
    }

    public static int randomColorExcluding(int c, int excluding) {
        int col;
        do {
            col = randomColor(c);
        } while (col == excluding);
        return col;
    }

    private void debug(String s) {
        if (recorder.isPresent())
            System.out.println(s);
    }

    /**
     * Implementation of Algorithm 1 (Rc) from the paper.
     * Key insight: GoForward() includes immediate Type-I backtracking if
     * destination has self.color.
     */
    public Metrics traverse(List<Node> allNodes) {
        Objects.requireNonNull(allNodes);

        metrics = new Metrics();

        // Track overall visited nodes to implement stopping condition
        Set<Integer> overallVisited = new HashSet<>();

        int outer = 0;
        while (outer < maxOuterIterations && (overallVisited.size() != allNodes.size())) {
            // Line 2: Choose self.color uniformly at random from {1,2,...,c}\{self.color}
            activeColor = randomColorExcluding(c, activeColor);
            // init - all nodes start with parent = ⊥ (represented as -1)
            for (Node n : allNodes) {
                n.parent = -2;
                if (n.color < 0)
                    n.color = 0;
            }

            vCur = allNodes.get(0);
            vCur.parent = -1;
            pin = 0;
            recorder.ifPresent(r -> r.recordColorChange(vCur, allNodes, activeColor));

            debug("\n--- outer " + outer + " activeColor=" + activeColor + " ---");

            // Track visits and color-changes in this outer pass
            Set<Integer> visitedThisOuter = new HashSet<>();
            boolean colorChangedThisOuter = false;

            // Line 3: vcur.color ← self.color (UNCONDITIONAL in the paper!)
            boolean colorChanged = (vCur.color != activeColor);
            vCur.color = activeColor;
            if (colorChanged) {
                colorChangedThisOuter = true;
                recorder.ifPresent(r -> r.recordColorChange(vCur, allNodes, activeColor));
                metrics.colorChanges++;
            }
            visitedThisOuter.add(vCur.id);
            overallVisited.add(vCur.id);

            // Line 4: GoForward(0)
            goForward(0, allNodes, visitedThisOuter, overallVisited);

            // Lines 5-15: Main loop
            while (!(vCur.parent == -1 && pin == vCur.delta() - 1)) {
                // Line 6: if vcur.color ≠ self.color then
                if (vCur.color != activeColor) {
                    // Lines 7-8: recolor and set parent
                    vCur.color = activeColor;
                    vCur.parent = pin;
                    colorChangedThisOuter = true;
                    recorder.ifPresent(r -> r.recordColorChange(vCur, allNodes, activeColor));
                    metrics.colorChanges++;

                    // Line 9: GoForward(nextR(vcur))
                    goForward(nextR(), allNodes, visitedThisOuter, overallVisited);
                } else {
                    // Line 11: if vcur.parent = nextR(vcur) then
                    if (vCur.parent == nextR()) {
                        // Line 12: vcur.parent ← ⊥
                        vCur.parent = -1;
                        // Line 13: Migrate to N(vcur, nextR(vcur)) - Type II backtracking
                        migrate(vCur, nextR());
                        visitedThisOuter.add(vCur.id);
                        overallVisited.add(vCur.id);
                        recorder.ifPresent(r -> r.recordMove(vCur, allNodes, activeColor));
                    } else {
                        // Line 15: GoForward(nextR(vcur))
                        goForward(nextR(), allNodes, visitedThisOuter, overallVisited);
                    }
                }
                metrics.iterations++;
            }

            debug("End of outer " + outer + ": visitedThisOuter=" + visitedThisOuter.size() +
                    ", overallVisited=" + overallVisited.size() + "/" + allNodes.size() +
                    ", colorChanged=" + colorChangedThisOuter);

            // Stopping criterion: visited all nodes overall AND no color changed this
            // iteration
            if (overallVisited.size() == allNodes.size() && !colorChangedThisOuter) {
                debug("Stopping: visited all nodes and no color changed in this outer iteration.");
                break;
            }

            outer++;
        }

        debug("Traverse finished after outer iterations: " + outer);

        return metrics;
    }

    /**
     * Lines 16-19: GoForward function
     * CRITICAL: This function includes Type-I backtracking (line 19)
     */
    private void goForward(int q, List<Node> allNodes,
            Set<Integer> visitedThisOuter, Set<Integer> overallVisited) {
        // Line 17: Migrate to node N(vcur, q) - forward move
        migrate(vCur, q);
        visitedThisOuter.add(vCur.id);
        overallVisited.add(vCur.id);
        recorder.ifPresent(r -> r.recordMove(vCur, allNodes, activeColor));

        // Line 18-19: if vcur.color = self.color then Type-I backtracking
        if (vCur.color == activeColor) {
            debug("  Type-I backtrack: found node already colored " + activeColor);
            // Migrate to node N(vcur, pin) - backtrack
            migrate(vCur, pin);
            visitedThisOuter.add(vCur.id);
            overallVisited.add(vCur.id);
            recorder.ifPresent(r -> r.recordMove(vCur, allNodes, activeColor));
        }
    }

    private void migrate(Node v, int i) {
        if (i < 0 || i >= v.neighbors.size()) {
            throw new IllegalArgumentException("Index out of bounds at node " + v.id +
                    " (degree=" + v.neighbors.size() + ", index=" + i + ")");
        }
        vCur = v.neighbors.get(i);
        pin = vCur.neighbors.indexOf(v);
        metrics.moves++;
        debug("migrated -> vCur=" + vCur.id + " pin=" + pin);
    }

    private int nextR() {
        return (pin + 1) % vCur.delta();
    }

    public static void main(String[] args) {
        // create 10 nodes, each with 5 available colors
        /*Node n1 = new Node(5, 1);
        Node n2 = new Node(5, 2);
        Node n3 = new Node(5, 3);
        Node n4 = new Node(5, 4);
        Node n5 = new Node(5, 5);
        Node n6 = new Node(5, 6);
        Node n7 = new Node(5, 7);
        Node n8 = new Node(5, 8);
        Node n9 = new Node(5, 9);
        Node n10 = new Node(5, 10);

        // create edges (more densely connected)
        Node.createEdge(n1, n2);
        Node.createEdge(n1, n3);
        Node.createEdge(n1, n4);
        // Node.createEdge(n2, n3);
        // Node.createEdge(n2, n4);
        Node.createEdge(n2, n5);
        Node.createEdge(n2, n6);
        // Node.createEdge(n3, n5);
        // Node.createEdge(n3, n6);
        Node.createEdge(n3, n7);
        Node.createEdge(n4, n7);
        Node.createEdge(n4, n8);
        // Node.createEdge(n5, n8);
        // Node.createEdge(n5, n9);
        Node.createEdge(n6, n9);
        Node.createEdge(n6, n10);
        // Node.createEdge(n7, n10);
        // Node.createEdge(n8, n10);
        Node.createEdge(n9, n10);
        java.util.List<Node> nodes = java.util.Arrays.asList(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10);*/

        List<Node> nodes = Node.generateRandomConnectedGraph(20, 0.1, 5);

        Recorder recorder = new Recorder();
        Rc rc = new Rc(5, recorder);
        rc.traverse(nodes);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        Path out = Paths.get("target", "traces", "trace-" + timestamp + ".json");
        recorder.saveToFile(out);
        System.out.println("Trace saved to " + out);
    }
}