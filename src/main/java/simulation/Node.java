package simulation;

import java.util.ArrayList;
import java.util.List;

public class Node {

    public int id; //for visualization purposes - no unique ID assumed
    public int color;
    //-1 represents \bottom
    public int parent;
    public List<Node> neighbors = new ArrayList<>();

    public Node(int c, int id) {
        if (c < 2)
            throw new IllegalArgumentException("C must be greater or equal to 2!");
        this.color = simulation.Rc.randomColor(c);

        this.id = id;
    }

    public int delta() {
        return neighbors.size();
    }
    public static void createEdge(Node a, Node b) {
        a.neighbors.add(b);
        b.neighbors.add(a);

    }
}
