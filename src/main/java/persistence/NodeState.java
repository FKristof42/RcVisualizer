package persistence;

import simulation.Node;

import java.util.List;

public class NodeState {
    public int id;
    public int color;
    public int parent; //TODO maybe not necessary
    public List<Integer> neighborIds; //TODO maybe edges instead?

    public NodeState(int id, int color, int parent, List<Integer> neighborIds) {
        this.id = id;
        this.color = color;
        this.parent = parent;
        this.neighborIds = neighborIds;
    }

    public NodeState(Node v) {
        this(v.id, v.color, v.parent, v.neighbors.stream().map(n -> n.id).toList());
    }
}
